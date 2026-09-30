# CRL Hitbox initialization baseline

## Paths and provenance

- Canonical target path: `.`
- Canonical template path: `<template-checkout>`
- The local template is authoritative for initialization and is read-only.
- Initialization date: 2026-08-30 (Asia/Shanghai)
- Initial target state: the target path did not exist and had no Git repository or user files.
- Git state after repository creation: empty `main` branch with an unborn HEAD; no commit was created and all initialized project files are untracked.
- Template state observed at initialization: branch `main`, HEAD `e64cd6fffdd1512f15cb5841653da02a470ec005`, ahead of `origin/main` by one commit, with pre-existing changes to `build.gradle` and pre-existing untracked `bin/` content.

## Verified minimum platform baseline

| Component | Exact baseline | Declaration |
| --- | --- | --- |
| Minecraft | `1.12.2` | `build.gradle`, `unimined.minecraft.version` |
| MCP mappings | channel `stable`, version `39-1.12` | `build.gradle`, `mappings.mcp(...)` |
| Cleanroom Loader | `0.6.8-alpha` | `build.gradle`, `unimined.minecraft.cleanroom.loader` |
| Java toolchain | Java `25` | `build.gradle`, `java.toolchain.languageVersion` |
| Main source/target | Java `25` | `build.gradle`, `compileJava.sourceCompatibility` and `targetCompatibility` |
| Test source/target/runtime | Java `25` | `build.gradle`, `compileTestJava` and `test.javaLauncher` |
| Gradle wrapper | `9.6.1` binary distribution | `gradle/wrapper/gradle-wrapper.properties` |
| Foojay toolchain resolver | `1.0.0` | `settings.gradle` |
| Shadow plugin | `com.gradleup.shadow` `9.5.1` | `build.gradle` |
| IntelliJ IDEA extension plugin | `org.jetbrains.gradle.plugin.idea-ext` `1.4.1` | `build.gradle` |
| Unimined plugin | `xyz.wagyourtail.unimined` `1.4.27-kappa` | `build.gradle` |
| Blossom plugin | `net.kyori.blossom` `2.2.0` | `build.gradle` |
| Unit-test framework | JUnit Jupiter `6.0.3` with JUnit Platform launcher | `build.gradle` |

The built-in Gradle plugins `java`, `java-library`, and `maven-publish` are also applied without separate plugin-version declarations. Maven publishing is disabled by project properties.

The exact minimum supported Cleanroom version is `0.6.8-alpha`. Upward-compatible means the project is compiled and verified against this minimum baseline and avoids later-only Cleanroom APIs or assumptions that prevent execution under compatible later Cleanroom releases. Later releases are not the source or build baseline, and the project imposes no artificial upper version cap.

## Build and artifacts

Authoritative local Windows build command:

```powershell
$env:JAVA_HOME = '<gradle-user-home>\jdks\eclipse_adoptium-25-amd64-windows.2'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = '<gradle-user-home>'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=<gradle-user-home>\tmp'
.\gradlew.bat clean build --stacktrace
```

The `JAVA_TOOL_OPTIONS` setting is a local-host workaround, not a project or Cleanroom requirement. On this host, JDK 25's selector wakeup pipe fails when its Windows Unix-domain socket is created below the current `%TEMP%` path, which contains an 8.3 alias. Directing `jdk.net.unixdomain.tmpdir` to the short Gradle cache path makes the same build reproducible without changing or bypassing any Gradle task. Other hosts should omit this setting unless they reproduce the same `Unable to establish loopback connection` / `Invalid argument: connect` failure.

The template-native `build` lifecycle includes the `test` task. With shadowing disabled, `jar` produces a development-classifier jar and finalizes the Unimined `remapJar` task. The initialization build verified the following historical artifact names while the template placeholders were still in use.

- Primary remapped artifact: `build\libs\crlhitbox-1.0.0.jar`
- Development artifact: `build\libs\crlhitbox-1.0.0-dev.jar`
- Sources artifact: `build\libs\crlhitbox-1.0.0-sources.jar`

Before the repository's first commit, identity normalization changed the artifact filenames to `crlhitbox-0.1.0-SNAPSHOT.jar`, `crlhitbox-0.1.0-SNAPSHOT-dev.jar`, and `crlhitbox-0.1.0-SNAPSHOT-sources.jar`. A post-normalization Java 25 wrapper build verified all three filenames before the baseline commit.

The primary artifact contains only `CrlHitbox`, generated `Reference`, `mcmod.info`, `pack.mcmeta`, and its manifest. Its compiled classes use class-file major version `69` (Java 25).

## Mixin and transformation state

