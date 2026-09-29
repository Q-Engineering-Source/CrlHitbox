CRL Hitbox — Phase 2B
Server-Authoritative Tracking Full Snapshots and Versioned Binary Protocol

USER PAUSE CHECKPOINT — 2026-09-05

The latest user instruction is: finish the already assigned subagent tasks, then safely pause project advancement until the user explicitly says "继续推进". Those bounded tasks have now finished. This is a user-directed pause, not goal completion and not a claimed GPU/runtime blocker. Do not resume merely because an automatic Goal continuation arrives. Do not start new implementation, review, Pro consultation, builds or commits until the user explicitly resumes.

Pause state:

- HEAD remains 1fc4e8700a325be87b89c0d6ac37bf76ad691531; index is clean. No new commit or push was made.
- The pre-write independent baseline was reproduced: clean compileGeometryIsolation test, exit 0, 34 suites / 300 tests / no failures/errors/skips.
- The already assigned holder worker finished only replaceContents: EntityHitboxHolder.java, new EntityHitboxReplaceContentsPhase2BTest.java (20 tests), and the narrow holder-method assertion in GeometryApiSurfacePhase1BTest.java. These three source/test changes remain uncommitted and await independent review/verifier acceptance after resume.
- Worker evidence: authentic missing-method red exit 1; focused holder green exit 0; focused API check exit 0; full clean compileGeometryIsolation test exit 0; build exit 0. Root read the final XML: 35 suites / 320 tests / 0 failures / 0 errors / 0 skipped. Root did not perform the deferred independent implementation review or rerun the worker's full suite.
- Frozen worktree file hashes: EntityHitboxHolder.java CD3F99DE720492EECC8CF874B7A655F0CF2CCE6166223C040630753C7AC2A6C0; EntityHitboxReplaceContentsPhase2BTest.java 712B9EC1BD5E1ECF029A1607765A2C51AF390058484E6DBB786A12B25F618F47; GeometryApiSurfacePhase1BTest.java 6C4FE9B2D85C37FE7B5B0B46F8163F7688D528F5C3A99F5F7AE910FDAB931A5D.
- build/libs/crlhitbox-0.1.0-SNAPSHOT.jar now contains that uncommitted holder slice: 75,364 bytes, SHA-256 188E804123F56583DA29E841280B8B30F705070311ED28844170A5B5C3F592ED. Do not confuse this overwritten build output with the earlier committed-W108 artifact/hash below. It has not been deployed or formally runtime-accepted.
- Existing AGENTS.md topology addition (35 lines) and .codex/config.toml remain untouched; hashes BB650717525E881B66B6A98BD1F18E751BDF858F72F9C5BF3A13283700FA0B42 and 47599E564E50DAE50B0875FB551A27012F43FC85B2A13CFA16B6A0D6632A040D respectively. Generated logs/ remain untracked. This packet remains excluded via the unchanged /docs/input.md rule.
- No subagent-owned Gradle/Java task remains running. No Minecraft server/client was launched and no EULA was accepted.

Read-only findings to resume from, not implementation approval:

- Exact 0.6.8-alpha ResourceLocation source/bytecode shows a potentially decisive wire-representation conflict: the two two-argument values ("ab", "cd:ef") and ("ab:cd", "ef") compare unequal but both stringify as "ab:cd:ef". A one-character namespace also does not survive the one-string constructor because splitObjectName adopts the namespace only when the first colon index is greater than 1. No executable JUnit/ByteBuf fixture or Pro decision for this issue was produced before pause. Do not silently restrict valid existing IDs or change the frozen wire format to conceal it.
- A verified available C:/GradleCaches MCP binary is C:/GradleCaches/caches/unimined/net/minecraft/minecraft/1.12.2/Cleanroom-FG3/0.6.8-alpha/mcp-stable-39-1.12-searge-1.12.2-20260220.202731/forge-0.6.8-alpha-Cleanroom-FG3+cleanroom-0.6.8-alpha-mcp.jar, SHA-256 B00D3C0114F21542F599B47148F04DB31DDFCBBFE47DDB4C4552CAE9EEB73BEB. Root verified its existence/hash. An earlier explorer statement that this C:/GradleCaches tree did not exist was incorrect and is superseded. The separate IDE source/binary pair under C:/Users/Administrator/.gradle was also inspected; do not equate the two artifacts without checking actual compile/test classpath provenance.
- Static client facts: Minecraft implements IThreadListener; addScheduledTask queues off-thread calls and executes on the client loop; ClientTick END is posted through MinecraftForge.EVENT_BUS; network connected/disconnected events are posted from NetworkDispatcher and have no client-main-thread guarantee; WorldClient/Entity dimension-ID-UUID lookup APIs exist. net.minecraftforge.fml.common.SidedProxy is the verified annotation package. These are static facts, not live-runtime proof.
- Static network facts: handler-instance registration is available; the indexed codec constructs messages via public getConstructor().newInstance(); fromBytes receives the post-discriminator slice and must enforce its own exact exhaustion/all-or-nothing decode. StartTracking insertion order, login/respawn/dimension posting chains and the selected server-thread assertion still require final provenance-backed confirmation.
- No approved authoritative dedicated-server instance, EULA-ready test instance, independent GPU host/client directory, endpoint, handoff or runtime evidence was established. Developer run configurations are not acceptance instances.

First work after explicit resume: inspect this checkpoint against the actual worktree; independently review/verify the holder slice; reproduce the ResourceLocation ambiguity against the actual build classpath and consult the designated Pro before deciding any conflicting protocol contract; then continue the remaining Phase 2B requirements without restarting W108 or broadening scope.

Real GPU acceptance: not executed

EXECUTION REVISION — 2026-09-04

The user authorized this incremental operational update and resumption of Phase 2B.
Core functional, public API, geometry-freeze, wire-format, ordering, limits, and test specifications below are unchanged.
This revision updates only starting-state accounting, runtime acceptance, cross-node evidence, Definition of Done, and Git closeout, including their corresponding report fields.
The earlier standby restriction is lifted for this Phase 2B task only; CrlOzzAPI design/integration/migration and Phase 2C remain outside scope.

W108 IMPLEMENTATION AUTHORIZATION — 2026-09-05

The user has authorized ordinary continued project development and owner-decided local commits, with unresolved technical conditions discussed with the designated Pro advisor. The reviewed W108 plan is authorized as a separate core prerequisite repair before Phase 2B networking resumes.

This addendum supersedes only the conflicting reconstruction/freeze/test/commit prerequisites below:

- Permit one new public Rotation3d.reconstructExact(double,double,double,double) entry and one package-private ExactRotationReconstruction helper under api/geometry. Preserve the old public normalizer, fields, equality/hash and all query arithmetic; every other existing geometry production file remains byte-frozen.
- Use the exact constructor-image W108 contract in docs/EXACT_ROTATION_RECONSTRUCTION_DESIGN.md. No approximate equality, domain expansion, private bit injection or unproved heuristic enumeration is allowed.
- Rotation decoding reads the same four stored binary64 fields and calls Rotation3d.reconstructExact, whose geometry-owned implementation finds and verifies a bounded legacy-constructor witness. It does not call the raw normalizer directly on the transmitted tuple. All other wire field order, limits and value-equality requirements are unchanged.
- Keep the old (1,1,3,2) direct-constructor failure fact, both z-bit patterns and offsets 23/72 as explicitly named passing legacy negative regressions. Add independent exact-success tests for the new entry; do not hide, disable or relabel the old behavior as fixed.
- A reviewed, passing W108 core repair may be committed independently after applicable automated gates. Such a commit is not the Phase 2B network feature commit and does not assert server/client acceptance. After it lands, record its exact HEAD and test baseline before starting networking; retain all original 285 tests plus the new reconstruction tests.
- The Phase 2B feature commit/completion still requires its own full implementation and applicable dedicated-server/independent-GPU evidence. Preserve existing unrelated dirty files; no commit-count target authorizes rewriting history or staging unrelated changes.

No published Phase 2B v1 implementation is present in this repository. Any discovered external deployment or divergent format must be assessed before choosing protocol compatibility changes; the W108 repair does not silently claim external deployment facts.

W108 CORE CHECKPOINT — 2026-09-05

The separate core prerequisite is now locally committed on main:

- Commit: 1fc4e8700a325be87b89c0d6ac37bf76ad691531 — feat(core): add exact rotation reconstruction.
- Parent / historical Phase 2A baseline: 4ba9a496000e9433fc1a4fefeb0e44a51fee491b. Use the W108 commit as the next implementation starting point; the older EXPECTED STARTING STATE below remains historical context.
- Final automated baseline: 34 suites / 300 tests / 0 failures / 0 errors / 0 skipped. Independent clean geometry-isolation/test/build passed; the final test-only legacy-control strengthening was then independently compiled and tested in the full suite, with build/JAR unchanged. Both numerical and code reviews, plus the final evidence audit, closed at P0/P1/P2 = 0/0/0.
- Artifact: build/libs/crlhitbox-0.1.0-SNAPSHOT.jar, 74,454 bytes, SHA-256 F7BB3A1CECAF1CBA70E389CE3DDE99B0C45CCF433B923531C9B5B6905D7816EE. The source is the development checkout's reviewed W108 snapshot, not a server/client deployment.
- Read docs/W108_RECONSTRUCTION_VERIFICATION.md for exact inputs, commands, independent review, historical red/green epochs, artifact identity and same-harness resource measurements. The old direct N(q) one-ULP mismatch remains explicitly tested; Rotation3d.reconstructExact is the approved decoder boundary.
- The pre-existing 35-line AGENTS.md topology hunk and .codex/config.toml were deliberately not included in the core commit. This input.md remains local-only and excluded from Git; do not stage it or use broad cleanup.
- Phase 2B networking itself is still incomplete: no production codec/message, holder generation/replacement, tracking sender, client installer or pending/lifecycle implementation has been added by W108. Next work resumes those original functional requirements with the approved rotation boundary, after refreshing exact Cleanroom 0.6.8-alpha source/class provenance.
- Pure-core throughput observations are not whole-packet resource acceptance. Actual decoder/maximum-payload/CPU/allocation/GC budgets, dedicated-server and correlated independent-GPU evidence remain open. No Minecraft client/server was launched, no EULA was accepted, and no push/tag/release occurred.

Real GPU acceptance: not executed

ROLE

You are the local L0 repository owner for CRL Hitbox.

You own:

- Repository inspection.
- Cleanroom/Forge 0.6.8 source validation.
- Implementation.
- Public API compatibility.
- Wire-format correctness.
- Client/server threading correctness.
- Entity identity and replica-state correctness.
- Malformed-payload resistance.
- Unit, deterministic property, bytecode, build, and controlled runtime-smoke validation.
- Documentation.
- Independent read-only review coordination.
- Git history and worktree safety.

Repository truth comes only from:

1. The current target repository:
   D:\WI - Dev Workspace\CrlHitbox-src

2. Applicable AGENTS.md and AGENTS.override.md files.

3. The read-only local template:
   D:\WI - Dev Workspace\CleanroomModTemplate

4. Exact local Cleanroom 0.6.8 / Forge 1.12.2 source and mapped development classes.

5. Reproducible local source, bytecode, test, build, JAR, Git, and controlled runtime-smoke output.

The facts in this packet are expected handoff facts. Verify every material fact locally before relying on it.

PHASE GOAL

Implement the first server-authoritative network replication slice for Entity hitbox holders.

Add:

1. A versioned, bounded, direct-binary full-snapshot wire format.
2. A server-local holder generation/incarnation.
3. Full snapshot sending to a player when that player starts tracking an Entity.
4. Explicit full-snapshot resend APIs for current tracking players and a specific recipient.
5. Self snapshots for player login, respawn, and dimension transitions where exact 0.6.8 lifecycle events support them.
6. Client network-thread to client-main-thread handoff.
7. Atomic replacement of client holder contents.
8. Client stale-generation and stale-revision rejection.
9. Bounded pending snapshots for the rare case where the target client Entity is not yet available.
10. Client world/disconnection cleanup.
11. Direct binary codecs for every existing Solid3d and PlacedSolid3d representation.
12. Tests for codec determinism, malformed inputs, generation/revision ordering, tracking triggers, pending delivery, thread boundaries, and server-safe class linkage.
13. One verified Phase 2B feature commit.

