# CRL Hitbox

> An independent, non-official Cleanroom-only runtime hitbox and collision-query library for Minecraft 1.12.2.
>
> 面向 Minecraft 1.12.2 Cleanroom-only 环境的运行时碰撞箱与碰撞查询库。

CRL Hitbox is an independent project, not an official CleanroomMC component. Its experimental geometry foundation provides immutable, JDK-only, double-precision pointwise primitives and solid unions. It is common/server-safe and deliberately has no Minecraft, Forge, Cleanroom, JOML, LWJGL, networking, rendering, combat, or persistence dependency.

The implemented primitives are `Vec3d`, `Rotation3d`, `Aabb`, `Sphere`, `Obb`, `Segment3d`, and `Capsule`, with `Bounded3d`, distance helpers, and explicit intersection overloads. `Solid3d` is sealed to the four primitive solids plus `Composite`; `Segment3d` remains a finite non-solid query primitive. Geometry is finite, closed-set, and non-certified: touching intersects, but results are ordinary IEEE-754 pointwise evaluations and are never admission, continuous-collision, terrain-non-penetration, persistence, or proof authority.

The pointwise solid-solid matrix is complete for `Aabb`, `Sphere`, `Obb`, and `Capsule`: every unordered pair is supported, and each mixed pair has both symmetric overload orders. Generic sealed dispatch preserves those typed kernels. Immutable, nonempty `Composite` values represent finite unions whose nested inputs are flattened into an ordered primitive-leaf sequence; their bounds are broad-phase enclosures only. Finite `Segment3d` queries are supported against every primitive solid and `Composite`, in both overload orders.

Generic distances, hit-leaf or hit-path results, local transforms, solid metadata, collision manifolds, penetration depth, contact normals, time of impact, swept or continuous queries, certified geometry, and any platform/entity/network/combat/render integration are deferred. The sealed solid set does not admit third-party implementations in this phase. This library does not claim to replace Minecraft physics.

The finalized project identity is Maven group and Java base package `dev.crlhitbox`, main class `dev.crlhitbox.CrlHitbox`, and initial development version `0.1.0-SNAPSHOT`. Authors, project URLs, issue tracker, and distribution license remain unresolved.

License decision pending; no distribution license granted by repository metadata.
