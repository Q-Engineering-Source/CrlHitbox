package dev.crlhitbox.api.geometry;

import java.util.Objects;

/**
 * An immutable three-dimensional vector with finite double-precision components.
 *
 * <p>Instances canonicalize signed zero. Vector-producing operations reject a result
 * whose components cannot be represented as finite doubles; squared magnitudes may
 * instead be positive infinity when squaring finite values overflows.</p>
 */
public final class Vec3d {
    private final double x;
    private final double y;
    private final double z;

    /** Creates a vector from finite Cartesian components. */
    public Vec3d(double x, double y, double z) {
        this.x = canonicalFinite(x, "x");
        this.y = canonicalFinite(y, "y");
        this.z = canonicalFinite(z, "z");
    }

    /** Returns the x component. */
    public double x() {
        return x;
    }

    /** Returns the y component. */
    public double y() {
        return y;
    }

    /** Returns the z component. */
    public double z() {
        return z;
    }

    /** Returns this vector plus {@code other}. */
    public Vec3d add(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    /** Returns this vector minus {@code other}. */
    public Vec3d subtract(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    /** Returns the additive inverse of this vector. */
    public Vec3d negate() {
        return new Vec3d(-x, -y, -z);
    }

    /** Returns this vector multiplied by the finite scalar {@code scalar}. */
    public Vec3d multiply(double scalar) {
        canonicalFinite(scalar, "scalar");
        return new Vec3d(x * scalar, y * scalar, z * scalar);
    }

    /** Returns the scalar dot product with {@code other}. */
    public double dot(Vec3d other) {
        Objects.requireNonNull(other, "other");
        double result = x * other.x + y * other.y + z * other.z;
        if (Double.isNaN(result)) {
            throw new IllegalArgumentException("dot product is not representable");
        }
        return canonicalZero(result);
    }

    /** Returns the right-handed cross product with {@code other}. */
    public Vec3d cross(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return new Vec3d(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    /** Returns the squared Euclidean length, possibly positive infinity on overflow. */
    public double lengthSquared() {
        return nonNegativeSquared(x, y, z);
    }

    /** Returns the squared Euclidean distance to {@code other}, possibly positive infinity on overflow. */
    public double distanceSquared(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return nonNegativeSquared(x - other.x, y - other.y, z - other.z);
    }

    /** Returns the component-wise minimum of this vector and {@code other}. */
    public Vec3d min(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return new Vec3d(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    /** Returns the component-wise maximum of this vector and {@code other}. */
    public Vec3d max(Vec3d other) {
        Objects.requireNonNull(other, "other");
        return new Vec3d(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    static double canonicalFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite, but was " + value);
        }
        return canonicalZero(value);
    }

    static double canonicalZero(double value) {
        return value == 0.0D ? 0.0D : value;
    }

    private static double nonNegativeSquared(double x, double y, double z) {
        double result = x * x + y * y + z * z;
        if (Double.isNaN(result)) {
            throw new IllegalArgumentException("squared distance is not representable");
        }
        return result == 0.0D ? 0.0D : result;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Vec3d other
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y)
                && Double.doubleToLongBits(z) == Double.doubleToLongBits(other.z);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        return result;
    }

    @Override
    public String toString() {
        return "Vec3d[x=" + x + ", y=" + y + ", z=" + z + "]";
    }
}
