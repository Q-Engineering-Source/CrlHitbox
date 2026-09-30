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

### Second run — allocation-free leaf dispatch (substitute host, same protocol)

The first optimisation removed the per-query stack, frame record and placement composition for plain
leaf pairs, and stopped composing an identity transform:

| ID | Benchmark | Before | After | Speed-up | Verdict |
| --- | --- | ---: | ---: | ---: | --- |
| P01 | `aabbPair` | 108,958,806 | 227,772,940 | 2.09× | below (93.61 % short) |
| P02 | `capsulePair` | 85,090,893 | 182,575,343 | 2.15× | **met** (8.17×) |
| P03 | `rotatedCapsulePair` | 7,744,341 | 14,419,711 | 1.86× | below (14.38 % short) |
| P04 | `obbPair` | 88,093,805 | 197,247,854 | 2.24× | **met** (35.68×) |
| P05 | `rotatedObbPair` | 1,008,450 | 1,310,934 | 1.30× | below (71.80 % short) |
| P06 | `spherePair` | 87,683,203 | 236,843,703 | 2.70× | **met** (1.98×) |

Verdict: **3 of 6 targets not met** (was 4). Raw evidence:
[optimized JSON](../benchmark/results/2026-09-30-jmh-results-optimized.json) and
[optimized measured](../benchmark/results/2026-09-30-jmh-measured-optimized.txt).

Still open: `aabbPair` (15.6× short), `rotatedObbPair` (3.55× short), `rotatedCapsulePair`
(1.17× short). The rotated items rebuild a collider inside the measured region, and every snapshot
construction still precomputes parent-frame bounds through a placed-solid value even when the
placement is the identity.

A note on `P01`: its target of 3.56×10⁹ ops/s corresponds to roughly 0.28 ns per operation on a
multi-gigahertz core, which is about one cycle. That is worth an explicit comparability check with
the requirement owner (roadmap section 5.3 asks for exactly that for this item) before treating the
gap as an implementation defect; the target itself has not been adjusted.

### Third attempt — rejected (identity-placement flag caching)

A candidate cached an "identity placement" flag on each snapshot and short-circuited bounds
precomputation for identity placements, aiming at the two rotated items. It was **rejected**: every
item regressed against the accepted candidate, well outside the reported error.

| ID | Benchmark | Accepted candidate | Rejected candidate | Change |
| --- | --- | ---: | ---: | ---: |
| P01 | `aabbPair` | 227,772,940 | 199,314,382 | −12.5 % |
| P02 | `capsulePair` | 182,575,343 | 164,248,424 | −10.0 % |
| P03 | `rotatedCapsulePair` | 14,419,711 | 14,145,586 | −1.9 % |
| P04 | `obbPair` | 197,247,854 | 183,564,364 | −6.9 % |
| P05 | `rotatedObbPair` | 1,310,934 | 1,292,619 | −1.4 % |
| P06 | `spherePair` | 236,843,703 | 199,768,194 | −15.6 % |

The likely cause is that the extra field and the wider internal call signature pushed the hot methods
past the JIT's inlining budget. The change was rolled back; the accepted candidate remains the
previous commit, and the rejected raw results are retained as evidence
([rejected JSON](../benchmark/results/2026-09-30-jmh-results-rejected-identity-flag.json),
[rejected measured](../benchmark/results/2026-09-30-jmh-measured-rejected-identity-flag.txt)). No
target was adjusted, and the rollback was verified by re-running the full test suite.

This is a negative result worth keeping: it shows that the remaining `aabbPair` gap is not caused by
redundant placement work on snapshots, and that further gains must come from somewhere else
(profiling the fused path, or reconsidering what the reference `P01` operation actually measures).

### Diagnostic decomposition

Run with the diagnostic benchmark set (`ColliderDiagnostics`, 1 fork, 3+3 iterations of 1 s). These
numbers are **indicative only** — the short run is noisy — and are not part of the six targets.

| Diagnostic | ops/s | Meaning |
| --- | ---: | --- |
| `constantAabbPair` | 634,942,040 ±286M | validity control with constant inputs; no implausible folding appeared, but the error is enormous |
| `rawAabbPair` | 348,277,253 | the frozen typed kernel alone: six `double` comparisons |
| `snapshotAabbPair` | 225,918,456 | the public production entry |
| `rawSpherePair` | 119,652,681 | the noise level is visible here (it must not be slower than the snapshot path) |
| `snapshotSpherePair` | 234,383,648 | the public production entry |

Order-of-magnitude conclusions:

1. **P01 is not comparable with this entry point.** The frozen AABB kernel itself — six comparisons,
   no dispatch, no allocation — measures ≈3.5×10⁸ ops/s. The P01 target is 3.56×10⁹, i.e. **10.2×
   above the cost of the kernel alone**, and its implied 0.28 ns per operation is below the cost of a
   single method call plus JMH's blackhole consumption. Removing the entire snapshot layer could not
   close that gap, so the item's measurement definition needs confirmation with the requirement owner
   (roadmap section 5.3 asks exactly for this check for P01). The target has not been changed and the
   item stays open.
