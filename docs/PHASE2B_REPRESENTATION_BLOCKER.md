# Phase 2B representation prerequisite — historical blocker

Status: **PHASE 2B INCOMPLETE**. Historical diagnosis: 2026-09-04.

## Current disposition — 2026-09-05

The user authorized the separately reviewed W108 exact-reconstruction core repair and owner-decided local commits. That core implementation now passes the final independent 300-test/build/isolation gates and independent code/numerical reviews; evidence and artifact identity are recorded in [W108_RECONSTRUCTION_VERIFICATION.md](W108_RECONSTRUCTION_VERIFICATION.md). The new entry resolves the exact-value/byte reconstruction prerequisite without making the legacy normalizer idempotent or completing Phase 2B networking. The original mismatch below remains an explicit negative regression, alongside separate exact-success tests for the new public entry.

The remaining sections preserve the earlier diagnosis and stop decision as historical evidence. Their descriptions of an unchanged/red worktree, absence of a selected remedy and required future authorization refer to that earlier snapshot; the later narrow authorization is defined in [the reviewed design](EXACT_ROTATION_RECONSTRUCTION_DESIGN.md) and AGENTS.md. No production codec, lifecycle synchronization, dedicated-server or GPU acceptance is implied by this update.

## Historical record

The operational task-packet update is complete and execution began. A real Java 25 / Netty test then demonstrated that the mandated stored-quaternion/public-constructor round trip does not preserve exact value equality or payload bytes. No production networking implementation has been introduced.

## Verified starting state and scope

- Repository: D:/WI - Dev Workspace/CrlHitbox-src, branch main, HEAD 4ba9a496000e9433fc1a4fefeb0e44a51fee491b.
- Pre-existing changes: AGENTS.md's 35-line topology addition and untracked .codex/config.toml, both preserved. The index was clean; no remote or tag exists.
- Read-only template: D:/WI - Dev Workspace/CleanroomModTemplate, HEAD e64cd6fffdd1512f15cb5841653da02a470ec005, main ahead of origin/main by one commit, existing modified build.gradle and untracked bin/. No template changes were made.
- The local-only [task packet](<D:/WI - Dev Workspace/CrlHitbox-src/docs/input.md>) was incrementally updated only for starting-state accounting, runtime acceptance, cross-node evidence, Definition of Done and Git closeout, including corresponding report fields.
- Revised packet SHA-256: 58854CF66C0E63D49FB55ED0E317C9D186A7B6BB5362A0C39A2EE4035C4BB31B. It remains excluded by the existing /docs/input.md entry in .git/info/exclude; that exclude file was not changed.
- The 54,798-character specification block from HARD PLATFORM BASELINE through the section before LOCAL BUILD ENVIRONMENT is text-identical to the original packet. No wire layout, exact-equality rule, public API, protocol limit, generation/revision rule or geometry-freeze requirement was weakened.

## Existing contracts and the new incompatibility

[Rotation3d](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:31>) publicly constructs a rotation by max-component scaling and one ordinary binary64 normalization. It then canonicalizes quaternion sign and signed zero. Its [equality](<D:/WI - Dev Workspace/CrlHitbox-src/src/main/java/dev/crlhitbox/api/geometry/Rotation3d.java:145>) compares the exact stored component bits. The existing geometry contract does not promise that constructing another Rotation3d from those stored components is bitwise idempotent.

The Phase 2B [wire-construction and round-trip requirements](<D:/WI - Dev Workspace/CrlHitbox-src/docs/input.md:1039>) require writing the stored rotation components, reconstructing through the existing public constructor without bypassing normalization, retaining exact existing value equality, and obtaining identical bytes on re-encoding. Its [explicit stop clause](<D:/WI - Dev Workspace/CrlHitbox-src/docs/input.md:1079>) requires preserving a failing fixture and reporting the representation blocker rather than editing frozen geometry or adding an unsafe backdoor.

The verified conflict concerns that prescribed direct reconstruction path. It is not evidence of a regression in the existing pointwise collision contract, nor a proof that every conceivable alternative representation or reconstruction algorithm is mathematically impossible. Alternative algorithms or changed representation rules require a separately reviewed scope/contract decision; this task does not silently introduce one.

## Reproducible fixture

