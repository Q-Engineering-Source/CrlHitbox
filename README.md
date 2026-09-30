# CRL Hitbox

> An independent, non-official Cleanroom-only runtime hitbox and collision-query library for Minecraft 1.12.2.
>
> 面向 Minecraft 1.12.2 Cleanroom-only 环境的运行时碰撞箱与碰撞查询库。

CRL Hitbox is an independent project, not an official CleanroomMC component. Its experimental geometry foundation provides immutable, JDK-only, double-precision pointwise primitives, solid unions, and rigid-transform arithmetic. It is common/server-safe and deliberately has no Minecraft, Forge, Cleanroom, JOML, LWJGL, networking, rendering, combat, or persistence dependency.

The implemented primitives are `Vec3d`, `Rotation3d`, `Aabb`, `Sphere`, `Obb`, `Segment3d`, and `Capsule`, with `Bounded3d`, distance helpers, and explicit intersection overloads. `Solid3d` is sealed to the four primitive solids plus `Composite`; `Segment3d` remains a finite non-solid query primitive. Geometry is finite, closed-set, and non-certified: touching intersects, but results are ordinary IEEE-754 pointwise evaluations and are never admission, continuous-collision, terrain-non-penetration, persistence, or proof authority.

The pointwise solid-solid matrix is complete for `Aabb`, `Sphere`, `Obb`, and `Capsule`: every unordered pair is supported, and each mixed pair has both symmetric overload orders. Generic sealed dispatch preserves those typed kernels. Immutable, nonempty `Composite` values represent finite unions whose nested inputs are flattened into an ordered primitive-leaf sequence; their bounds are broad-phase enclosures only. Finite `Segment3d` queries are supported against every primitive solid and `Composite`, in both overload orders.

`RigidTransform3d` supplies immutable active point/vector mapping, inverse mapping, and explicitly ordered composition. It is arithmetic only: transformed-solid wrappers, automatic local-to-world solid projection, frame IDs, entity pose extraction, and model/bone attachment remain deferred.

`PlacedSolid3d` adds non-materializing rigid placement of a local solid into a caller-defined parent frame. It provides conservative parent-frame bounds plus exact pointwise placed-solid and finite `Segment3d` queries while retaining local AABB min/max intervals directly. Callers are responsible for placing both operands in the same parent frame.

`EntityHitboxHolder` adds deterministic `ResourceLocation`-named, entity-local `PlacedSolid3d` state. Effective mutations alone advance a checked monotonic revision, and `EntityHitboxSnapshot` captures immutable insertion-ordered values. A fresh non-sided capability provider is attached to every `Entity`; it is intentionally non-persistent, owns no Entity/World back-reference, and keeps server and client holders independent.

Entity hitbox state is replicated by a versioned, direct-binary, server-authoritative full-snapshot protocol documented in [docs/FULL_SNAPSHOT_PROTOCOL.md](docs/FULL_SNAPSHOT_PROTOCOL.md): one `crlhitbox` channel, one client-bound message, frozen shape tags and limits, strict bounded VarInt coding, exact round trips checked against an independently written oracle, provider generations, internal replica state, `EntityHitboxSync.sendFullTo`/`sendFullToTrackingAndSelf`, automatic delivery when a player starts tracking an Entity and on player login/respawn/dimension change, client-main-thread installation with stale-generation/revision rejection, and a bounded client pending store. Mutating a holder never sends a packet: an already-tracked Entity requires an explicit resend. There is no client-to-server path, no delta packet, no resync request, and no persistence. Automated gates pass, but no dedicated-server run and no real-GPU client run have been performed, so live network replication is not claimed.

Automatic entity-to-world pose extraction, delta packets, client resync requests, acknowledgements, persistence, clone or respawn copying, rendering, combat, hit/hurt roles, model/bone attachment, generic distances, hit-leaf or hit-path results, solid metadata, collision manifolds, penetration depth, contact normals, time of impact, swept or continuous queries, certified geometry, and other platform integration remain deferred. The sealed solid set does not admit third-party implementations in this phase. This library does not claim to replace Minecraft physics.

The finalized project identity is Maven group and Java base package `dev.crlhitbox`, main class `dev.crlhitbox.CrlHitbox`, and initial development version `0.1.0-SNAPSHOT`. Authors, project URLs, issue tracker, and distribution license remain unresolved.

License decision pending; no distribution license granted by repository metadata. The collision feature set is aligned with HitboxAPI's public README and interfaces, which are a read-only functional reference; see [docs/HITBOXAPI_PARITY.md](docs/HITBOXAPI_PARITY.md) and the authoritative handover plan in [docs/CRL_HITBOX_ROADMAP.md](docs/CRL_HITBOX_ROADMAP.md).
