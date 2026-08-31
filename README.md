# CRL Hitbox

> An independent, non-official Cleanroom-only runtime hitbox and collision-query library for Minecraft 1.12.2.
>
> 面向 Minecraft 1.12.2 Cleanroom-only 环境的运行时碰撞箱与碰撞查询库。

CRL Hitbox is an independent project, not an official CleanroomMC component. Its experimental geometry foundation provides immutable, JDK-only, double-precision pointwise primitives. It is common/server-safe and deliberately has no Minecraft, Forge, Cleanroom, JOML, LWJGL, networking, rendering, combat, or persistence dependency.

The implemented primitives are `Vec3d`, `Rotation3d`, `Aabb`, `Sphere`, `Obb`, `Segment3d`, and `Capsule`, with `Bounded3d`, distance helpers, and explicit intersection overloads. Geometry is finite, closed-set, and non-certified: touching intersects, but results are ordinary IEEE-754 pointwise evaluations and are never admission, continuous-collision, terrain-non-penetration, persistence, or proof authority.

The pointwise solid-solid matrix is complete for `Aabb`, `Sphere`, `Obb`, and `Capsule`: every unordered pair is supported, and each mixed pair has both symmetric overload orders. Finite `Segment3d` queries are also supported against each of those four solids, again in both overload orders. `Segment3d` remains a finite query primitive, not a solid collider.

`Composite` shapes, generic shape dispatch, collision manifolds, penetration depth, contact normals, time of impact, swept or continuous queries, certified geometry, and any platform/entity/network/combat/render integration are deferred. This library does not claim to replace Minecraft physics.

The finalized project identity is Maven group and Java base package `dev.crlhitbox`, main class `dev.crlhitbox.CrlHitbox`, and initial development version `0.1.0-SNAPSHOT`. Authors, project URLs, issue tracker, and distribution license remain unresolved.

License decision pending; no distribution license granted by repository metadata.
