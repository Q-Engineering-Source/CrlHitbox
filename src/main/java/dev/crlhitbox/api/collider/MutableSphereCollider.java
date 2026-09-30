package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;

/**
 * Mutable sphere collider.
 *
 * <p>A zero radius is a valid closed point set; a negative, non-finite or otherwise invalid update is
 * rejected before any state change. {@link #setShape(Vec3d, double)} publishes the center and the
 * radius as one effective change instead of two intermediate states.</p>
 */
public final class MutableSphereCollider extends AbstractMutableCollider {
    private Vec3d center;
    private double radius;

    /** Creates one sphere collider from a local center and radius. */
    public MutableSphereCollider(Vec3d center, double radius) {
        super(new Sphere(requirePoint(center, "center"),
                requireNonNegativeFinite(radius, "radius")));
        this.center = requirePoint(center, "center");
        this.radius = requireNonNegativeFinite(radius, "radius");
    }

    /** Returns the local center. */
    public Vec3d center() {
        return center;
    }

    /** Returns the radius. */
    public double radius() {
        return radius;
    }

    /** Replaces the local center. */
    public boolean setCenter(Vec3d center) {
        Vec3d checked = requirePoint(center, "center");
        if (checked.equals(this.center)) {
            return false;
        }
        if (!publishSolid(new Sphere(checked, radius))) {
            return false;
        }
        this.center = checked;
        return true;
    }

    /** Replaces the radius. */
    public boolean setRadius(double radius) {
        double checked = requireNonNegativeFinite(radius, "radius");
        if (Double.compare(checked, this.radius) == 0) {
            return false;
        }
        if (!publishSolid(new Sphere(center, checked))) {
            return false;
        }
        this.radius = checked;
        return true;
    }

    /**
     * Replaces the center and radius as one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean setShape(Vec3d center, double radius) {
        Vec3d checkedCenter = requirePoint(center, "center");
        double checkedRadius = requireNonNegativeFinite(radius, "radius");
        if (checkedCenter.equals(this.center) && Double.compare(checkedRadius, this.radius) == 0) {
            return false;
        }
        if (!publishSolid(new Sphere(checkedCenter, checkedRadius))) {
            return false;
        }
        this.center = checkedCenter;
        this.radius = checkedRadius;
        return true;
    }

    @Override
    Solid3d currentSolid() {
        return new Sphere(center, radius);
    }
}
