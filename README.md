# CRL Hitbox

> An independent, non-official Cleanroom-only runtime hitbox and collision-query library for Minecraft 1.12.2.
>
> 面向 Minecraft 1.12.2 Cleanroom-only 环境的运行时碰撞箱与碰撞查询库。

CRL Hitbox is an independent project, not an official CleanroomMC component. Phase 1A provides an experimental, immutable, JDK-only, double-precision pointwise geometry foundation. It is common/server-safe and deliberately has no Minecraft, Forge, Cleanroom, JOML, LWJGL, networking, rendering, combat, or persistence dependency.

The implemented Phase 1A primitives are `Vec3d`, `Rotation3d`, `Aabb`, `Sphere`, `Obb`, `Segment3d`, and `Capsule`, with `Bounded3d`, distance helpers, and explicit intersection overloads. Geometry is finite, closed-set, and non-certified: touching intersects, but results are ordinary IEEE-754 pointwise evaluations and are never admission, continuous-collision, terrain-non-penetration, persistence, or proof authority.

Only these intersection pairs are supported in Phase 1A:

- `Aabb` / `Aabb`
- `Sphere` / `Sphere`, `Aabb`, and `Obb`
- `Capsule` / `Sphere` and `Capsule`
- `Segment3d` / `Sphere`, `Aabb`, `Obb`, and `Capsule`

Each listed mixed pair has both symmetric overload orders. In particular, this does not advertise a complete collider matrix: `Obb` / `Obb`, `Obb` / `Aabb`, `Capsule` / `Aabb`, `Capsule` / `Obb`, composites, generic shape dispatch, manifolds, penetration depth, normals, time of impact, and swept/continuous queries are deferred.

The finalized project identity is Maven group and Java base package `dev.crlhitbox`, main class `dev.crlhitbox.CrlHitbox`, and initial development version `0.1.0-SNAPSHOT`. Authors, project URLs, issue tracker, and distribution license remain unresolved.

License decision pending; no distribution license granted by repository metadata.
