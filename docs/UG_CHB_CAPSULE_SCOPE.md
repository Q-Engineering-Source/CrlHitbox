# UG-CHB-CAPSULE-V1 — CHB scope and M0 handoff

Date: 2026-09-12. Owner: CHB task `01a055f9-ac8f-7392-ba56-21ae6397be46`.
Coordinator/consumer: UG task `01a050ec-1654-7bc1-adbb-3e2921713f20`.

This is an authorization, ownership and candidate-source record, not an adopted
continuous-collision algorithm, a delivered Interface or an acceptance pass.
The consumer contract is
`D:/WI - Dev Workspace/UniversalGravitation-src/UniversalGravitation/docs/ug-chb-capsule-execution-contract-v1.md`.
UG owns that contract and its progress records; CHB does not edit them.

## 1. Scope-limited resumption

The latest user instruction in the CHB task is:

> 与 UG 协作的部分你可以继续推进，可以由UG协调进行构建、提交。

This resumes only the UG collaboration and permits UG to coordinate CHB builds
and local commits. It does not resume the unrelated Phase 2B holder/networking,
persistence, pose extraction, combat or other consumer work. It does not grant
push, publication, licensing, EULA acceptance or destructive test-state authority.
It does not change UG's own Git permissions.

UG subsequently requested this bounded M0 handoff: record the minimal exception,
complete current write allowlist and candidate-source separation; send a reviewable
diff and baseline; do not implement or build yet. A local CHB commit is deferred
until independent review of the intended change and an explicit UG-coordinated
commit window. Existing dirty work is not implicitly included in that window.

## 2. Accepted CHB responsibility and minimal exception

The new Module is a pure JDK-only continuous-translation query for an arbitrary
but fixed-orientation Capsule against one static Aabb. Motion is a world-space
delta over an explicitly defined request interval. UG supplies the actual shape
and individual static collision boxes and owns Minecraft candidate collection,
box identity/aggregation, sliding, standing/support, physical state transitions,
world mutation, networking and runtime consumer acceptance.

The exception permits only additive query/result/private-kernel production files
after the M1 gate below. All 21 existing geometry production files stay byte-for-byte
frozen. Existing constructors, quaternion normalization/representation/equality,
the W108 contract, solid permits, placed-solid semantics and legacy query arithmetic
are unchanged. This is not a general geometry unfreeze or permission to modify a
legacy kernel to obtain contact witnesses.

The new query must distinguish whole-request CLEAR, HIT, INITIAL_OVERLAP and
UNRESOLVED/invalid-call behavior in its eventual Interface. These are required
semantic distinctions, not a finalized enum, field list or numerical guarantee.
Runtime guarantees must name their supported domain, error and failure conditions;
neither existing pointwise queries nor the new query become certified interval
geometry, admission authority or an unconditional non-penetration proof.

General rotational TOI, all-shape CCD, a full manifold/MTV engine, dynamic worlds,
player physics and platform adapters are outside this implementation exception.
Rotation/size/anchor-path safety remains a separate decision needed by UG before
mixed-motion integration; fixed-orientation translation does not close it.

## 3. Complete active write allowlist

For this M0 handoff, the complete write allowlist is:

| File | Permitted change |
| --- | --- |
| `AGENTS.md` | Scope-limited resumption, additive capsule exception and routing to this record; preserve the preceding constraints synchronization |
| `docs/UG_CHB_CAPSULE_SCOPE.md` | This M0 scope, gates, baseline, candidate-source and handoff record |

No production source, test, Gradle/build configuration, dependency, metadata,
local exclusion, existing task-input file or other project is in the active M0
write allowlist. No implementation placeholder or mock may be presented as a
delivered capsule query.

For M1, `src/main/java/dev/crlhitbox/api/geometry/cast/` and corresponding tests
are the proposed location, not an unrestricted directory write grant. Final new
public types, private helpers, tests and numerical-contract/verification documents
must be enumerated by exact path after the Pro/adoption gate. Earlier proposed
type names are not a frozen Interface. New public surface needs its own explicit
inventory check; the legacy package's existing surface test is not a loophole.
The currently dirty `GeometryApiSurfacePhase1BTest.java` remains protected.

Any later change to an existing geometry production file or to the fixed platform,
build, dependency or ownership scope requires a separately reviewed exception;
it cannot be inferred from this additive allowance.

## 4. Reverified CHB baseline and protected work

Repository: `D:/WI - Dev Workspace/CrlHitbox-src`.
Branch: `main`.
HEAD: `1fc4e8700a325be87b89c0d6ac37bf76ad691531`
(`feat(core): add exact rotation reconstruction`).
Only this worktree existed at M0 resumption; the index was clean.

The pre-M0-edit SHA-256 of `AGENTS.md` was
`751D8EA56F8B2EA64D21641693FD1E6F598AA241CE0DA7D8BB38A15F2DFCFEED`.
That file already contained uncommitted topology and Astra-native synchronization.
A diff against HEAD therefore includes earlier work; review the capsule increment
against this pre-edit state rather than treating the entire HEAD diff as capsule work.

Protected pre-existing worktree changes:

- `AGENTS.md`: preserve the earlier governance changes while applying only the
  allowlisted capsule increment.
