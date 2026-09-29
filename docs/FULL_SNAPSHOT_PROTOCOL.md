# Phase 2B full-snapshot protocol

Status: **PHASE 2B INCOMPLETE**. Implemented and covered by automated gates: the deterministic
direct-binary codec, the `crlhitbox` S2C channel with its single client-bound message, provider
generation allocation, internal replica state, `EntityHitboxSync`, StartTracking and
player-lifecycle delivery, client-main-thread installation, and the bounded pending store. What
remains is runtime acceptance: real maximum-payload decode cost, an authoritative dedicated-server
run, and a correlated independent real-GPU client run.

This document is the record of the frozen Phase 2B wire and replication contract and of the exact
boundary between what has been built and what remains. Passing automated gates is not live
server/client acceptance.

## 1. Channel and message identity

| Property | Frozen value |
| --- | --- |
| SimpleNetworkWrapper channel name | `crlhitbox` |
| Protocol version (first payload byte) | `1` |
| Message discriminator | `0` |
| Direction | server to client only (S2C) |
| Semantic name | full Entity hitbox snapshot |

The protocol version is independent of the project/mod version and of Java serialization version
IDs. The decoder rejects any other version before decoding entries. The protocol version remains
internal implementation state; no stable public type exposes it.

No second discriminator, no handshake, no C2S message, no resync request, and no acknowledgement
exist or are authorized in this phase.

## 2. Exact wire frame

The payload excludes the transport discriminator. Fixed-width numeric values use the buffer's
ordinary network byte order.

1. `protocolVersion` — unsigned byte, required value `1`
2. `dimensionId` — signed 32-bit integer
3. `entityId` — nonnegative VarInt, at most 5 encoded bytes
4. `entityUuidMost` — signed 64-bit long holding raw UUID bits
5. `entityUuidLeast` — signed 64-bit long holding raw UUID bits
6. `holderGeneration` — signed 64-bit long, must be `> 0`
7. `serverRevision` — signed 64-bit long, must be `>= 0`
8. `entryCount` — nonnegative VarInt
9. `entryCount` entry records in holder insertion order

Each entry record is:

1. ResourceLocation identifier
2. `Solid3d` payload
3. `RigidTransform3d` payload

Entries are never reordered or sorted; class names, enum ordinals, and reflection order never
appear on the wire.

### 2.1 Identifier encoding

The canonical `ResourceLocation.toString()` UTF-8 bytes are encoded as a nonnegative VarInt byte
length followed by the exact bytes. The length must be at least 1 and at most
`MAX_RESOURCE_LOCATION_BYTES`. Decoding uses malformed-input **reporting** (never replacement
characters), never Java modified UTF-8, never NBT strings, and never a platform-default charset.
Decoded text is accepted only by the exact 1.12.2 `ResourceLocation` constructor, which lower-cases
and defaults an empty namespace; no additional canonicalization is invented.

Duplicate identifiers in one snapshot are rejected on encode and on decode. Both sides compare the
identifier the **receiver** reconstructs from the transmitted bytes — not the sender's in-process
`ResourceLocation` value — so the encoder and the decoder accept and reject exactly the same entry
sequences, and a server can never transmit a snapshot that every receiver is guaranteed to reject as
a duplicate. Because reconstruction can change the canonical text (a missing or one-character
namespace becomes `minecraft`, and the text is lower-cased), the encoder applies the
`MAX_RESOURCE_LOCATION_BYTES` limit to that reconstructed text as well, and the decoder rejects any
identifier whose reconstructed text exceeds it. Every accepted payload therefore stays re-encodable
(Section 8).

### 2.2 Stable solid tags

| Tag | Shape |
| --- | --- |
| `0` | `Aabb` |
| `1` | `Sphere` |
| `2` | `Obb` |
| `3` | `Capsule` |
| `4` | `Composite` |

Tags are frozen protocol literals. They are never derived from class names, enum ordinals, hash
codes, sealed-permit order, or reflection order. An unknown tag is a decoder error.

### 2.3 Primitive and transform field order

Every double is one IEEE-754 binary64 value (8 bytes, network order).

- `Aabb` (tag 0): `min.x, min.y, min.z, max.x, max.y, max.z`
- `Sphere` (tag 1): `center.x, center.y, center.z, radius`
- `Obb` (tag 2): `center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z,
  orientation.x, orientation.y, orientation.z, orientation.w`
- `Capsule` (tag 3): `centerline.start.x, .y, .z, centerline.end.x, .y, .z, radius`
- `RigidTransform3d`: `rotation.x, rotation.y, rotation.z, rotation.w, translation.x,
  translation.y, translation.z`