2. **The snapshot layer costs about 1.5× over the raw kernel** for AABB, which bounds what further
   dispatch work can win on this path.
3. The short diagnostic run is too noisy for per-item conclusions (see `rawSpherePair`); only the
   order-of-magnitude comparison above is used.

Raw diagnostics:
[2026-09-30-jmh-diagnostics.json](../benchmark/results/2026-09-30-jmh-diagnostics.json).

### Fourth attempt — rejected (identity bounds short-circuit)

A second, strictly single-variable candidate made the shared bounds placement return the local bounds
directly when the placement is the exact identity, removing one `PlacedSolid3d` construction and one
bounds recomputation per snapshot. It was **rejected**, because the two items it was meant to help
regressed:

| ID | Benchmark | Accepted candidate | Rejected candidate | Change |
| --- | --- | ---: | ---: | ---: |
| P01 | `aabbPair` | 227,772,940 | 234,472,159 | +2.9 % |
| P02 | `capsulePair` | 182,575,343 | 192,035,405 | +5.2 % |
| P03 | `rotatedCapsulePair` | 14,419,711 | 13,895,991 | **−3.6 %** |
| P04 | `obbPair` | 197,247,854 | 215,724,284 | +9.4 % |
| P05 | `rotatedObbPair` | 1,310,934 | 1,299,358 | **−0.9 %** |
| P06 | `spherePair` | 236,843,703 | 239,163,702 | +1.0 % |

The non-rotated items improved slightly, but the acceptance rule is per item and the two open items
got worse, so the change was rolled back. Evidence:
[rejected JSON](../benchmark/results/2026-09-30-jmh-results-rejected-bounds-shortcircuit.json).

> **Later correction (see "Run-to-run spread and the acceptance rule" below).** A repeated run of the
> accepted candidate moved by −3.1 % to −14.1 % with no code change, while this candidate's two open
> items differed by only −3.6 % and −0.9 %. That is inside the spread, so the rejection is now
> recorded as **inconclusive** rather than as a proven regression. The candidate was still not
> adopted, because an inconclusive result cannot demonstrate an improvement on the open items.

**Diagnosis this buys us:** the rotated items are *not* limited by bounds placement. Removing an
entire placed-solid construction per snapshot did not help them, so their cost lies inside the frozen
geometry constructors themselves — `Segment3d`, `Capsule`, `Obb` and the bounds computations they
run at construction. Those files are byte-frozen, so closing P03/P05 now requires one of:

1. a scoped freeze exception naming the exact constructors and the semantics that must not change
   (returned bounds value, immutability, equality, and the closed-set rule), or
2. recording P03/P05 as NG candidates with the evidence above and leaving them open.

Neither option is taken here; the accepted candidate is unchanged and both items remain open.

### Rotated-item decomposition (diagnostic, 1 fork, 3+3 iterations)

Each step the rotated items perform inside their measured region, measured separately:

| Diagnostic step | ops/s | ≈ ns/op | Meaning |
| --- | ---: | ---: | --- |
| `segmentConstruct` | 40,043,751 | 25 | building one `Segment3d` from two points |
| `capsuleConstruct` | 28,565,214 | 35 | rotating the axis and building the capsule |
| `capsuleBounds` | 27,505,655 | 36 | the above plus `Capsule.bounds()` |
| `capsuleSnapshotBuild` | 26,798,326 | 37 | the full snapshot the setter publishes |
| `capsuleSetOrientation` | 26,390,756 | 38 | the setter exactly as the benchmark uses it |
| `capsuleQueryOnly` | 302,122,915 | 3.3 | the collision query alone |
| `obbBounds` | 8,767,945 | 114 | `Obb.bounds()` after a fresh construction |

Findings:

1. **The rotated items are dominated by construction, not by querying.** The capsule setter runs at
   ≈26.4×10⁶ ops/s while the query alone runs at ≈302×10⁶ ops/s: more than 90 % of the measured time
   is building the new value.
2. **`Segment3d` construction is the largest single step** in that chain (~25 ns of the ~38 ns setter),
   followed by the shape's own bounds computation (~11 ns on top).
3. **`Obb` bounds computation is the most expensive step measured overall** (~114 ns/op), which is why
   `rotatedObbPair` sits far below its target.
4. Consequently, the only remaining levers for P03 and P05 are inside frozen geometry constructors:
   `Segment3d` construction for P03 and `Obb` bounds for P05. **No frozen file has been modified**;
   this measurement exists to size a scoped freeze-exception request precisely, or to justify an NG
   record.

Raw diagnostics:
[2026-09-30-jmh-rotation-diagnostics.json](../benchmark/results/2026-09-30-jmh-rotation-diagnostics.json).

