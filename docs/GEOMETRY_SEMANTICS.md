# Phase 1E geometry semantics

## Status and numeric model

Phase 1E geometry is experimental, immutable, pointwise IEEE-754 `double` geometry. It is not certified interval arithmetic, an admission decision, a continuous-collision proof, a terrain non-penetration proof, a persistence authority, or a proof-receipt producer. Runtime query results must never be promoted to any of those authorities.

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

`Solid3d` is the sealed abstraction for closed solid geometry. Its exact permitted implementations are `Aabb`, `Sphere`, `Obb`, `Capsule`, and `Composite`. It adds no method beyond the inherited `Bounded3d.bounds()` contract. `Segment3d` implements `Bounded3d` but not `Solid3d`; there is no broad `Shape3d`, type enum, visitor, registry, or package-external solid implementation SPI.

`Capsule` is the closed set of all points whose distance from its finite centerline segment is at most its radius. “Centerline length” means the length of that segment; “complete end-to-end exterior length” means centerline length plus `2 * radius`. The API deliberately does not use an ambiguous “height” term. A zero-length capsule is geometrically a sphere, while a zero-radius capsule is geometrically its centerline.

`Rotation3d` stores a quaternion normalized once in binary64 arithmetic. Raw finite nonzero component tuples are scaled by their largest-magnitude component before normalization to avoid avoidable over/underflow, and signed zero is canonicalized. Tuples whose binary64 component values are exactly proportional as real numbers, including the equivalent `q` / `-q` forms, produce the same canonical stored rotation and compare exactly equal. Separately rounded component scaling or independently written decimal tuples that are not exactly proportional need not compare equal; equality remains an exact comparison of canonical stored bits, never a fuzzy or angular comparison. Rotation is never reconstructed from Euler angles during a query.

Every `Bounded3d` primitive returns a deterministic, finite, closed conservative world-axis-aligned `Aabb`. Bounds must already be representable when a primitive is created: for example, center-plus-radius, capsule expansion, and OBB center-plus-rotated extent may not overflow to infinity. An OBB uses `abs(R) * localHalfExtents` for its world extent; it is not bounded by partial corner sampling.

## Composite union semantics

`Composite` is an immutable, nonempty finite closed union of solid primitive leaves. Its public constructor takes a defensive snapshot of a `List<? extends Solid3d>`; null lists and null children are rejected, and the empty union is rejected because the current `Bounded3d` contract has no canonical finite bounds for it. No child collection, array, iterator, stream, or mutation API is exposed.

Nested `Composite` constructor inputs are accepted but their already-canonical primitive leaves are copied into one flat sequence without recursive traversal. Encounter order and duplicate leaves are preserved, while grouping boundaries are discarded. `childCount()` and `child(int)` therefore expose only the canonical sequence of `Aabb`, `Sphere`, `Obb`, and `Capsule` leaves; a stored child is never another `Composite`.

Composite equality is exact canonical structural equality. Leaf order and duplicate multiplicity affect `equals` and `hashCode`; nested and flat construction with the same leaf sequence compare equal. This is deliberately not general geometric-set equality: different sequences may represent the same union while remaining structurally unequal. Intersection results and bounds remain independent of leaf order, and duplicate insertion does not change either geometric result.

Composite bounds are computed eagerly as the exact coordinate-wise `Aabb` union of all leaf bounds. The calculation selects the six finite extrema without averaging, inflation, tolerance, or extra margin. These bounds are conservative broad-phase data only: bounds overlap never constitutes a positive Composite intersection result.

Generic solid intersection is existential over primitive leaves. Primitive/primitive dispatch is fixed `O(1)` and calls an existing typed overload. Composite/primitive dispatch is `O(n)` worst case, Composite/Composite dispatch is `O(n*m)` worst case, and Segment3d/Composite dispatch is `O(n)` worst case. Root and per-leaf bounds may reject candidates, but every positive result comes from an existing typed primitive narrow-phase kernel. Flat storage and iterative loops mean neither construction nor query execution performs user-controlled recursive descent. `Composite` has no local transform, ID, name, metadata, callback, enabled state, ownership hierarchy, or behavior.

## Rigid transform arithmetic

`RigidTransform3d` is the immutable active transform from a source/local coordinate frame into a target/parent coordinate frame. It stores one canonical `Rotation3d` and one finite `Vec3d` translation. It carries no frame ID, name, ownership, entity, model, or bone state.

For `T = (R, t)`, point and vector mappings are distinct:

```text
transformPoint(p)  = R(p) + t
transformVector(v) = R(v)
```

Translation affects points but never vectors. The direct inverse mappings are:

```text
inverseTransformPoint(p)  = R^-1(p - t)
inverseTransformVector(v) = R^-1(v)
```

`inverse()` constructs the rigid inverse:

```text
T^-1 = (R^-1, -R^-1(t))
```

Composition order is deliberately explicit:

```text
a.andThen(b) = b ∘ a
a.andThen(b).transformPoint(p) = b.transformPoint(a.transformPoint(p))
```

If `a = (Ra, ta)` and `b = (Rb, tb)`, the composed transform stores `Rb ∘ Ra` and `Rb(ta) + tb`. A typical future interpretation is `localToWorld = localToEntity.andThen(entityToWorld)`. `Rotation3d` remains the sole orientation representation owner; Phase 1D adds no public rotation-composition, quaternion, matrix, or Euler API. The private quaternion product is routed back through the existing `Rotation3d` constructor for normalization and q/-q canonicalization.

Transform equality remains exact equality of the canonical stored rotation and translation. Mathematical rigid transforms are associative, but ordinary binary64 rounding means separately grouped composed values are not promised to be bitwise equal. Observational composition and inverse checks therefore use strict test-only scale-derived tolerances; production has no transform epsilon or approximate equality.

Every operation either returns finite canonical values or fails with the existing finite-result convention. If a point, vector, inverse translation, composed translation, or composed rotation cannot be represented as finite binary64 output, delegated `Vec3d` or `Rotation3d` construction throws `IllegalArgumentException`. Values are never clamped to `Double.MAX_VALUE`, saturated, silently replaced with zero, or returned with NaN or infinity.

`RigidTransform3d` represents proper rotation plus translation only. It has no scale, shear, reflection, public matrix, Euler angles, yaw/pitch conversion, frame type parameter, or frame ID.

Phase 1D deliberately provides no production transformation method for `Solid3d`, `Bounded3d`, `Segment3d`, any primitive, or `Composite`, and adds no transformed-solid wrapper. `Aabb` stores exact min/max endpoints; not every valid endpoint pair is losslessly representable as an `Obb` center plus symmetric half extents. Future AABB rigid projection must therefore not silently recast min/max storage into a lossy center/half-extent representation. The representation contract for local/world solid projection remains separately gated.

## Non-materializing placed solids

`PlacedSolid3d` is the immutable coordinate-placement value for one local `Solid3d` and one local/source-to-parent/target `RigidTransform3d`. For `P = PlacedSolid3d(S, T)`, the represented parent-frame set is:

```text
P = { T.transformPoint(p) | p belongs to S }
```

`PlacedSolid3d` implements `Bounded3d` but not `Solid3d`. The sealed solid set remains exactly `Aabb`, `Sphere`, `Obb`, `Capsule`, and `Composite`; placement is a coordinate value rather than another concrete solid. Placement has no frame ID, Entity, dimension, owner, holder, name, tag, behavior, serialization, or mutable state.

Both operands of `intersects(PlacedSolid3d, PlacedSolid3d)` must map into the same caller-defined parent frame. A `segmentInParent` passed to a placed query must be expressed in that same frame. The API deliberately cannot detect a caller’s frame mismatch. A future world placement can be formed explicitly as:

```text
new PlacedSolid3d(
    localHitbox,
    localHitboxToEntity.andThen(entityToWorld)
)
```

Placement equality is exact structural equality of the local solid and local-to-parent transform. Derived bounds do not participate independently in equality. Differently decomposed placements may represent the same mathematical set without comparing equal.

Parent-frame bounds are eager, finite, deterministic, conservative `Aabb` values used only for negative broad-phase pruning. Bounds overlap never produces a positive narrow-phase result. Exact identity placement returns the original local bounds without recomputation.

Primitive placed bounds are derived as follows:

- Local `Aabb` retains its original min/max endpoints and rigid frame. It is never converted through `Aabb.center()`, `halfExtents()`, or a materialized `Obb`.
- Local `Obb` uses its intrinsic `(orientation, center)` pose followed by the placement, while retaining its symmetric local half-extent intervals.
- Local `Sphere` transforms the center and preserves radius.
- Local `Capsule` transforms both finite centerline endpoints and preserves radius.
- Local `Composite` applies one placement to every canonical flat primitive leaf and unions the resulting leaf bounds; it stores no placed child wrappers.

