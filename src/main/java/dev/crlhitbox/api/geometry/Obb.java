package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** A closed oriented box defined by a center, local half-extents, and normalized orientation. */
public final class Obb implements Bounded3d {
    private final Vec3d center;
    private final Vec3d halfExtents;
    private final Rotation3d orientation;
    private final Aabb bounds;

    /** Creates an oriented box and eagerly validates the representability of its finite bounds. */
    public Obb(Vec3d center, Vec3d halfExtents, Rotation3d orientation) {
        this.center = Objects.requireNonNull(center, "center");
        this.halfExtents = Objects.requireNonNull(halfExtents, "halfExtents");
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        if (halfExtents.x() < 0.0D || halfExtents.y() < 0.0D || halfExtents.z() < 0.0D) {
            throw new IllegalArgumentException("halfExtents must be nonnegative on every axis");
        }
        Vec3d basisX = orientation.basisX();
        Vec3d basisY = orientation.basisY();
        Vec3d basisZ = orientation.basisZ();
        Vec3d worldHalfExtents = new Vec3d(
                Math.abs(basisX.x()) * halfExtents.x() + Math.abs(basisY.x()) * halfExtents.y() + Math.abs(basisZ.x()) * halfExtents.z(),
                Math.abs(basisX.y()) * halfExtents.x() + Math.abs(basisY.y()) * halfExtents.y() + Math.abs(basisZ.y()) * halfExtents.z(),
                Math.abs(basisX.z()) * halfExtents.x() + Math.abs(basisY.z()) * halfExtents.y() + Math.abs(basisZ.z()) * halfExtents.z()
        );
        this.bounds = conservativeBounds(
                center,
                halfExtents,
                orientation,
                new Vec3d(center.x() - worldHalfExtents.x(), center.y() - worldHalfExtents.y(), center.z() - worldHalfExtents.z()),
                new Vec3d(center.x() + worldHalfExtents.x(), center.y() + worldHalfExtents.y(), center.z() + worldHalfExtents.z())
        );
    }

    /** Returns the center in world coordinates. */
    public Vec3d center() { return center; }
    /** Returns the nonnegative local half-extents. */
    public Vec3d halfExtents() { return halfExtents; }
    /** Returns the local-to-world orientation. */
    public Rotation3d orientation() { return orientation; }

    /** Converts a finite world point into this box's local coordinate system. */
    public Vec3d worldToLocal(Vec3d point) {
        Objects.requireNonNull(point, "point");
        return orientation.inverseRotate(point.subtract(center));
    }

    /** Converts a finite local point into world coordinates. */
    public Vec3d localToWorld(Vec3d point) {
        Objects.requireNonNull(point, "point");
        return center.add(orientation.rotate(point));
    }

    /** Returns whether {@code point} is inside this closed oriented box, including its boundary. */
    public boolean contains(Vec3d point) {
        Objects.requireNonNull(point, "point");
        double dx = point.x() - center.x();
        double dy = point.y() - center.y();
        double dz = point.z() - center.z();
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) return false;
        Vec3d basisX = orientation.basisX();
        Vec3d basisY = orientation.basisY();
        Vec3d basisZ = orientation.basisZ();
        double localX = dx * basisX.x() + dy * basisX.y() + dz * basisX.z();
        double localY = dx * basisY.x() + dy * basisY.y() + dz * basisY.z();
        double localZ = dx * basisZ.x() + dy * basisZ.y() + dz * basisZ.z();
        if (!Double.isFinite(localX) || !Double.isFinite(localY) || !Double.isFinite(localZ)) return false;
        Vec3d local = new Vec3d(localX, localY, localZ);
        if (withinClosedLocalBounds(local)) return true;
        return reconstructsFromClosedLocalPoint(local, point);
    }

    /** Returns the eagerly validated finite world-axis-aligned bounds. */
    @Override public Aabb bounds() { return bounds; }
    @Override public boolean equals(Object object) { return this == object || object instanceof Obb other && center.equals(other.center) && halfExtents.equals(other.halfExtents) && orientation.equals(other.orientation); }
    @Override public int hashCode() { int result = center.hashCode(); result = 31 * result + halfExtents.hashCode(); return 31 * result + orientation.hashCode(); }
    @Override public String toString() { return "Obb[center=" + center + ", halfExtents=" + halfExtents + ", orientation=" + orientation + "]"; }

    private boolean withinClosedLocalBounds(Vec3d local) { return Math.abs(local.x()) <= halfExtents.x() && Math.abs(local.y()) <= halfExtents.y() && Math.abs(local.z()) <= halfExtents.z(); }

    private boolean reconstructsFromClosedLocalPoint(Vec3d local, Vec3d point) {
        double[] xs = {clamp(local.x(), -halfExtents.x(), halfExtents.x()), -halfExtents.x(), halfExtents.x()};
        double[] ys = {clamp(local.y(), -halfExtents.y(), halfExtents.y()), -halfExtents.y(), halfExtents.y()};
        double[] zs = {clamp(local.z(), -halfExtents.z(), halfExtents.z()), -halfExtents.z(), halfExtents.z()};
        // A strict inverse-transform miss is accepted only with an exact local closed-set witness that reproduces this point.
        for (double x : xs) for (double y : ys) for (double z : zs) if (localToWorld(new Vec3d(x, y, z)).equals(point)) return true;
        return false;
    }

    private static double clamp(double value, double minimum, double maximum) { return Math.max(minimum, Math.min(maximum, value)); }

    private static Aabb conservativeBounds(Vec3d center, Vec3d half, Rotation3d orientation, Vec3d minimum, Vec3d maximum) {
        Vec3d min = minimum;
        Vec3d max = maximum;
        for (int signs = 0; signs < 8; signs++) {
            Vec3d corner = orientation.rotate(new Vec3d((signs & 1) == 0 ? -half.x() : half.x(), (signs & 2) == 0 ? -half.y() : half.y(), (signs & 4) == 0 ? -half.z() : half.z())).add(center);
            min = min.min(corner);
            max = max.max(corner);
        }
        return new Aabb(min, max);
    }
}