This phase must not implement:

- Delta packets.
- A resync-request C2S packet.
- Automatic mutation observation.
- Entity-to-world pose extraction.
- World-space hitbox caching.
- Rendering.
- F3+B integration.
- Combat.
- Hit/hurt roles.
- Damage.
- Persistence.
- Continuous collision.
- Certified geometry.

EXPECTED STARTING STATE

The Phase 2A implementation baseline remains the following HEAD. Historical test/artifact results below must be reproduced or identified explicitly as historical evidence, not current-run passes.

- Canonical target:
  D:\WI - Dev Workspace\CrlHitbox-src

- Canonical read-only template:
  D:\WI - Dev Workspace\CleanroomModTemplate

- Branch:
  main

- Exact HEAD:
  4ba9a496000e9433fc1a4fefeb0e44a51fee491b

- Existing commits, oldest first:

  9fba3f6cf81d3a8c916b94affcd3c313816a6b85
  chore: establish CRL Hitbox 0.6.8 baseline

  940c09166332489c0e89b8e86fb424c943db74b2
  feat(core): add immutable geometry foundation

  399db8b1ba051e078f0931480d62c39b9325e25a
  feat(core): complete primitive intersection matrix

  31fbccfd46c21ff5f622ae293b580839d175202f
  feat(core): add composite solid dispatch

  27bc0734be9434253772311da7fa949c03d433e2
  feat(core): add rigid transform foundation

  8f1754be6c1e287c0692c78faa26d3dd9725e7c7
  feat(core): add rigidly placed solid queries

  4ba9a496000e9433fc1a4fefeb0e44a51fee491b
  feat(platform): add entity hitbox capability

- Index/worktree:
  - index clean at this execution revision
  - pre-existing unstaged AGENTS.md topology addition (35 added lines)
  - pre-existing untracked .codex/config.toml
  - preserve and separately account for both; neither is Phase 2B implementation output
  - AGENTS.md pre-existing SHA-256:
    17A1A8B268F67EEE420CF83F63B488210052B49FD35C48D1FB5000FFB0FEF5EF
  - .codex/config.toml pre-existing SHA-256:
    47599E564E50DAE50B0875FB551A27012F43FC85B2A13CFA16B6A0D6632A040D
  - re-read current status before every implementation or staging boundary; record further drift rather than assuming this snapshot remains current

- Remotes:
  none

- Tags:
  none

- Identity:
  - Maven group: dev.crlhitbox
  - Java base package: dev.crlhitbox
  - Main class: dev.crlhitbox.CrlHitbox
  - Mod ID: crlhitbox
  - Artifact ID: crlhitbox
  - Version: 0.1.0-SNAPSHOT

- Platform floor:
  - Minecraft 1.12.2
  - Cleanroom Loader 0.6.8-alpha
  - MCP stable / 39-1.12
  - Java 25
  - Gradle wrapper 9.6.1
  - Unimined 1.4.27-kappa
  - JUnit Jupiter 6.0.3

- Public geometry API:
  - exactly 14 top-level types
  - exactly 6 GeometryDistances methods
  - exactly 30 GeometryIntersections overloads

- Solid3d permits exactly:
  - Aabb
  - Sphere
  - Obb
  - Capsule
  - Composite

- Public entity API:

  public final class EntityHitboxHolder {
      public EntityHitboxHolder();
      public long revision();
      public int size();
      public boolean isEmpty();
      public Optional<PlacedSolid3d> find(ResourceLocation id);
      public boolean put(ResourceLocation id, PlacedSolid3d localPlacement);
      public boolean remove(ResourceLocation id);
      public boolean clear();
      public EntityHitboxSnapshot snapshot();
  }

  public final class EntityHitboxSnapshot {
      // no public constructor
      public long revision();
      public int size();
      public boolean isEmpty();
      public ResourceLocation id(int index);
      public PlacedSolid3d placement(int index);
      public Optional<PlacedSolid3d> find(ResourceLocation id);
      public boolean equals(Object other);
      public int hashCode();
      public String toString();
  }

  public final class EntityHitboxes {
      public static Optional<EntityHitboxHolder> find(Entity entity);
      public static EntityHitboxHolder require(Entity entity);
  }

- Holder semantics:
  - ResourceLocation → PlacedSolid3d
  - deterministic insertion order
  - unequal replacement preserves position
  - equal replacement is a no-op
  - remove/re-add appends
  - revision starts at 0
  - effective mutations increment exactly once
  - no-op operations do not increment
  - revision overflow fails before mutation

- Snapshot semantics:
  - immutable defensive capture
  - ordered IDs and placements
  - equality includes revision and ordered entries
  - O(1) indexed access
  - O(n) find
  - safe cross-thread handoff after construction

- Capability:
  - type: EntityHitboxHolder
  - attachment key: crlhitbox:entity_hitboxes
  - provider owns a fresh holder
  - provider implements ICapabilityProvider only
  - non-sided capability access
  - no Entity or World back-reference
  - no global holder registry
  - inert capability storage
  - non-persistent

- Current side/frame state:
  - server and client holders are independent side-local state
  - no synchronization exists
  - holder parent frame is an abstract caller-defined entity-local frame
  - no entity-to-world adapter exists
  - no yaw/pitch/body/head/model/bone convention exists

- Existing tests:
  - historical Phase 2A result, to be rerun before production changes
  - 285 total
  - 0 failures
  - 0 errors
  - 0 skipped
  - 0 disabled

- Existing cumulative deterministic randomized iterations:
  33,792

- Existing Phase 2A seeds:
  - 0x5EED_2A01L
  - 0x5EED_2A02L
  - 0x5EED_2A03L
  - 2,048 iterations each

- Existing geometry isolation task:
  compileGeometryIsolation

- Existing primary artifact:
  D:\WI - Dev Workspace\CrlHitbox-src\build\libs\crlhitbox-0.1.0-SNAPSHOT.jar

- Existing artifact report:
  - size: 69,068 bytes
  - SHA-256:
    8D9D7CD5C5AA0A0FCB7E82114F28028A5B4EA498DCB8136F395BD84CA8DED5E1

Verify every material fact locally before writing.

If the repository has drifted:

- Preserve every user change.
- Do not reset, clean, discard, force checkout, or overwrite it.
- Record the exact drift.
- Continue only when it can be reconciled without destroying work.
- Otherwise report PHASE 2B INCOMPLETE with the exact blocker.

KNOWN EVIDENCE BOUNDARY

Phase 2A did not execute a real game-container Entity capability lifecycle.

Its evidence consisted of:

- exact Cleanroom 0.6.8 source
- direct AttachCapabilitiesEvent unit testing
- classfile and constant-pool auditing
- Java 25 unit testing
- complete build and JAR inspection

Do not rewrite this as live runtime proof.

Phase 2B must preserve that distinction.

LOCAL TASK-PACKET STATE

The Phase 2A report states that docs/input.md was local-only and excluded through .git/info/exclude.

The reported Phase 2A packet state was:

- path:
  docs/input.md

- size:
  63,420 bytes

- SHA-256:
  0A19DD9D7508A7730A356711F70A21BAEAD4ACCAA7A6C15DD59865D385CC252F

The user may have replaced it with this Phase 2B packet.

Inspect the actual current state.

Do not:

- Commit a task packet.
- Add it to tracked .gitignore.
- Delete or move it.
- Change .git/info/exclude without explicit user authorization.
- Assume the previous size or hash still applies.

Report the current path, size, and SHA-256.

PRE-WRITE SAFETY GATE

Before modifying any target file:

1. Read every applicable AGENTS.md and AGENTS.override.md.
2. Resolve and record canonical target and template paths.
3. Confirm target and template remain distinct sibling repositories.
4. Inspect:
   - git status --short --branch
   - git status --porcelain=v1 --untracked-files=all
   - git rev-parse HEAD
   - git log --oneline --decorate
   - git diff
   - git diff --cached
   - git remote -v
   - git tag
5. Confirm HEAD is the expected Phase 2A commit or document exact drift.
6. Inspect all entity API and internal capability implementation files.
7. Inspect all geometry production files and record hashes.
8. Inspect all existing tests and classfile-audit helpers.
9. Inspect:
   - CrlHitbox.java
   - README.md
   - AGENTS.md
   - docs/BASELINE.md
   - docs/ENTITY_HOLDER_SEMANTICS.md
   - build.gradle
   - gradle.properties
   - local .git/info/exclude
10. Inspect exact local Cleanroom 0.6.8 definitions and call paths for:
    - SimpleNetworkWrapper
    - NetworkRegistry.newSimpleChannel
    - IMessage
    - IMessageHandler
    - MessageContext
    - SimpleIndexedCodec
    - FMLIndexedMessageToMessageCodec message construction
    - PlayerEvent.StartTracking
    - EntityTrackerEntry tracking order
    - EntityPlayerMP
    - World and WorldClient entity lookup
    - IThreadListener.addScheduledTask
    - FMLCommonHandler buses
    - PlayerLoggedInEvent
    - PlayerRespawnEvent
    - PlayerChangedDimensionEvent
    - ClientTickEvent
    - FMLNetworkEvent.ClientConnectedToServerEvent
    - FMLNetworkEvent.ClientDisconnectionFromServerEvent
    - WorldEvent.Load/Unload
    - EntityJoinWorldEvent, only if considered
    - SidedProxy and SideOnly, if used
11. Confirm every selected API exists at the 0.6.8 baseline.
12. Record template initial HEAD/status without modifying it.
13. Run the full Phase 2A regression suite before production changes.

Required baseline command:

.\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain

All existing 285 tests must execute and pass before Phase 2B production code begins.

If the baseline does not reproduce:

- Diagnose the exact cause.
- Do not weaken, skip, or rewrite tests.
- Do not upgrade platform/build pins.
- Do not begin Phase 2B implementation until the discrepancy is understood.

HARD PLATFORM BASELINE

Preserve exactly:

- Minecraft 1.12.2.
- Cleanroom Loader 0.6.8-alpha.
- MCP stable 39-1.12.
- Java 25.
- Gradle wrapper 9.6.1.
- Unimined 1.4.27-kappa.
- Existing plugin pins.
- Existing project identity and version.

Do not:

- Upgrade Cleanroom.
- Upgrade Forge APIs.
- Upgrade Gradle.
- Upgrade Unimined.
- Upgrade mappings.
- Add a dynamic dependency version.
- Add NeoForge networking.
- Add modern SimpleChannel APIs.
- Add CustomPacketPayload.
- Add StreamCodec.
- Add LazyOptional.
- Add CapabilityToken.
- Add a runtime dependency.
- Modify the authoritative template.

Use only the exact Forge/Cleanroom 0.6.8 networking and lifecycle facilities available locally.

GEOMETRY CORE FREEZE

No tracked production file beneath:

src/main/java/dev/crlhitbox/api/geometry

may be modified.

Before implementation:

- Record SHA-256 for every geometry production source.
- Record public 14/6/30 closure.
- Record Solid3d permits.
- Record geometryIsolation class inventory.

After implementation:

- Confirm every geometry source hash is unchanged.
- Confirm public geometry API remains 14/6/30.
- Confirm Solid3d permits remain unchanged.
- Confirm compileGeometryIsolation still reports only java.base.

Do not add wire factories, trusted constructors, codecs, serializers, IDs, or protocol hooks to geometry.

The wire codec must use existing public immutable constructors.

If existing geometry constructors cannot reproduce a server-generated canonical value exactly enough to satisfy the frozen round-trip tests, stop and report the precise representation blocker. Do not modify geometry or weaken exact value semantics inside Phase 2B.

FROZEN NETWORK ARCHITECTURE

Phase 2B establishes:

- Server-authoritative full snapshots.
- One server-local generation per attached Entity provider/incarnation.
- One source-holder revision per full snapshot.
- Direct binary wire encoding.
- StartTracking-triggered delivery.
- Explicit server resend.
- Client-main-thread atomic installation.
- Stale full-snapshot rejection.
- Bounded pending delivery for a not-yet-resolved client Entity.

