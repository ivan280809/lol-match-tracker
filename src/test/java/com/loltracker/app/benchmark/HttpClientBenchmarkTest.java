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
    // For rate limiting and error scenarios we need to increment the
    // request counter and capture the remote port as the benchmark
    // expects these requests to be recorded for both transport types.
    // Handlers for error scenarios record headers similar to the benchmark
    // endpoint so that the test can verify that tokens and regions are
    // captured for every request, regardless of status code.
    server.createContext("/rate-limit", exchange -> {
      requestCount.incrementAndGet();
      remotePorts.add(exchange.getRemoteAddress().getPort());
      recordHeaders(exchange);
      respond(exchange, 429, "rate limited");
    });
    server.createContext("/error", exchange -> {
      requestCount.incrementAndGet();
      remotePorts.add(exchange.getRemoteAddress().getPort());
      recordHeaders(exchange);
      respond(exchange, 503, "unavailable");
    });
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
    assertTrue(simple.statusCodes().stream().allMatch(status -> status == 200),
            "All simple transport responses should be 200");
    assertTrue(jdk.statusCodes().stream().allMatch(status -> status == 200),
            "All JDK shared transport responses should be 200");
    assertTrue(simple.uniqueRemotePorts() > 0);
    assertTrue(jdk.uniqueRemotePorts() > 0);
    assertEquals(REQUESTS_PER_TRANSPORT, simple.uniqueTokens());
    assertEquals(REQUESTS_PER_TRANSPORT, jdk.uniqueTokens());
    assertEquals(3, simple.uniqueRegions());
    assertEquals(3, jdk.uniqueRegions());

    // Verify that the latency data has the expected length and contains
    // meaningful values. The benchmark sends 1,000 requests per transport
    // so the latency list should match that count.
    assertEquals(REQUESTS_PER_TRANSPORT, simple.latenciesNanos().size(),
            "Simple transport should record 1,000 latency samples");
    assertEquals(REQUESTS_PER_TRANSPORT, jdk.latenciesNanos().size(),
            "JDK shared transport should record 1,000 latency samples");

    // Percentile and mean latency should be > 0, otherwise the benchmark
    // measurement logic failed.
    assertTrue(percentile95Millis(simple.latenciesNanos()) > 0,
            "Simple transport p95 latency should be positive");
    assertTrue(percentile95Millis(jdk.latenciesNanos()) > 0,
            "JDK shared transport p95 latency should be positive");

    // Process CPU time should increase for each transport unless the OS
    // does not support the API.  In that case the value is reported as -1.
    assertTrue(simple.cpuNanos() >= 0 || simple.cpuNanos() == -1,
            "Simple transport CPU measurement should be >= 0 or unsupported");
    assertTrue(jdk.cpuNanos() >= 0 || jdk.cpuNanos() == -1,
            "JDK shared transport CPU measurement should be >= 0 or unsupported");

    // Heap delta should be non‑negative or unsupported.
    assertTrue(simple.heapDeltaBytes() >= 0 || simple.heapDeltaBytes() == -1,
            "Simple transport heap delta should be >= 0 or unsupported");
    assertTrue(jdk.heapDeltaBytes() >= 0 || jdk.heapDeltaBytes() == -1,
            "JDK shared transport heap delta should be >= 0 or unsupported");

    // Elapsed time for the entire loop should be > 0.
    assertTrue(simple.elapsedNanos() > 0,
            "Simple transport elapsed time should be positive");
    assertTrue(jdk.elapsedNanos() > 0,
            "JDK shared transport elapsed time should be positive");

    printMeasurement(simple);
    printMeasurement(jdk);
    System.out.println("HTTP_BENCHMARK handshake_note=distinct_remote_ports_are_a_connection_proxy_not_a_packet_level_handshake_count; jdk17_httpclient_has_no_close_method");
  }

  @Test
  void recordsRemoteErrorsAnd429UsingFictitiousConfigurationForBothTransports() {
    for (ClientHttpRequestFactory factory : List.of(
        simpleFactory(Duration.ofSeconds(2)), jdkFactory(Duration.ofSeconds(2)))) {
      resetCounters();
      RestClient client = RestClient.builder().requestFactory(factory).build();
      int rl = statusFor(client, "/rate-limit", "fake-token-1", "region-a");
      assertEquals(429, rl, "Rate limited status should be 429");
      int err = statusFor(client, "/error", "fake-token-2", "region-b");
      assertEquals(503, err, "Error status should be 503");
      // Verify that the server received each request and recorded ports.
      assertEquals(2, requestCount.get(), "Server should have received two error requests");
      assertTrue(remotePorts.size() > 0, "Remote ports should be captured for error requests");
      assertEquals(2, tokens.size(), "Tokens should be recorded for each error request");
      assertEquals(2, regions.size(), "Regions should be recorded for each error request");
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

  /**
   * Extracts the {@code Authorization} and {@code X-Region} headers from the
   * request and records them in the shared token/region sets.  The benchmark
   * test asserts that these headers are captured for error responses as well.
   */
  private void recordHeaders(HttpExchange exchange) {
    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
    String region = exchange.getRequestHeaders().getFirst("X-Region");
    if (authorization != null) {
      tokens.add(authorization);
    }
    if (region != null) {
      regions.add(region);
    }
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

  /**
   * Builds a {@link JdkClientHttpRequestFactory} backed by a {@link HttpClient}
   * with the specified connection timeout. The read timeout is derived from the
   * same {@link Duration} value.
   *
   * <p>The original implementation mistakenly passed a {@link Duration}
   * directly to {@link JdkClientHttpRequestFactory#setReadTimeout(int)}, which
   * expects milliseconds as an {@code int}. The compiler error caused the test
   * suite to fail before the benchmark could run. This method now converts the
   * duration to milliseconds explicitly.
   */
  private static ClientHttpRequestFactory jdkFactory(Duration timeout) {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout((int) timeout.toMillis());
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
