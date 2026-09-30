# HitboxAPI parity and porting plan

Status: **planning record, source-verified**. The source repository is cloned locally for comparison
at `D:\Code\HitboxAPI` (read-only reference, outside this repository). Licensing and provenance
changes are applied. No ported code exists in this repository yet.

## 1. Source facts (verified)

| Property | Value | Evidence |
| --- | --- | --- |
| Repository | `https://github.com/AnECanSaiTin/HitboxAPI` | clone |
| License | **GNU GPL-3.0** | `LICENSE`; `gradle.properties` `mod_license=GNU GPL 3.0` |
| Minecraft | **1.21.1** | `gradle.properties` |
| Loader | **NeoForge 21.1.73** | `gradle.properties` |
| Mappings | Parchment 2024.07.28 | `gradle.properties` |
| Mod id / package | `hitboxapi` / `cn.anecansaitin` | `gradle.properties` |
| Size | **64 Java files, ~5,316 lines**, plus one Mixin config, one PNG, one lang file, one NeoForge mods.toml | clone inventory |
| Math | `float` through **JOML** (`Vector3f`, `Quaternionf`, `Intersectionf`) | `ColliderUtil`, `ICoordinateConverter` |

## 2. Owner decisions

1. The owner authorized a **direct port/adaptation** of HitboxAPI code and accepted that this project
   becomes a GPL-3.0 derivative work. This supersedes the earlier clean-room clauses for adapted
   material (already applied to `AGENTS.md`, `docs/BASELINE.md`, `THIRD_PARTY_NOTICES.md`,
   `README.md`, `LICENSE`).
2. For the target API surface, the owner selected **`onCollide` callbacks plus a `disable` flag,
   confined to the adapter layer**: the frozen geometry kernel stays pure, immutable and free of
   callbacks.

## 3. Source module inventory (verified)

```
api/common/collider/          ICollider, IAABB, IOBB, ISphere, ICapsule, IRay, IComposite,
                              ColliderTyep, ColliderUtil
api/common/collider/local/    ILocalCollider, ILocalAABB, ILocalOBB, ILocalSphere, ILocalCapsule,
                              ILocalRay, ILocalComposite, ICoordinateConverter
api/common/collider/battle/   IHitCollider, IHurtCollider, IIncremental
api/common/attachment/        IEntityColliderHolder
api/client/collider/          IColliderRender, ColliderRenderUtil
common/collider/basic/        AABBPlus, Sphere, OBB, Capsule, Ray, Composite
common/collider/local/        Local{AABB,Sphere,OBB,Capsule,Ray,Composite},
                              EntityCoordinateConverter, LocalCompositeCoordinateConverter
common/collider/battle/hit/   HitLocal{AABB,Sphere,OBB,Capsule,Ray,Composite}
common/collider/battle/hurt/  HurtLocal{AABB,Sphere,OBB,Capsule,Ray,Composite}
common/attachment/            EntityColliderHolder
common/network/               S2CBattleColliderFullSyne, S2CBattleColliderIncrementalSyne
common/                       HitboxDataAttachments, HitboxNetwork, listener/LevelTick
client/collider/render/       Entity{AABB,Sphere,OBB,Capsule,Ray,Composite}Render
mixin/client/                 EntityRenderDispatcherMixin
mixin/common/                 ServerEntityMixin
```

## 4. Source API shape (verified by reading the interfaces)

- `ICollider<T, D>`: generic over an **attached entity type** `T` and a **per-collision payload
  type** `D`; declares `getType()`, a nullable `getFastCollider()` AABB broad-phase,
  `setDisable(boolean)` / `disable()`, and a default `onCollide(T entity, O otherEntity,
  ICollider<O,?> other, D data)` hook.
- `ColliderTyep`: `OBB, SPHERE, CAPSULE, AABB, RAY, COMPOSITE` (spelling as in the source).
- `ILocalCollider<T, D> extends ICollider<T, D>`: a marker for **local-space** colliders.
  `ICoordinateConverter` supplies `positionVersion()` / `rotationVersion()` (`short`) plus
  `getPosition()` / `getRotation()` — a **version-stamped** local-to-world converter used for cache
  invalidation, implemented by `EntityCoordinateConverter` and
  `LocalCompositeCoordinateConverter`.
- `ColliderUtil` holds the narrow-phase kernels and the 6×6 dispatch, plus segment distance,
  closest-point and segment-crossing helpers; tolerances `1e-6` / `0.01` appear in the source.