It does not establish:

- Delta history.
- Base-revision patches.
- Client resync requests.
- Acknowledgements.
- Mutation listeners.
- Automatic update packets after every holder mutation.
- Persistence.
- Cross-server generation identity.

SERVER-AUTHORITY CONTRACT

Only the server sends synchronization messages.

Required:

- The network channel registers no C2S message.
- No handler is registered for Side.SERVER.
- No public API sends to the server.
- No client holder mutation can reach the server.
- Server full snapshots overwrite client holder contents when accepted.
- Client holder mutation remains technically possible through the existing API but is local-only and may be overwritten by the next accepted server full snapshot.
- Server authority applies to this replication channel only; Phase 2B does not prohibit local client experimentation.

Do not add fake two-way synchronization.

PUBLIC API DELTA

Modify EntityHitboxHolder by adding exactly one public method:

public boolean replaceContents(
        EntityHitboxSnapshot snapshot
);

After Phase 2B, its exact public method surface is:

public final class EntityHitboxHolder {

    public EntityHitboxHolder();

    public long revision();

    public int size();

    public boolean isEmpty();

    public Optional<PlacedSolid3d> find(
            ResourceLocation id
    );

    public boolean put(
            ResourceLocation id,
            PlacedSolid3d localPlacement
    );

    public boolean remove(
            ResourceLocation id
    );

    public boolean clear();

    public boolean replaceContents(
            EntityHitboxSnapshot snapshot
    );

    public EntityHitboxSnapshot snapshot();
}

Add exactly one new stable public entity API type:

package dev.crlhitbox.api.entity;

public final class EntityHitboxSync {

    public static void sendFullTo(
            Entity entity,
            EntityPlayerMP recipient
    );

    public static void sendFullToTrackingAndSelf(
            Entity entity
    );
}

Required EntityHitboxSync properties:

- final utility class
- no public constructor
- exactly two public static methods
- no public fields
- no channel accessor
- no message accessor
- no protocol-state accessor
- no generation accessor
- no client method
- no C2S method
- no codec method
- no collection return

After Phase 2B:

- Stable public types under dev.crlhitbox.api.entity are exactly:
  1. EntityHitboxHolder
  2. EntityHitboxSnapshot
  3. EntityHitboxes
  4. EntityHitboxSync

- EntityHitboxHolder has exactly:
  - one public no-arg constructor
  - nine declared public methods

- EntityHitboxSnapshot remains:
  - no public constructor
  - nine declared public methods

- EntityHitboxes remains:
  - no public constructor
  - exactly two public static methods

- EntityHitboxSync has:
  - no public constructor
  - exactly two public static methods

Do not introduce a stable public type for:

- protocol version
- packet/message
- codec
- generation
- replica status
- pending snapshot
- network channel
- entry DTO
- decoded result
- client installer
- transport
- acknowledgement
- resync request

REPLACECONTENTS SEMANTICS

replaceContents replaces the holder’s ordered entry contents atomically.

It does not adopt snapshot.revision().

The source snapshot revision is descriptive of the source holder. The receiving holder keeps its own local monotonic mutation revision.

Required behavior:

1. Reject null snapshot before any state access or mutation.
2. Compare current ordered IDs and placements with the snapshot’s ordered IDs and placements.
3. If contents and order are exactly equal:
   - return false
   - do not increment local holder revision
   - ignore any difference in source snapshot revision
4. If contents or order differ:
   - construct the complete replacement storage before changing holder state
   - validate duplicate IDs defensively
   - calculate checked next local revision before mutation
   - replace storage in one state transition
   - increment local revision exactly once
   - return true
5. An empty snapshot clears a nonempty holder in one effective mutation.
6. Replacing an empty holder with empty contents is a no-op.
7. At local revision Long.MAX_VALUE:
   - a content-changing replacement throws IllegalStateException
   - entries and revision remain unchanged
   - an equal-content replacement remains a legal no-op
8. Preserve snapshot order exactly.
9. Preserve exact immutable PlacedSolid3d values.
10. Do not sort.
11. Do not derive holder revision from source snapshot revision.
12. Do not invoke networking from replaceContents.

This method is a general atomic content replacement operation. It must not contain client-only or protocol-specific behavior.

WIRE REVISION VERSUS CLIENT HOLDER REVISION

Distinguish these values explicitly:

1. Server wire revision:
   - source EntityHitboxSnapshot.revision()
   - carried in the full message
   - used for stale-message ordering
   - stored in internal client replica state

2. Client holder local revision:
   - maintained by EntityHitboxHolder
   - increments once when replaceContents changes entries
   - may differ from the server wire revision
   - is not overwritten by a packet

The client replica state must also remember:

- local holder revision immediately after the last successful install

This enables a future delta phase to detect client-local modification before applying a patch.

Do not expose this replica state publicly in Phase 2B.

INTERNAL HOLDER GENERATION

Add an internal, non-persistent synchronization state attached through the existing per-Entity provider.

Every provider/incarnation receives one positive process-local generation.

Required generation properties:

- type:
  long

- valid range:
  positive nonzero values

- assigned once per provider instance

- immutable for that provider

- distinct for provider instances until allocator exhaustion

- monotonically allocated within one physical JVM process

- no random UUID

- no wall-clock time

- no Entity ID derivation

- no Entity UUID derivation

- no persistence

- no NBT

- no public getter

- no global Entity map

- no retained Entity or World reference

A process-global AtomicLong-style allocator is permitted solely for generation allocation.

It must:

- retain no Entity, holder, provider, UUID, World, or snapshot reference
- fail before returning a nonpositive or wrapped value
- never silently wrap
- be independently tested near exhaustion using a test-only permitted inspection technique
- not become a general object registry

Generation lifetime:

- A new provider receives a new generation.
- Replacing an Entity object therefore creates a new generation.
- A physical server restart may restart the allocator.
- Client connection/disconnection cleanup makes generation ordering session-local.
- Phase 2B does not claim globally unique or persistent generations.

INTERNAL REPLICA STATE

Extend the internal capability provider so it owns:

- the existing EntityHitboxHolder
- one internal synchronization/replica state

The internal state must contain, conceptually:

- final localGeneration
- acceptedRemoteGeneration
- acceptedRemoteRevision
- holderLocalRevisionAtLastInstall
- whether a remote snapshot has been accepted

The provider may expose this state only through an internal capability or another exact internal 0.6.8-compatible access seam.

Required:

- No public stable access.
- No serialization.
- No Entity/World field.
- No static map keyed by Entity.
- Same provider returns the same internal state.
- Different providers return different internal states.
- Holder capability behavior remains unchanged.
- Attachment key remains:
  crlhitbox:entity_hitboxes
- One attachment provider may serve both internal and public capabilities.
- Capability registration for every supported capability occurs before the attachment listener is registered.

Do not put network replica fields into EntityHitboxHolder.

FULL-SNAPSHOT ACCEPTANCE ORDER

For a fully decoded message targeting an existing matching Entity:

Let:

- incoming generation = G
- incoming server revision = R
- previously accepted generation = G0
- previously accepted server revision = R0

Required acceptance:

- no previous accepted snapshot:
  accept

- G > G0:
  accept, regardless of whether R is lower than R0

- G < G0:
  reject as stale

- G == G0 and R < R0:
  reject as stale

- G == G0 and R == R0:
  accept as an idempotent authoritative reassertion

- G == G0 and R > R0:
  accept

After a successful install:

- acceptedRemoteGeneration = G
- acceptedRemoteRevision = R
- holderLocalRevisionAtLastInstall = holder.revision() after replaceContents

Do not update replica state before holder replacement succeeds.

A same-generation/same-revision full snapshot is deliberately accepted so that it can repair a client-local mutation.

A full snapshot with a newer generation replaces the old incarnation even when its source revision is smaller.

Do not use unsigned long comparison. All accepted generation/revision values are validated as nonnegative or positive before comparison.

ENTITY IDENTITY ON THE WIRE

Each full snapshot identifies its target using all of:

- signed dimension ID
- nonnegative runtime entity ID
- Entity UUID most-significant bits
- Entity UUID least-significant bits
- provider generation

Client installation requires:

- current client world dimension equals message dimension
- resolved entity ID exists
- resolved entity UUID exactly equals message UUID
- CRL Hitbox holder capability exists
- CRL Hitbox internal replica capability exists

Never apply a packet based on entity ID alone.

If an Entity currently exists under that ID but its UUID differs:

- reject the message
- do not apply it to the new Entity
- do not queue it for the mismatched Entity

Do not use world name.

Do not use entity class name as identity.

NETWORK CHANNEL

Use exactly:

crlhitbox

as the SimpleNetworkWrapper channel name.

Register exactly one message in Phase 2B:

- discriminator:
  0

- direction:
  Side.CLIENT

- semantic name:
  full Entity hitbox snapshot

No other discriminator is authorized.

Do not call sendToServer.

Do not register a Side.SERVER handler.

Do not add a handshake packet.

Do not add a C2S resync request.

PROTOCOL VERSION

Use exactly:

1

as the first byte of the message payload after the SimpleNetworkWrapper discriminator.

The decoder must reject any other version before decoding snapshot entries.

The protocol version is documented but remains internal implementation state in Phase 2B.

Do not use project/mod semantic version as the wire protocol version.

Do not use Java serialization version IDs.

EXACT WIRE FRAME

Use the following deterministic field order.

All fixed-width numeric values use the ByteBuf’s ordinary network byte order.

Message payload, excluding the SimpleNetworkWrapper discriminator:

1. protocolVersion
   - unsigned byte
   - required value: 1

2. dimensionId
   - signed 32-bit int

3. entityId
   - nonnegative VarInt
   - maximum encoded width: 5 bytes

4. entityUuidMost
   - signed 64-bit long containing raw UUID bits

5. entityUuidLeast
   - signed 64-bit long containing raw UUID bits

6. holderGeneration
   - signed 64-bit long
   - must be > 0

7. serverRevision
   - signed 64-bit long
   - must be >= 0

8. entryCount
   - nonnegative VarInt

9. entry records in EntityHitboxSnapshot insertion order

Each entry record:

1. ResourceLocation ID
2. Solid3d payload
3. RigidTransform3d payload

A PlacedSolid3d is therefore encoded as:

- its local Solid3d
- then its localToParent transform

Do not reorder entries.

Do not sort IDs.

Do not encode Java class names.

Do not encode enum ordinals.

RESOURCELOCATION ENCODING

Encode the canonical ResourceLocation.toString() UTF-8 bytes as:

1. nonnegative VarInt byte length
2. exact UTF-8 bytes

Required:

- length must be at least 1
- length must not exceed MAX_RESOURCE_LOCATION_BYTES
- UTF-8 decoding must use malformed-input reporting, not replacement characters
- decoded text must be accepted by the exact 1.12.2 ResourceLocation constructor
- duplicate IDs in one snapshot are rejected
- a decoded ID is canonicalized only by ResourceLocation’s existing semantics
- encode/decode of a valid existing ResourceLocation must return an equal value

Do not use Java modified UTF.

Do not use NBT strings.

Do not use platform-default charset.

SOLID TYPE TAGS

Freeze these stable protocol tags:

0 = Aabb
1 = Sphere
2 = Obb
3 = Capsule
4 = Composite

Do not derive tags from:

- class name
- enum ordinal
- hash code
- sealed permits order
- reflection order

Unknown tags are decoder errors.

PRIMITIVE FIELD ENCODING

Every double is written and read as one IEEE-754 binary64 value using ByteBuf double/raw-long semantics.

Aabb tag 0:

- min.x
- min.y
- min.z
- max.x
- max.y
- max.z

Sphere tag 1:

- center.x
- center.y
- center.z
- radius

Obb tag 2:

- center.x
- center.y
- center.z
- halfExtents.x
- halfExtents.y
- halfExtents.z
- orientation.x
- orientation.y
- orientation.z
- orientation.w

Capsule tag 3:

- centerline.start.x
- centerline.start.y
- centerline.start.z
- centerline.end.x
- centerline.end.y
- centerline.end.z
- radius

Composite tag 4:

- primitiveLeafCount as a nonnegative VarInt
- each canonical primitive leaf in encounter order:
  - one tag in range 0 through 3
  - corresponding primitive payload

A Composite payload must not contain tag 4.

This follows the existing canonical flat primitive-leaf contract.

Do not encode:

- Composite grouping history
- bounds
- cached values
- type names
- entry IDs inside Composite
- child transforms
- metadata
- callbacks

RIGIDTRANSFORM3D ENCODING

Encode:

1. rotation.x
2. rotation.y
3. rotation.z
4. rotation.w
5. translation.x
6. translation.y
7. translation.z

Decode using the existing public constructors:

- new Rotation3d(...)
- new Vec3d(...)
- new RigidTransform3d(...)

Do not add a trusted wire constructor to geometry.

Do not skip existing normalization, canonicalization, or finite checks.

ROUND-TRIP REQUIREMENT

For every valid server-generated entry and snapshot:

- IDs preserve exact ResourceLocation equality.
- Aabb endpoints preserve exact value equality.
- Sphere, Obb, Capsule, Composite, RigidTransform3d, and PlacedSolid3d preserve exact existing value equality.
- Entry order is unchanged.
- Server wire revision is preserved independently from the decoded temporary snapshot’s local revision.
- Encoding the successfully decoded message again produces identical payload bytes.

The decoded temporary EntityHitboxSnapshot may have a different internal local snapshot revision because it is reconstructed through a temporary holder. That local revision is not the wire revision and must not be used for stale ordering.

Compare:

- wire revision separately
- ordered decoded IDs and placements exactly

If an existing public geometry constructor prevents exact convergence for a valid canonical server-generated value:

- preserve the failing fixture
- do not modify geometry
- do not add an unsafe backdoor
- report PHASE 2B INCOMPLETE with the exact type/components and bit-level discrepancy

PROTOCOL LIMITS

Freeze these limits:

MAX_MESSAGE_BYTES =
    1,048,576

MAX_ENTRIES =
    4,096

MAX_RESOURCE_LOCATION_BYTES =
    1,024

MAX_COMPOSITE_LEAVES =
    4,096

MAX_TOTAL_PRIMITIVE_LEAVES =
    16,384

These limits are internal protocol limits, not holder limits.

Required:

- Validate message byte size before count-driven allocation.
- Validate every count before allocating storage.
- Track total primitive leaves across the whole message.
- A primitive local solid contributes one leaf.
- A Composite contributes its canonical primitive child count.
- Reject zero-leaf Composite payloads.
- Reject nested Composite tags.
- Reject duplicate entry IDs.
- Reject trailing unread bytes.
- Reject truncated data.
- Reject negative counts.
- Reject overlong VarInts.
- Reject unknown tags.
- Reject non-finite geometry through existing constructors.
- Reject invalid radii/extents/Aabb ordering.
- Reject unrepresentable mandatory bounds.
- Reject generation <= 0.
- Reject revision < 0.
- Reject entity ID < 0.
- Never allocate an array/list/map using an unvalidated peer count.
- Never partially install a decoded snapshot.

Do not add compression.

Do not split a full snapshot into multiple packets.

A server snapshot that exceeds the Phase 2B limits is unsendable and must fail before transmission.

Fragmentation/chunking is a separately gated future protocol change.

ENCODER BEHAVIOR

A server-side full-message factory must capture, once:

- dimension ID
- entity ID
- Entity UUID
- provider generation
- one immutable EntityHitboxSnapshot

The message must remain self-consistent if the holder is mutated after capture.

Before transmission:

- validate all counts and IDs
- validate protocol budgets
- calculate or verify encoded size
- fail before channel send when limits are exceeded

The public explicit send method must surface an actionable exception for an unsendable snapshot.

Automatic tracking/lifecycle handlers must:

- catch this local server-state encoding failure
- log one actionable error containing entity ID, UUID, generation, revision, entry count, and reason
- send nothing
- not crash the event bus or server tick
- not log ordinary successful sends

Do not silently truncate entries.

Do not silently drop only the oversized entry.

DECODER BEHAVIOR

The IMessage decoder must parse into local temporary state.

Required:

- Message class has the public no-arg constructor required by the exact 0.6.8 indexed codec.
- The message class may be technically public under an internal package but is not stable public API.
- It has no public mutable fields.
- Parsing is all-or-nothing.
- Assign decoded message fields only after the complete payload is valid.
- Use a duplicate/slice or equivalent so failed decoding cannot expose a partially initialized message.
- Throw a decoder/runtime protocol exception on malformed input.
- Do not catch and continue with a partial snapshot.
- Do not mutate an Entity, holder, capability, world, or pending queue during fromBytes.
- Require exact payload exhaustion.

The handler must never receive an incompletely decoded snapshot.

NO NBT WIRE FORMAT

Do not use:

- NBTTagCompound
- NBTTagList
- NBTTagDouble
- ByteBufUtils.writeTag
- ByteBufUtils.readTag
- INBTSerializable
- Java ObjectOutputStream
- Gson
- JSON
- DataFixer

The existing capability storage remains inert.

The full snapshot protocol is direct binary.

INTERNAL MESSAGE/TRANSPORT VISIBILITY

Because exact Forge 1.12.2 indexed decoding constructs message classes through a public no-arg constructor, the message implementation may require public JVM visibility.

Required policy:

- Place it under dev.crlhitbox.internal.network.
- Document it as a technical Forge-instantiation type, not stable API.
- Do not expose it from dev.crlhitbox.api.entity.
- Do not expose its constructor accepting production data publicly unless exact Forge registration requires it.
- Prefer package-private factories/accessors for internal production use.
- Register a handler instance when that permits the handler class itself to remain package-private.

Do not broaden stable public API merely to satisfy reflection.

SERVER SEND API

EntityHitboxSync.sendFullTo(Entity, EntityPlayerMP):

- reject null entity
- reject null recipient
- require server-side Entity
- require server logical game thread
- require the public holder capability
- require the internal synchronization state
- capture one snapshot
- capture the provider generation
- construct one validated full message
- send only to the specified EntityPlayerMP using SimpleNetworkWrapper.sendTo
- do not require that the recipient currently tracks the Entity
- do not mutate the holder
- do not alter server revision
- do not schedule asynchronously

EntityHitboxSync.sendFullToTrackingAndSelf(Entity):

- reject null entity
- require server-side Entity
- require server logical game thread
- capture one immutable message
- send it to all current tracking players using SimpleNetworkWrapper.sendToAllTracking
- when entity is an EntityPlayerMP, also send directly to that player
- reuse the same captured generation/revision/contents for all recipients
- do not capture separately per recipient
- do not send to the server
- do not mutate the holder

If exact 0.6.8 APIs do not provide a safe main-thread assertion, document and enforce the server-thread caller contract through the strongest available local API rather than inventing a later-version method.

STARTTRACKING DELIVERY

Register a server/common event handler on the exact bus that receives:

net.minecraftforge.event.entity.player.PlayerEvent.StartTracking

Required:

- Target may be any Entity.
- Player must be an EntityPlayerMP before server send.
- Send directly only to the player entering tracking.
- Send a full snapshot even when the holder is empty.
- Do not broadcast the StartTracking snapshot to other players.
- Do not inspect Entity class.
- Do not filter to living entities or players.
- Do not scan the world.
- Do not use a tick loop.
- Do not use Mixin.
- Do not inject into EntityTrackerEntry.

The empty snapshot is meaningful: it establishes generation/revision and clears stale client contents.

SELF-SNAPSHOT LIFECYCLE

Inspect the exact local event buses and implement server-side self snapshots for:

- PlayerLoggedInEvent
- PlayerRespawnEvent
- PlayerChangedDimensionEvent

Use the exact 0.6.8 FML game-event bus where those events are posted.

Required:

- Act only when event.player is EntityPlayerMP.
- Send the current player Entity’s own full snapshot directly to that player.
- Preserve current non-persistence semantics:
  a respawn-created provider may hold an empty revision-0 holder.
- Do not copy old holder state.
- Do not add a PlayerEvent.Clone handler.
- Do not synthesize persistence.
- Duplicate same-generation/same-revision full packets are safe and accepted.

Do not add a logout packet.

Client disconnect cleanup handles session state.

NO AUTOMATIC MUTATION SYNC

EntityHitboxHolder.put/remove/clear/replaceContents must not automatically send packets.

Phase 2B behavior for an already tracked Entity is:

1. Mutate the server holder on the server thread.
2. Explicitly call:
   EntityHitboxSync.sendFullToTrackingAndSelf(entity)

Tracking entry remains automatic.

Phase 2C will separately define delta production and resynchronization.

Do not add hidden callbacks from holder mutations to an Entity or network channel.

NETWORK THREAD BOUNDARY

The SimpleNetworkWrapper handler runs on the network thread.

Required handler behavior:

1. Receive only a fully decoded immutable message.
2. Verify it is executing for Side.CLIENT.
3. Perform no:
   - world lookup
   - Entity lookup
   - capability access
   - holder mutation
   - pending-queue mutation
4. Submit one task to the client logical game thread.
5. Return null immediately.

The scheduled client-main-thread task performs all:

- world selection
- dimension checks
- entity lookup
- UUID checks
- capability access
- stale-order checks
- holder replacement
- pending-queue operations

Do not access Minecraft world state from fromBytes or the network handler.

CLIENT-ONLY LINKAGE BOUNDARY

All production code that directly imports any of the following must live beneath a clearly client-only internal package:

- net.minecraft.client.*
- net.minecraftforge.client.*
- WorldClient
- Minecraft.getMinecraft()
- client tick/render state

Recommended boundary:

dev.crlhitbox.internal.client

Common/server-safe classes may include:

- wire codec
- IMessage
- common IMessageHandler
- server sender
- tracking handler
- generation state
- common side-dispatch interface

Use exact Forge 1.12.2 sided indirection, such as a locally verified SidedProxy arrangement, when needed.

Required:

- Dedicated server must not load client implementation classes.
- Common handler must not have a CONSTANT_Class reference to a client-only CRL Hitbox implementation.
- A client implementation class name used as a SidedProxy string is not permission for direct linkage.
- Do not use Class.forName.
- Do not use MethodHandle lookup.
- Do not catch NoClassDefFoundError as side detection.
- Do not rely only on @SideOnly while hard-linking a client class from common code.

The server-side implementation of the client-dispatch seam must fail clearly if called unexpectedly, though the Side.CLIENT registration should make that path unreachable.

CLIENT TARGET RESOLUTION

On the scheduled client game thread:

1. Read the current client world.
2. If no world exists:
   queue the message as pending.
3. If current world dimension differs:
   queue the message under its target dimension.
4. Resolve entity by runtime entity ID.
5. If no entity exists:
   queue pending.
6. If an entity exists but UUID differs:
   reject as stale identity
   do not queue for that entity
7. Require public holder capability.
8. Require internal replica-state capability.
9. Apply generation/revision acceptance rules.
10. Atomically replace contents.
11. Update internal replica state only after successful replacement.

Do not create a replacement Entity.

Do not attach a capability manually.

Do not search every loaded Entity by UUID.

Do not apply a packet to a matching UUID under a different runtime entity ID.

BOUNDED PENDING FULL SNAPSHOTS

Implement a client-only bounded pending store for messages whose target world/entity is not yet available.

Freeze:

MAX_PENDING_MESSAGES =
    256

MAX_PENDING_ENCODED_BYTES =
    16 * 1,048,576

PENDING_TTL_CLIENT_TICKS =
    200

Pending key:

- dimension ID
- entity ID
- Entity UUID

Stored pending data:

- immutable decoded full snapshot message/state
- encoded payload byte size
- insertion/update tick metadata

Do not store:

- Entity reference
- World reference
- holder reference
- capability reference
- NetHandler reference

For the same pending key:

- apply the same generation/revision stale-order rules
- discard an older incoming pending snapshot
- replace with a newer or equal authoritative snapshot
- equal generation/equal revision may replace the pending payload

Capacity behavior:

- maintain deterministic insertion order
- before inserting, evict oldest entries until both count and total-byte limits are satisfied
- never exceed either limit
- a single valid message larger than the pending byte budget cannot occur because MAX_MESSAGE_BYTES is smaller
- updating an existing key must update accounted bytes correctly
- no unbounded list/map

