# HitboxAPI parity and porting plan

Status: **planning record**. No ported code exists in this repository yet. This document records the
source facts, the owner's decision to port, the exact capability and structure gaps, the licensing
obligations that decision creates, and the two possible porting routes.

## 1. Source facts (verified)

| Property | Value | Evidence |
| --- | --- | --- |
| Repository | `https://github.com/AnECanSaiTin/HitboxAPI` | README |
| License | **GNU GPL-3.0** | `LICENSE` file; `gradle.properties` `mod_license=GNU GPL 3.0` |
| Minecraft | **1.21.1** | `gradle.properties` `minecraft_version` |
| Loader | **NeoForge 21.1.73** | `gradle.properties` `neo_version` |
| Mappings | Parchment 2024.07.28 | `gradle.properties` |
| Mod id / package | `hitboxapi` / `cn.anecansaitin` | `gradle.properties` |
| Author | AnECanSaiTin | `gradle.properties` |
| Purpose | "Add more colliders for Minecraft" | `gradle.properties` `mod_description` |

## 2. Owner decision

The repository owner has authorized a **direct port/adaptation of HitboxAPI code** and accepted that
this project becomes a GPL-3.0 derivative work. That decision supersedes, for ported and adapted
material, the clean-room clauses previously recorded in `AGENTS.md` and `docs/BASELINE.md`
("HitboxAPI may later be consulted only as a functional reference", "do not copy or adapt its
source, package structure, assets, metadata, serialization, tests, or algorithms").

Section 9 lists the obligations this creates and Section 10 lists the files that must change before
any ported code is committed.

## 3. Source capability surface (from its README)

- Collider kinds: AABB, Oriented Bounding Box, Sphere, Capsule, Ray, and Compound.
- Compound colliders combine several colliders and **may be nested**.
- Entry point for detection: `ColliderUtil` static methods.
- For entities: a collision-box cache is attached through `HitboxDataAttachments#COLLISION`.
- That cached collision box can be drawn with **F3 + B**.
- The attached data has **no persistence implementation**.
- Performance claims come from a **JMH** benchmark (JDK 21, float-based `org.joml` math).

## 4. Source API shape (from `ColliderUtil.java`)

The source is generic and entity-bound, not a pure value kernel:

- Collider interfaces: `ICollider<T1, D1>`, `IAABB`, `IOBB`, `ISphere`, `ICapsule`, `IRay`,
  `IComposite`, with a `ColliderTyep` enum (spelling as in the source) driving dispatch.
- The two type parameters bind a collider to an **entity type** and a **data type**; detection
  methods take `(collider, entity, data, other, entity, data)` so colliders living in **different
  coordinate systems** can be compared through a coordinate-transform stack.
- Every collider may expose a **fast collider** (`getFastCollider()`, an `IAABB`) used as a cheap
  broad-phase rejection before narrow phase.
- Every collider can be **disabled** (`disable()`), and `colliding(x, x)` short-circuits to `true`.
- Detection **fires callbacks** (`onCollide(entity1, entity2, other, data)`) when a hit is found.
- Compound dispatch walks `getCollidersCount()` / `getCollider(i)` and recurses into nested
  compounds.
- Math is `float` through **JOML** (`Vector3f`, `Intersectionf`), including `testObOb`,
  `testAabSphere`, `testRayAab`, `testLineSegmentSphere`.
- Representation differs from this project's kernel:
  - `IOBB`: center + three axes + half extents + cached vertices.
  - `ICapsule`: center + direction + height (not two centerline endpoints).
  - `IRay`: origin + direction + **length** plus `getEnd()`, so it is a **finite** ray/segment, not
    an infinite ray.
- Tolerance constants appear in the source (`1e-6`, `0.01`); this project's kernel instead forbids a
  global collision epsilon and requires closed-set `<=` comparisons.

## 5. Current CRL Hitbox surface

Already implemented and verified in this repository:

- Immutable, JDK-only, `double` geometry: `Vec3d`, `Rotation3d`, `Aabb`, `Sphere`, `Obb`, `Capsule`,
  `Segment3d`, `Composite`, `PlacedSolid3d`, `RigidTransform3d`, `Bounded3d`, `Solid3d`, plus
  `GeometryDistances` (6 methods) and `GeometryIntersections` (30 overloads, including placed
  queries). `Composite` is a **flattened** canonical leaf sequence.
- `Rotation3d.reconstructExact` (W108) for exact wire round trips.
- Entity-local `EntityHitboxHolder` + immutable `EntityHitboxSnapshot` + Forge capability.
- Server-authoritative, versioned, direct-binary full-snapshot synchronization over the `crlhitbox`
  S2C channel, with provider generation, replica state, explicit resend, StartTracking and player
  lifecycle delivery, client-main-thread installation, and a bounded pending store.

## 6. Capability-level gap table

| Source capability | Current state | Gap |
| --- | --- | --- |
| AABB / Sphere / OBB / Capsule detection | implemented (`double`, no callbacks) | covered functionally |
| Compound, **nested** | flattened `Composite` only | nesting/hierarchy semantics missing |
| Ray | `Segment3d` (finite segment, canonical endpoints) | representation differs (origin+direction+length); no "ray" type |
| Fast collider broad phase | per-query bounds pruning only | no per-collider cached `IAABB` |
| Disable flag | none (immutability) | not represented |
| `onCollide` callbacks | none (purity is a hard project rule) | not represented |
| Entity/data generics + transform stack | `PlacedSolid3d` + caller-supplied frame | different mechanism |
| Entity collision-box cache | `EntityHitboxHolder` + capability | covered |
| F3+B debug rendering | none | missing, and currently forbidden |
| No persistence | same (non-persistent holder/replica state) | already aligned |
| JMH benchmark | a manual pure-core probe only | missing, and currently excluded |

## 7. Structural conflicts a port must resolve

1. **Numeric type.** The source is `float`/JOML; this kernel is `double`, JDK-only, and its geometry
   package is byte-frozen with an isolation task that compiles with an empty external classpath.
   Porting JOML-based code wholesale would break `compileGeometryIsolation` and the `jdeps
   -> java.base` gate.
2. **Purity.** The source fires `onCollide` callbacks and reads entity state; this project's rule is
   that collision queries have no callbacks, world access, or side effects. Adopting callbacks
   requires an explicit amendment of that rule.
3. **Immutability.** The source has `disable()` and cached derived state (axes, vertices, fast
   collider); this kernel is immutable with no cached mutable state.
4. **Representation.** Capsule-as-center+direction+height versus two endpoints, and OBB-as-axes
   versus stored `Rotation3d`, are different value models; "porting" either one means redefining the
   existing frozen types or adding parallel ones.
5. **Nesting.** `Composite` deliberately discards grouping; nested compound semantics need a new
   design (hierarchy and per-level transforms).
6. **Platform.** `DataAttachment` (1.21.1/NeoForge), modern rendering, and `@Mod` differences mean
   the entity/rendering side must be written for 1.12.2/Cleanroom regardless of how much algorithm
   code is adapted.
7. **Forbidden features.** F3+B rendering and JMH are explicitly excluded by the current scope
   documents; both need a new scoped authorization.

## 8. Two porting routes

**Route A — capability parity (adapter layer on top of the frozen kernel).** Keep the existing
`double`, JDK-only, pure, immutable kernel as the numeric authority. Add a separate, non-frozen
`api/collider` layer that mirrors the source's *capability* surface: a `ColliderTyep`-equivalent
union, an `ICollider`-equivalent protocol, a fast-collider broad phase, nested compound values, a
ray value with finite length, and a `ColliderUtil`-equivalent dispatch entry. Callbacks, if kept,
live in that layer and never inside the geometry package. Consequences: geometry stays frozen and
isolated; the public API grows; the source's JOML/float arithmetic is not reproduced, so numeric
results will not be bit-comparable with the source.

**Route B — representation parity (port the value model itself).** Replace or extend the frozen
geometry types with the source's representations (axes-based OBB, center+direction+height capsule,
cached fast colliders, mutable disable flags, callbacks). Consequences: the geometry freeze, the
isolation gate, exact wire round trips (W108), the existing 14/6/30 public closure, and the existing
protocol would all have to be reworked; the existing test suite would be substantially invalidated.

