package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;

import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of one finite {@link Ray3d} with a rigid placement and an enable state.
 *
 * <p>The ray value is immutable, so no defensive copy is needed. The parent-frame bounds are the
 * local ray bounds placed once through the frozen placed-solid semantics; a ray longer than a
 * display convention is never clipped here, because clipping the value would change the query
 * domain.</p>
 */
public final class RayColliderSnapshot implements ColliderSnapshot {
    private final Ray3d ray;
    private final RigidTransform3d localToParent;
    private final boolean enabled;
    private final Aabb bounds;

    /**
     * Creates one ray snapshot.
     *
     * @param ray the local finite ray; never {@code null}
     * @param localToParent the placement into the parent frame; never {@code null}
     * @param enabled whether the snapshot participates in queries and rendering
     * @throws IllegalArgumentException when an enabled ray has no representable finite parent-frame
     *         bounds
     */
    public RayColliderSnapshot(Ray3d ray, RigidTransform3d localToParent, boolean enabled) {
        this.ray = ColliderSnapshotFactory.requireRay(ray);
        this.localToParent = ColliderSnapshotFactory.requireTransform(localToParent);
        this.enabled = enabled;
        this.bounds = ColliderSnapshotFactory.placeBounds(ray.bounds(), localToParent, enabled);
    }

    /** Returns the local finite ray. */
    public Ray3d ray() {
        return ray;
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
        return this == object || object instanceof RayColliderSnapshot other
                && enabled == other.enabled
                && ray.equals(other.ray)
                && localToParent.equals(other.localToParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ray, localToParent, enabled);
    }

    @Override
    public String toString() {
        return "RayColliderSnapshot[ray=" + ray
                + ", localToParent=" + localToParent
                + ", enabled=" + enabled
                + ", bounds=" + bounds + "]";
    }
}
