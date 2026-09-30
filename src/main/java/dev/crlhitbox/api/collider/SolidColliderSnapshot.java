package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of one sealed {@link Solid3d} with a rigid placement and an enable state.
 *
 * <p>The solid and the transform are immutable geometry values, so no defensive copy is needed; the
 * parent-frame bounds are computed once at construction through the existing placed-solid semantics,
 * which keeps the frozen geometry package as the sole owner of placement arithmetic.</p>
 */
public final class SolidColliderSnapshot implements ColliderSnapshot {
    private final Solid3d solid;
    private final RigidTransform3d localToParent;
    private final boolean enabled;
    private final Aabb bounds;

    /**
     * Creates one solid snapshot.
     *
     * @param solid the local solid; never {@code null}
     * @param localToParent the placement into the parent frame; never {@code null}
     * @param enabled whether the snapshot participates in queries and rendering
     * @throws IllegalArgumentException when an enabled solid has no representable finite parent-frame
     *         bounds
     */
    public SolidColliderSnapshot(Solid3d solid, RigidTransform3d localToParent, boolean enabled) {
        this.solid = ColliderSnapshotFactory.requireSolid(solid);
        this.localToParent = ColliderSnapshotFactory.requireTransform(localToParent);
        this.enabled = enabled;
        this.bounds = ColliderSnapshotFactory.placeBounds(
                ColliderSnapshotFactory.localBoundsOf(solid), localToParent, enabled);
    }

    /** Returns the local solid. */
    public Solid3d solid() {
        return solid;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public RigidTransform3d localToParent() {
        return localToParent;
    }

    @Override
    public Optional<Aabb> bounds() {
        return Optional.ofNullable(bounds);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof SolidColliderSnapshot other
                && enabled == other.enabled
                && solid.equals(other.solid)
                && localToParent.equals(other.localToParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(solid, localToParent, enabled);
    }

    @Override
    public String toString() {
        return "SolidColliderSnapshot[solid=" + solid
                + ", localToParent=" + localToParent
                + ", enabled=" + enabled
                + ", bounds=" + bounds + "]";
    }
}
