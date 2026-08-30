package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** A closed capsule consisting of a finite centerline segment expanded by a nonnegative radius. */
public final class Capsule implements Bounded3d {
    private final Segment3d centerline;
    private final double radius;
    private final Aabb bounds;

    /** Creates a capsule and validates that its mandatory finite bounds are representable. */
    public Capsule(Segment3d centerline, double radius) {
        this.centerline = Objects.requireNonNull(centerline, "centerline");
        this.radius = Vec3d.canonicalFinite(radius, "radius");
        if (radius < 0.0D) throw new IllegalArgumentException("radius must be nonnegative, but was " + radius);
        Aabb lineBounds = centerline.bounds();
        this.bounds = new Aabb(new Vec3d(lineBounds.min().x() - radius, lineBounds.min().y() - radius, lineBounds.min().z() - radius), new Vec3d(lineBounds.max().x() + radius, lineBounds.max().y() + radius, lineBounds.max().z() + radius));
    }

    /** Returns the finite closed centerline segment. */
    public Segment3d centerline() { return centerline; }
    /** Returns the nonnegative radius. */
    public double radius() { return radius; }
    /** Returns the centerline length, possibly positive infinity when that length is not representable. */
    public double centerlineLength() { Vec3d delta = centerline.delta(); return Math.hypot(Math.hypot(delta.x(), delta.y()), delta.z()); }
    /** Returns the complete end-to-end exterior length, equal to centerline length plus twice the radius. */
    public double exteriorLength() { return centerlineLength() + radius + radius; }
    /** Returns the eagerly validated finite bounds. */
    @Override public Aabb bounds() { return bounds; }
    @Override public boolean equals(Object object) { return this == object || object instanceof Capsule other && centerline.equals(other.centerline) && Double.doubleToLongBits(radius) == Double.doubleToLongBits(other.radius); }
    @Override public int hashCode() { return 31 * centerline.hashCode() + Double.hashCode(radius); }
    @Override public String toString() { return "Capsule[centerline=" + centerline + ", radius=" + radius + "]"; }
}