Retry behavior:

- on client tick END, attempt pending messages applicable to the current client world
- do not scan beyond the bounded store
- remove a message after successful installation
- remove a message after a definitive UUID mismatch
- expire entries older than 200 client ticks
- retain entries for other dimensions until expiry or matching world availability

Cleanup:

- clear all pending messages on ClientConnectedToServerEvent before accepting a new session
- clear all pending messages on ClientDisconnectionFromServerEvent
- on WorldEvent.Unload, remove pending entries targeting that unloaded dimension
- do not retain pending state across server connections

Use the exact local FML and Forge buses for these events.

Do not add a server tick handler.

CLIENT LOCAL-TAMPER REPAIR

Required test and behavior:

1. Client accepts generation G, revision R, contents A.
2. Client code locally changes its holder to contents B.
3. Server sends the same generation G and same revision R with authoritative contents A.
4. The full snapshot is accepted.
5. replaceContents restores A.
6. Replica state records the client holder’s new local revision after repair.

This is why equal generation/equal wire revision is accepted.

Do not treat equal wire revision as an automatic no-op before comparing/replacing contents.

NO STOPTRACKING PACKET

Do not add a removal/clear packet on StopTracking.

When the client Entity is removed, its attached provider and holder naturally disappear with that Entity object.

Pending data is bounded and expires.

A later lifecycle phase may add stronger stop-tracking cleanup only if executable evidence shows it is necessary.

Do not maintain a static Entity→holder mirror.

PROTOCOL ERROR POLICY

Malformed inbound payload:

- throw an actionable decoder exception
- no partial decoded message escapes
- no world/holder mutation occurs
- allow the networking layer to close or fail the connection according to its normal behavior
- do not continue with default geometry
- do not substitute an empty snapshot

Valid but stale identity/generation/revision:

- silently or debug-level reject without mutation
- do not disconnect
- do not log per-tick/per-duplicate noise

Valid message whose Entity is temporarily absent:

- enter bounded pending state

Valid message with matching Entity but missing expected CRL Hitbox capability:

- reject
- emit one actionable error
- do not attach or construct replacement capability state manually

Server-local oversized snapshot:

- explicit send API throws before send
- automatic lifecycle handler logs and sends nothing
- do not disconnect unrelated clients

No ordinary packet should produce info-level per-message logging.

CODEC DETERMINISM

For identical captured message state:

- encoded bytes must be identical
- entry insertion order must be preserved
- Composite leaf encounter order must be preserved
- ResourceLocation bytes must be canonical and deterministic
- no hash-map iteration order may affect output
- no timestamp
- no random nonce
- no compressed stream metadata
- no platform-default charset

Repeated encode must not mutate the message or snapshot.

THREADING CONTRACT

Server:

- Holder mutations occur on the owning server thread.
- Explicit send methods require server thread.
- StartTracking and player lifecycle captures occur on server event thread.
- Message captures immutable snapshot state before network encoding.

Network:

- fromBytes/toBytes perform codec work only.
- Client handler only schedules.

Client:

- target lookup, stale-state checks, pending-store changes, and holder replacement occur on client logical thread.
- Client holder remains main-thread-confined.
- Immutable decoded snapshots may cross from network thread to client thread.

Do not add synchronized holder methods.

Do not mutate client holder from Netty.

NO ENTITY POSE OR WORLD GEOMETRY

Full snapshots carry existing entity-local PlacedSolid3d values exactly.

Do not encode or derive:

- Entity position
- previous position
- yaw
- pitch
- body yaw
- head yaw
- renderYawOffset
- eye height
- world bounds
- partial ticks
- model pose
- bone pose
- animation state

Do not compose entityLocalToWorld.

Do not transform local hitboxes into world space.

The client receives the same abstract entity-local holder contents as the server.

Entity/world pose remains Phase 2C-or-later independent scope.

NO ROLE, COMBAT, OR ENABLED STATE

Do not add:

- hit role
- hurt role
- collision role
- trigger role
- enabled/disabled field
- damage
- damage type
- attack instance
- team filter
- callback
- predicate
- user data
- metadata
- tags

The wire format contains only:

- entity identity
- generation
- revision
- ordered IDs
- immutable geometric placements

NO PERSISTENCE

Both holder state and internal synchronization state remain non-persistent.

Do not add:

- NBT holder storage
- generation persistence
- accepted replica state persistence
- Entity save keys
- player clone copy
- dimension-transfer copy
- WorldSavedData
- data fixer
- disk schema

A new provider starts with:

- empty holder revision 0
- a fresh local generation
- no accepted remote snapshot

CLEAN-ROOM REIMPLEMENTATION RULE

Do not access, clone, download, inspect, compare, translate, port, or adapt AnECanSaiTin/HitboxAPI during this phase.

Do not copy its:

- full packet
- incremental packet
- NBT wire format
- holder UUID
- change log
- EntityColliderHolder
- tracking Mixin
- packet names
- packet IDs
- client installation behavior
- codec
- class/package layout
- tests
- comments
- assets
- metadata
- license

Implement only from:

- this frozen Phase 2B contract
- current CRL Hitbox repository truth
- exact local Cleanroom 0.6.8 APIs
- independently written executable tests

Do not use upstream behavior as an oracle.

TEST-FIRST DEVELOPMENT

Add authentic failing tests before implementing each capability:

1. EntityHitboxHolder.replaceContents.
2. Atomic revision and overflow behavior.
3. Wire protocol constants and message shell.
4. Primitive codecs.
5. Composite codec.
6. RigidTransform3d and PlacedSolid3d codec.
7. Full message codec and exact round trip.
8. Malformed/truncated payload rejection.
9. Protocol budget enforcement.
10. Internal generation allocation.
11. Replica acceptance state machine.
12. Explicit server send facade.
13. StartTracking direct delivery.
14. Player self lifecycle delivery.
15. Client scheduling boundary.
16. Direct matching-Entity installation.
17. Missing-Entity pending queue.
18. Pending stale replacement.
19. Pending expiry/capacity/cleanup.
20. Client local-tamper repair.
21. Common/server class-linkage isolation.
22. Final public API closure.

Preserve authentic red/green evidence.

Do not manufacture missing-symbol or assertion failures after implementation.

EXISTING REGRESSION GATE

All existing 285 tests must remain:

- present
- enabled
- discoverable
- executable
- passing

Do not:

- delete tests
- disable tests
- add assumptions that skip them
- change existing seeds
- reduce iteration counts
- weaken geometry tolerances
- change Phase 2A holder expectations except where replaceContents explicitly extends the frozen API
- exclude existing suites from Gradle

All geometry production files must remain unchanged.

NEW TEST MINIMUM

Add at least:

- 64 new individually executed JUnit tests or dynamic cases
- 4 new deterministic fixed-seed property suites
- 2,048 iterations per new property suite

Use these seeds unless a concrete naming conflict exists:

- 0x5EED_2B01L
  Deterministic full-snapshot codec round trips for all geometry forms.

- 0x5EED_2B02L
  Malformed payload, truncation, count, duplicate-ID, tag, UTF-8, and budget rejection.

- 0x5EED_2B03L
  Generation/revision acceptance, stale rejection, local-tamper repair, and provider isolation.

- 0x5EED_2B04L
  Pending-message replacement, capacity, expiry, lifecycle cleanup, and atomic holder installation.

Each suite:

2,048 iterations

New deterministic randomized minimum:

8,192 iterations

Existing cumulative count:

33,792 iterations

Expected cumulative minimum after Phase 2B:

41,984 iterations

Every randomized failure must include:

- seed
- iteration
- protocol version
- dimension ID
- entity ID
- UUID
- generation
- wire revision
- ordered entry IDs
- solid/placement values
- encoded byte length
- expected result
- actual result
- buffer reader/writer indices where relevant
- pending-store state where relevant
- replica-state values where relevant

Do not add a property-testing dependency.

REPLACECONTENTS TESTS

Cover:

- null rejection
- empty into empty
- nonempty into empty
- empty into nonempty
- exact equal contents
- source snapshot revision differs but contents equal
- same mappings with different order
- one changed placement
- one changed ID
- added entry
- removed entry
- exact order preservation
- immutable placement instance preservation
- one effective local revision increment
- no per-entry revision increments
- no-op revision stability
- overflow fail-before-mutation
- duplicate-ID defensive rejection through a permitted test-only construction method
- earlier snapshot remains immutable
- no network access or side effect
- no Entity/World field introduced

WIRE ROUND-TRIP TESTS

Cover:

- empty snapshot revision 0
- empty snapshot nonzero revision
- one entry
- many entries
- insertion-order preservation
- ResourceLocation namespaces and paths
- every primitive local solid
- Composite containing all four primitive types
- duplicate geometries under different IDs
- identity transform
- rotated/translated transform
- signed-zero canonical values
- zero-radius Sphere
- zero-radius Capsule
- zero-length Capsule
- point/line/plane Aabb and Obb
- adjacent representable Aabb endpoints
- subnormal values
- large finite values
- q/-q canonical rotation behavior
- deterministic repeated encoding
- decode then encode byte equality
- exact ordered placement equality
- wire revision preserved separately from temporary holder revision
- maximum legal ResourceLocation byte length
- maximum legal small structural boundaries without excessive test allocation

Do not treat lossy approximate equality as a successful wire round trip.

MALFORMED CODEC TESTS

Cover:

- empty payload
- unsupported protocol version
- truncated after every header field
- truncated at representative byte boundaries through entry payloads
- negative entity ID encoding where representable
- overlong VarInt
- generation zero
- generation negative
- revision negative
- negative entry count
- entry count above limit
- ResourceLocation byte length zero
- ResourceLocation byte length above limit
- malformed UTF-8
- invalid ResourceLocation syntax
- duplicate entry IDs
- unknown solid tag
- nested Composite tag
- zero-leaf Composite
- Composite leaf count above limit
- total primitive leaf budget exceeded
- non-finite doubles
- negative radius
- negative half extent
- invalid Aabb order
- unrepresentable mandatory bounds
- trailing bytes
- message byte limit exceeded
- no allocation based on an unvalidated count
- no partially assigned message after failure
- no holder/replica mutation after failure

Use bounded test construction; do not allocate attacker-sized arrays merely to test rejection.

GENERATION TESTS

Cover:

- positive first generation
- distinct providers receive distinct generations
- monotonic allocation
- generation remains stable for one provider
- client replica state separate from local generation
- no Entity/World reference
- no static Entity map
- no randomness/time use
- near-exhaustion success where valid
- exhaustion fails before wrap
- no nonpositive returned generation
- concurrent allocator uniqueness using a bounded test, without implying holder thread safety
- no persistence/serialization surface

Use a narrowly scoped test-only inspection mechanism for allocator exhaustion.

Do not add a production reset hook.

REPLICA ORDERING TESTS

Cover:

- first full accepted
- higher generation accepted with lower revision
- lower generation rejected with higher revision
- same generation lower revision rejected
- same generation equal revision accepted
- same generation higher revision accepted
- state unchanged after stale rejection
- state updated only after holder replacement succeeds
- holder replacement overflow leaves replica state unchanged
- empty accepted snapshot clears stale contents
- same contents and newer wire revision update replica state without changing local holder revision
- client-local mutation repaired by same generation/revision full
- local holder revision at install recorded
- different provider has independent replica state
- disconnect/new provider starts with no accepted remote state

ENTITY IDENTITY TESTS

Cover:

- matching dimension/ID/UUID
- wrong dimension
- missing world
- missing entity
- matching ID with wrong UUID
- matching UUID under wrong ID
- missing public holder capability
- missing internal replica capability
- new Entity/provider with a newer generation
- old generation addressed to an ID reused by another UUID
- no lookup by class name or world scan

Use safe test seams rather than Unsafe.

Do not add production Entity constructors or public test hooks.

TRACKING AND SEND TESTS

Cover:

- channel name exactly crlhitbox
- discriminator exactly 0
- Side.CLIENT registration only
- no Side.SERVER registration
- no sendToServer call
- StartTracking targets any Entity
- StartTracking sends directly only to entering EntityPlayerMP
- empty holder still sends
- one captured snapshot per send operation
- explicit sendFullTo sends only to recipient
- sendFullToTrackingAndSelf uses tracking distribution
- player Entity receives explicit self send
- non-player Entity has no self send
- holder mutation is not triggered by send
- holder revision is not changed by send
- oversize explicit send fails before transport
- oversize automatic send is caught/logged and not transmitted
- player login self snapshot
- player respawn self snapshot
- player dimension-change self snapshot
- no PlayerEvent.Clone copying
- no StopTracking packet
- no world-tick scan

Use package-private immutable transport seams for tests where necessary.

Do not expose a mutable global test transport.

CLIENT THREAD TESTS

Cover:

- fromBytes performs no world access
- handler performs no world access before scheduling
- handler performs no capability access before scheduling
- handler returns null
- exactly one client-main-thread task submitted
- accepted holder replacement occurs only inside scheduled task
- pending-store mutation occurs only inside scheduled task
- common handler has no direct client implementation class reference
- client implementation has expected client references
- server implementation has none
- wrong receive side fails/rejects clearly

Use classfile inspection to verify constant-pool dependencies, not only source imports.

PENDING STORE TESTS

Cover:

- missing world queues
- wrong current dimension queues
- missing entity queues
- matching entity installs and removes pending
- UUID mismatch removes/rejects
- same key older generation ignored
- same key lower revision ignored
- same key equal revision replaced
- same key newer revision replaces
- same key newer generation replaces
- byte accounting on replacement
- count capacity
- total-byte capacity
- deterministic oldest eviction
- exactly-at-limit behavior
- over-limit behavior
- 199-tick retention
- 200/201-tick expiry according to the exact documented boundary
- current-dimension retry
- other-dimension retention
- world-unload dimension cleanup
- connection-start clear
- disconnect clear
- no Entity/World/holder references retained
- no unbounded collection
- no server tick integration

FULL INSTALL TESTS

Cover:

- exact content/order install
- empty clear
- source wire revision not copied into client local revision
- one local revision increment for changed contents
- local revision stable for equal contents
- remote state updates for equal-content newer wire revision
- same-revision repair after local mutation
- stale message no mutation
- failure no partial mutation
- accepted generation replacement
- duplicate message idempotence
- existing pre-network client contents overwritten authoritatively

INDEPENDENT CODEC ORACLE

Create a test-only reference encoder/decoder that is structurally independent from the production codec.

Required:

- Explicit fixed field order.
- Explicit stable tags.
- Direct ByteBuf operations.
- No call to production encode/decode helpers.
- No production protocol constant lookup when asserting frozen numeric values.
- Independently count entries and primitive leaves.
- Compare production bytes against independently encoded bytes for representative fixtures.
- Decode representative production bytes independently.
- Remain test-only.

Do not implement a second production codec.

INDEPENDENT REPLICA REFERENCE MODEL

Create a test-only model containing:

- optional accepted generation
- accepted revision
- ordered reference entries
- holder-local reference revision
- holder revision at install

Implement acceptance rules independently.

Do not call production stale-order helpers to compute expected results.

After every generated message compare:

- accepted/rejected
- ordered holder contents
- local revision
- accepted generation
- accepted wire revision
- local revision at install
- pending replacement state where applicable

API-SURFACE EXECUTABLE TEST

Extend the existing Java 25 java.lang.classfile-based closure.

Preserve every geometry and Phase 2A assertion.

Assert:

Geometry:

- public top-level geometry types remain exactly 14
- GeometryDistances remains exactly 6
- GeometryIntersections remains exactly 30
- Solid3d permits remain unchanged
- no geometry production signature changed

Stable public entity types are exactly four:

1. EntityHitboxHolder
2. EntityHitboxSnapshot
3. EntityHitboxes
4. EntityHitboxSync

EntityHitboxHolder:

- public final
- one public no-arg constructor
- exactly nine declared public methods
- includes replaceContents(EntityHitboxSnapshot)
- no public field
- no collection/array return
- no Entity/World field
- no network/codec method
- no serialization interface
- no equals/hashCode override

EntityHitboxSnapshot:

- unchanged public surface
- no public constructor
- nine declared public methods

EntityHitboxes:

- unchanged two-method surface

EntityHitboxSync:

- public final
- no public constructor
- exactly two public static void methods
- exact Entity/EntityPlayerMP parameters
- no channel/message/codec/generation return
- no client type parameter

Also assert no stable public type exists for:

- packet
- codec
- protocol
- generation
- replica status
- pending queue
- transport
- resync
- delta

Internal technical public classes required by Forge reflection must be identified separately and must live outside dev.crlhitbox.api.

SERVER-SAFETY CLASSFILE TEST

Scan all production classfiles.

For every class outside the client-only internal package, reject CONSTANT_Class references to:

- net/minecraft/client/
- net/minecraftforge/client/
- org/lwjgl/
- com/mojang/blaze3d/
- a CRL Hitbox internal client implementation class

A SidedProxy client class name stored only as a string is allowed only when:

- exact Forge 0.6.8 sided injection is verified
- there is no CONSTANT_Class linkage
- dedicated-server classloading evidence passes

Also reject common/server references to:

- rendering
- F3+B
- Tessellator
- BufferBuilder
- GlStateManager

Network/Netty references are allowed in internal network code.

They are not allowed in:

- geometry
- EntityHitboxHolder
- EntityHitboxSnapshot

NO GEOMETRY MODIFICATION

Do not modify any source beneath:

src/main/java/dev/crlhitbox/api/geometry

Do not modify geometry tests except the existing central API-closure test if needed to confirm the geometry surface remains unchanged.

Do not add:

- network codec interface to geometry
- wire type tags to geometry
- serializer methods
- trusted constructors
- frame IDs
- mutable hooks
- generation
- revision
- protocol state

NO MIXIN, AT, OR COREMOD

Do not add:

- Mixin class
- mixin config
- refmap
- MixinBooter
- Access Transformer
- coremod
- EntityTrackerEntry injection
- client renderer injection

All tracking functionality must use standard 0.6.8 events and networking.

DOCUMENTATION

Create:

docs/FULL_SNAPSHOT_PROTOCOL.md

It must document:

1. Channel:
   crlhitbox

2. Protocol version:
   1

3. Message discriminator:
   0

4. Direction:
   S2C only

5. Exact wire-field order.

6. Exact stable solid tags.

7. Primitive and transform field order.

8. Composite flat-leaf encoding.

9. Protocol limits.

10. Strict UTF-8 behavior.

11. Duplicate-ID rejection.

12. No NBT, compression, or fragmentation.

13. Entity identity:
    - dimension
    - runtime ID
    - UUID

14. Provider generation semantics.

15. Server wire revision semantics.

16. Client local holder revision distinction.

17. Full-snapshot acceptance ordering.

18. Same-generation/same-revision authoritative repair.

19. StartTracking delivery.

20. Player self lifecycle delivery.

21. Explicit full-resend API.

22. Client-main-thread installation.

23. Pending queue limits, expiry, and cleanup.

24. Server authority and no C2S path.

25. No automatic mutation sync.

26. No delta or resync request.

27. Non-persistence.

28. Abstract entity-local frame.

29. Malformed-payload fail-closed behavior.

30. Pointwise/non-certified geometry boundary.

Update:

docs/ENTITY_HOLDER_SEMANTICS.md

Add:

- replaceContents semantics
- atomic content replacement
- local holder revision behavior
- source snapshot revision is not adopted
- server wire revision is separate internal replica state
- full snapshots may overwrite client-local changes
- server is authoritative for the Phase 2B replication channel
- holder mutation itself does not send packets
- explicit resend requirement for already tracked entities

Update:

README.md

Advertise only completed capabilities:

- immutable geometry
- placed queries
- Entity-local holder and capability
- deterministic snapshots
- versioned S2C full-snapshot synchronization
- StartTracking delivery
- explicit full resend
- bounded client pending delivery

Clearly state absent capabilities:

- no delta packets
- no resync request
- no automatic mutation broadcasting
- no persistence
- no entity-to-world pose
- no rendering
- no combat
- no hit/hurt roles

Update:

AGENTS.md

Add durable constraints:

- Wire format is versioned and direct binary, not NBT.
- Stable shape tags and field order cannot change without a protocol-version change.
- Server wire revision is distinct from client holder local revision.
- Holder generation is process/session local and non-persistent.
- Equal generation/revision full snapshots remain authoritative repair messages.
- Client holder installation must occur on the client game thread.
- Bounds/geometry remain unchanged by networking.
- No C2S authority path.
- No delta logic may be hidden in the full-snapshot phase.
- Common/server code must not hard-link client implementations.

Do not add current test counts, packet hashes, commit SHA, or machine paths as durable architecture rules.

Update:

docs/BASELINE.md

Only as needed to record that versioned full-snapshot networking now exists.

Do not rewrite initialization history.

Update entity package documentation to describe:

- server-authoritative full snapshots
- explicit resend
- replaceContents local revision semantics
- no automatic delta
- no pose/render/combat behavior

Do not put Netty details in stable public method JavaDoc beyond what callers need.

NO LICENSE CHANGE

Distribution license remains unresolved.

Do not:

- add LICENSE
- add COPYING
- add NOTICE
- add SPDX headers
- infer a license
- copy template licensing
- copy HitboxAPI licensing
- publish artifacts

Preserve:

- empty authors
- empty project URL
- disabled publication

SCOPE EXCLUSIONS

Do not implement:

- delta packet
- change log
- base revision patch
- acknowledgement
- resync request
- C2S message
- automatic holder mutation observer
- periodic full broadcast
- server world tick scanning
- entity position/yaw/pitch adapter
- body/head/model/bone frame
- partial-tick interpolation
- world PlacedSolid3d cache
- render bounds
- debug rendering
- F3+B
- hit/hurt roles
- enabled state
- damage
- combat
- callbacks
- persistence
- NBT snapshot codec
- WorldSavedData
- clone copy
- player data copy
- transformed-solid materialization
- nearest leaf/hit path
- collision manifold
- penetration/contact/normal/MTV
- continuous collision
- time of impact
- certified interval geometry
- terrain proof
- Mixin
- AT
- coremod
- configuration
- command
- sample gameplay content
- benchmark/JMH

Do not create placeholder classes for excluded work.

BUILD-TIME ISOLATION

Preserve:

compileGeometryIsolation

It must continue to:

- compile every geometry production source
- use Java 25
- use an empty external classpath
- use an empty annotation-processor path
- output under build/
- remain wired into check
- remain outside normal JAR duplication
- report only java.base through jdeps

Networking must not enter this isolated source set.

FORBIDDEN PRODUCTION REFERENCES

Geometry and holder/snapshot classes must not reference:

- Netty ByteBuf
- IMessage
- SimpleNetworkWrapper
- MessageContext
- client classes
- NBT serialization
- logger
- transport
- generation allocator
- replica state

Common/server network classes must not reference:

- net.minecraft.client.*
- net.minecraftforge.client.*
- rendering classes
- client implementation classes as JVM Class constants

All production code must avoid:

- Class.forName
- MethodHandle-based side loading
- Unsafe
- Java serialization
- Object streams
- reflection-based geometry dispatch
- unbounded pending storage
- static Entity maps
- random generation IDs
- currentTimeMillis/nanoTime protocol ordering

LOCAL BUILD ENVIRONMENT

Use the previously verified host environment:

$env:JAVA_HOME = 'C:\GradleCaches\jdks\eclipse_adoptium-25-amd64-windows.2'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = 'C:\GradleCaches'
$env:JAVA_TOOL_OPTIONS = '-Djdk.net.unixdomain.tmpdir=C:\GradleCaches\tmp'

Use the project wrapper.

Do not commit the host-specific Unix-domain temporary-directory workaround into:

- build.gradle
- gradle.properties
- wrapper properties
- production source
- committed scripts
- project-wide JVM defaults

RUNTIME ACCEPTANCE AND NODE BOUNDARIES

Use the project AGENTS.md topology and gates:

Source / Static -> Build / Unit / Integration -> Dedicated Server -> Real GPU Client.

