# Phase 2A entity holder semantics

## Purpose and entry identity

`EntityHitboxHolder` is mutable identity-bearing state for named hitbox placements associated with
one Minecraft Entity capability provider. It retains no Entity or World reference. Each entry maps
one non-null `ResourceLocation` ID to one non-null immutable `PlacedSolid3d`; ResourceLocation owns
namespace/path validation, and different IDs may retain equal placements.

Entries preserve insertion order. A new ID appends, an unequal replacement retains the existing
position, an equal replacement is a no-op, removal deletes the position, and removal followed by
re-addition appends the ID at the end. No public mutable collection, iterator, stream, or map view is
exposed.

## Revision and mutation

Revision starts at `0`. Adding a new ID, replacing an existing ID with an unequal placement,
removing a present ID, or clearing a nonempty holder increments it exactly once. Reads, snapshots,
equal replacements, absent removals, and clearing an empty holder do not advance it. Clear never
resets revision and never increments once per entry.

An effective mutation at `Long.MAX_VALUE` fails with `IllegalStateException` before any entry or
revision change. Null validation also completes before mutation. Revision is holder-local only;
Phase 2A defines no generation, epoch, UUID, timestamp, tick, protocol revision, or network
incarnation.

`replaceContents(snapshot)` is a later local-only holder operation. It validates and copies the
snapshot's ordered IDs and placements before publishing a replacement, rejects duplicate IDs, and
uses this holder's revision rather than importing the snapshot revision. Equal ordered contents are
a no-op; a different sequence replaces all entries and advances the holder revision exactly once.
Validation or revision-overflow failure leaves the existing contents unchanged. This method adds no
network installation, side authority, persistence, or Phase 2B lifecycle protocol.

## Threading and snapshots

The holder is deliberately unsynchronized. Server-side mutation and capture belong on the owning
server thread; client-side mutation and capture belong on the owning client thread. Concurrent
access to one holder is unsupported, Phase 2A does not enforce side authority, and it performs no
automatic thread dispatch.

`EntityHitboxSnapshot` is an immutable defensive capture of revision, ordered IDs, and exact
immutable placements. Later additions, replacements, removals, or clear operations cannot change
an earlier snapshot. Indexed access is O(1), snapshot lookup is O(n), and structural equality
includes revision plus the ordered ID and placement sequences. A completed snapshot may safely be
handed to another thread.

## Frame contract

Every stored `PlacedSolid3d.localToParent()` targets one abstract caller-defined entity-local holder
frame. Phase 2A does not select an Entity origin, feet/center/eye convention, body or head frame,
model or bone axes, yaw/pitch sign, interpolation phase, partial tick, or server/client pose sampling
rule. The holder does not read position or rotation fields, produce world-space geometry, or cache
world bounds. A future reviewed adapter must explicitly supply `entityLocalToWorld` and compose it
with the existing transform rule.

## Capability, lifecycle, and ownership

Forge attaches one provider under `crlhitbox:entity_hitboxes` to every `Entity`. Each provider owns
one fresh holder, exposes the same holder for every `EnumFacing` including null, and has no owner
back-reference or global registry. Destroying an Entity naturally releases its provider and holder
when the Entity becomes unreachable.

The capability is intentionally non-persistent. Its required `Capability.IStorage` writes null and
performs no read mutation, while the provider implements `ICapabilityProvider` only—not
`ICapabilitySerializable` or `INBTSerializable`. No holder entry or revision is stored in NBT, and
no clone, respawn, dimension-transfer, logout, reload, restart, or data-fixer policy preserves the
state. A newly constructed Entity object therefore receives a new empty holder at revision zero.

The capability may be attached on both logical sides, but server and client holders are independent
side-local states. There is no mirroring, packet, tracking hook, snapshot installation, delta,
resynchronization, or automatic full snapshot. Server authority is a future intended policy, not a
Phase 2A enforcement mechanism.

## Phase 2B replication contract

Phase 2B defines a server-authoritative, versioned, direct-binary full-snapshot replication channel
for holder contents. Its exact wire frame, limits, tags, and acceptance rules are recorded in
[FULL_SNAPSHOT_PROTOCOL.md](FULL_SNAPSHOT_PROTOCOL.md). The channel is S2C only: no C2S message,
acknowledgement, or resync request exists.

The following are contract, not implementation detail:

- The **server wire revision** is the source snapshot revision, orders stale messages, and is stored
  in internal client replica state. The **client holder local revision** is the client holder's own
  mutation counter and is never overwritten by a packet.
- An accepted full snapshot overwrites client holder contents, so a client-local change may be
  replaced by the next accepted authoritative snapshot. Equal-generation/equal-revision snapshots
  are accepted deliberately so that they can repair such a local change.
- Holder mutation itself never sends a packet. An already-tracked entity requires an explicit full
  resend; no delta production, resynchronization request, or automatic mutation observation exists
  in this phase.
- `replaceContents` never adopts the source snapshot revision, and the server remains authoritative
  for this replication channel only.

Status: the codec, the `crlhitbox` S2C channel, provider generation, internal replica state,
`EntityHitboxSync`, tracking and player-lifecycle delivery, client-main-thread installation, and the
bounded pending store are implemented and covered by automated gates. Runtime acceptance is still
outstanding: no dedicated-server run and no real-GPU client run have been performed, so live
server/client replication is not claimed. See
[FULL_SNAPSHOT_PROTOCOL.md](FULL_SNAPSHOT_PROTOCOL.md) for the exact boundary.

## Explicit exclusions and authority boundary

Holder mutation performs no Entity/World access, event posting, networking, persistence I/O,
ordinary mutation logging, damage, callback, time/random access, world placement, registration, or
side inspection. Phase 2A defines no hit/hurt/collision role, enabled state, tags, metadata, damage
scale, animation, rendering, F3+B integration, Mixin, Access Transformer, or coremod.

The stored geometry remains ordinary pointwise IEEE-754 `double` geometry. Neither a holder nor a
snapshot is certified geometry, an admission authority, a continuous-collision proof, a terrain
non-penetration proof, persistence truth, or a proof receipt. Phase 2B's server-authoritative
tracking full snapshots, provider generation, protocol versioning, entity identity, and
stale-message handling are specified in [FULL_SNAPSHOT_PROTOCOL.md](FULL_SNAPSHOT_PROTOCOL.md); only
their codec core is implemented.
