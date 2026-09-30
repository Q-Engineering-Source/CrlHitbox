# Runtime acceptance handoff

Purpose: everything a person **with a real game instance** needs in order to execute the runtime gates
that this environment cannot. Nothing here has been executed. Dedicated-server, real-GPU-client and
formal-performance gates are all **NOT EXECUTED**, and this document does not claim otherwise.

Do not mark any check below as passed from source inspection, classfile inspection, software
rendering, unit tests or a screenshot-free claim. Each check needs the artifact and the evidence
listed with it.

## 0. Inputs to prepare

| Input | Requirement | Status here |
| --- | --- | --- |
| Mod artifact | `build/libs/crlhitbox-0.1.0-SNAPSHOT.jar`, record its SHA-256 | built locally; hash changes with every input change |
| Loader | Cleanroom Loader `0.6.8-alpha` (the project's minimum baseline) | not installed as a runnable instance |
| Java | 25 (Zulu 25.0.3+9-LTS was used for development) | installed on this host |
| Server instance | isolated directory, own port, own world, graceful start/stop | **UNKNOWN** — directory, port and EULA state are not established and must not be guessed |
| Client instance | real GPU, same loader and mod set | **NOT ESTABLISHED** |
| Companion test mods | the roadmap asks for a compatible Fugue and scalar build; verify their files and metadata before relying on them | **not verified here** |
| Isolation | test instance data, logs and worlds must be separate from any working or valuable world | required |

Never accept an EULA, delete a world, or change an instance's global configuration on the owner's
behalf.

## 1. Dedicated-server checks (F05, F07, F10 runtime half)

| ID | Check | Steps | Pass evidence |
| --- | --- | --- | --- |
| D1 | Server reaches readiness | start the isolated instance with the artifact, wait for the readiness line, then stop gracefully | full startup log, loader line, Java version, artifact hash |
| D2 | Capability lifecycle | on a live entity, resolve the holder and the generic cache; then let the entity be removed | log or test-mod output showing attach, read, and that a new entity gets an empty revision-0 cache |
| D3 | Cache semantics on the server | `put`, equal `put` (no-op), `remove`, `clear`, `replaceContents` | revision sequence and returned booleans, printed by the test mod |
| D4 | Explicit update entry | call `EntityColliders.requestUpdate(entity, reason)` on the server thread; a listener publishes a new snapshot | event fired exactly once, revision advanced exactly once, second call with unchanged listener reports `false` |
| D5 | Thread and reentrancy rules | call `requestUpdate` from a non-server thread, and recursively from a listener | first raises `IllegalStateException`, second raises the recursion error, and neither corrupts the cache |
| D6 | Dedicated-server linkage | run the full startup with the client absent | no client class is loaded; a `NoClassDefFoundError` for a client type is a failure |

## 2. Real-GPU client checks (F06, F07 runtime half)

| ID | Check | Steps | Pass evidence |
| --- | --- | --- | --- |
| C1 | Overlay toggle | press F3+B with a cached collider present | short recording showing outlines appear and disappear with the toggle |
| C2 | Shape fidelity | place an AABB, a sphere, an oriented box, a capsule and a ray | recording: each shape is drawn as itself, not as one bounding box |
| C3 | Transform and camera | move and rotate the entity, move the camera, watch interpolation | recording covering motion, rotation, camera offsets and partial-tick smoothness |
| C4 | GL state restoration | after drawing, check that the vanilla debug overlay still renders and that world rendering is unaffected | recording plus a note that no state leaked |
| C5 | Degenerate and extreme cases | zero radius, zero length, points and planes, far from the world origin | recording or log showing no artifacts and no crash |
| C6 | Lifecycle | remove the entity, switch worlds, disconnect | recording or log showing pending/overlay state is dropped and nothing is drawn afterwards |
| C7 | Disabled entries | disable one collider | it is not drawn and not queryable |

## 3. Formal performance checks (P01–P06)

Run on the machine recorded by the roadmap (section 5.2), or on a replacement the owner has
confirmed:

```powershell
$env:JAVA_HOME = 'D:\Program Files\Zulu\zulu-25'
$env:GRADLE_USER_HOME = 'D:\gradle'
.\gradlew.bat runJmh --console=plain "-PjmhArgs=-rf json -rff benchmark/results/<candidate>-jmh.json .*ColliderBenchmark.*"
```

Then extract one `benchmark=score` line per item (asserting the unit is `ops/s`) and run:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp build/classes/java/main `
    dev.crlhitbox.internal.bench.BenchmarkThresholds <measured.txt>
```

Record: machine, CPU, memory, OS, JDK, GC, background load, every raw JMH file, and the checker exit
code. A missing item is `NOT EXECUTED`, never zero. Do not subtract the reference error column and do
not average items.

## 4. Evidence package to hand back

1. Environment JSON (loader, Java, all test mods with hashes, instance directories, ports, world).
2. The artifact JAR with its SHA-256, identical for every gate above.
3. Raw logs for the server gates; recordings for the client gates.
4. Raw JMH JSON plus the extracted measurements and the checker output for the performance gates.
5. A short statement per gate: `PASS`, `FAIL` or `NOT EXECUTED`, with the evidence path.

## 5. Failure reporting

If a gate fails, keep the failing artifact as an NG candidate (do not overwrite the accepted one),
record the exact command, the observed output, and the first cause. Do not "fix" a failing gate by
changing the threshold, by moving work outside the measured region, or by reporting a partial run as
complete.

## 6. What is already known to be short

- P03 and P05 are short and their remaining levers are inside frozen geometry files (see the NG
  records in [COLLIDER_BENCHMARK_PROTOCOL.md](COLLIDER_BENCHMARK_PROTOCOL.md)).
- P01 is not comparable with this project's entry point until the requirement owner confirms what one
  operation is.