- **Battle layer (a core feature, not an add-on):**
  - `IHitCollider extends ILocalCollider<Entity, Void>, INBTSerializable<CompoundTag>,
    IIncremental<CompoundTag>`: carries damage and a `ResourceKey<DamageType>`, and its default
    `onCollide` **actually damages the other entity** via `enemy.hurt(...)`.
  - `IHurtCollider extends ILocalCollider<Entity, Void>, INBTSerializable<CompoundTag>,
    IIncremental<CompoundTag>`: `modifyDamage(float)` scales incoming damage and `setScale(float)`.
  - `IIncremental<T extends Tag>`: `shouldUpdate()`, `getUpdate()`, `update(T)` — the **delta**
    protocol used by the S2C incremental packet.
- `IEntityColliderHolder extends IIncremental<CompoundTag>` (its own comment calls it "a simple
  example"): `Map<String, IHurtCollider> getHurtBox()`, `Map<String, IHitCollider> getHitBox()`,
  add/remove by name, `getFastHitCollider()`, `getCoordinateConverter()`, `UUID getID()`.
- Networking is **NBT-payload based**: `HitboxNetwork` plus `S2CBattleColliderFullSyne` and
  `S2CBattleColliderIncrementalSyne` (full snapshot and delta).
- Client rendering: `IColliderRender` / `ColliderRenderUtil` plus six per-shape renderers, injected
  through `EntityRenderDispatcherMixin`; a server-side `ServerEntityMixin` also exists, with
  `hitboxapi.mixins.json`.

## 5. Current CRL Hitbox surface

- Frozen, JDK-only, `double`, pure, immutable geometry (`Aabb`, `Sphere`, `Obb`, `Capsule`,
  `Segment3d`, `Composite` (flattened), `PlacedSolid3d`, `RigidTransform3d`, `Vec3d`, `Rotation3d`,
  `Bounded3d`, `Solid3d`), `GeometryDistances` (6), `GeometryIntersections` (30), exact
  reconstruction (`reconstructExact`), and `compileGeometryIsolation` + `jdeps -> java.base`.
- Entity-local `EntityHitboxHolder` (`ResourceLocation` → `PlacedSolid3d`) + immutable snapshot +
  Forge capability, with no Entity/World reference.
- Self-designed server-authoritative synchronization: `crlhitbox` S2C channel, **binary** full
  snapshot (protocol version 1), provider generation, replica state, stale ordering, explicit
  resend, StartTracking and player-lifecycle delivery, client-main-thread install, bounded pending
  store. No C2S, no delta, no acknowledgement.

## 6. Gap table

| Source capability | Current state | Gap |
| --- | --- | --- |
| AABB / Sphere / OBB / Capsule narrow phase | implemented (`double`, typed kernels) | functionally covered |
| Compound, **nested** | `Composite` is flattened at construction | hierarchy and per-level transform missing |
| Ray (origin + direction + length) | `Segment3d` covers finite segments | no ray type; different representation |
| `ColliderUtil` single entry | split into `GeometryIntersections` + `GeometryDistances` | no unified entry |
| Fast AABB broad phase per collider | per-query bounds pruning only | no per-collider cached fast AABB |
| `disable` flag | none (immutability is a kernel rule) | **approved** for the adapter layer |
| `onCollide` callback | none (purity is a kernel rule) | **approved** for the adapter layer |
| `local/` colliders + version-stamped converter | `PlacedSolid3d` holds its own transform | different mechanism; no version stamps |
| `hit`/`hurt` battle colliders + damage application | none, and damage is currently forbidden | **large, currently out of scope** |
| NBT serialization (`INBTSerializable`) | none, holder is deliberately non-persistent | **conflicts with current policy** |
| Incremental delta sync (`IIncremental` + S2C incremental) | self-designed full-only binary protocol | **two incompatible synchronization designs** |
| Entity holder with hitBox/hurtBox maps + UUID | holder keyed by `ResourceLocation` | different key model and content |
| Client rendering (6 renderers + F3+B) | none, rendering is forbidden | needs new authorization |
| Mixins (server entity + render dispatcher) | none, Mixin is forbidden | needs new authorization |
| `LevelTick` listener | none | small, but platform-specific |

## 7. Structural conflicts a port must resolve

1. **Numeric type and dependencies.** Source is `float`/JOML; the kernel is `double`, JDK-only, and
   gated by `compileGeometryIsolation` (empty external classpath) plus `jdeps -> java.base`.
   Importing JOML or float math into the geometry package would break both gates.
2. **Purity versus combat callbacks.** The source's `IHitCollider.onCollide` **applies damage**.
   This project's rule is that collision queries perform no damage, no callbacks, no world mutation,
   and no logging. Adopting the battle layer therefore redefines the boundary: the pure kernel must
   remain the numeric authority, and damage application must live in a platform adapter that the
   caller invokes explicitly.
3. **Persistence.** The source serializes colliders to NBT and syncs deltas. This project's holder is
   deliberately non-persistent and its protocol is a self-designed binary full snapshot with
   generation/revision ordering. Choosing NBT-delta parity would replace the Phase 2B design rather
   than extend it.
4. **Two placement models.** Source: local collider + external version-stamped converter. Project:
   immutable `PlacedSolid3d` carrying its own `RigidTransform3d` under a caller-defined parent frame.
   Keeping both risks two competing truths about an entity's world colliders.
5. **Nesting.** `Composite` intentionally discards grouping; nested compound semantics (hierarchy plus
   per-level transforms) require a new value type, not a modification of the frozen one.
6. **Forbidden mechanisms.** Mixins (`ServerEntityMixin`, `EntityRenderDispatcherMixin`), client
   rendering (six renderers, F3+B), and NBT persistence are all explicitly excluded by the current
   scope documents. Each needs separate owner authorization.
7. **Platform.** `DataAttachment`, `INBTSerializable`, NeoForge payload registration, modern damage
   sources and render events have no 1.12.2 equivalent; that side must be written for
   Cleanroom/Forge regardless of how much algorithm code is adapted.

## 8. Porting routes

**Route A — capability parity on top of the frozen kernel (recommended).** Keep `double`, JDK-only,
pure, immutable geometry as the numeric authority. Add an adapter layer outside the frozen package
that mirrors the source's capability surface: collider protocol + `ColliderTyep`-equivalent union,
`ColliderUtil`-equivalent dispatch over the existing typed kernels, per-collider fast AABB broad
phase, nested compound values, a finite ray value, local-collider wrapper plus a version-stamped
converter that reuses `RigidTransform3d`, `disable` flags and `onCollide` callbacks confined to the
adapter, and an entity holder adapter over the existing capability. Damage application, if wanted,
becomes an explicit platform call the adapter may make; it never enters the kernel.

**Route B — representation parity.** Replace the frozen value model with the source's (axes OBB,
center+direction+height capsule, cached vertices/fast colliders, mutable disable, JOML float math).
This discards the exact-reconstruction work, the isolation gate, the current 14/6/30 public closure,
the wire protocol, and most of the 465-test suite.