For a rigid interval box with parent origin `o`, parent bases `u_i`, exact local intervals `[min_i,max_i]`, and candidate parent axis `L`, define `c_i = dot(u_i,L)`. Its closed projection interval is:

```text
low  = dot(o,L) + sum(c_i >= 0 ? min_i*c_i : max_i*c_i)
high = dot(o,L) + sum(c_i >= 0 ? max_i*c_i : min_i*c_i)
```

The production implementation evaluates these direct asymmetric supports with scale-aware power-of-two product decomposition and compensated summation. Exact-zero coefficients do not affect scale selection. Conservative box bounds combine analytic support extrema with all eight direct transformed local-corner witnesses, covering ordinary binary64 operation-order differences without an epsilon or arbitrary inflation.

Placed rigid box pairs use the complete 15-axis separating-axis theorem: three basis axes from each box and nine pairwise cross axes. A cross axis is skipped only when all three computed components are exactly zero. Near-parallel nonzero axes are retained. Projection intervals separate only when strictly disjoint, so boundary equality remains touching/intersection.

Segment, sphere, and capsule queries against a rigid interval box first use direct `inverseTransformPoint` mapping for every representable parent endpoint, preserving the observable binary64 point mapping. Per-axis scaled differences then avoid constructing a local `Segment3d` whose endpoint subtraction could overflow. If a direct inverse-mapped point itself cannot be represented, the query falls back to scale-aware relative parent-endpoint projection without constructing an inverse transform. Segment queries preserve the finite `[0,1]` slab; sphere uses point-to-local-AABB residual; capsule uses true finite centerline-to-local-AABB residual. Radius-zero, zero-centerline, point/line/plane boxes, subnormal rotation terms, and face/edge/corner contact retain the existing closed-set behavior.

Placed non-box primitive pairs reuse existing sphere/capsule numerical kernels through representation-preserving parent points/endpoints. Placed `Composite` queries iterate the canonical flat leaf sequence; Composite/Composite uses a Cartesian product, and Segment/Composite uses a leaf loop. Bounds are negative-only at both root and leaf levels. There is no recursion, BVH, spatial index, hit-leaf result, callback, or transformed-solid hierarchy.

Identity placement agrees exactly with existing typed/generic primitive and Segment3d queries. Applying one common moderate rigid placement to both operands preserves their pointwise query result observationally; generic invariance tests avoid accidental boundary fixtures except where independently constructed tangency is the specific regression.

Phase 1E adds no materialization method and exposes no transformed primitive, rigid-box helper, support interval, inverse cache, or placed child list. Entity/Capability holders and platform integration remain separately gated.

## Algorithms and local numerical policy

Distance helpers compute finite-segment projection/clamping, segment-to-segment distance, point-to-AABB distance, point-to-OBB distance, `segmentToAabbSquared(Segment3d, Aabb)`, and `segmentToObbSquared(Segment3d, Obb)`. Squared distances may be positive infinity if a squared finite quantity necessarily overflows, but they never return `NaN`, negative values, or negative zero.

Segment-to-box distance uses a finite active-set reduction. For a segment parameter in `[0, 1]`, each coordinate can cross the corresponding box minimum or maximum at most once. The implementation considers the two domain endpoints plus at most six such crossings (at most eight breakpoints total), then at most one stationary candidate in each of the at most seven open intervals. It therefore has fixed, bounded, non-iterative work and finds the true pointwise squared distance to the closed box. For an OBB, the segment and box are reduced to the same normalized local box representation used by the AABB path.

Capsule/box queries compare the centerline-to-box distance against the capsule radius in normalized coordinates, so the predicate avoids unreliable direct squaring at extreme magnitudes while retaining the closed `<=` tangency rule. A zero-radius capsule has the corresponding segment result; a point centerline has the corresponding sphere result.

Box pairs use fixed separating-axis tests. OBB/OBB tests all 15 candidate axes: the three local axes of each box and the nine pairwise cross-product axes. A cross-product axis is skipped only when its computed components are exactly zero. AABB/OBB uses the same 15-axis structure with the three world axes, the three OBB local axes, and the nine cross-product axes. AABB projections use their stored minimum and maximum endpoints, preserving degenerate planes, lines, and points without reconstructing them through a center/extent representation. Separation remains strict, so tangency is an intersection.

The SAT projection arithmetic is scale-aware per axis: it normalizes the candidate axis, decomposes products by binary power-of-two exponents and mantissas, and combines scaled terms with compensated summation. This is a local numerical technique, not certified arithmetic and not a tolerance. The segment-to-segment implementation may separately use a named, documented, scale-aware conditioning threshold only for its near-parallel denominator. There is no global collision epsilon: neither technique alters closed/touching semantics, becomes a project-wide epsilon, or justifies overlap inflation. All other intersection decisions use the stated closed comparisons without a global tolerance.

