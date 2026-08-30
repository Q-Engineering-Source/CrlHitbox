package dev.crlhitbox.api.geometry;

import java.util.Objects;

/**
 * An immutable three-dimensional rotation stored as a floating-point-normalized,
 * canonical quaternion.
 *
 * <p>The constructor accepts every finite, nonzero binary64 component tuple. It first
 * scales by the largest-magnitude component to avoid avoidable overflow and underflow,
 * then normalizes once in binary64 arithmetic. The stored components are deterministic
 * floating-point approximations; recomputing their squared length is not promised to
 * produce bitwise {@code 1.0D}. Query methods never renormalize the stored orientation.</p>
 *
 * <p>Inputs whose {@code double} values are exactly proportional as real numbers,
 * including a negative proportional scale, produce the same canonical stored tuple.
 * Separately rounded component scaling and independently written decimal tuples that are
 * not exactly proportional need not compare equal. {@link #equals(Object)} and
 * {@link #hashCode()} compare only the canonical stored component bits and perform no
 * tolerance or angular comparison.</p>
 */
public final class Rotation3d {
    private static final Rotation3d IDENTITY = new Rotation3d(0.0D, 0.0D, 0.0D, 1.0D, true);

    private final double x;
    private final double y;
    private final double z;
    private final double w;

    /** Creates a rotation from finite, nonzero raw quaternion components {@code (x, y, z, w)}. */
    public Rotation3d(double x, double y, double z, double w) {
        Vec3d.canonicalFinite(x, "quaternion x");
        Vec3d.canonicalFinite(y, "quaternion y");
        Vec3d.canonicalFinite(z, "quaternion z");
        Vec3d.canonicalFinite(w, "quaternion w");
        double largest = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.max(Math.abs(z), Math.abs(w)));
        if (largest == 0.0D) {
            throw new IllegalArgumentException("quaternion must not be zero");
        }
        double scaledX = x / largest;
        double scaledY = y / largest;
        double scaledZ = z / largest;
        double scaledW = w / largest;
        double inverseLength = 1.0D / Math.sqrt(scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ + scaledW * scaledW);
        double normalizedX = scaledX * inverseLength;
        double normalizedY = scaledY * inverseLength;
        double normalizedZ = scaledZ * inverseLength;
        double normalizedW = scaledW * inverseLength;
        if (shouldNegate(normalizedX, normalizedY, normalizedZ, normalizedW)) {
            normalizedX = -normalizedX;
            normalizedY = -normalizedY;
            normalizedZ = -normalizedZ;
            normalizedW = -normalizedW;
        }
        this.x = Vec3d.canonicalZero(normalizedX);
        this.y = Vec3d.canonicalZero(normalizedY);
        this.z = Vec3d.canonicalZero(normalizedZ);
        this.w = Vec3d.canonicalZero(normalizedW);
    }

    private Rotation3d(double x, double y, double z, double w, boolean normalized) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /** Returns the identity rotation. */
    public static Rotation3d identity() {
        return IDENTITY;
    }

    /** Returns the canonical normalized quaternion x component. */
    public double x() {
        return x;
    }

    /** Returns the canonical normalized quaternion y component. */
    public double y() {
        return y;
    }

    /** Returns the canonical normalized quaternion z component. */
    public double z() {
        return z;
    }

    /** Returns the canonical normalized quaternion w component. */
    public double w() {
        return w;
    }

    /** Rotates {@code vector} from local coordinates into world coordinates. */
    public Vec3d rotate(Vec3d vector) {
        Objects.requireNonNull(vector, "vector");
        return rotateComponents(vector.x(), vector.y(), vector.z(), x, y, z, w);
    }

    /** Rotates {@code vector} from world coordinates into local coordinates. */
    public Vec3d inverseRotate(Vec3d vector) {
        Objects.requireNonNull(vector, "vector");
        return rotateComponents(vector.x(), vector.y(), vector.z(), -x, -y, -z, w);
    }

    /** Returns this rotation's inverse. */
    public Rotation3d inverse() {
        return canonicalNormalized(-x, -y, -z, w);
    }

    /** Returns the world-space direction of the local positive x axis. */
    public Vec3d basisX() {
        return rotateComponents(1.0D, 0.0D, 0.0D, x, y, z, w);
    }

    /** Returns the world-space direction of the local positive y axis. */
    public Vec3d basisY() {
        return rotateComponents(0.0D, 1.0D, 0.0D, x, y, z, w);
    }

    /** Returns the world-space direction of the local positive z axis. */
    public Vec3d basisZ() {
        return rotateComponents(0.0D, 0.0D, 1.0D, x, y, z, w);
    }

    private static Vec3d rotateComponents(double vx, double vy, double vz, double qx, double qy, double qz, double qw) {
        double twiceCrossX = 2.0D * (qy * vz - qz * vy);
        double twiceCrossY = 2.0D * (qz * vx - qx * vz);
        double twiceCrossZ = 2.0D * (qx * vy - qy * vx);
        return new Vec3d(
                vx + qw * twiceCrossX + qy * twiceCrossZ - qz * twiceCrossY,
                vy + qw * twiceCrossY + qz * twiceCrossX - qx * twiceCrossZ,
                vz + qw * twiceCrossZ + qx * twiceCrossY - qy * twiceCrossX
        );
    }

    private static boolean shouldNegate(double x, double y, double z, double w) {
        return w < 0.0D || w == 0.0D && (x < 0.0D || x == 0.0D && (y < 0.0D || y == 0.0D && z < 0.0D));
    }

    private static Rotation3d canonicalNormalized(double x, double y, double z, double w) {
        if (shouldNegate(x, y, z, w)) return new Rotation3d(Vec3d.canonicalZero(-x), Vec3d.canonicalZero(-y), Vec3d.canonicalZero(-z), Vec3d.canonicalZero(-w), true);
        return new Rotation3d(Vec3d.canonicalZero(x), Vec3d.canonicalZero(y), Vec3d.canonicalZero(z), Vec3d.canonicalZero(w), true);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Rotation3d other
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y)
                && Double.doubleToLongBits(z) == Double.doubleToLongBits(other.z)
                && Double.doubleToLongBits(w) == Double.doubleToLongBits(other.w);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        result = 31 * result + Double.hashCode(w);
        return result;
    }

    @Override
    public String toString() {
        return "Rotation3d[x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + "]";
    }
}
