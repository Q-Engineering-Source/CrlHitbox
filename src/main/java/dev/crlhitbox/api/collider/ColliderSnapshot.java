package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;

import java.util.Optional;

/**
 * Immutable, thread-safe view of one collider's local shape, placement and enable state.
 *
 * <p>A snapshot is the only thing a query or renderer reads. It owns its children defensively, so
 * later mutation of the mutable object that produced it cannot change it, and it precomputes its
 * conservative bounds when it is constructed rather than lazily during a query.</p>
 *
 * <p>{@link #bounds()} is expressed in the snapshot's parent frame. It is broad-phase data only: a
 * bounds overlap never proves an intersection, and only the narrow phase may return {@code true}.</p>
 */
public sealed interface ColliderSnapshot
        permits SolidColliderSnapshot, RayColliderSnapshot, CompoundColliderSnapshot {
    /** Returns whether this snapshot participates in queries and rendering. */
    boolean enabled();

    /** Returns the rigid placement from this snapshot's local frame into its parent frame. */
    RigidTransform3d localToParent();

    /**
     * Returns the conservative parent-frame bounds, or {@link Optional#empty()} when this snapshot
     * represents the empty set (it is disabled, or it is a compound without enabled leaves).
     */
    Optional<Aabb> bounds();
}