## 9. GPL-3.0 obligations

Status: **applied** (`LICENSE`, `gradle.properties`, `src/main/resource-templates/mcmod.info`,
`build.gradle` Blossom property, `AGENTS.md`, `docs/BASELINE.md`, `THIRD_PARTY_NOTICES.md`,
`README.md`). Adapted files must still carry a per-file derivation notice naming the source and
stating that changes were made.

## 10. Open questions

1. **Battle scope.** Is the `hit`/`hurt` layer — which applies real damage — in scope? If yes, which
   parts (damage application, `modifyDamage`/`setScale`, `DamageType` mapping to 1.12.2 damage
   sources) and under whose authority (server only)?
2. **Synchronization.** Keep this project's binary full-snapshot protocol (already implemented and
   verified) and add an incremental delta on top, or replace it with the source's NBT full+incremental
   design? The two are not compatible.
3. **Persistence.** Adopt NBT serialization for colliders, or keep the deliberate non-persistent
   holder and treat persistence separately?
4. **Mixin and rendering.** Authorize a client-only rendering phase and the two Mixin injections, or
   leave both out and skip F3+B?
5. **Naming.** Keep the source's public names (`ICollider`, `ColliderUtil`, `ColliderTyep` with its
   spelling) for familiarity, or use this project's naming conventions?
6. **Entity holder model.** Source uses `Map<String, ...>` with a `UUID` identity and separate
   hit/hurt maps; this project uses `ResourceLocation` keys over `PlacedSolid3d`. Which model wins?

## 11. Next steps

1. Owner answers Section 10 (at minimum: battle scope and synchronization).
2. Read the remaining source modules in detail — `HitboxNetwork`, both S2C packets,
   `EntityCoordinateConverter`, `LocalCompositeCoordinateConverter`, one `common/collider/basic`
   implementation per shape, and the entity attachment — and record their exact signatures here.
3. Implement Route A in reviewed slices, starting with the collider protocol, `ColliderTyep`
   equivalent, and the `ColliderUtil` equivalent dispatch over the frozen kernel, with the
   `disable`/`onCollide` adapter behavior the owner approved.
