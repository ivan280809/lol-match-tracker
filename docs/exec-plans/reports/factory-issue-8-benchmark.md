# Issue #8 benchmark evidence

Measured run: `2026-10-07-queue-recovery-benchmark`, Java Temurin 17.0.20.1.
Command: `./mvnw.cmd -B -ntp -Dtest=HttpClientBenchmarkTest,RiotClientTest test`.
Result: **13 tests, 0 failures, 0 errors, 0 skipped**.

The companion [measurement manifest](factory-issue-8-benchmark.json) records the observed output, command, timestamp, source hashes and raw-log hash. These are measurements of that source snapshot, not claims about a future commit SHA. The supervisor records full-suite verification separately on the delivered commit.

| Transport | Requests | p95 ms | Mean ms | Elapsed ms | Remote ports | CPU ms | Heap delta bytes |
|---|---:|---:|---:|---:|---:|---:|---:|
| simple | 1000 | 0.462 | 0.222 | 226.115 | 1 | 703 | 17379144 |
| jdk-shared | 1000 | 0.679 | 0.396 | 399.123 | 1 | 1875 | -1 |

Each transport sent 1,000 serial requests to an in-process localhost server with fictitious rotating authorization and region values. Both reused one observed remote port. Port counts are a connection-reuse proxy, not packet-level handshake counts. Tests also exercise HTTP 429/503 and delayed responses without calling Riot or Telegram.

This single run has no warmup and is not a production load measurement. The JDK transport had higher p95, mean and process CPU in this run, so production retains the existing transport. Heap measurements are sensitive to garbage collection; `-1` denotes an unavailable delta, not a one-byte reduction. The server and its executor are stopped after each test; Java 17 HttpClient has no explicit close method.
