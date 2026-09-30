# HitboxAPI functional reference inventory

Purpose: record what the reference project exposes **as observable behaviour**, so the roadmap's
acceptance matrix can be checked against it. This is a read-only functional reference: no source,
algorithm, network protocol, license, package structure, asset or test is copied or adapted, and
HitboxAPI is not a dependency or a correctness oracle.

Authoritative plan: [CRL_HITBOX_ROADMAP.md](CRL_HITBOX_ROADMAP.md).

## 1. Reference facts (read-only)

| Property | Value |
| --- | --- |
| Repository | `https://github.com/AnECanSaiTin/HitboxAPI` |
| Platform | Minecraft 1.21.1 / NeoForge 21.1.73 (not this project's platform) |
| Reference license | GNU GPL-3.0 — **not** adopted by this project, and no code is taken from it |
| Reference size | 64 Java files, ~5,316 lines (clone kept outside this repository) |
| Documented feature set | README: AABB, OBB, Sphere, Capsule, Ray, Composite (nestable); `ColliderUtil` as the detection entry; an Entity cache attachment; F3+B rendering; no persistence; a JMH performance table |

The six absolute throughput targets quoted by the roadmap come from that README table and are
reproduced there as P01–P06; this file does not restate them as this project's measurements.

## 2. Read-only interface notes

These notes come from reading the reference's **public interface declarations** in order to describe
observable behaviour. They are descriptions, not designs to copy.

- A generic collider interface exposes a type tag, a nullable fast-AABB broad-phase value, an
  enable/disable flag, and a default collision hook. Its generic parameters bind a collider to an
  attached entity type and a per-collision payload type.
- A shape tag enum covers OBB, Sphere, Capsule, AABB, Ray and Composite.
- A separate local-space layer pairs local colliders with a coordinate converter that publishes
  position and rotation **version stamps** alongside a position and a rotation.
- A battle layer defines hit colliders (damage plus damage type, whose default collision hook applies
  the damage) and hurt colliders (damage scaling and a scale setter); both are serializable and
  expose a delta interface.
- An entity holder interface exposes separate hit-box and hurt-box maps keyed by `String`, plus an
  entity coordinate converter and an identity value.
- Networking is two S2C packets: a full collider snapshot and an incremental delta.
- Client rendering is a render interface plus six per-shape renderers, wired through a render
  dispatcher injection; a server entity injection also exists.

## 3. Capability comparison

The roadmap's F-IDs are the acceptance identifiers; this table maps the reference's observable
capability onto them and onto the current repository state.

| Reference capability | Roadmap item | Current state | Work implied by the roadmap |
| --- | --- | --- | --- |
| AABB / OBB / Sphere / Capsule detection | F01 | implemented (`double`, typed kernels, 30 overloads) | reuse; no kernel rewrite |
| Ray with length, origin and direction | F02 | only `Segment3d` (finite segment) | new `Ray3d` value plus a mutable collider in `api.collider` |
| Nestable composite with editing | F03 | `Composite` flattens at construction | new editable, snapshot-owned compound layer |
| Single convenient query entry | F04 | split between `GeometryIntersections` and `GeometryDistances` | `ColliderQueries` entry over snapshots |
| Per-collider fast broad phase | F04 | per-query bounds pruning only | snapshot `bounds()` used for negative-only pruning |
| Entity cache, non-persistent | F05 | `EntityHitboxHolder` + capability (solids only) | generic `EntityColliderHolder` for Ray/compound/enabled |
| Enable/disable flag and collision hook | F10 | not present (the kernel is immutable and pure) | mutable collider facade with `enabled`; the kernel stays pure |
| Version-stamped coordinate converter | F05 | `PlacedSolid3d` carries its own transform | entity-local → world adapter with an explicit frame contract |
| Explicit update entry and events | F10 | none | `EntityColliderUpdateEvent` plus `EntityColliders.requestUpdate` |
| F3+B debug rendering | F06 | none | client-only renderer over holder snapshots |
| Full and incremental network sync | deferred (roadmap §10.1) | a self-designed binary full snapshot exists in this repository | not part of the current main line; a separate decision |
| Hit and hurt battle layer, damage, persistence | excluded (roadmap §6) | none | out of scope for this stage |

## 4. Boundaries this inventory respects

- No reference source, algorithm, protocol layout, package naming, asset, metadata or test is copied,
  translated or adapted; the reference is not a dependency.
- The reference's GPL-3.0 license is not adopted, and nothing here implies adoption. This project's
  license remains unresolved.
- The reference's battle layer (damage application, serialization) and its delta protocol are
  recorded as **reference behaviour only**; the roadmap explicitly defers or excludes them.
- Every interface this project adds is its own design under `dev.crlhitbox`, judged by the roadmap's
  acceptance matrix rather than by reference fidelity.

## 5. How this inventory is maintained

Update it only when a reference capability becomes relevant to an acceptance item, and cite
observable behaviour rather than implementation detail. When the roadmap's R0 stage freezes the
functional and performance contract, replace the comparison column here with the confirmed F01–F10
decisions.
