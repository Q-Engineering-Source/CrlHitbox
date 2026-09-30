package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;

/**
 * Read-only view of one collider: an immutable snapshot plus the identity of the mutable object that
 * published it.
 *
 * <p>A {@code Collider} is either a mutable facade owned by one logical thread or an external
 * provider implemented by another mod. Queries never call provider code: they consume the immutable
 * {@link ColliderSnapshot} returned by {@link #snapshot()}, so a provider cannot change shape in the
 * middle of a query or inject behaviour into the narrow phase. A provider must keep the snapshot it
 * returns stable until a new one is published.</p>
 *
 * <p>{@code localToParent} is the whole collider's extra placement. A shape's own orientation (for
 * example an OBB's or a capsule's) is internal to the shape and is not the same value.</p>
 */
public interface Collider {
    /** Returns the checked monotonic revision of this collider; effective mutations advance it once. */
    long revision();

    /** Returns whether this collider participates in queries and rendering. */
    boolean enabled();

    /** Returns the rigid placement from this collider's local frame into its parent frame. */
    RigidTransform3d localToParent();

    /** Returns the immutable snapshot that queries and rendering read. */
    ColliderSnapshot snapshot();
}
