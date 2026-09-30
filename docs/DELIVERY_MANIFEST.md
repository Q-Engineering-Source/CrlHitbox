# Delivery manifest

Status: **CANDIDATE / CHECKPOINT, not OK.** This manifest lists what is actually delivered, the
evidence that supports it, and — explicitly — what has not been executed. It follows the roadmap's
completion definition (section 11): an item is only `OK` when the corresponding evidence exists.

## Candidate identity

| Property | Value |
| --- | --- |
| Base commit | `3d8d597b71eab6ecd83d2c55d9a87b2ec4488b39` — `bench: add diagnostic decomposition for the collider query cost` |
| Delivered slice on top | independent consumer fixture (`src/example/java`) + this manifest |
| Branch | `main` |
| Shipped artifact entries | no `jmh`, no `bench`, no example classes |

The artifact hashes below were built from the working tree that contains the consumer fixture; a
later commit that changes production or resource inputs invalidates them. Rebuild and re-hash before
handing the artifact to another node.

## Build environment

| Property | Value |
| --- | --- |
| JDK | Zulu OpenJDK 25.0.3+9-LTS (`<jdk-home>`) |
| Gradle | project wrapper, Gradle 9.6.1 |
| Cache | `GRADLE_USER_HOME=<gradle-user-home>` |
| Platform pins | Minecraft 1.12.2, MCP stable 39-1.12, Cleanroom Loader 0.6.8-alpha, Java 25 |

The machine used for this work is **not** the machine recorded in roadmap section 5.2; see the
benchmark protocol for the substitute-host warning.

Commands: `.\gradlew.bat clean compileGeometryIsolation test` then `.\gradlew.bat build`.

## Artifacts

| Artifact | Size (bytes) | SHA-256 |
| --- | ---: | --- |
| `build/libs/crlhitbox-0.1.0-SNAPSHOT.jar` (remapped) | 173,297 | `7BE9570E298B69417B47E69A5D87CFCE8714C532589595D481A564E4DFDF0A1F` |
| `build/libs/crlhitbox-0.1.0-SNAPSHOT-dev.jar` | 174,313 | `CD212568038530F1A5286829C341B0AD59B85443E60A813E0AC93CCA302714C1` |
| `build/libs/crlhitbox-0.1.0-SNAPSHOT-sources.jar` | 119,438 | `02BD9DF4A1204B14B455B55C4F1B9712A9FD038C58C36FAEB8E904554556CBE6` |

## Automated evidence

| Gate | Result |
| --- | --- |
| `clean compileGeometryIsolation test` | exit 0; **54 suites / 571 tests / 0 failures / 0 errors / 0 skipped** |
| `build` | exit 0 |
| `jdeps build/classes/java/geometryIsolation` | `geometryIsolation -> java.base` |
| Frozen geometry production files | 21 files, unchanged (`git status` clean for that package) |
| Consumer fixture | compiled from its own source set; classfile audit proves no `internal`, reflection, `Class.forName` or `Unsafe` use |
| Server-safety classfile audit | common and server classes hold no client, LWJGL, Netty or rendering linkage; only `dev.crlhitbox.internal.client` may |
| Cross-project consumer build | `tools/Verify-ConsumerBuild.ps1` compiles `src/example/java` with `javac` into `build/consumer-classes` using only the dev artifact and the platform jar; 1 class produced, repository build directories are not on its classpath |

## Functional status (roadmap F01–F10)