- `src/main/java/dev/crlhitbox/api/entity/EntityHitboxHolder.java`.
- `src/test/java/dev/crlhitbox/api/geometry/GeometryApiSurfacePhase1BTest.java`.
- Untracked `.codex/config.toml` and `logs/latest.log`.
- Untracked `src/test/java/dev/crlhitbox/api/entity/EntityHitboxReplaceContentsPhase2BTest.java`.
- Ignored `docs/input.md` and the repository-local `.git/info/exclude` entries.

The 21 tracked geometry production files have no diff from HEAD. Their raw-byte
manifest SHA-256 is
`AA7210A302E7027D32BBBBCE02DF8C0BC6555E1BD2DE2B5CB4A4BA6E3F72A09C`.
The manifest uses sorted repository-relative forward-slash paths, each followed
by `|` and its uppercase SHA-256, UTF-8 without BOM, LF separators and a terminal LF.
Raw-byte freezing and Git-normalized content checks are distinct checks.

Existing artifact, inspected without rebuilding:

- `build/libs/crlhitbox-0.1.0-SNAPSHOT.jar`, 75,364 bytes.
- SHA-256: `188E804123F56583DA29E841280B8B30F705070311ED28844170A5B5C3F592ED`.
- It includes the prior uncommitted holder slice and has no continuous capsule
  query. It is not a clean-HEAD build or an M1 candidate and must not be handed to
  UG as one. Matching the existing versioned filename does not change this status.

Mod ID `crlhitbox`, group `dev.crlhitbox`, version `0.1.0-SNAPSHOT`, Java 25 and
Cleanroom `0.6.8-alpha` remain unchanged. No new published Maven location or
compatibility range is established by this record.

## 5. Candidate source, build and commit coordination

The chosen source-separation method for M1 is a new, isolated CHB worktree rooted
at the exact HEAD above, with only the reviewed capsule changes applied. Do not
build the candidate from the current mixed worktree or copy its existing JAR.
Do not copy the dirty holder implementation/test or unrelated local files into the
candidate. Do not stash, reset, clean or revert them to manufacture a clean tree.

The worktree is not created by this M0 document handoff. After review and UG
coordination, CHB will resolve and record a new unused directory and branch, verify
the base commit and applicable constraints, and apply the capsule-only change set.
The new worktree must retain the applicable global/project safety and freeze rules;
its constraints must be reviewed rather than inferred from its location. If an
additional prerequisite is found necessary, report it instead of silently importing
the suspended holder slice. An alternate base requires an explicit reviewed reason.

CHB remains the sole writer of its source and Git staging/commit operations. UG
coordinates the delivery/build/commit window and consumes the resulting identity;
UG coordination is not cross-repository write ownership. Before a local commit,
check the exact staged diff, include only the reviewed capsule change set, run the
risk-matched gates required by that set and communicate the resulting commit ID.
The separate dirty main checkout remains intact.

Build with that isolated checkout's wrapper and the project cache setting. M1
verification includes `compileGeometryIsolation`, direct behavior and compatibility
tests, full build and the agreed performance measurements. No dependency or wrapper
upgrade is authorized to obtain a green result.

Each candidate receipt must state its exact worktree/branch/commit or uncommitted
diff identity, build command/result, public Interface revision, actual remapped JAR
filename/size/SHA-256 and applicable dev/source artifacts. Resolve the real remap
consumption route with UG rather than inventing a Maven publication. Preserve a
distinct immutable candidate; do not overwrite a same-named dependency while UG
builds against it. UG must test the real public entry, not only compile a mock.

CHB pure-geometry evidence does not close UG's dedicated-server or Real GPU gates.
The user-authorized UG test topology is consumer acceptance, not a new CHB default
instance. This work does not launch Minecraft on the development node.

## 6. Pro/adoption gate and next handoff

UG owns the single consolidated capsule consultation through its designated Pro
advisor under the consumer contract. For this slice, CHB does not start another
consultation in its default advisor chat. The prepared packet is
`D:/WI - Dev Workspace/UniversalGravitation-src/UniversalGravitation/docs/progress/ug-chb/capsule-advisor-packet-20260912.md`.
A prepared packet or visible Pro mode is not a complete Pro answer. Prior W108
and floor advice do not cover the new continuous-collision decision.

Before M1 selection/implementation, the complete answer and CHB adoption review
must settle or explicitly isolate:

- The supported finite domain and units, including zero delta, zero-length
  centerline, radius zero, degenerate AABB and non-representable intermediates.
- Whole-request coverage, including the cylindrical middle, thin-wall
  free-to-hit-to-free motion and the 24.5 m high-speed reference request.
- Initial penetration versus touching inward/outward/tangent; normal direction,
  validity, ties and what a caller may assume when no usable normal exists.
- Whether HIT returns an estimated contact fraction, a bracket or a last-safe
  bound, its exact meaning on `[0,1]`, error units and uncertainty handling.
- Numerical failure, maximum work/iterations, UNRESOLVED behavior and measured
  performance acceptance criteria. Budget exhaustion cannot become CLEAR.
- Separate rotation/size/anchor safety and the pure-query versus UG orchestration
  split; this must not be silently added as a general rotational TOI implementation.
- The final minimal Interface and exact M1 file allowlist, with independent
  analytic/reference cases and preserved legacy regression requirements.

Until then, only the allowlisted M0 documents are changed. No continuous algorithm
has been selected, no new production/test implementation or candidate build has
been made, and no local commit or runtime acceptance is claimed by this handoff.
UG should review this increment, update its own authority/progress records and
return the complete Pro material before requesting the next CHB implementation slice.

Real GPU acceptance: not executed