Executable source: [GeometryWireRepresentationPhase2BTest.java](<D:/WI - Dev Workspace/CrlHitbox-src/src/test/java/dev/crlhitbox/internal/network/GeometryWireRepresentationPhase2BTest.java:19>).

The raw server-side constructor input is (1.0, 1.0, 3.0, 2.0). The test writes the resulting stored x/y/z/w with actual Netty ByteBuf.writeDouble, reads with readDouble, and invokes the real public Rotation3d constructor. No production codec, reflected private constructor, approximate comparison or fabricated expected failure is involved.

| Component | Original stored value | Reconstructed value | Original raw bits | Reconstructed raw bits |
| --- | --- | --- | --- | --- |
| x | 0.2581988897471611 | 0.2581988897471611 | 3fd08654a2d4f6da | 3fd08654a2d4f6da |
| y | 0.2581988897471611 | 0.2581988897471611 | 3fd08654a2d4f6da | 3fd08654a2d4f6da |
| z | 0.7745966692414834 | 0.7745966692414835 | 3fe8c97ef43f7248 | 3fe8c97ef43f7249 |
| w | 0.5163977794943222 | 0.5163977794943222 | 3fe08654a2d4f6da | 3fe08654a2d4f6da |

The z component changes by one ULP. Consequently:

- Rotation3d.equals returns false.
- The 32-byte rotation fragment differs on re-encoding at zero-based byte offset 23: 0x48 becomes 0x49.
- A second test uses an OBB with center (0,0,0), half extents (1,2,3), the same rotation, and an outer rigid placement with that rotation and translation (4,5,6).
- Its 137-byte tag-2 OBB-plus-transform fragment fails exact Obb, RigidTransform3d and PlacedSolid3d equality, and re-encoded bytes first differ at offset 72.
- These fragments intentionally test representation prerequisites, not a complete Entity identity/header/holder message or a live network exchange.
- The quarter-turn control constructed from (0,0,1,1) passes both exact value and byte round trips.

## Executed validation

Environment: Temurin Java 25.0.3+9 LTS, Gradle wrapper 9.6.1, JAVA_HOME=C:/GradleCaches/jdks/eclipse_adoptium-25-amd64-windows.2, GRADLE_USER_HOME=C:/GradleCaches. JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/GradleCaches/tmp was process-local; no build or global configuration was changed.

1. Before the new test, the independent verifier ran:

   ```powershell
   .\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain
   ```

   Result: 32 suites, 285 tests, zero failures/errors/skips. Gradle daemon log daemon-43292.out.log records BUILD SUCCESSFUL in 31s, 8 executed tasks, completed execution and ReturnResult Success. The original tool formatter did not retain the client-shell numeric exit code; it is not reported as a captured 0.

2. Root then ran the authentic red prerequisite:

   ```powershell
   .\gradlew.bat test --tests dev.crlhitbox.internal.network.GeometryWireRepresentationPhase2BTest --stacktrace --console=plain
   ```

   Captured process exit: 1. Result: 3 tests, 2 failures, zero errors/skips. The quarter-turn control passed; both canonical-server exact-round-trip tests failed with the bit-level evidence above. Compilation succeeded. There is no green implementation result for these requirements.

3. The independent verifier then reran the full clean suite, including the new tests, with the same command as step 1. Captured process exit: 1, no live session remaining. Gradle reported BUILD FAILED in 18s, 8 executed tasks, and 288 tests completed with 2 failures. Final XML contains 33 suites / 288 tests / 2 failures / 0 errors / 0 skipped. All original 285 tests still pass; the only failures are the two new representation prerequisites, and their quarter-turn control passes. This is a deliberately preserved red acceptance boundary, not a passing Phase 2B build.

4. The initial independent jdeps check returned exit 0 and geometryIsolation -> java.base; the isolated output contained 21 class files. compileGeometryIsolation completed again in the final clean run before the test failure. The verifier compared all 20 production geometry source SHA-256 values against the pre-change baseline: 20/20 unchanged. No existing production or test source has a Git diff.

The normal JUnit outputs are [test XML](<D:/WI - Dev Workspace/CrlHitbox-src/build/test-results/test/TEST-dev.crlhitbox.internal.network.GeometryWireRepresentationPhase2BTest.xml>) and [HTML report](<D:/WI - Dev Workspace/CrlHitbox-src/build/reports/tests/test/index.html>). These generated files may be replaced by later Gradle runs; the checked-in-candidate test source and this explicit fixture preserve the diagnosis independently.

