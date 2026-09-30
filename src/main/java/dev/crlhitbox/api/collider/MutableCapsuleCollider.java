package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Vec3d;

/**
 * Mutable capsule collider described by a center, a centerline length, a radius and an orientation.
 *
 * <p>Following the roadmap contract, the length is the <em>centerline</em> length and excludes the two
 * end hemispheres, so the complete end-to-end exterior length is {@code length + 2 * radius}. The
 * default axis is the local {@code +Y} axis, and the centerline endpoints are
 * {@code center ± orientation.rotate(0, length / 2, 0)}. A zero length degenerates to a sphere and a
 * zero radius degenerates to the centerline; both are valid closed sets.</p>
 */
public final class MutableCapsuleCollider extends AbstractMutableSolidCollider {
    private Vec3d center;
    private double centerlineLength;
    private double radius;
    private Rotation3d orientation;

    /** Creates one capsule collider in local coordinates. */
    public MutableCapsuleCollider(
            Vec3d center,
            double centerlineLength,
            double radius,
            Rotation3d orientation
    ) {
        super(capsuleOf(
                requirePoint(center, "center"),
                requireNonNegativeFinite(centerlineLength, "centerlineLength"),
                requireNonNegativeFinite(radius, "radius"),
                requireRotation(orientation)));
        this.center = requirePoint(center, "center");
        this.centerlineLength = requireNonNegativeFinite(centerlineLength, "centerlineLength");
        this.radius = requireNonNegativeFinite(radius, "radius");
        this.orientation = requireRotation(orientation);
    }

    /** Returns the local center of the capsule (the midpoint of its centerline). */
    public Vec3d center() {
        return center;
    }

    /** Returns the centerline length, excluding the two end hemispheres. */
    public double centerlineLength() {
        return centerlineLength;
    }

    /** Returns the radius. */
    public double radius() {
        return radius;
    }

    /** Returns the orientation of the capsule's axis (local {@code +Y} by default). */
    public Rotation3d orientation() {
        return orientation;
    }

    /** Replaces the local center. */
    public boolean setCenter(Vec3d center) {
        Vec3d checked = requirePoint(center, "center");
        if (checked.equals(this.center)) {
            return false;
        }
        if (!publishSolid(capsuleOf(checked, centerlineLength, radius, orientation))) {
            return false;
        }
        this.center = checked;
        return true;
    }

    /** Replaces the centerline length. */
    public boolean setCenterlineLength(double centerlineLength) {
        double checked = requireNonNegativeFinite(centerlineLength, "centerlineLength");
        if (Double.compare(checked, this.centerlineLength) == 0) {
            return false;
        }
        if (!publishSolid(capsuleOf(center, checked, radius, orientation))) {
            return false;
        }
        this.centerlineLength = checked;
        return true;
    }

    /** Replaces the radius. */
    public boolean setRadius(double radius) {
        double checked = requireNonNegativeFinite(radius, "radius");
        if (Double.compare(checked, this.radius) == 0) {
            return false;
        }
        if (!publishSolid(capsuleOf(center, centerlineLength, checked, orientation))) {
            return false;
        }
        this.radius = checked;
        return true;
    }

    /** Replaces the axis orientation. */
    public boolean setOrientation(Rotation3d orientation) {
        Rotation3d checked = requireRotation(orientation);
        if (checked.equals(this.orientation)) {
            return false;
        }
        if (!publishSolid(capsuleOf(center, centerlineLength, radius, checked))) {
            return false;
        }
        this.orientation = checked;
        return true;
    }

    /**
     * Replaces center, centerline length, radius and orientation as one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean setShape(
            Vec3d center,
            double centerlineLength,
            double radius,
            Rotation3d orientation
    ) {
        Vec3d checkedCenter = requirePoint(center, "center");
        double checkedLength = requireNonNegativeFinite(centerlineLength, "centerlineLength");
        double checkedRadius = requireNonNegativeFinite(radius, "radius");
        Rotation3d checkedOrientation = requireRotation(orientation);
        if (checkedCenter.equals(this.center)
                && Double.compare(checkedLength, this.centerlineLength) == 0
                && Double.compare(checkedRadius, this.radius) == 0
                && checkedOrientation.equals(this.orientation)) {
            return false;
        }
        if (!publishSolid(capsuleOf(
                checkedCenter, checkedLength, checkedRadius, checkedOrientation))) {
            return false;
        }
        this.center = checkedCenter;
        this.centerlineLength = checkedLength;
        this.radius = checkedRadius;
        this.orientation = checkedOrientation;
        return true;
    }

    @Override
    Solid3d currentSolid() {
        return capsuleOf(center, centerlineLength, radius, orientation);
    }

    /** Builds the geometric capsule whose endpoints are {@code center ± axis * length / 2}. */
    private static Capsule capsuleOf(
            Vec3d center,
            double centerlineLength,
            double radius,
            Rotation3d orientation
    ) {
        Vec3d axis = orientation.rotate(new Vec3d(0.0D, 1.0D, 0.0D));
        Vec3d half = axis.multiply(centerlineLength * 0.5D);
        return new Capsule(new Segment3d(center.subtract(half), center.add(half)), radius);
    }
}
