package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** An immutable closed ball with a finite center and nonnegative finite radius. */
public final class Sphere implements Bounded3d {
    private final Vec3d center;
    private final double radius;
    private final Aabb bounds;

    /** Creates a closed sphere. Its mandatory finite bounds are validated eagerly. */
    public Sphere(Vec3d center, double radius) {
        this.center = Objects.requireNonNull(center, "center");
        this.radius = Vec3d.canonicalFinite(radius, "radius");
        if (radius < 0.0D) throw new IllegalArgumentException("radius must be nonnegative, but was " + radius);
        this.bounds = new Aabb(new Vec3d(center.x() - radius, center.y() - radius, center.z() - radius), new Vec3d(center.x() + radius, center.y() + radius, center.z() + radius));
    }

    /** Returns the center. */
    public Vec3d center() { return center; }
    /** Returns the nonnegative radius. */
    public double radius() { return radius; }
    /** Returns the eagerly validated finite bounds. */
    @Override public Aabb bounds() { return bounds; }
    @Override public boolean equals(Object object) { return this == object || object instanceof Sphere other && center.equals(other.center) && Double.doubleToLongBits(radius) == Double.doubleToLongBits(other.radius); }
    @Override public int hashCode() { return 31 * center.hashCode() + Double.hashCode(radius); }
    @Override public String toString() { return "Sphere[center=" + center + ", radius=" + radius + "]"; }
}