| ID | Requirement | Status | Evidence / gap |
| --- | --- | --- | --- |
| F01 | AABB/OBB/Sphere/Capsule queries | **OK** | frozen kernels plus the unified entry; 571 tests green |
| F02 | Finite directed ray | **OK** | `Ray3d`, `MutableRayCollider`, four ray entries incl. ray-ray |
| F03 | Nestable editable compound | **OK** | `MutableCompoundCollider` + snapshot compounds, iterative traversal |
| F04 | Unified query entry | **OK** | `ColliderQueries` with four overloads |
| F05 | Non-persistent entity cache | **CODE OK / RUNTIME NOT EXECUTED** | capability + holder + snapshot implemented; real entity lifecycle needs a dedicated server |
| F06 | F3+B debug overlay | **CODE OK / REAL GPU NOT EXECUTED** | client-only renderer compiles and passes structural audits; no client instance available |
| F07 | Usable on Cleanroom | **NOT EXECUTED** | no dedicated-server or client instance was launched; no EULA accepted |
| F08 | Six throughput targets | **PARTIAL (3/6)** | P02, P04, P06 met; P01 recorded as not comparable with this entry point; P03 (−14.4 %) and P05 (−71.8 %) open |
| F09 | Public API for add-ons | **PARTIAL** | independent consumer *source set* compiles against the public API only, and `tools/Verify-ConsumerBuild.ps1` compiles the same sources with `javac` into a directory **outside** the repository using only the packaged dev artifact plus the platform jar (1 class, 3,617 bytes, SHA-256 `69CE3CCFEE3CECAD238BFDE01DF9F10A405894052140C2D20D7EBC0846871EDF`). Still missing: resolving a *published* artifact from a Maven repository, which publication policy does not allow |
| F10 | Mutable shapes + event-driven updates | **CODE OK / RUNTIME NOT EXECUTED** | setters, atomic publication and the update entry are tested; the live event chain needs a server |

## Performance status (roadmap P01–P06)

| ID | Target ops/s | Best measured | Status |
| --- | ---: | ---: | --- |
| P01 | 3,561,837,865.930 | 227,772,940 | not comparable: target is 10.2× the cost of the bare frozen kernel |
| P02 | 22,350,681.198 | 182,575,343 | met in both runs (176.9–182.6×10⁶) |
| P03 | 16,842,309.523 | 14,419,711 | open, 14.4–20.1 % short across runs |
| P04 | 5,528,493.224 | 197,247,854 | met in both runs (182.9–197.2×10⁶) |
| P05 | 4,648,963.750 | 1,310,934 | open, 71.8–75.0 % short across runs |
| P06 | 119,556,589.705 | 236,843,703 | met in both runs (203.5–236.8×10⁶) |

Two runs of the same accepted candidate differ by 3–14 % per item, which is larger than JMH's reported
per-item error; the protocol document records this and requires medians of repeated runs for item
verdicts. The verdicts above (P02/P04/P06 met, P01/P03/P05 open) are unchanged in both runs.

Every run used the frozen protocol in [COLLIDER_BENCHMARK_PROTOCOL.md](COLLIDER_BENCHMARK_PROTOCOL.md);
raw JMH JSON for all runs is kept under [benchmark/results](../benchmark/results), including the
rejected candidates and the diagnostics. The three unfinished items have formal NG records in that
document: P03 and P05 with the exact frozen-file levers that would unblock them, and P01 as
comparability-blocked. An NG record never lowers a target.

## Not executed (must not be reported as passed)

- Authoritative dedicated-server run: startup, capability lifecycle, update entry, F3+B data source.
- Real-GPU client run: overlay visibility, entity motion, camera offsets, GL state restoration.
- Formal performance acceptance on the machine recorded by the roadmap.
- Resolving a *published* artifact from a Maven repository in a consumer project; the cross-project
  compile uses the local dev artifact because publication policy does not allow publishing.
- Any networking scope decision (roadmap section 8): full synchronization remains a deferred item.

The check tables, required inputs and evidence format for the first three are prepared in
[RUNTIME_ACCEPTANCE_HANDOFF.md](RUNTIME_ACCEPTANCE_HANDOFF.md). Preparing them is not executing them:
every gate there remains `NOT EXECUTED`.

## Known limits

- Runtime results are ordinary pointwise `double` results, not certified geometry; touching counts as
  intersection and there is no global tolerance.
- The overlay and the update entry are display- and caller-driven: this library never generates a
  default shape, never sends packets on its own and never persists collider state.
- The absolute throughput targets were defined on different hardware than the substitute host used
  here, so the numbers are observations, not an acceptance statement.

## License

No project license has been selected. Third-party notices are preserved in
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). Nothing in this manifest grants publication,
deployment, tagging or push authority.
