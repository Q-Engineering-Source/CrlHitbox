package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.Objects;

/**
 * Shared mutable state for colliders backed by one sealed {@link Solid3d}.
 *
 * <p>Shape setters build the candidate solid first, so a rejected value never reaches the published
 * snapshot and the previous shape stays observable through the cached snapshot.</p>
 */
abstract class AbstractMutableSolidCollider extends AbstractMutableCollider {
    AbstractMutableSolidCollider(Solid3d initialSolid) {
        super(new SolidColliderSnapshot(
                Objects.requireNonNull(initialSolid, "initialSolid"),
                RigidTransform3d.identity(), true));
    }

    @Override
    final ColliderSnapshot buildSnapshot(boolean enabled, RigidTransform3d localToParent) {
        return new SolidColliderSnapshot(currentSolid(), localToParent, enabled);
    }

    /** Rebuilds the current geometric value from this collider's shape parameters. */
    abstract Solid3d currentSolid();

    /**
     * Publishes one already validated shape value as a new snapshot.
     *
     * @return whether the published snapshot differs from the previous one
     */
    final boolean publishSolid(Solid3d solid) {
        return publishSnapshot(new SolidColliderSnapshot(
                Objects.requireNonNull(solid, "solid"), localToParent(), enabled()));
    }
}