A `PlacedSolid3d` is its local solid followed by its `localToParent` transform; there is no
placement tag of its own.

Decoding constructs values only through existing public immutable geometry constructors. Rotations
are restored through `Rotation3d.reconstructExact`, the geometry-owned exact reconstruction entry,
so the four transmitted stored component bits survive the round trip. No trusted wire constructor,
reflection path, or private bit injection exists, and the legacy normalizer is unchanged.

### 2.4 Composite flat-leaf encoding

`Composite` (tag 4) is a nonnegative VarInt `primitiveLeafCount` followed by that many leaf records,
each a tag in `0..3` and its primitive payload, in canonical encounter order. Flat grouping
boundaries are already discarded by the geometry value, so grouping history is never encoded.
Bounds, cached values, type names, entry identifiers inside a Composite, child transforms, and
metadata are never encoded.

A Composite payload must not contain tag 4, and zero-leaf Composite payloads are rejected.

## 3. Protocol limits

| Limit | Frozen value |
| --- | --- |
| `MAX_MESSAGE_BYTES` | 1,048,576 |
| `MAX_ENTRIES` | 4,096 |
| `MAX_RESOURCE_LOCATION_BYTES` | 1,024 |
| `MAX_COMPOSITE_LEAVES` | 4,096 |
| `MAX_TOTAL_PRIMITIVE_LEAVES` | 16,384 |

These are internal protocol limits, not holder limits. A primitive local solid contributes one
leaf; a Composite contributes its canonical primitive child count; the total is tracked across the
whole payload.

Required rejection behavior:

- payload byte size is validated before count-driven allocation;
- every count is validated before storage is allocated;
- negative counts, overlong VarInts, unknown tags, nested Composite tags, zero-leaf Composites,
  duplicate entry identifiers, truncated data, trailing unread bytes, non-finite doubles, negative
  radii and half-extents, invalid AABB ordering, unrepresentable mandatory bounds, `generation <= 0`,
  `revision < 0`, and negative entity IDs are rejected;
- no array, list, or map is ever allocated from an unvalidated peer count;
- no partially installed or partially decoded snapshot is ever published.

VarInt decoding is bounded to five encoded bytes; a fifth byte whose payload exceeds `0x0F`, a
fifth byte that still requests continuation, a non-shortest (redundant) encoding, or a final value
that does not fit the nonnegative 32-bit domain is rejected. Only the canonical encoding of a value
is accepted.

There is no compression and no fragmentation. A server snapshot that exceeds these limits is
unsendable and must fail before transmission; chunking is a separately gated future change.

## 4. Entity identity on the wire

A full snapshot identifies its target with all of: signed dimension ID, nonnegative runtime entity
ID, UUID most-significant bits, UUID least-significant bits, and the provider generation.

Client installation requires that the current client world dimension equals the message dimension,
that the entity resolved by runtime ID exists, that its UUID exactly equals the message UUID, and
that both the public holder capability and the internal replica state are attached. A message is
never applied on entity ID alone, never applied when the UUID differs, and never queued for a
mismatched entity. World names and entity class names are never identity.

## 5. Generation and revision semantics

The server-side generation is one positive `long` per attached provider/incarnation. It is assigned
once, immutable for that provider, distinct for provider instances until allocator exhaustion,
monotonically allocated inside one JVM process, never random, never wall-clock derived, never
derived from an entity ID or UUID, never persisted, and never exposed publicly. A process-global
`AtomicLong`-style allocator is permitted only for this allocation, must retain no Entity, holder,
provider, UUID, world, or snapshot reference, and must fail before returning a nonpositive or
wrapped value. Replacing an Entity object creates a new generation; a server restart may restart the
allocator; client cleanup makes ordering session-local. Uniqueness and persistence across processes
are explicitly not claimed.

Two revisions are deliberately distinct:

1. **Server wire revision** — the source `EntityHitboxSnapshot.revision()` carried in the message,
   used for stale-message ordering and stored in internal client replica state.
2. **Client holder local revision** — the client holder's own monotonic mutation counter, advanced
   once when `replaceContents` changes entries. It is never overwritten by a packet.

Internal client replica state additionally records the client holder's local revision immediately
after the last successful install, which prepares a future delta phase to detect client-local
modification. It is not public in this phase.

## 6. Full-snapshot acceptance ordering

For a fully decoded message targeting an existing matching entity, with incoming generation `G` and
wire revision `R` against previously accepted `G0` and `R0`:

| Condition | Result |
| --- | --- |
| no previous accepted snapshot | accept |
| `G > G0` | accept, even when `R < R0` |
| `G < G0` | reject as stale |
| `G == G0` and `R < R0` | reject as stale |
| `G == G0` and `R == R0` | accept as an idempotent authoritative reassertion |
| `G == G0` and `R > R0` | accept |

After a successful install, `acceptedRemoteGeneration = G`, `acceptedRemoteRevision = R`, and
`holderLocalRevisionAtLastInstall = holder.revision()`. Replica state is never updated before holder
replacement succeeds. Comparisons never use unsigned arithmetic, and both values are validated
before comparison.

## 7. Repair, delivery, and lifecycle contract

- Equal-generation/equal-revision full snapshots are deliberately accepted so that an authoritative
  reassertion repairs a client-local mutation. Equal wire revision is never treated as an automatic
  no-op before contents are compared and replaced.
- `StartTracking` delivery sends one full snapshot directly to the entering `EntityPlayerMP`,
  including when the holder is empty: the empty snapshot is meaningful because it establishes
  generation/revision and clears stale client contents. No broadcast, class filtering, world scan,
  tick loop, Mixin, or tracker injection is used.
- Player login, respawn, and dimension-change self snapshots are sent to the player's own entity
  directly. Non-persistence is preserved: a respawn-created provider may hold an empty revision-0
  holder, and no clone copy is synthesized.
- The explicit server API is `EntityHitboxSync.sendFullTo(Entity, EntityPlayerMP)` and
  `EntityHitboxSync.sendFullToTrackingAndSelf(Entity)`: server-side, server-thread-confined, one
  captured immutable snapshot per operation, no holder mutation, no revision change, and no
  asynchronous scheduling.
- Holder mutation itself never sends packets; already-tracked entities require an explicit resend.
- The client handler schedules exactly one task onto the client logical game thread and returns
  `null`; world selection, dimension checks, entity lookup, UUID checks, capability access,
  stale-order checks, holder replacement, and pending-store changes happen only inside that task.
- Pending store limits are `MAX_PENDING_MESSAGES = 256`, `MAX_PENDING_ENCODED_BYTES = 16 * 1,048,576`,
  and `PENDING_TTL_CLIENT_TICKS = 200`, keyed by dimension ID, entity ID, and UUID, holding no
  Entity, world, holder, capability, or network-handler reference. Pending state is cleared on
  connection start and disconnection and pruned for unloaded dimensions.
- There is no StopTracking removal packet, no static entity-to-holder mirror, and no automatic
  delta or resync path. Phase 2C owns delta production and resynchronization.

`replaceContents` is the atomic client installation primitive: it never adopts the source snapshot
revision, validates and copies the ordered replacement before publishing, rejects duplicate
identifiers, preserves snapshot order and exact immutable placements, and advances the local
revision exactly once only when contents or order actually change. It contains no client-only or
protocol-specific behavior and never invokes networking.

## 8. Determinism and encoder/decoder behavior

For identical captured message state, encoded bytes are identical: entry insertion order and
Composite leaf encounter order are preserved, identifier bytes are canonical and deterministic, and
no hash-map iteration order, timestamp, random nonce, compressed-stream metadata, or platform-default
charset can affect output. Repeated encoding does not mutate the message or snapshot.

Encoding captures dimension ID, entity ID, UUID, provider generation, and one immutable snapshot
once; the message stays self-consistent when the holder is mutated afterwards. Counts, identifiers,
the leaf budget, and the total encoded size are validated before the first byte is written, so an
unsendable snapshot fails before any channel send. The explicit send path surfaces an actionable
exception; automatic lifecycle handlers must catch that local failure, log one actionable error
containing entity ID, UUID, generation, revision, entry count, and reason, and send nothing.

Decoding is all-or-nothing and requires exact payload exhaustion. Fields are assigned only after the
complete payload is valid, a failed decode exposes no partially initialized value, and no entity,
holder, capability, world, or pending state is touched while parsing. Malformed input throws an
actionable protocol failure; valid but stale identity/generation/revision is rejected silently or at
debug level without mutation and without disconnecting.

Any byte sequence that the decoder accepts must decode to a stable value: decoding its re-encoding
reproduces exactly the same identity, generation, revision, ordered identifiers, and placements, and
the second re-encoding is byte-identical. This holds because the decoder rejects non-shortest VarInt
encodings and requires exact exhaustion; both properties are enforced by the fixed-seed property
suite rather than asserted by review alone.

