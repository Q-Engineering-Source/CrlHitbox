/**
 * Entity-local named hitbox ownership above the immutable geometry kernel.
 *
 * <p>{@link dev.crlhitbox.api.entity.EntityHitboxHolder} is mutable identity state confined to its
 * owning logical game thread. Entries map immutable Minecraft ResourceLocation IDs to immutable
 * {@link dev.crlhitbox.api.geometry.PlacedSolid3d} values in one abstract, caller-defined
 * entity-local frame. The holder owns no Entity or World reference and performs no pose or
 * world-space conversion.</p>
 *
 * <p>{@link dev.crlhitbox.api.entity.EntityHitboxSnapshot} is an immutable deterministic capture
 * of a holder revision and insertion-ordered entries. The Entity capability is attached on both
 * logical sides, is non-sided for facing lookup, and remains non-persistent and unsynchronized.
 * Phase 2A defines no networking, tracking protocol, holder generation, world-pose adapter,
 * rendering, hit/hurt role, or combat behavior.</p>
 */
package dev.crlhitbox.api.entity;