The authoritative template's selected `main` branch contains no Mixin plugin declaration, Mixin configuration, refmap, bootstrap dependency, or Mixin class. The Unimined-remapped artifact automatically carries the inert manifest attribute `Fabric-Loom-Mixin-Remap-Type: mixin`, so generic remap metadata exists, but no Mixin runtime/configuration/class is present or active. Access Transformer support remains available in the build convention but is disabled, and no Access Transformer file is present. Coremod support is disabled.

## Project identity

Finalized identity:

- Display name: `CRL Hitbox`
- Repository name: `crl-hitbox` (the local target directory is `CrlHitbox-src`)
- Mod ID: `crlhitbox`
- Artifact base name: `crlhitbox`
- Main class: `dev.crlhitbox.CrlHitbox`
- Maven group and Java base package: `dev.crlhitbox`
- Initial development version: `0.1.0-SNAPSHOT`
- Reserved future network channel: `crlhitbox` (not implemented)
- Reserved future configuration prefix: `crlhitbox` (not implemented)
- Description: `A Cleanroom-only runtime hitbox and collision-query library for Minecraft 1.12.2.`

Initialization history:

- The initialization report temporarily used template placeholders `com.example` for the root package/Maven group and `1.0.0` for the project version.
- Those placeholders were replaced with the finalized identity above before the repository's first commit.

Unresolved fields:

- Authors
- Project URL
- Issue tracker
- Credits
- Update JSON URL
- Logo

Distribution license remains unresolved; no distribution license is granted by repository metadata.

## Functional reference boundary

The functional target is the collision feature set described by HitboxAPI's public README and public
interfaces, implemented independently in this project. HitboxAPI
(`https://github.com/AnECanSaiTin/HitboxAPI`) is a **read-only functional reference**: its source,
algorithms, network protocol, license, package structure, assets and tests are not copied, translated
or adapted, and it is neither a project dependency nor a correctness oracle.

`docs/HITBOXAPI_PARITY.md` records the read-only interface inventory and the capability gap.
`docs/CRL_HITBOX_ROADMAP.md` is the authoritative plan for the next stage: its acceptance matrix
(F01–F10), the six absolute throughput targets (P01–P06), the phased plan (R0–R6), the method-level
API proposals, and the local environment build-out tasks (B01–B07).

## Future architecture constraints (recorded, not implemented)

1. The future runtime-hitbox core will be common/server-safe and pure-query.
2. Runtime geometry will use an explicitly defined double-precision boundary.
3. Collision queries must not execute damage, callbacks, world mutation, networking, or rendering.
4. Platform adaptation for Minecraft 1.12.2 will remain outside the geometry kernel.
5. Client debug rendering will remain client-only and must never link on a dedicated server.
6. Network synchronization will later use explicit protocol, holder generation, and revision semantics.
7. Optional combat orchestration will remain above the collision kernel.
8. Runtime hitbox results are non-certified pointwise results.
9. Runtime hitboxes must never become an authority for certified interval geometry, admission, continuous terrain non-penetration, persistence truth, or proof receipts.
10. The minimum implementation/API baseline remains CRL `0.6.8-alpha` even when later Cleanroom versions are tested.

## Initialization non-goals

This phase does not implement gameplay behavior, collision primitives, holders, capabilities, combat callbacks, synchronization, packets, rendering, adapters, example content, world mutation, debug hooks, certified geometry, Mixin, Access Transformers, coremods, publication, release automation, or runtime compatibility branches. It does not launch a graphical client or dedicated server.

## Initialization baseline versus Phase 1A

This document records the completed initialization baseline, not a claim that all later feature work was present in that baseline. The finalized identity (`dev.crlhitbox` for Maven group and Java base package, `dev.crlhitbox.CrlHitbox`, and `0.1.0-SNAPSHOT`) was established before the initialization baseline commit, while this report retains the historical temporary `com.example` and `1.0.0` values above for provenance.

The initialization baseline commit is `9fba3f6cf81d3a8c916b94affcd3c313816a6b85` (`chore: establish CRL Hitbox 0.6.8 baseline`). Its platform pins remain the authoritative initialization floor: Minecraft `1.12.2`, MCP stable `39-1.12`, Cleanroom Loader `0.6.8-alpha`, Java `25`, Gradle wrapper `9.6.1`, and the plugin versions recorded in this document.

Phase 1A is later, separately scoped work that adds the experimental immutable pointwise geometry foundation and its build-time isolation check. It does not revise the initialization history, platform pins, template provenance, or unresolved license status. This section records scope chronology only; it does not assert that Phase 1A tests or builds have passed.

## Phase 2A entity capability layer