## Review and authority boundary

The incremental operational packet was independently reviewed; one temporary-server-directory ambiguity was corrected to require a disposable directory outside both the development checkout and read-only template. Its final scoped review has no residual P0/P1/P2 findings.

The independent representation-prerequisite reviewer found no test-fidelity, buffer-lifetime or overclaim defect (P0/P1/P2 = 0/0/0). Separately, it identified the demonstrated P1 execution blocker and confirmed the packet's stop clause. The test fragment accurately follows the prescribed field order and direct constructor path; absent full-message headers cannot repair the changed geometry bits.

The read-only consultation in the [user-designated in-app-browser Pro conversation](https://chatgpt.com/c/6a94036c-e798-83e8-b921-198beb49698b) completed. The actual in-app browser UI showed Pro; the completed reply showed a 10m 58s thinking duration and normal reply actions after generation ended. No external browser or ordinary subagent was counted as Pro.

The advisor's complete decision, independently checked by the repository owner against the source and Java/Netty fixture, is:

- Let N be the existing public constructor and q=N(1,1,3,2). The prescribed direct decode produces N(q), while the frozen exact round trip requires N(q)=q. The observed z-bit change disproves that universal requirement for one valid server-generated value; a production message implementation is not needed to establish this prerequisite failure.
- This is a Phase 2B representation-contract incompatibility, not by itself a violation of the previously accepted one-normalization, pointwise geometry contract. Existing exact equality is behaving as documented.
- Packet headers, generation/revision handling or snapshot installation cannot repair a mismatch already introduced by the mandated direct geometry reconstruction. Retaining bytes alone would not restore object equality; restricting accepted rotations would violate the current every-valid-value coverage.
- No remedy was selected. Separately reviewable families include a rigorously validated bit-preserving canonical reconstruction boundary; a redesigned idempotent canonicalization contract; changed wire/value equivalence; opaque wire state plus runtime reconstruction; an alternative orientation representation or constructor preimage; or an explicitly restricted/quantized domain. Each changes a protected boundary and carries distinct validity, equality/hash, precision, security, memory, or protocol/delta risks. None is authorized as a Phase 2B implementation detail.
- Synthesizing different constructor inputs instead of directly using the transmitted components is not proven impossible in general, but changes the stipulated reconstruction semantics and has no established universal algorithm or bounded proof here. Repeated normalization likewise has no established universal fixed-point/convergence guarantee.
- Preserve both failing fixtures and the passing control; retain the exact test counts, exit codes, raw bits and differing offsets. Do not weaken assertions, add a backdoor, proceed with production network/state/lifecycle implementation or create a partial feature commit.
- The reasoning assumes direct use of transmitted stored components in the public constructor, the verified binary64/equals behavior and validity of the fixture. It does not establish failure frequency, behavior on every JDK, convergence after repeated normalization, a pointwise collision error, or the safety of any future remedy. It is not a universal impossibility claim about redesigned codecs.

Root decision: accept the verified stop condition only. The advisor's remedy discussion is recorded as alternatives requiring explicit owner/user authorization, not repository truth, a chosen design, or implementation permission. Existing multi-node runtime gates remain required for any later authorized design and were not executed here.

## Stopping point

- No production source, geometry source, existing test, public API, dependency or Gradle configuration was modified.
- No replaceContents, EntityHitboxSync, codec/message, generation state, replica installer, pending store or lifecycle sender has been introduced.
- The two red tests remain enabled and uncommitted as the frozen task's retained failing fixture; they deliberately keep the new exact-wire acceptance requirement visible rather than masking it with a green approximation.
- No Phase 2B feature commit, tag, remote, push or publication was created.
- No CrlOzzAPI work or Phase 2C work was started.
- No Minecraft client or dedicated server was launched, and no EULA was accepted. Runtime acceptance and cross-node artifact handoff were not executed; no Phase 2B artifact is claimed.
- The test logging setup generated an empty logs/latest.log. After verification, only that known task-generated zero-byte file was removed; it contained no server/client evidence. No unrelated user log or test result was deleted.

Real GPU acceptance: not executed

Further production work requires an explicit owner/user decision on the representation/constructor/wire contract. A tolerance workaround, private-constructor bypass, silent canonicalization change or restricted set of sendable rotations would change the retained core requirements and is not authorized by the operational update.
