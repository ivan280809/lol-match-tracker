package com.loltracker.app.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.management.OperatingSystemMXBean;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Reproducible local benchmark; it never calls Riot or Telegram. */
class HttpClientBenchmarkTest {
  private static final int REQUESTS_PER_TRANSPORT = 1_000;
  private HttpServer server;
  private ExecutorService serverExecutor;
  private int port;
  private final AtomicInteger requestCount = new AtomicInteger();
  private final Set<Integer> remotePorts = ConcurrentHashMap.newKeySet();
  private final Set<String> tokens = ConcurrentHashMap.newKeySet();
  private final Set<String> regions = ConcurrentHashMap.newKeySet();
  // Counter for the /slow endpoint to verify that a read timeout actually
  // caused the request to be sent and the server received it.
  private final AtomicInteger slowRequestCount = new AtomicInteger();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    port = server.getAddress().getPort();
    server.createContext("/benchmark", this::handleBenchmark);
    server.createContext("/rate-limit", exchange -> respond(exchange, 429, "rate limited"));
    server.createContext("/error", exchange -> respond(exchange, 503, "unavailable"));
    server.createContext("/slow", exchange -> {
      // Increment counter so that tests can verify that the request actually
      // reached the server before timing out.
      slowRequestCount.incrementAndGet();
      try {
        Thread.sleep(250);
        respond(exchange, 200, "slow response");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        exchange.close();
      }
    });
    serverExecutor = Executors.newFixedThreadPool(8);
    server.setExecutor(serverExecutor);
    server.start();
  }

  @AfterEach
  void stopServer() throws InterruptedException {
    if (server != null) {
      server.stop(0);
    }
    if (serverExecutor != null) {
      serverExecutor.shutdownNow();
      assertTrue(serverExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
  }

  @Test
  @DisplayName("Compares exactly 1,000 local requests per HTTP transport")
  void benchmarkDefaultAgainstSharedJdkTransport() {
    Measurement simple = measure(simpleFactory(Duration.ofSeconds(2)), "simple");
    Measurement jdk = measure(jdkFactory(Duration.ofSeconds(2)), "jdk-shared");

    assertEquals(REQUESTS_PER_TRANSPORT, simple.serverRequests());
    assertEquals(REQUESTS_PER_TRANSPORT, jdk.serverRequests());
    assertEquals(REQUESTS_PER_TRANSPORT, simple.statusCodes().size());
    assertEquals(REQUESTS_PER_TRANSPORT, jdk.statusCodes().size());
    assertTrue(simple.statusCodes().stream().allMatch(status -> status == 200));
    assertTrue(jdk.statusCodes().stream().allMatch(status -> status == 200));
    assertTrue(simple.uniqueRemotePorts() > 0);
    assertTrue(jdk.uniqueRemotePorts() > 0);
    assertEquals(REQUESTS_PER_TRANSPORT, simple.uniqueTokens());
    assertEquals(REQUESTS_PER_TRANSPORT, jdk.uniqueTokens());
    assertEquals(3, simple.uniqueRegions());
    assertEquals(3, jdk.uniqueRegions());

    printMeasurement(simple);
    printMeasurement(jdk);
    System.out.println("HTTP_BENCHMARK handshake_note=distinct_remote_ports_are_a_connection_proxy_not_a_packet_level_handshake_count; jdk17_httpclient_has_no_close_method");
  }

  @Test
  void recordsRemoteErrorsAnd429UsingFictitiousConfigurationForBothTransports() {
    for (ClientHttpRequestFactory factory : List.of(
        simpleFactory(Duration.ofSeconds(2)), jdkFactory(Duration.ofSeconds(2)))) {
      RestClient client = RestClient.builder().requestFactory(factory).build();
      assertEquals(429, statusFor(client, "/rate-limit", "fake-token-1", "region-a"));
      assertEquals(503, statusFor(client, "/error", "fake-token-2", "region-b"));
    }
  }

  @Test
  void reportsReadTimeoutAgainstSlowLocalServerForBothTransports() {
    for (ClientHttpRequestFactory factory : List.of(
        simpleFactory(Duration.ofMillis(50)), jdkFactory(Duration.ofMillis(50)))) {
      slowRequestCount.set(0); // reset counter for each transport
      RestClient client = RestClient.builder().requestFactory(factory).build();
      ResourceAccessException exception = assertThrows(ResourceAccessException.class, () -> client.get()
          .uri(url("/slow"))
          .header("Authorization", "Bearer fake-timeout-token")
          .header("X-Region", "region-test")
          .retrieve()
          .body(String.class));
      // Verify that the server received the request.
      assertEquals(1, slowRequestCount.get(), "Server should have received one /slow request");
      // Inspect cause chain for a timeout type exception.
      Throwable cause = exception.getCause();
      boolean timeoutFound = false;
      while (cause != null) {
        if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.http.HttpTimeoutException) {
          timeoutFound = true;
          break;
        }
        cause = cause.getCause();
      }
      assertTrue(timeoutFound, "Exception chain should contain a timeout exception");
    }
  }

  private Measurement measure(ClientHttpRequestFactory factory, String name) {
    resetCounters();
    RestClient client = RestClient.builder().requestFactory(factory).build();
    List<Integer> statuses = new ArrayList<>(REQUESTS_PER_TRANSPORT);
    List<Long> latenciesNanos = new ArrayList<>(REQUESTS_PER_TRANSPORT);
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    long memoryBefore = heapUsed(memory);
    long cpuBefore = processCpuNanos();

    long loopStart = System.nanoTime();
    for (int i = 0; i < REQUESTS_PER_TRANSPORT; i++) {
      String fixtureAuthorization = "Bearer fake-" + name + "-credential-" + i;
      String region = "region-" + (i % 3);
      long started = System.nanoTime();
      try {
        client.get()
            .uri(url("/benchmark"))
            .header("Authorization", fixtureAuthorization)
            .header("X-Region", region)
            .retrieve()
            .body(String.class);
        statuses.add(200);
      } catch (RestClientResponseException e) {
        statuses.add(e.getStatusCode().value());
      }
      latenciesNanos.add(System.nanoTime() - started);
    }
    long loopEnd = System.nanoTime();
    long elapsed = loopEnd - loopStart;

    long cpuAfter = processCpuNanos();
    long memoryAfter = heapUsed(memory);
    return new Measurement(name, List.copyOf(statuses), List.copyOf(latenciesNanos),
        requestCount.get(), remotePorts.size(), tokens.size(), regions.size(),
        cpuBefore < 0 || cpuAfter < 0 ? -1 : cpuAfter - cpuBefore,
        memoryBefore < 0 || memoryAfter < 0 ? -1 : memoryAfter - memoryBefore, elapsed);
  }

  private void handleBenchmark(HttpExchange exchange) throws IOException {
    requestCount.incrementAndGet();
    remotePorts.add(exchange.getRemoteAddress().getPort());
    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
    String region = exchange.getRequestHeaders().getFirst("X-Region");
    if (authorization == null || !authorization.startsWith("Bearer fake-") || region == null) {
      respond(exchange, 400, "invalid fixture headers");
      return;
    }
    tokens.add(authorization.substring("Bearer ".length()));
    regions.add(region);
    respond(exchange, 200, "ok");
  }

  private int statusFor(RestClient client, String path, String token, String region) {
    try {
      client.get().uri(url(path))
          .header("Authorization", "Bearer " + token)
          .header("X-Region", region)
          .retrieve().body(String.class);
      return 200;
    } catch (RestClientResponseException e) {
      return e.getStatusCode().value();
    }
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    try (var output = exchange.getResponseBody()) {
      output.write(bytes);
    } finally {
      exchange.close();
    }
  }

  private void resetCounters() {
    requestCount.set(0);
    remotePorts.clear();
    tokens.clear();
    regions.clear();
    slowRequestCount.set(0);
  }

  private String url(String path) {
    return "http://127.0.0.1:" + port + path;
  }

  private static ClientHttpRequestFactory simpleFactory(Duration timeout) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Math.toIntExact(timeout.toMillis()));
    factory.setReadTimeout(Math.toIntExact(timeout.toMillis()));
    return factory;
  }

  private static ClientHttpRequestFactory jdkFactory(Duration connectTimeout) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout)
        .version(HttpClient.Version.HTTP_1_1).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(connectTimeout);
    return factory;
  }

  private static double percentile95Millis(List<Long> nanos) {
    List<Long> sorted = new ArrayList<>(nanos);
    Collections.sort(sorted);
    int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
    return sorted.get(index) / 1_000_000.0;
  }

  private static double meanMillis(List<Long> nanos) {
    return nanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
  }

  private static long heapUsed(MemoryMXBean memory) {
    MemoryUsage usage = memory.getHeapMemoryUsage();
    return usage == null ? -1 : usage.getUsed();
  }

  private static long processCpuNanos() {
    java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
    return bean instanceof OperatingSystemMXBean os ? os.getProcessCpuTime() : -1;
  }

  private static void printMeasurement(Measurement result) {
    double p95 = percentile95Millis(result.latenciesNanos());
    double mean = meanMillis(result.latenciesNanos());
    double elapsedMs = result.elapsedNanos() < 0 ? -1 : result.elapsedNanos() / 1_000_000.0;
    System.out.printf(Locale.ROOT,
        "HTTP_BENCHMARK name=%s requests=%d server_requests=%d p95_ms=%.3f mean_ms=%.3f elapsed_ms=%.3f unique_remote_ports=%d process_cpu_ms=%d heap_delta_bytes=%d%n",
        result.name(), result.statusCodes().size(), result.serverRequests(), p95, mean, elapsedMs,
        result.uniqueRemotePorts(), result.cpuNanos() < 0 ? -1 : result.cpuNanos() / 1_000_000,
        result.heapDeltaBytes());
  }

  private record Measurement(String name, List<Integer> statusCodes, List<Long> latenciesNanos,
      int serverRequests, int uniqueRemotePorts, int uniqueTokens, int uniqueRegions,
      long cpuNanos, long heapDeltaBytes, long elapsedNanos) {}
}
