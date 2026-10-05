# Issue #8 benchmark evidence

Command: `./mvnw.cmd -B -ntp -Dtest=HttpClientBenchmarkTest test` under Java 17. The localhost fixture verified exactly 1,000 requests for each transport, 1,000 distinct fictitious authorization values, and three rotating fictitious regions.

| Transport | Requests | p95 | Mean | Total elapsed | Unique remote ports | Process CPU | Heap delta |
|---|---:|---:|---:|---:|---:|---:|---:|
| Existing `SimpleClientHttpRequestFactory` | 1,000 | 0.425 ms | 0.216 ms | 219.905 ms | 1 | 781 ms | +17,436,608 bytes |
| JDK `HttpClient` via `JdkClientHttpRequestFactory` | 1,000 | 0.654 ms | 0.383 ms | 386.273 ms | 1 | 1,578 ms | +24,851,040 bytes |

This is one serial run against an in-process localhost server without warmup, so it is directional evidence rather than a production load result. Both transports reused one observed client port. The shared JDK transport did not improve p95, mean latency, or process CPU, so production remains on the existing transport. Port counts are a connection-reuse proxy, not packet-level handshake measurements. Java 17 `HttpClient` has no explicit close method; the fixture server and executor are stopped after each test. The timeout test uses a delayed local endpoint. Heap delta is a before/after process snapshot and is sensitive to garbage collection.

The targeted benchmark plus Riot configuration test report 13 tests, 0 failures, 0 errors, 0 skipped (`-Dtest=HttpClientBenchmarkTest,RiotClientTest`) under Java 17. The cases apply fictitious token and region rotations, test 429 and 503 over both transports, and verify each read-timeout exception cause chain and that the slow fixture received the request. The latest run records 1,000 requests per transport and total loop runtime: Simple p95 0.425 ms, mean 0.216 ms, total 219.905 ms, CPU 781 ms, heap delta +17,436,608 bytes; JDK shared p95 0.654 ms, mean 0.383 ms, total 386.273 ms, CPU 1,578 ms, heap delta +24,851,040 bytes. Both saw one unique remote port. This is one serial localhost run without warmup; process heap deltas are noisy and not evidence of a production memory gain.

The latest full `./mvnw.cmd -B -ntp verify` with Temurin 17.0.20.1 reports 209 tests, 0 failures, 0 errors, 2 PostgreSQL Testcontainers tests skipped because Docker was unavailable. The current transport remains unchanged because the measurements do not justify reuse. Human review of the production transport decision is pending. The JDK HttpClient has no explicit close method in Java 17; fixture server and executor shutdown are tested, while client-side resource reclamation cannot be observed deterministically by this test.
