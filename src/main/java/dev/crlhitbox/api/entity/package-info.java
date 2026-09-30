/**
 * Entity-local named collider ownership above the immutable geometry kernel.
 *
 * <p>{@link dev.crlhitbox.api.entity.EntityHitboxHolder} is mutable identity state confined to its
 * owning logical game thread. Entries map immutable Minecraft ResourceLocation IDs to immutable
 * {@link dev.crlhitbox.api.geometry.PlacedSolid3d} values in one abstract, caller-defined
 * entity-local frame. The holder owns no Entity or World reference and performs no pose or
 * world-space conversion.</p>
 *
 * <p>{@link dev.crlhitbox.api.entity.EntityColliderHolder} is the generic cache beside it: it stores
 * immutable {@link dev.crlhitbox.api.collider.ColliderSnapshot} values, so finite rays and compounds
 * can be attached as well. Publishing is explicit — mutating a standalone collider changes the cache
 * only when its snapshot is stored — and the cache is non-persistent and holds no Entity or World
 * reference.</p>
 *
 * <p>{@link dev.crlhitbox.api.entity.EntityColliderFrames} adapts entity-local snapshots to a world
 * frame. Its translation-only frame uses the entity position point with local axes parallel to the
 * world axes; it reads no yaw, pitch, model pose or partial tick, and a caller needing a rotated frame
 * supplies it explicitly.</p>
 *
 * <p>Both snapshots are immutable deterministic captures of a holder revision and its
 * insertion-ordered entries. The Entity capabilities are attached on both logical sides, are
 * non-sided for facing lookup, and remain non-persistent.</p>
 */
package dev.crlhitbox.api.entity;
