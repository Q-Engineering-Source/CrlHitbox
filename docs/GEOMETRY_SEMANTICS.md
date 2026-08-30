# Phase 1A geometry semantics

## Status and numeric model

Phase 1A geometry is experimental, immutable, pointwise IEEE-754 `double` geometry. It is not certified interval arithmetic, an admission decision, a continuous-collision proof, a terrain non-penetration proof, a persistence authority, or a proof-receipt producer. Runtime query results must never be promoted to any of those authorities.

All public geometry inputs are finite `double` values. `NaN` and both infinities are rejected at construction and operation boundaries where applicable. Construction canonicalizes `-0.0` to `0.0`, so signed zero has one stored representation. Value equality and `hashCode` are exact over these canonical stored values; there is no fuzzy equality.

Geometry represents closed sets. A point on a boundary is contained, and touching counts as intersection. This policy is exact in the API contract: production code has no global collision epsilon and does not inflate shapes or overlap tests.

## Valid degenerate shapes

The following are valid closed sets, not errors:

- A `Segment3d` whose endpoints coincide.
- A zero-radius `Sphere`.
- A zero-radius `Capsule`.
- A `Capsule` whose centerline endpoints coincide.
- An `Aabb` with zero width on one or more axes.
- An `Obb` with zero half-extent on one or more axes.

Negative radii and negative OBB half-extents are invalid. An `Aabb` requires component-wise `min <= max`. A raw quaternion must be finite and nonzero. A primitive is also rejected if its mandatory finite bounds cannot be represented.

## Primitive semantics

`Segment3d` is a finite, closed segment, never an infinite ray and never itself a solid collider. Endpoint order does not change the represented geometry; its stored endpoint convention is canonical. Both endpoints must be finite and their component-wise delta must be representable as finite `double` values, so distance and clipping operations have a finite segment direction. A zero-length segment is a point.

`Capsule` is the closed set of all points whose distance from its finite centerline segment is at most its radius. “Centerline length” means the length of that segment; “complete end-to-end exterior length” means centerline length plus `2 * radius`. Phase 1A deliberately does not use an ambiguous “height” term. A zero-length capsule is geometrically a sphere, while a zero-radius capsule is geometrically its centerline.

`Rotation3d` stores a quaternion normalized once in binary64 arithmetic. Raw finite nonzero component tuples are scaled by their largest-magnitude component before normalization to avoid avoidable over/underflow, and signed zero is canonicalized. Tuples whose binary64 component values are exactly proportional as real numbers, including the equivalent `q` / `-q` forms, produce the same canonical stored rotation and compare exactly equal. Separately rounded component scaling or independently written decimal tuples that are not exactly proportional need not compare equal; equality remains an exact comparison of canonical stored bits, never a fuzzy or angular comparison. Rotation is never reconstructed from Euler angles during a query.

Every `Bounded3d` primitive returns a deterministic, finite, closed conservative world-axis-aligned `Aabb`. Bounds must already be representable when a primitive is created: for example, center-plus-radius, capsule expansion, and OBB center-plus-rotated extent may not overflow to infinity. An OBB uses `abs(R) * localHalfExtents` for its world extent; it is not bounded by partial corner sampling.

## Algorithms and local numerical policy

Distance helpers compute finite-segment projection/clamping, segment-to-segment distance, point-to-AABB distance, and point-to-OBB distance. Squared distances may be positive infinity if a squared finite quantity necessarily overflows, but they never return `NaN`, negative values, or negative zero.

The segment-to-segment implementation may use a named, documented, scale-aware conditioning threshold only for its near-parallel denominator. That threshold is local to that distance algorithm and does not alter closed/touching semantics, become a project-wide epsilon, or justify overlap inflation. All other intersection decisions use the stated closed comparisons without a global tolerance.

## Explicit Phase 1A query surface

The supported overloads are exactly the following 17 operations. Mixed-shape reverse orders are symmetric delegations to the canonical implementation.

| Category | Supported queries |
| --- | --- |
| Solid-solid | `Aabb/Aabb`; `Sphere/Sphere`; `Sphere/Aabb` and `Aabb/Sphere`; `Sphere/Obb` and `Obb/Sphere`; `Capsule/Sphere` and `Sphere/Capsule`; `Capsule/Capsule` |
| Finite-segment | `Segment3d/Sphere` and `Sphere/Segment3d`; `Segment3d/Aabb` and `Aabb/Segment3d`; `Segment3d/Obb` and `Obb/Segment3d`; `Segment3d/Capsule` and `Capsule/Segment3d` |

The explicitly deferred surface is: `Obb/Obb`; `Obb/Aabb` and `Aabb/Obb`; `Capsule/Aabb` and `Aabb/Capsule`; `Capsule/Obb` and `Obb/Capsule`; any `Composite` pair; generic shape dispatch; collision manifolds; penetration depth; contact normals; time of impact; and swept or continuous collision detection. Phase 1A has no universal `intersects(Object, Object)`-style dispatcher or partially supported generic collision abstraction.

## Purity, isolation, and future integration

Every geometry and query operation is side-effect-free. It performs no callbacks, damage, input or shape mutation, world/entity access, packet send, NBT write, normal-path logging, global-state allocation, time access, thread-local access, or random access.

The geometry package is JDK-only and uses no Minecraft, Forge, Cleanroom, LWJGL, JOML, `javax.vecmath`, networking, NBT, or platform API. The `compileGeometryIsolation` verification task compiles only this package with Java 25, an empty external classpath, and an empty annotation-processor path; its isolated output remains under `build/` and is not added to runtime packaging.

Minecraft- or loader-specific adaptation belongs to future layers. Such code must convert platform values at the geometry-package boundary and may not leak platform types, reflection-based adapters, string class names, or `Object`-typed adaptation seams into the geometry API.
