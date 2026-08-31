package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** An immutable closed axis-aligned box specified by finite minimum and maximum corners. */
public final class Aabb implements Solid3d {
    private final Vec3d min;
    private final Vec3d max;

    /** Creates a closed box with component-wise {@code min <= max}. */
    public Aabb(Vec3d min, Vec3d max) {
        this.min = Objects.requireNonNull(min, "min");
        this.max = Objects.requireNonNull(max, "max");
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("min must not exceed max on any axis");
        }
    }

    /** Returns the minimum closed corner. */
    public Vec3d min() { return min; }

    /** Returns the maximum closed corner. */
    public Vec3d max() { return max; }

    /** Returns the finite center point of this box. */
    public Vec3d center() {
        return new Vec3d(midpoint(min.x(), max.x()), midpoint(min.y(), max.y()), midpoint(min.z(), max.z()));
    }

    /** Returns the nonnegative half-extents of this box. */
    public Vec3d halfExtents() {
        return new Vec3d(halfExtent(min.x(), max.x()), halfExtent(min.y(), max.y()), halfExtent(min.z(), max.z()));
    }

    /** Returns whether {@code point} lies inside this closed box, including its boundary. */
    public boolean contains(Vec3d point) {
        Objects.requireNonNull(point, "point");
        return point.x() >= min.x() && point.x() <= max.x()
                && point.y() >= min.y() && point.y() <= max.y()
                && point.z() >= min.z() && point.z() <= max.z();
    }

    /** Returns this immutable box itself. */
    @Override public Aabb bounds() { return this; }

    @Override public boolean equals(Object object) { return this == object || object instanceof Aabb other && min.equals(other.min) && max.equals(other.max); }
    @Override public int hashCode() { return 31 * min.hashCode() + max.hashCode(); }
    @Override public String toString() { return "Aabb[min=" + min + ", max=" + max + "]"; }

    private static double midpoint(double min, double max) {
        double sum = min + max;
        return Double.isFinite(sum) ? sum / 2.0D : min / 2.0D + max / 2.0D;
    }

    private static double halfExtent(double min, double max) {
        double difference = max - min;
        return Double.isFinite(difference) ? difference / 2.0D : max / 2.0D - min / 2.0D;
    }
}
