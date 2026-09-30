# Collider throughput benchmark protocol

Frozen before the first measurement, as required by the handover roadmap section 5.3. The six
absolute targets are per-item gates: no average, total score, best item or multi-thread aggregate may
compensate for a slow item, and the reference error column is never subtracted from a target.

## Targets

| ID | Benchmark | Minimum ops/s | Reference error |
| --- | --- | ---: | ---: |
| P01 | `aabbPair` | 3,561,837,865.930 | 157,982,649.213 |
| P02 | `capsulePair` | 22,350,681.198 | 238,993.658 |
| P03 | `rotatedCapsulePair` | 16,842,309.523 | 168,423.918 |
| P04 | `obbPair` | 5,528,493.224 | 64,548.996 |
| P05 | `rotatedObbPair` | 4,648,963.750 | 111,369.535 |
| P06 | `spherePair` | 119,556,589.705 | 171,114.656 |

The values come from the reference README snapshot recorded in the roadmap. The original harness is
not publicly available, so this project measures its **own** production entry point with the protocol
below; that is what the roadmap asks for, and it is not a claim of reproducing the unpublished
harness.

## Measured call path

Every item calls the public production entry `ColliderQueries.intersects(ColliderSnapshot,
ColliderSnapshot)` over pre-built immutable snapshots. No private kernel, no test-only shortcut, and
no reflection is used to obtain a score. The snapshot entry performs its own disabled/empty handling,
conservative bounds pruning, and typed narrow phase, so the measurement includes the real production
dispatch.

The two rotated items rebuild a collider through the public setter and take a fresh snapshot **inside
the measured operation**, using a pre-generated non-equal orientation sequence. Their allocation is
part of the measurement and is never moved into the setup.

## Protocol

| Property | Value |
| --- | --- |
| Mode | Throughput (`ops/s`) |
| Threads | 1 |
| Forks | 3 independent JVM forks |
| Warm-up | 5 iterations × 1 s |
| Measurement | 5 iterations × 1 s |
| JVM | `-Xms256m -Xmx2g` (set by the `runJmh` task) |
| Input set | 64 pre-built pairs per shape, alternating clearly intersecting and clearly separated, built once per trial outside the measurement |
| Per-operation work | exactly one collision query; no batching, no `OperationsPerInvocation` multiplication |
| Result handling | the boolean result is returned to JMH, so dead-code elimination cannot remove the call |

The input mix deliberately includes separated pairs so an item cannot score by always taking an early
exit, and intersecting pairs so it cannot score by only pruning. Inputs are read by index, so the
measured operation allocates nothing of its own beyond what the production code allocates.

## Running

```powershell
$env:JAVA_HOME = 'D:\Program Files\Zulu\zulu-25'
$env:GRADLE_USER_HOME = 'D:\gradle'
.\gradlew.bat runJmh --console=plain "-PjmhArgs=-rf json -rff build/jmh-results.json .*ColliderBenchmark.*"
```

The raw JMH JSON is the primary evidence and is kept unchanged. The benchmark workflow extracts one
`benchmark=score` line per item (asserting the unit is `ops/s`) and runs:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp build/classes/java/main `
    dev.crlhitbox.internal.bench.BenchmarkThresholds build/jmh-measured.txt
```

Exit code 0 means all six targets were met; 1 means at least one item failed or was not executed. The
checker reports a missing item as `NOT EXECUTED` and never as zero.

## Validity checks

- The measured method returns the query result, so the JIT cannot fold the call away.
- No argument is a compile-time constant: centers, separations and orientations are generated into
  arrays once per trial.
- Rotated items really rotate: the setter runs inside the measured region and changes the stored
  orientation, so a no-op setter cannot produce a score.
- The benchmark source set is isolated (`src/jmh/java`, `jmh` configuration) and no JMH type is
  reachable from the published mod.

## Measured results — first run

**Environment warning.** This run was executed on a **substitute host**, not on the machine recorded
in roadmap section 5.2 (Ryzen 5 5950X / Windows Server 2025 / 63.9 GiB). The substitute is an
AMD Ryzen 9 8945HX (16 cores / 32 threads), Windows 11 Pro 10.0.26200, 31.2 GiB RAM, Zulu
25.0.3+9-LTS, Gradle wrapper 9.6.1, JVM `-Xms256m -Xmx2g`. The roadmap requires confirming a
replacement machine before it can serve as the acceptance environment, so these numbers are an
observation on a substitute host and **are not a formal performance acceptance**, and they are not
comparable to the reference table's original hardware either.

| ID | Benchmark | Target ops/s | Measured ops/s | Error | Verdict |
| --- | --- | ---: | ---: | ---: | --- |
| P01 | `aabbPair` | 3,561,837,865.930 | 108,958,806.146 | ±6,477,414 | below (96.94 % short) |
| P02 | `capsulePair` | 22,350,681.198 | 85,090,892.564 | ±607,387 | **met** (3.81×) |
| P03 | `rotatedCapsulePair` | 16,842,309.523 | 7,744,341.090 | ±393,241 | below (54.02 % short) |
| P04 | `obbPair` | 5,528,493.224 | 88,093,805.358 | ±2,180,825 | **met** (15.94×) |
| P05 | `rotatedObbPair` | 4,648,963.750 | 1,008,450.230 | ±345,454 | below (78.31 % short) |
| P06 | `spherePair` | 119,556,589.705 | 87,683,202.917 | ±12,466,824 | below (26.66 % short) |

Counts are 15 per item (3 forks × 5 measurement iterations). Raw evidence is kept unchanged in
[2026-09-30-jmh-results.json](../benchmark/results/2026-09-30-jmh-results.json) with the extracted
[2026-09-30-jmh-measured.txt](../benchmark/results/2026-09-30-jmh-measured.txt).

Verdict from the checker: **4 of 6 targets not met**; exit code 1. No target was lowered, no error
column was subtracted, no average was substituted, and the failing items were not dropped.

Observed shape of the gap (diagnosis, not an accepted optimisation plan):

- `rotatedObbPair` and `rotatedCapsulePair` are the two items that rebuild a collider inside the
  measured region; their scores show that per-operation snapshot construction dominates them.
- `aabbPair` is 32.7× below target, so the box-box path itself needs profiling before any tuning.
- `spherePair` is 1.36× below target, the smallest gap of the four.

The roadmap's R5 stage owns closing these gaps item by item; the work has not started.

## Environment of record

Record the exact machine, JDK, GC and background load with every reported run. The values in this
document are targets, not measurements; actual results are recorded separately with their raw JSON.

## Known limits

- The reference harness is unavailable, so cross-project comparison is by absolute value only, on the
  machine recorded with each run.
- Absolute throughput is hardware- and JIT-sensitive; a passing or failing result does not by itself
  prove or disprove algorithmic superiority over any other implementation.
- These items measure static collider pairs. They do not measure game load, network traffic, entity
  pose extraction or rendering cost.
