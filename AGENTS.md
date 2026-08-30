# CRL Hitbox project constraints

- This is a Minecraft 1.12.2, Cleanroom-only, Java 25 project. The minimum implementation and API baseline is Cleanroom Loader `0.6.8-alpha`; compile and verify against that exact baseline without dynamic versions, dependency substitution, or later-only APIs.
- Preserve the Gradle wrapper, plugin, mapping, repository, manifest, and loader conventions derived from the read-only local template at `D:\WI - Dev Workspace\CleanroomModTemplate`. Do not upgrade them without explicit authorization.
- Use `C:\GradleCaches` as `GRADLE_USER_HOME` unless an explicit project-local configuration supersedes it. Build with the target wrapper.
- Keep this initialization skeleton non-feature-bearing. Do not add gameplay examples, collision logic, capabilities, networking, rendering, combat orchestration, certified geometry, debug hooks, coremods, Access Transformers, or Mixin injections without a later scoped task.
- Keep common/runtime-hitbox code dedicated-server safe. Client-only code must remain isolated and must never link on a dedicated server.
- HitboxAPI may later be consulted only as a functional reference. It is not the code, algorithm, protocol, licensing, or behavioral baseline. Do not copy or adapt its source, package structure, assets, metadata, serialization, tests, or algorithms.
- Collision queries are pure queries: they must not execute damage, callbacks, world mutation, networking, or rendering. Runtime results are non-certified pointwise results and must never become authority for certified interval geometry, admission, continuous terrain non-penetration, persistence truth, or proof receipts.
- The project identity is finalized as Maven group and Java base package `dev.crlhitbox`, main class `dev.crlhitbox.CrlHitbox`, and development version `0.1.0-SNAPSHOT`. Preserve this identity unless the user explicitly changes it.
- No license has been selected. Do not add a license, SPDX header, publishing configuration, commit, tag, release, or push without explicit authorization.
