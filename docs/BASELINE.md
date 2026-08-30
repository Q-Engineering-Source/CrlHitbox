# CRL Hitbox initialization baseline

## Paths and provenance

- Canonical target path: `D:\WI - Dev Workspace\CrlHitbox-src`
- Canonical template path: `D:\WI - Dev Workspace\CleanroomModTemplate`
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
$env:JAVA_HOME = 'C:\GradleCaches\jdks\eclipse_adoptium-25-amd64-windows.2'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = 'C:\GradleCaches'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:\GradleCaches\tmp'
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
- Distribution license

License decision pending; no distribution license granted by repository metadata.

## Clean-room reimplementation boundary

HitboxAPI may later be consulted only as a functional reference. It is not the code, algorithm, protocol, licensing, or behavioral baseline.

No HitboxAPI source, package structure, assets, logo, metadata, serialization layout, class bodies, comments, tests, or algorithms may be copied, translated, ported, pasted, or adapted. HitboxAPI is not a project dependency and is not a correctness oracle.

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