## NG records (roadmap section 5.4)

Each record states what was measured, why it is short, what was already tried and rejected, and what
would have to change. A record is not an acceptance and never lowers a target.

### NG-1 — P03 `rotatedCapsulePair` (open)

- **Status:** NOT MET. Accepted candidate 14,419,711 ops/s against 16,842,309 (14.4 % short).
- **Evidence:** accepted run
  [JSON](../benchmark/results/2026-09-30-jmh-results-optimized.json); decomposition above.
- **Cause:** more than 90 % of the measured region is the construction chain. `Segment3d`
  construction alone is ≈25 ns of the ≈38 ns setter, and it lives in a byte-frozen geometry file.
- **Already tried:** identity-flag caching (1.4–15.6 % regressions, rejected) and identity bounds
  short-circuit (this item moved −3.6 %, inside the run-to-run spread, so it is recorded as
  inconclusive rather than as a regression). Neither was adopted; both raw results are kept.
- **What would unblock it:** a scoped freeze exception for `Segment3d` construction that preserves
  endpoint canonicalisation, finiteness checks, immutability and equality; otherwise the gap stands.

### NG-2 — P05 `rotatedObbPair` (open)

- **Status:** NOT MET. Accepted candidate 1,310,934 ops/s against 4,648,964 (71.8 % short).
- **Evidence:** same accepted run; `obbBounds` diagnostic measures ≈8.77×10⁶ ops/s (≈114 ns/op).
- **Cause:** the measured region must rebuild the oriented box each operation, and `Obb.bounds()`
  is the most expensive single step measured anywhere in this project.
- **Already tried:** the two candidates above; neither touched this step (both were inconclusive or
  negative and were not adopted).
- **What would unblock it:** a scoped freeze exception for `Obb` bounds computation that preserves
  the returned conservative `Aabb` value, immutability, equality and the closed-set rule; otherwise
  the gap stands.

### NG-3 — P01 `aabbPair` (comparability blocked)

- **Status:** NOT MET and **not comparable**. Accepted candidate 227,772,940 ops/s against
  3,561,837,866 (93.6 % short).
- **Evidence:** the frozen AABB kernel alone — six `double` comparisons, no dispatch, no allocation —
  measures ≈3.48×10⁸ ops/s, i.e. the target is **10.2× the bare kernel cost**, and its implied
  0.28 ns/op is below a method call plus JMH blackhole consumption.
- **Cause:** the target's operation definition is unknown and the reference harness is unpublished.
- **What would unblock it:** confirmation from the requirement owner of what one P01 operation is.
  The target is unchanged and the item stays open either way.

## Run-to-run spread and the acceptance rule

Two runs of the **same accepted candidate**, on the same host with the same protocol and no code
change in between, produced systematically lower numbers the second time:

| ID | Benchmark | Run A | Run B | Delta |
| --- | --- | ---: | ---: | ---: |
| P01 | `aabbPair` | 227,772,940 | 215,244,258 | −5.5 % |
| P02 | `capsulePair` | 182,575,343 | 176,862,524 | −3.1 % |
| P03 | `rotatedCapsulePair` | 14,419,711 | 13,450,134 | −6.7 % |
| P04 | `obbPair` | 197,247,854 | 182,872,946 | −7.3 % |
| P05 | `rotatedObbPair` | 1,310,934 | 1,164,513 | −11.2 % |
| P06 | `spherePair` | 236,843,703 | 203,486,620 | −14.1 % |

JMH reported a per-item error of roughly 1–5 %, but the observed run-to-run spread is 3–14 %, i.e.
larger than the reported error. Consequences, recorded rather than glossed over:

1. **An item verdict must come from a median of repeated runs**, not from a single run. One run cannot
   resolve differences smaller than the spread. The measured range for each item is the two runs above
   (more runs would tighten it).
2. **One earlier rejection must be downgraded to inconclusive.** The first rejected candidate
   (identity-flag caching) showed 1.4–15.6 % regressions, which exceeds the spread, so that rejection
   stands. The second (identity bounds short-circuit) showed −3.6 % and −0.9 % on the two open items —
   **inside the spread** — so its rejection is recorded here as **inconclusive rather than a proven
   regression**. It was not adopted (an inconclusive candidate cannot be shown to improve the open
   items), but this document no longer claims it made them slower.
3. **The three open items are unaffected.** P01, P03 and P05 fail by 20–94 %, far above the spread, so
   their NG records stand regardless of which run is used.

Protocol adjustment from here on: run each candidate at least three times, compare medians, and keep
the raw JSON of every run. The per-item error column is never used as an allowance against a target.

Raw runs: [run A](../benchmark/results/2026-09-30-jmh-results-optimized.json),
[run B](../benchmark/results/2026-09-30-jmh-results-final.json).

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
