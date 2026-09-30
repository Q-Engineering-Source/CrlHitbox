package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;

/**
 * Mutable facade for a collider's common state.
 *
 * <p>Every effective change validates its input completely and then publishes a new immutable
 * snapshot in one step; an invalid input throws before any state changes and leaves the previous
 * snapshot intact. A setter that is given a value equal to the current one returns {@code false} and
 * does not advance the revision or allocate a new snapshot.</p>
 *
 * <p>Mutable colliders are confined to the logical game thread that owns them; immutable snapshots
 * may be handed to another thread.</p>
 */
public interface MutableCollider extends Collider {
    /**
     * Enables or disables participation in queries and rendering.
     *
     * @return whether this call changed the state
     */
    boolean setEnabled(boolean enabled);

    /**
     * Replaces the whole collider's placement into its parent frame.
     *
     * @return whether this call changed the state
     */
    boolean setLocalToParent(RigidTransform3d transform);
}