Phase 2A adds a server-safe platform layer without revising the initialization history or geometry
baseline. A fresh non-persistent `EntityHitboxHolder` capability is attached to every Minecraft
`Entity` through the standard Forge 1.12.2 `AttachCapabilitiesEvent<Entity>` lifecycle. Holder
entries are deterministic `ResourceLocation` to entity-local `PlacedSolid3d` mappings; immutable
snapshots capture their checked monotonic revision and insertion order.

The capability provider owns no Entity or World reference, implements no NBT serialization
interface, and does not copy state across clone, respawn, reload, dimension transfer, or process
restart. Server and client holders are independent side-local state. Phase 2A adds no network
channel, tracking protocol, entity-to-world pose adapter, rendering, hit/hurt role, or combat
behavior. The geometry source and its Java-only isolation task remain unchanged.

## Phase 2B full-snapshot synchronization

The Phase 2B server-authoritative replication contract is recorded in
[FULL_SNAPSHOT_PROTOCOL.md](FULL_SNAPSHOT_PROTOCOL.md). The implemented slice adds the deterministic
direct-binary codec under the internal wire package (frozen protocol constants and limits, strict
bounded VarInt coding, direct binary encoding for every `Solid3d`, `PlacedSolid3d`, and
`RigidTransform3d` representation including flat Composite leaves, all-or-nothing decoding with
pre-allocation validation and exact exhaustion, and the immutable payload shared by both directions,
with rotations restored through the geometry-owned exact reconstruction entry and every geometry
value built through existing public immutable constructors).

On top of that codec it adds the `crlhitbox` SimpleNetworkWrapper channel with exactly one
client-bound message (discriminator `0`, `Side.CLIENT`), provider generation allocation, an internal
replica-state capability, the frozen generation/revision acceptance ordering, `EntityHitboxSync`
with its two server-thread-confined send methods, StartTracking and player login/respawn/dimension
delivery, a client-only SidedProxy seam with client-main-thread installation, and a bounded client
pending store with tick retry.

What remains is runtime acceptance only: no maximum-legal-payload resource measurement, no
dedicated-server run, and no real-GPU client run has been performed, so Phase 2B stays incomplete and
no live replication is claimed. The platform pins, geometry source, and geometry isolation task are
unchanged.

## Open collider API, general entity cache, debug overlay and benchmarks

The roadmap's function-gap stage adds a public collider layer beside the frozen kernel rather than
changing it. `dev.crlhitbox.api.collider` provides the `Collider`/`MutableCollider` contracts, the
sealed `ColliderSnapshot` hierarchy (solid, ray and compound), the finite `Ray3d` value, mutable
AABB/sphere/oriented-box/capsule/ray/compound colliders, and the unified `ColliderQueries` entry.
Setters validate completely before publishing, equal updates are no-ops, compounds own immutable child
snapshots, and compound queries are expanded iteratively with conservative negative pruning; the
geometry package keeps every placement and narrow-phase computation. `compileGeometryIsolation` was
extended to this layer, so it is held to the same empty-external-classpath rule, and `jdeps` still
reports only `java.base`.

`dev.crlhitbox.api.entity` gains a general cache beside the Phase 2A holder: `EntityColliderHolder`,
`EntityColliderSnapshot`, `EntityColliders` and `EntityColliderFrames`, exposed through a third
capability served by the same provider. `dev.crlhitbox.api.event.EntityColliderUpdateEvent` together
with `EntityColliders.requestUpdate` is the explicit update entry; it is requested on the owning
logical thread, fires once, and reports whether the cache revision really advanced. A client-only
F3+B overlay draws the caches as real outlines and reads the vanilla debug toggle. An isolated
`src/jmh` source set carries six absolute throughput benchmarks, a frozen protocol and a threshold
checker; no JMH type and no verification-only class reaches the shipped artifact.

Evidence for this stage: 54 suites / 571 tests green with `clean compileGeometryIsolation test` and a
successful `build`; the frozen geometry production package unchanged; raw benchmark results, rejected
candidates and diagnostics under `benchmark/results`; three NG records and the frozen protocol in
[COLLIDER_BENCHMARK_PROTOCOL.md](COLLIDER_BENCHMARK_PROTOCOL.md); the delivery state in
[DELIVERY_MANIFEST.md](DELIVERY_MANIFEST.md); and the prepared runtime check tables in
[RUNTIME_ACCEPTANCE_HANDOFF.md](RUNTIME_ACCEPTANCE_HANDOFF.md).

Only three of the six throughput targets are met; the other three are recorded as NG entries rather
than accepted. Dedicated-server, real-GPU and formal-performance acceptance remain **not executed**,
the license is still unresolved, and the roadmap's remaining main-line work is the platform runtime
gates plus whatever the owner decides about the frozen-file levers.