Byte-level identity between an arbitrary accepted input and its re-encoding is **not** promised, and
deliberately so: 1.12.2 `ResourceLocation` lower-cases and re-splits identifier text, so text such as
`CRLHITBOX:Part` is accepted and re-encoded canonically without rejecting the identifier (see
Section 9). This encoder never emits bytes that the decoder would reject: duplicate detection and the
identifier byte limit both key on the bytes actually written and on the identifier the receiver
reconstructs from them, and entries whose reconstruction would collide or exceed the limit fail
before transmission.

## 9. Known identifier conflict (open, preserved)

The Protocol 2.1 rule encodes `ResourceLocation.toString()`, so identifier text that the exact
1.12.2 two-argument constructor accepts without validation can collapse when re-split:

| Original | Encoded text | Decoded |
| --- | --- | --- |
| `("a", "b")` | `a:b` | `("minecraft", "b")` |
| `("crl:hitbox", "part")` | `crl:hitbox:part` | `("crl", "hitbox:part")` |

The one-character namespace case collapses because 1.12.2 `splitObjectName` adopts a namespace only
when the first colon index is greater than 1. Both cases are recorded with executable evidence in
`EntityHitboxWireCodecPhase2BTest` and are deliberately **not** repaired by silently restricting
valid identifiers or by changing the frozen wire format. The two consequences are instead handled
explicitly and are covered by tests:

- Encode-side duplicate detection and the byte-limit check both use the identifier the receiver
  reconstructs from the transmitted text, so a snapshot whose entries would collapse into one
  received identifier — or whose received text would exceed the limit — fails before transmission
  instead of being rejected by every receiver.
- The decoder rejects an identifier whose reconstructed text would exceed the frozen byte limit, so
  the accepted domain stays re-encodable.

## 10. Exclusions and authority boundary

The wire carries only entity identity, generation, revision, ordered identifiers, and immutable
geometric placements. It never carries entity position, previous position, yaw, pitch, body or head
yaw, `renderYawOffset`, eye height, world bounds, partial ticks, model or bone pose, animation state,
roles, damage, team filters, callbacks, metadata, or tags. No entity-to-world pose composition and no
world-space hitbox cache exist.

Both holder state and synchronization state remain non-persistent: no NBT holder storage, no
generation persistence, no accepted replica-state persistence, no entity save key, and no player
clone copy.

The stored geometry remains ordinary pointwise IEEE-754 `double` geometry. Neither a holder, a
snapshot, nor a decoded payload is certified interval geometry, an admission authority, a
continuous-collision proof, a terrain non-penetration proof, persistence truth, or a proof receipt.

## 11. Implemented versus outstanding

Implemented and verified by automated gates:

- frozen protocol constants and limits;
- strict bounded VarInt coding;
- direct binary codec for every `Solid3d`, `PlacedSolid3d`, and `RigidTransform3d` representation,
  including flat Composite encoding, via existing public immutable geometry constructors;
- all-or-nothing decoding with exact exhaustion and pre-allocation validation;
- the immutable payload value shared by both directions, plus an encoder that validates limits before
  writing the first byte;
- the `crlhitbox` channel with exactly one message (discriminator `0`, `Side.CLIENT`), registered
  from the Phase 2A/2B bootstrap after both capabilities exist;
- the transport-free network-thread handler that verifies the receive side and only schedules
  (`FullSnapshotInboundDispatch`, `FullSnapshotHandler`);
- provider generation allocation, the internal replica-state capability, and the frozen acceptance
  ordering, all with fixed-seed property coverage;
- `EntityHitboxSync.sendFullTo` / `sendFullToTrackingAndSelf`, including the server-side and
  server-thread guards and a pre-transport unsendable-snapshot failure carrying entity ID, UUID,
  generation, revision, and entry count;
- StartTracking delivery and player login/respawn/dimension-change self snapshots, with fail-safe
  logging that sends nothing and never crashes the event bus;
- client-only SidedProxy seam, client-main-thread installation, and the bounded pending store with
  tick retry and connection/world-unload cleanup.

Outstanding and required before any Phase 2B completion claim:

- a measured maximum-legal-payload decoder cost (CPU, allocation, GC, peak heap) against a payload
  built by this encoder, including an invalid-tail variant;
- an authoritative dedicated-server run in which the isolated server reaches readiness and
  participates in the documented window;
- a correlated independent real-GPU client run with matching artifact identity.

The remaining work is runtime acceptance, not missing implementation. No live network exchange,
dedicated-server readiness, or GPU evidence exists, so the honest status of this phase remains
**PHASE 2B INCOMPLETE**.

Real GPU acceptance: not executed
