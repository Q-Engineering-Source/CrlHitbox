package dev.crlhitbox.api.geometry;

import java.util.Objects;

/**
 * An immutable active rigid transform from a source/local frame into a target/parent frame.
 * Points map as {@code p' = rotation(p) + translation}; vectors map as
 * {@code v' = rotation(v)} without translation.
 *
 * <p>Operations use ordinary binary64 arithmetic. If a required finite result cannot be
 * represented, delegated {@link Vec3d} or {@link Rotation3d} construction throws
 * {@link IllegalArgumentException}; results are never saturated, replaced with zero, or returned
 * with a NaN or infinite component.</p>
 */
public final class RigidTransform3d {
    private static final RigidTransform3d IDENTITY = new RigidTransform3d(
            Rotation3d.identity(),
            new Vec3d(0.0D, 0.0D, 0.0D));

    private final Rotation3d rotation;
    private final Vec3d translation;

    /** Creates a rigid transform with a rotation followed by a translation for points. */
    public RigidTransform3d(Rotation3d rotation, Vec3d translation) {
        this.rotation = Objects.requireNonNull(rotation, "rotation");
        this.translation = Objects.requireNonNull(translation, "translation");
    }

    /** Returns the identity transform. */
    public static RigidTransform3d identity() {
        return IDENTITY;
    }

    /** Returns this transform's source-to-target rotation. */
    public Rotation3d rotation() {
        return rotation;
    }

    /** Returns this transform's target-frame translation. */
    public Vec3d translation() {
        return translation;
    }

    /** Returns {@code rotation(point) + translation}. */
    public Vec3d transformPoint(Vec3d point) {
        Objects.requireNonNull(point, "point");
        return rotation.rotate(point).add(translation);
    }

    /** Returns {@code rotation(vector)} without applying translation. */
    public Vec3d transformVector(Vec3d vector) {
        Objects.requireNonNull(vector, "vector");
        return rotation.rotate(vector);
    }

    /** Returns {@code rotation^-1(point - translation)}. */
    public Vec3d inverseTransformPoint(Vec3d point) {
        Objects.requireNonNull(point, "point");
        return rotation.inverseRotate(point.subtract(translation));
    }

    /** Returns {@code rotation^-1(vector)} without applying translation. */
    public Vec3d inverseTransformVector(Vec3d vector) {
        Objects.requireNonNull(vector, "vector");
        return rotation.inverseRotate(vector);
    }

    /** Returns the rigid inverse {@code (rotation^-1, -rotation^-1(translation))}. */
    public RigidTransform3d inverse() {
        return new RigidTransform3d(
                rotation.inverse(),
                rotation.inverseRotate(translation).negate());
    }

    /**
     * Returns this transform followed by {@code after}.
     * For every representable point {@code p}, {@code a.andThen(b).transformPoint(p)} means
     * {@code b.transformPoint(a.transformPoint(p))}; equivalently, {@code a.andThen(b) = b ∘ a}.
     */
    public RigidTransform3d andThen(RigidTransform3d after) {
        Objects.requireNonNull(after, "after");
        return new RigidTransform3d(
                compose(rotation, after.rotation),
                after.rotation.rotate(translation).add(after.translation));
    }

    private static Rotation3d compose(Rotation3d before, Rotation3d after) {
        return new Rotation3d(
                after.w() * before.x() + after.x() * before.w()
                        + after.y() * before.z() - after.z() * before.y(),
                after.w() * before.y() - after.x() * before.z()
                        + after.y() * before.w() + after.z() * before.x(),
                after.w() * before.z() + after.x() * before.y()
                        - after.y() * before.x() + after.z() * before.w(),
                after.w() * before.w() - after.x() * before.x()
                        - after.y() * before.y() - after.z() * before.z());
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof RigidTransform3d other
                && rotation.equals(other.rotation)
                && translation.equals(other.translation);
    }

    @Override
    public int hashCode() {
        return 31 * rotation.hashCode() + translation.hashCode();
    }

    @Override
    public String toString() {
        return "RigidTransform3d[rotation=" + rotation + ", translation=" + translation + "]";
    }
}