Recommendation: **Route A**, with callbacks and rendering decided separately, because Route B
discards verified work (exact reconstruction, isolation, wire protocol) for representation fidelity
that the 1.12.2 platform does not require.

## 9. GPL-3.0 obligations created by the owner decision

Adapting GPL-3.0 code makes this project a covered work. Before any adapted code is committed:

1. Add the full GPL-3.0 text as `LICENSE` and set the project license consistently in
   `gradle.properties` (`mod_license`) and the resource templates (`mcmod.info` / `mcmod.info`
   templates generated by Blossom).
2. Keep the upstream copyright notice and license notices intact, and state clearly that this is a
   **modified** port (source project, author, and the fact that changes were made).
3. Convey the complete corresponding source of the whole work under GPL-3.0 (no additional
   restrictions, no per-file proprietary terms).
4. Record the origin in `THIRD_PARTY_NOTICES.md` and in `docs/BASELINE.md`, replacing the current
   clean-room section with the porting provenance.
5. Remove or amend every contradictory statement in `AGENTS.md` (clean-room clauses, "no license has
   been selected", "do not add a SPDX header").
6. Keep the template MIT notice accurate: GPL-3.0 for this project does not remove the template's
   MIT notice for template-derived files.

This document does not provide legal advice; it records the obligations implied by choosing to adapt
GPL-3.0 code so the owner can accept them explicitly.

## 10. Files that must change for licensing

| File | Change |
| --- | --- |
| `LICENSE` | new file: verbatim GPL-3.0 text |
| `gradle.properties` | `mod_license` and any license URL fields |
| `src/main/resource-templates/mcmod.info` | license field |
| `AGENTS.md` | replace clean-room and "no license" clauses with porting provenance and GPL-3.0 terms |
| `docs/BASELINE.md` | replace the clean-room boundary section with porting provenance |
| `THIRD_PARTY_NOTICES.md` | add HitboxAPI (GPL-3.0) notice and the modification statement |
| `README.md` | state the license and the porting relationship |
| Source files adapted from the source | per-file notice that the file is derived and modified |

## 11. Open questions

1. Which porting route (Section 8) does the owner want?
2. Are `onCollide` callbacks and the entity/data generic + transform-stack model part of the target
   surface, given they conflict with the current purity and value-model rules?
3. Is F3+B debug rendering in scope, and if so is a client-only rendering phase authorized?
4. Should the ported work keep the source's public names (`ICollider`, `ColliderUtil`,
   `ColliderTyep` including its spelling) for drop-in familiarity, or use this project's naming
   conventions?
5. Is a max-payload/performance benchmark wanted at all, given the current scope excludes JMH?

## 12. Next steps

1. Owner answers Section 11.
2. Apply the Section 10 licensing changes in one reviewed commit.
3. Fetch the remaining source modules (collider interfaces and implementations, the entity data
   attachment, and the rendering path) as porting input, and record their exact inventory here.
4. Implement Route A in reviewed slices, starting with the collider protocol and the
   `ColliderUtil`-equivalent dispatch over the existing frozen kernel.
