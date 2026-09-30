package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;

/**
 * Finite directed ray value: an origin, a unit direction and a length.
 *
 * <p>The ray is the closed point set {@code origin + t * direction} for {@code t} in
 * {@code [0, length]}. It is deliberately finite: there is no infinite-ray type in this project, and
 * a ray is not a {@code Solid3d}. The published origin and direction are preserved exactly as given
 * (apart from direction normalization); only {@link #asSegment()} produces a canonical ordered
 * segment, and that ordering never rewrites this value's own state.</p>
 *
 * <p>Direction normalization divides by the largest absolute component first, so extreme but finite
 * directions do not underflow or overflow on the way to a unit vector.</p>
 */
public final class Ray3d {
    private final Vec3d origin;
    private final Vec3d direction;
    private final double length;
    private final Vec3d end;
    private final Aabb bounds;

    /**
     * Creates one finite ray.
     *
     * @param origin the finite start point
     * @param direction a finite, non-zero direction; it is normalized to unit length
     * @param length the finite nonnegative ray length
     * @throws IllegalArgumentException when the direction is zero, the length is negative or
     *         non-finite, or the end point is not representable as finite components
     */
    public Ray3d(Vec3d origin, Vec3d direction, double length) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.direction = normalizeDirection(Objects.requireNonNull(direction, "direction"));
        if (!Double.isFinite(length) || length < 0.0D) {
            throw new IllegalArgumentException(
                    "ray length must be finite and nonnegative: " + length);
        }
        this.length = length;
        Vec3d computedEnd;
        try {
            computedEnd = origin.add(this.direction.multiply(length));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "ray end point is not representable as finite components", failure);
        }
        this.end = computedEnd;
        this.bounds = new Aabb(
                new Vec3d(
                        Math.min(origin.x(), computedEnd.x()),
                        Math.min(origin.y(), computedEnd.y()),
                        Math.min(origin.z(), computedEnd.z())),
                new Vec3d(
                        Math.max(origin.x(), computedEnd.x()),
                        Math.max(origin.y(), computedEnd.y()),
                        Math.max(origin.z(), computedEnd.z())));
    }

    /** Returns the ray origin. */
    public Vec3d origin() {
        return origin;
    }

    /** Returns the unit direction; zero length is represented as the origin point itself. */
    public Vec3d direction() {
        return direction;
    }

    /** Returns the finite nonnegative length. */
    public double length() {
        return length;
    }

    /** Returns {@code origin + direction * length}. */
    public Vec3d end() {
        return end;
    }

    /** Returns the local conservative axis-aligned bounds of this ray. */
    public Aabb bounds() {
        return bounds;
    }

    /** Returns the equivalent finite segment; the canonical endpoint order does not change this ray. */
    public Segment3d asSegment() {
        return new Segment3d(origin, end);
    }

    /**
     * Normalizes a finite non-zero direction using largest-component scaling.
     *
     * @throws IllegalArgumentException when the direction is exactly zero
     */
    private static Vec3d normalizeDirection(Vec3d direction) {
        double x = direction.x();
        double y = direction.y();
        double z = direction.z();
        double scale = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (scale == 0.0D) {
            throw new IllegalArgumentException("ray direction must be non-zero");
        }
        double scaledX = x / scale;
        double scaledY = y / scale;
        double scaledZ = z / scale;
        double norm = Math.sqrt(scaledX * scaledX + scaledY * scaledY + scaledZ * scaledZ);
        return new Vec3d(scaledX / norm, scaledY / norm, scaledZ / norm);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof Ray3d other
                && origin.equals(other.origin)
                && direction.equals(other.direction)
                && Double.compare(length, other.length) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, direction, length);
    }

    @Override
    public String toString() {
        return "Ray3d[origin=" + origin + ", direction=" + direction
                + ", length=" + length + ", end=" + end + "]";
    }
}