The development repository is D:\WI - Dev Workspace\CrlHitbox-src on the always-on development/orchestration server. It is the source of the artifact under test, not a Minecraft client test instance.
Never launch a Minecraft 3D client on this development node for acceptance. A basic display adapter, software renderer, or virtual display is not a Real GPU substitute.

Authoritative Dedicated Server:

- Use a project-test-only dedicated-server directory/instance separate from the development checkout, production instances, and valuable worlds.
- Its exact directory, endpoint, launch/stop commands, timeout, readiness marker, and evidence paths are currently UNCONFIRMED. Establish and record these from actual approved instance/configuration facts before a formal run; do not invent them.
- Record a graceful stop procedure and bounded supervision before launch. Process existence or an early preInit log alone is not readiness.
- Formal readiness requires completed startup and evidence that the server can accept the test client's connection; preserve the relevant startup, warnings, errors and connection logs.
- Do not accept the Minecraft EULA on the user's behalf. If no appropriately authorized, EULA-ready isolated test instance is available, report that prerequisite and stop the affected runtime gate.
- Do not overwrite or use a real user's world. Do not leave a task-owned server process running after its test window.

Real GPU Client:

- Phase 2B changes client network handling and combined client/server lifecycle, so independent Real GPU Client acceptance and an actual client/server full-snapshot exchange are required for formal PHASE 2B COMPLETE.
- Use the project's isolated Minecraft test client on a different real-GPU host. Host, instance path, launch/reset/control procedures and client evidence paths are currently UNCONFIRMED; resolve them without guessing or repurposing valuable client data.
- Exercise the implemented tracking, explicit resend, same-revision repair, pending delivery and applicable lifecycle scenarios using the same identified artifact as the server. Preserve both sides' evidence; client visual state or a healthy server log alone is insufficient.
- If the GPU node, structured control, server prerequisites or an authorized test window is unavailable, continue safe automated work and prepare a minimal repeatable acceptance package. Mark the missing gate as not executed; do not claim formal completion.
- Whenever no real GPU run occurred, state exactly: Real GPU acceptance: not executed.

Supplementary controlled classloading/preInit smoke:

- A bounded, isolated template-supported dedicated-server smoke may be used as additional low-level evidence only in an explicitly resolved, project-controlled disposable temporary directory outside both the development checkout and the read-only template. Keep its generated state out of source control. This diagnostic directory is never an implicit authoritative acceptance instance; if the required isolation cannot be established, skip the smoke.
- Keep eula=false or allow normal EULA refusal; do not accept the EULA, overwrite worlds or leave a process running.
- Its narrow success requires observed mod construction/preInit, capability and network registration, and no client-linkage/duplicate-registration/Mixin/AT/coremod failure. It proves only those observed stages, not server readiness or live replication.
- One concise startup info log containing protocol version and successful bootstrap is permitted. No ordinary per-entity/per-packet logging is added.
- If isolation cannot be established, do not launch. Preserve available lower-layer evidence and report the skipped diagnostic accurately.

CROSS-NODE ARTIFACT AND EVIDENCE RECORD

For every formal acceptance window record:

- source commit plus exact worktree/diff identity, distinguishing pre-existing unrelated changes from Phase 2B changes;
- build command, result and build/test-session identifier; exact primary development-produced remapped jar filename, size and SHA-256;
- resolved server/client instances, endpoint, environment/configuration deviations and deployment method;
- destination jar filenames and independently checked SHA-256 on both nodes; matching artifact identity is mandatory when both use the same Mod;
- server launch/stop/ready evidence and the actual connected test window;
- server/client logs, crash reports when present, diagnostics and relevant screenshots/recordings/performance evidence produced by the actual nodes;
- a shared session/test ID or an unambiguous bounded timestamp window connecting both evidence sets;
- each gate's actual passed/failed/not-executed status and concrete remaining prerequisites.

Artifact handoff and test instance paths/ports are not yet fixed; record actual values rather than persisting guessed or transient endpoints as project defaults.
Deploy the intended server artifact before that instance starts; if it is changed after readiness, stop/restart and re-establish readiness before testing. Deploy and verify the matching client artifact before client launch/connection.
Manual GPU-node changes or pack differences are deviations, not repository truth. Mismatched artifacts or uncorrelated logs permit environment-probe findings only, not a combined formal acceptance pass.
Do not commit generated logs, configs, libraries, worlds, EULA files, binary test packages or task packets.

INDEPENDENT READ-ONLY REVIEW

After implementation and before committing:

1. Reuse a reliable Phase 2A reviewer when available and still valid.
2. Otherwise create independent read-only reviewers.

At minimum obtain:

- one protocol/codec reviewer
- one client/server lifecycle and class-linkage reviewer
- one final verifier

Provide:

- this Phase 2B contract
- exact diff
- wire protocol documentation
- message/codec implementation
- holder replaceContents
- generation and replica-state implementation
- provider changes
- tracking/self lifecycle handlers
- client thread bridge and pending store
- tests/oracles
- API/classfile output
- server-safety scan
- runtime-smoke output, if any

Require review of:

- exact wire-field order
- stable tags
- all decoder bounds
- UTF-8 handling
- duplicate-ID rejection
- exact round-trip behavior
- no partial decode/install
- generation uniqueness and wrap handling
- stale-order rules
- same-revision repair
- Entity ID + UUID + dimension matching
- holder local revision versus wire revision
- client-main-thread mutation
- pending limits/accounting/expiry
- connection/world cleanup
- StartTracking bus and timing
- self lifecycle bus
- no C2S path
- no client linkage from common/server classes
- no geometry changes
- no NBT/persistence
- no accidental stable API growth
- test-oracle independence

Reviewers must not modify files.

Local L0 must independently validate each finding.

Resolve every in-scope P0, P1, and P2 finding before committing.

Do not claim Sol-Pro involvement unless an actual Sol-Pro browser consultation occurred.

Do not describe ordinary Codex/Terra reviewers as Sol-Pro.

REQUIRED VALIDATION

Run and report at least:

1. java -version

2. .\gradlew.bat --version --console=plain

3. Phase 2A baseline regression:

   .\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain

4. Targeted authentic red/green commands during implementation.

5. Final clean suite:

   .\gradlew.bat clean compileGeometryIsolation test --stacktrace --console=plain

6. Full build:

   .\gradlew.bat build --stacktrace --console=plain

7. Build wiring:

   .\gradlew.bat build --dry-run --console=plain

8. Whitespace:

   git diff --check

9. Geometry source pre/post hash comparison.

10. Forbidden source-reference scan.

11. Full production classfile server/client-linkage scan.

12. jdeps summary and verbose inspection of:

    build/classes/java/geometryIsolation

13. javap -public/-private and/or java.lang.classfile inspection for:

    EntityHitboxHolder
    EntityHitboxSnapshot
    EntityHitboxes
    EntityHitboxSync
    the full-snapshot message
    wire codec
    generation allocator/state
    provider
    tracking handler
    common message handler
    sided-dispatch interface/implementations
    client replica installer
    pending store
    CrlHitbox
    PlacedSolid3d
    Solid3d
    GeometryDistances
    GeometryIntersections

14. Confirm public entity API closure:
    - exactly four stable public types
    - Holder one ctor / nine methods
    - Snapshot zero public ctor / nine methods
    - EntityHitboxes two methods
    - EntityHitboxSync two methods

15. Confirm geometry closure:
    - 14 public geometry types
    - 6 distance methods
    - 30 intersection overloads
    - unchanged Solid3d permits

16. Confirm network registration:
    - channel crlhitbox
    - discriminator 0
    - Side.CLIENT only
    - no C2S registration
    - no sendToServer call

17. Confirm protocol constants and limits through executable tests/classfile inspection.

18. Confirm all production classfiles use major version 69.

19. Inspect primary JAR inventory.

20. Confirm:
    - every production class appears exactly once
    - no test class is packaged
    - no geometryIsolation output is packaged
    - no task packet is packaged
    - no duplicate JAR entry exists
    - no bundled third-party dependency exists
    - no NBT schema exists
    - no delta/resync packet exists
    - no rendering class exists
    - no Mixin/AT/coremod exists

21. Inspect generated mcmod.info.

22. Confirm loader/mapping/plugin/wrapper pins remain unchanged.

23. Calculate primary artifact:
    - path
    - size
    - SHA-256

24. Execute/report the Authoritative Dedicated Server gate under RUNTIME ACCEPTANCE AND NODE BOUNDARIES. Report any supplementary preInit smoke separately; it is not a substitute for this gate.

25. Final Git status/log/remotes/tags.

26. Template final HEAD/status comparison.

27. Execute/report independent Real GPU Client full-snapshot exchange with the CROSS-NODE ARTIFACT AND EVIDENCE RECORD. If prerequisites are unavailable, prepare the repeatable acceptance package and leave this gate not executed.

Do not launch or use a real user world.

Do not accept the EULA.

Do not claim live client/server replication unless it actually occurred.

GIT REQUIREMENTS

Do not modify the existing seven commits.

Do not:

- amend
- rebase
- squash
- reset
- git clean
- force checkout
- add a remote
- tag
- push
- publish

Only after the required implementation, review and formal runtime acceptance gates pass, create exactly one new Phase 2B feature commit under the user's authorization to execute this task:

feat(network): add tracking full snapshots

The commit may contain only:

- EntityHitboxHolder replaceContents
- EntityHitboxSync
- internal generation/replica state capability
- provider/bootstrap evolution
- direct binary full-snapshot message and codec
- tracking and player self lifecycle handlers
- client main-thread bridge
- bounded client pending store
- Phase 2B tests and independent test oracles
- API/server-safety audit evolution
- narrow documentation updates
- minimal sided bootstrap wiring
- one concise startup bootstrap log, if used for controlled server smoke

Do not commit:

- build/
- .gradle/
- class files
- JAR files
- IDE state
- local task packets
- .git/info/exclude
- run directories
- server logs
- configs
- generated EULA files
- worlds
- temporary protocol dumps
- local audit scripts
- benchmark output

Expected feature history if the original seven-commit baseline remains unchanged, oldest first:

1. chore: establish CRL Hitbox 0.6.8 baseline
2. feat(core): add immutable geometry foundation
3. feat(core): complete primitive intersection matrix
4. feat(core): add composite solid dispatch
5. feat(core): add rigid transform foundation
6. feat(core): add rigidly placed solid queries
7. feat(platform): add entity hitbox capability
8. feat(network): add tracking full snapshots

Preserve every pre-existing unrelated edit and untracked file. Do not stage an entire mixed-change AGENTS.md, add .codex/config.toml, or clean the checkout merely to obtain a globally clean status.
Stage only reviewed Phase 2B changes, using hunk-level selection when a file also contains pre-existing edits. Verify the staged diff excludes the pre-existing topology addition and all local task/environment files.
After a successful feature commit, the index and Phase 2B-owned change set must be clean; the worktree may retain the explicitly recorded pre-existing unrelated changes. Report the actual final status and their preservation instead of claiming global cleanliness.
If runtime acceptance or a core representation prerequisite is blocked, keep the scoped work and evidence uncommitted, report PHASE 2B INCOMPLETE, and do not create a completion-labelled or partial feature commit. If new commits or overlapping changes appear concurrently, reconcile the exact baseline before staging; do not force an eighth-commit count by rewriting history.

DEFINITION OF DONE

Report PHASE 2B COMPLETE only when every applicable condition is true:

- Starting repository state was locally verified.
- Existing 285 tests passed before implementation.
- Platform/build pins remain unchanged.
- No new runtime dependency was added.
- Every geometry production source remains byte-for-byte unchanged.
- Geometry public closure remains 14/6/30.
- Solid3d permits remain unchanged.
- EntityHitboxHolder exposes exactly the added replaceContents operation.
- replaceContents is atomic.
- replaceContents preserves snapshot order.
- replaceContents increments local revision exactly once on content change.
- replaceContents ignores source snapshot revision.
- replaceContents no-op and overflow behavior are correct.
- EntityHitboxSync exists with exactly the two specified methods.
- Explicit send methods are server-only and server-thread-confined.
- Channel name is exactly crlhitbox.
- Protocol version is exactly 1.
- Full message discriminator is exactly 0.
- Message direction is S2C only.
- No C2S message or sendToServer path exists.
- Wire field order matches the frozen contract.
- Solid tags are exactly 0–4 as frozen.
- All primitive and transform field orders match the contract.
- Composite encoding is flat and nonrecursive.
- Wire encoding uses direct binary values, not NBT.
- All protocol limits are enforced before unsafe allocation.
- Strict UTF-8 decoding is used.
- Duplicate IDs, nested Composite, trailing bytes, malformed values, and truncation are rejected.
- Full decode is all-or-nothing.
- Server-generated canonical snapshots round-trip exactly according to the frozen comparison rules.
- Provider generations are positive, distinct, monotonic, non-persistent, and non-wrapping.
- No generation Entity registry exists.
- Client stale-generation/revision rules exactly match the contract.
- Equal generation/equal revision full snapshots can repair client-local mutation.
- Client holder local revision remains distinct from wire revision.
- Replica state updates only after successful holder replacement.
- Target identity checks dimension, runtime ID, and UUID.
- StartTracking sends a full snapshot directly to the entering tracking player.
- Empty holder snapshots are sent.
- Player login/respawn/dimension self snapshots use the exact appropriate event bus.
- No PlayerEvent.Clone persistence copy exists.
- No StopTracking packet exists.
- No automatic mutation networking exists.
- Client world/capability mutation occurs only on the client game thread.
- Common/network handler performs no world access before scheduling.
- Pending messages are bounded by count, total bytes, and TTL.
- Pending state contains no Entity or World references.
- Pending state clears on connection/disconnection and appropriate world unload.
- Common/server classes do not hard-link client implementation classes.
- Dedicated-server safety evidence passes.
- The isolated Authoritative Dedicated Server actually reached readiness and participated in the documented test window; classfile/preInit-only evidence is insufficient.
- Independent Real GPU Client acceptance and actual full-snapshot exchange passed with matching artifact identity and correlated server/client evidence.
- At least 64 new tests execute.
- Four new fixed seeds execute at least 2,048 iterations each.
- Cumulative deterministic randomized iterations are at least 41,984.
- Independent wire and replica-state oracles exist.
- No test is skipped or disabled.
- compileGeometryIsolation still succeeds with empty external classpath.
- jdeps for geometry still reports only java.base.
- Full build succeeds.
- Primary JAR contains all production classes exactly once.
- No delta, resync, pose, rendering, combat, persistence, Mixin, AT, coremod, certified, or continuous-collision implementation entered the project.
- Documentation exactly describes the protocol and evidence boundary.
- Distribution license remains unresolved and explicit.
- Template integrity is unchanged.
- Independent review has no unresolved P0/P1/P2 finding.
- The authorized Phase 2B feature commit exists on the verified baseline, without rewriting unrelated history.
- Final index and Phase 2B-owned changes are clean after commit; any remaining pre-existing unrelated worktree changes are enumerated and preserved.
- No tag, remote, publication, or push occurred.

A safely unavailable runtime gate is not grounds to fabricate proof or lower the completion threshold. Report Source / Static, Build / Unit / Integration, Dedicated Server and Real GPU Client separately. Safe automated implementation may continue, but missing formal Dedicated Server or Real GPU Client evidence leaves PHASE 2B INCOMPLETE. If no real GPU run occurred, include exactly: Real GPU acceptance: not executed.

Otherwise report PHASE 2B INCOMPLETE and identify the exact blocker.

Do not describe partial codec or classfile-only work as proven live network replication.

FINAL RESPONSE FORMAT

Return exactly these sections:

1. STATUS

- PHASE 2B COMPLETE or PHASE 2B INCOMPLETE.
- One-sentence reason.

2. VERIFIED START STATE

- Canonical target path.
- Canonical template path.
- Applicable AGENTS files.
- Initial branch and exact HEAD.
- Initial index/worktree.
- Pre-existing unrelated changes and their preservation identifiers, distinct from Phase 2B output.
- Initial remotes/tags.
- Existing seven-commit log.
- Any drift from the handoff.
- Local task-packet/exclude state.
- Template initial HEAD/status.

3. PHASE 2A REGRESSION

- Exact baseline command.
- Java/Gradle versions.
- Existing 285-test result.
- Existing seeds and cumulative randomized iterations.
- Initial compileGeometryIsolation result.
- Geometry source hashes.
- Phase 2A live-runtime evidence boundary.
- Any baseline issue.

4. PUBLIC API DELTA

- Exact EntityHitboxHolder replaceContents signature.
- Final exact Holder public surface.
- Exact EntityHitboxSync declaration and two methods.
- Public entity type count.
- Snapshot and EntityHitboxes unchanged surfaces.
- Geometry 14/6/30 closure.
- Confirmation that no protocol implementation type entered stable public API.
- Technical public internal classes required by Forge and why.

5. HOLDER REPLACEMENT SEMANTICS

- Ordered-content comparison.
- Atomic replacement.
- Local revision behavior.
- Source snapshot revision treatment.
- Empty replacement.
- Overflow atomicity.
- Duplicate defensive validation.
- Complexity.
- No networking side effect.

6. PROTOCOL AND CODEC

- Channel.
- Discriminator.
- Direction.
- Protocol version.
- Exact header field order.
- Exact entry order.
- ResourceLocation encoding.
- Stable solid tags.
- Primitive field layouts.
- Transform layout.
- Composite flat encoding.
- Protocol limits.
- Strict UTF-8 policy.
- Malformed/truncated/trailing-data policy.
- Deterministic byte evidence.
- Exact round-trip evidence.
- No-NBT evidence.

7. GENERATION AND REPLICA STATE

- Generation allocator.
- Range and exhaustion.
- Provider ownership.
- Non-persistence.
- Stored replica fields.
- Acceptance matrix.
- Same-revision repair.
- Wire revision versus local holder revision.
- State-update atomicity.
- No static Entity registry.

8. SERVER DELIVERY

- Exact network registration.
- Explicit sendFullTo behavior.
- Explicit sendFullToTrackingAndSelf behavior.
- StartTracking handler and bus.
- Empty snapshot behavior.
- Login/respawn/dimension self handlers and bus.
- Oversize handling.
- No mutation observer.
- No C2S or StopTracking packet.
- Server-thread checks.

9. CLIENT INSTALLATION AND PENDING DELIVERY

- Network-thread behavior.
- Main-thread scheduling.
- Sided class-linkage boundary.
- Entity resolution by dimension/ID/UUID.
- Capability requirements.
- Immediate application.
- Pending key/value.
- Count/byte/TTL limits.
- Stale pending replacement.
- Retry behavior.
- World/session cleanup.
- Local-tamper repair.
- No retained Entity/World references.

10. TESTS AND INDEPENDENT ORACLES

- Test files/classes.
- Original 285 tests preserved.
- New individual test count.
- Final total test count.
- New seeds.
- Iterations per seed.
- Final cumulative randomized total.
- Independent binary codec oracle.
- Independent replica-state model.
- replaceContents tests.
- malformed-payload tests.
- lifecycle/transport tests.
- thread-boundary tests.
- pending tests.
- failures/errors/skips/disabled count.
- Authentic red/green evidence.

11. SECURITY, THREADING, AND SCOPE AUDIT

- Allocation bounds.
- All-or-nothing decode/install.
- Server authority.
- No C2S.
- No world access on network thread.
- No client linkage from common/server classes.
- No Java serialization/NBT wire format.
- No Entity registry.
- No time/random protocol ordering.
- No pose/render/combat/persistence/delta work.
- Exact evidence boundary.

12. GEOMETRY INTEGRITY

- Pre/post geometry source hashes.
- Public type count.
- Distance/intersection counts.
- Solid3d permits.
- compileGeometryIsolation result.
- jdeps result.
- Confirmation that no geometry production file changed.

13. API AND CLASSFILE AUDIT

- Stable public API closure.
- Internal technical-public type inventory.
- Constructor/method counts.
- Network registration classfile evidence.
- Client/server constant-pool separation.
- Class-file major version.
- No unexpected public protocol state.
- No Mixin/AT/coremod.

14. ISOLATION, BUILD, AND CROSS-NODE RUNTIME VALIDATION

- Exact commands and exit codes.
- clean test result.
- full build result.
- dry-run wiring.
- git diff --check.
- forbidden-reference scans.
- jdeps/javap/classfile output.
- controlled dedicated-server smoke setup.
- whether preInit/network registration was observed.
- whether normal EULA refusal occurred.
- whether any live multiplayer exchange was performed.
- clearly distinguish build/classfile proof from live runtime proof.
- each formal evidence gate's status, resolved test instances, server-ready evidence, and independent GPU-host result.
- shared test/session ID or bounded time window linking server/client evidence, plus unresolved operational prerequisites.

15. ARTIFACT AUDIT

- Primary artifact path.
- Size.
- SHA-256.
- Source commit/worktree identity, build identifier, deployment method, and server/client destination hashes for the same tested artifact.
- JAR entry counts.
- Expected production classes.
- Duplicate count.
- Absence of tests, isolation output, task packets, run files, worlds, EULA, bundled dependencies, delta packets, NBT schema, rendering, Mixin, AT, and coremod.
- mcmod.info values.
- Platform pins.

16. DOCUMENTATION

- Every file created/updated.
- Exact protocol documentation.
- Holder local-versus-wire revision documentation.
- Generation documentation.
- StartTracking/self delivery documentation.
- Pending and threading documentation.
- Deferred delta/resync/pose/render/combat/persistence scope.
- License/author/project URL state.
- Confirmation that no unimplemented feature was advertised.

17. REVIEW FINDINGS

- Protocol reviewer identity/type.
- Lifecycle/server-safety reviewer identity/type.
- Final verifier identity/type.
- Whether any reviewer was reused.
- P0/P1/P2 findings.
- Resolution of every finding.
- Explicit statement when no actual Sol-Pro consultation occurred.
- Do not fabricate reviewer or runtime evidence.

18. FEATURE COMMIT AND FINAL GIT STATE

- Commit SHA.
- Exact subject:
  feat(network): add tracking full snapshots
- Files included.
- Final actual commit log; expected eighth feature commit only if that baseline still applies, or explicit no-commit reason while incomplete.
- Final branch and HEAD.
- Final index/worktree.
- Phase 2B-owned change status separately from preserved pre-existing unrelated changes.
- Remotes/tags.
- Confirmation of no amend/rebase/squash/reset/git-clean/push/publication.

19. TEMPLATE AND CLEAN-ROOM INTEGRITY

- Template final HEAD/status.
- Whether any template change was introduced.
- Local task-packet/exclude integrity.
- Confirmation that HitboxAPI was not accessed or copied.
- Confirmation that no license was inferred or added.

20. DEVIATIONS, BLOCKERS, OR RESIDUAL RISKS

- None, or exact factual items.
- Separate blockers from completed-scope residual risks.
- State explicitly whether live dedicated-server preInit smoke passed.
- State explicitly whether live client/server snapshot exchange was performed.
- State Authoritative Dedicated Server and Real GPU Client acceptance separately; include Real GPU acceptance: not executed when applicable.
- Do not list delta, resync, entity pose, rendering, combat, or persistence as Phase 2B defects.

21. NEXT-PHASE HANDOFF

State exact repository facts Phase 2C may safely rely on:

- exact HEAD SHA
- actual worktree state, separating Phase 2B-owned changes from preserved pre-existing unrelated changes
- exact stable public entity API
- replaceContents semantics
- channel/discriminator/protocol version
- complete wire layout and limits
- solid type tags
- provider generation semantics
- accepted replica-state ordering
- server wire revision versus client local revision
- StartTracking/self delivery triggers
- explicit full resend API
- client scheduling and pending behavior
- geometry 14/6/30 closure
- Solid3d permits
- test seeds/counts
- isolation-task name
- verified build commands
- each runtime gate and its evidence boundary; supplementary smoke is not formal server/client acceptance
- primary artifact path and hash
- explicit statement that no delta, C2S resync, entity pose, rendering, persistence, or combat implementation exists

End with:

Phase 2C revisioned delta synchronization and explicit resynchronization have not begun.
