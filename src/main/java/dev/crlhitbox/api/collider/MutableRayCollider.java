package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Vec3d;

/**
 * Mutable finite ray collider.
 *
 * <p>The value follows the {@link Ray3d} contract: a finite closed set
 * {@code origin + t * direction} for {@code t} in {@code [0, length]}, with a unit direction. There
 * is no infinite-ray mode, and the ray never becomes a {@code Solid3d}. Setters rebuild the ray
 * before publishing, so a zero direction, a negative or non-finite length, or an unrepresentable end
 * point leaves the previous ray, placement and revision untouched.</p>
 */
public final class MutableRayCollider extends AbstractMutableCollider {
    private Ray3d ray;

    /**
     * Creates one ray collider.
     *
     * @param origin the finite local origin
     * @param direction a finite non-zero direction, normalized to unit length
     * @param length the finite nonnegative length
     */
    public MutableRayCollider(Vec3d origin, Vec3d direction, double length) {
        this(new Ray3d(origin, direction, length));
    }

    private MutableRayCollider(Ray3d ray) {
        super(new RayColliderSnapshot(ray, RigidTransform3d.identity(), true));
        this.ray = ray;
    }

    /** Returns the local origin. */
    public Vec3d origin() {
        return ray.origin();
    }

    /** Returns the unit direction. */
    public Vec3d direction() {
        return ray.direction();
    }

    /** Returns the finite nonnegative length. */
    public double length() {
        return ray.length();
    }

    /** Returns {@code origin + direction * length}. */
    public Vec3d end() {
        return ray.end();
    }

    /** Replaces the origin. */
    public boolean setOrigin(Vec3d origin) {
        return update(new Ray3d(requirePoint(origin, "origin"), ray.direction(), ray.length()));
    }

    /** Replaces the direction; it is normalized and must be finite and non-zero. */
    public boolean setDirection(Vec3d direction) {
        return update(new Ray3d(ray.origin(), requirePoint(direction, "direction"), ray.length()));
    }

    /** Replaces the length. */
    public boolean setLength(double length) {
        return update(new Ray3d(ray.origin(), ray.direction(),
                requireNonNegativeFinite(length, "length")));
    }

    /**
     * Replaces origin, direction and length as one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean setShape(Vec3d origin, Vec3d direction, double length) {
        return update(new Ray3d(
                requirePoint(origin, "origin"),
                requirePoint(direction, "direction"),
                requireNonNegativeFinite(length, "length")));
    }

    @Override
    ColliderSnapshot buildSnapshot(boolean enabled, RigidTransform3d localToParent) {
        return new RayColliderSnapshot(ray, localToParent, enabled);
    }

    private boolean update(Ray3d candidate) {
        if (candidate.equals(ray)) {
            return false;
        }
        if (!publishSnapshot(new RayColliderSnapshot(candidate, localToParent(), enabled()))) {
            return false;
        }
        ray = candidate;
        return true;
    }
}