## Explicit Phase 1E query surface

`GeometryDistances` exposes exactly six public static methods:

- `pointToSegmentSquared(Vec3d, Segment3d)`
- `segmentToSegmentSquared(Segment3d, Segment3d)`
- `pointToAabbSquared(Vec3d, Aabb)`
- `segmentToAabbSquared(Segment3d, Aabb)`
- `pointToObbSquared(Vec3d, Obb)`
- `segmentToObbSquared(Segment3d, Obb)`

Of the 30 public static `GeometryIntersections` overloads, the 27-overload non-placed surface remains the existing 24 typed primitive overloads plus exactly these three generic overloads:

- `intersects(Solid3d, Solid3d)`
- `intersects(Segment3d, Solid3d)`
- `intersects(Solid3d, Segment3d)`

Phase 1E adds exactly three placed-query overloads:

- `intersects(PlacedSolid3d, PlacedSolid3d)`
- `intersects(Segment3d, PlacedSolid3d)`
- `intersects(PlacedSolid3d, Segment3d)`

The four-primitive-solid unordered matrix remains complete for `Aabb`, `Sphere`, `Obb`, and `Capsule`; the six mixed primitive pairs have both argument orders. Those typed overloads remain the sole numerical kernels. Generic solid dispatch adds sealed runtime routing and Composite leaf-union traversal without changing primitive numerical semantics. The finite segment-to-solid surface likewise has both generic orders, and the solid-first overload delegates to the canonical Segment3d-first implementation.

The public geometry inventory is exactly 14 top-level types. `GeometryDistances` remains at six public methods and `GeometryIntersections` exposes exactly 30 public overloads.

| Category | Supported queries |
| --- | --- |
| Solid-solid | `Aabb/Aabb`; `Sphere/Sphere`; `Obb/Obb`; `Capsule/Capsule`; `Aabb/Sphere` and `Sphere/Aabb`; `Aabb/Obb` and `Obb/Aabb`; `Aabb/Capsule` and `Capsule/Aabb`; `Sphere/Obb` and `Obb/Sphere`; `Sphere/Capsule` and `Capsule/Sphere`; `Obb/Capsule` and `Capsule/Obb` |
| Finite-segment | `Segment3d/Aabb` and `Aabb/Segment3d`; `Segment3d/Sphere` and `Sphere/Segment3d`; `Segment3d/Obb` and `Obb/Segment3d`; `Segment3d/Capsule` and `Capsule/Segment3d` |
| Generic solid | Any permitted `Solid3d/Solid3d` pair, including primitive/Composite and Composite/Composite unions |
| Generic finite-segment | `Segment3d/Solid3d` and `Solid3d/Segment3d`, including Composite unions |
| Placed solid | `PlacedSolid3d/PlacedSolid3d` in one caller-defined parent frame |
| Placed finite-segment | `Segment3d/PlacedSolid3d` and `PlacedSolid3d/Segment3d` |

The explicitly deferred surface is transformed-solid materialization; production transformed primitive getters; Entity/Capability holders; frame IDs; generic distance to `PlacedSolid3d`, `Solid3d`, or `Composite`; nearest or intersecting leaves; hit paths; hit result types; collision manifolds; penetration depth; contact normals; metadata; time of impact; swept or continuous collision detection; and platform integration. Phase 1E has no universal `intersects(Object, Object)` dispatcher, no `intersects(Bounded3d, Bounded3d)` overload, and no Solid3d/PlacedSolid3d convenience overload.

## Purity, isolation, and future integration

Every geometry and query operation is side-effect-free. It performs no callbacks, damage, input or shape mutation, world/entity access, packet send, NBT write, normal-path logging, global-state allocation, time access, thread-local access, or random access.

The geometry package is JDK-only and uses no Minecraft, Forge, Cleanroom, LWJGL, JOML, `javax.vecmath`, networking, NBT, or platform API. The `compileGeometryIsolation` verification task compiles only this package with Java 25, an empty external classpath, and an empty annotation-processor path; its isolated output remains under `build/` and is not added to runtime packaging.

Minecraft- or loader-specific adaptation belongs to future layers. Such code must convert platform values at the geometry-package boundary and may not leak platform types, reflection-based adapters, string class names, or `Object`-typed adaptation seams into the geometry API.
