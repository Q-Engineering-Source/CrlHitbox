package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** Pure closed-set intersection queries for the explicitly supported Phase 1A pair set. */
public final class GeometryIntersections {
    private GeometryIntersections() {
    }

    /** Returns whether two closed axis-aligned boxes overlap or touch. */
    public static boolean intersects(Aabb first, Aabb second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return first.min().x() <= second.max().x() && first.max().x() >= second.min().x()
                && first.min().y() <= second.max().y() && first.max().y() >= second.min().y()
                && first.min().z() <= second.max().z() && first.max().z() >= second.min().z();
    }

    /** Returns whether two closed spheres overlap or touch. */
    public static boolean intersects(Sphere first, Sphere second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return centersWithin(first.center(), second.center(), first.radius(), second.radius());
    }

    /** Returns whether a closed sphere and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Sphere sphere, Aabb box) {
        Objects.requireNonNull(sphere, "sphere");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.pointToAabbWithin(sphere.center(), box, sphere.radius());
    }

    /** Returns whether a closed axis-aligned box and closed sphere overlap or touch. */
    public static boolean intersects(Aabb box, Sphere sphere) { return intersects(sphere, box); }

    /** Returns whether a closed sphere and closed oriented box overlap or touch. */
    public static boolean intersects(Sphere sphere, Obb box) {
        Objects.requireNonNull(sphere, "sphere");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.pointToObbWithin(sphere.center(), box, sphere.radius());
    }

    /** Returns whether a closed oriented box and closed sphere overlap or touch. */
    public static boolean intersects(Obb box, Sphere sphere) { return intersects(sphere, box); }

    /** Returns whether a closed capsule and closed sphere overlap or touch. */
    public static boolean intersects(Capsule capsule, Sphere sphere) {
        Objects.requireNonNull(capsule, "capsule");
        Objects.requireNonNull(sphere, "sphere");
        return GeometryDistances.pointToSegmentWithin(sphere.center(), capsule.centerline(), sphere.radius(), capsule.radius());
    }

    /** Returns whether a closed sphere and closed capsule overlap or touch. */
    public static boolean intersects(Sphere sphere, Capsule capsule) { return intersects(capsule, sphere); }

    /** Returns whether two closed capsules overlap or touch. */
    public static boolean intersects(Capsule first, Capsule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return GeometryDistances.segmentToSegmentWithin(first.centerline(), second.centerline(), first.radius(), second.radius());
    }

    /** Returns whether a finite closed segment and closed sphere overlap or touch. */
    public static boolean intersects(Segment3d segment, Sphere sphere) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(sphere, "sphere");
        return GeometryDistances.pointToSegmentWithin(sphere.center(), segment, sphere.radius(), 0.0D);
    }

    /** Returns whether a closed sphere and finite closed segment overlap or touch. */
    public static boolean intersects(Sphere sphere, Segment3d segment) { return intersects(segment, sphere); }

    /** Returns whether a finite closed segment and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Segment3d segment, Aabb box) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(box, "box");
        return intersectsSlab(segment.start(), segment.end(), box.min(), box.max());
    }

    /** Returns whether a closed axis-aligned box and finite closed segment overlap or touch. */
    public static boolean intersects(Aabb box, Segment3d segment) { return intersects(segment, box); }

    /** Returns whether a finite closed segment and closed oriented box overlap or touch. */
    public static boolean intersects(Segment3d segment, Obb box) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(box, "box");
        if (box.contains(segment.start()) || box.contains(segment.end())) return true;
        Vec3d center = box.center();
        Vec3d half = box.halfExtents();
        double rawX = segment.start().x() - center.x();
        double rawY = segment.start().y() - center.y();
        double rawZ = segment.start().z() - center.z();
        Vec3d delta = segment.delta();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, delta.x(), delta.y(), delta.z(), half.x(), half.y(), half.z())
                : maximumMagnitude(segment.start().x(), segment.start().y(), segment.start().z(), segment.end().x(), segment.end().y(), segment.end().z(), center.x(), center.y(), center.z(), half.x(), half.y(), half.z());
        if (scale == 0.0D) return true;
        Vec3d basisX = box.orientation().basisX();
        Vec3d basisY = box.orientation().basisY();
        Vec3d basisZ = box.orientation().basisZ();
        return intersectsSlab(
                localCoordinate(segment.start(), center, scale, basisX, rawX, rawY, rawZ, directRelativeCoordinates), localCoordinate(segment.start(), center, scale, basisY, rawX, rawY, rawZ, directRelativeCoordinates), localCoordinate(segment.start(), center, scale, basisZ, rawX, rawY, rawZ, directRelativeCoordinates),
                localDifference(segment.end(), segment.start(), delta, scale, basisX, directRelativeCoordinates), localDifference(segment.end(), segment.start(), delta, scale, basisY, directRelativeCoordinates), localDifference(segment.end(), segment.start(), delta, scale, basisZ, directRelativeCoordinates),
                half.x() / scale, half.y() / scale, half.z() / scale);
    }

    /** Returns whether a closed oriented box and finite closed segment overlap or touch. */
    public static boolean intersects(Obb box, Segment3d segment) { return intersects(segment, box); }

    /** Returns whether a finite closed segment and closed capsule overlap or touch. */
    public static boolean intersects(Segment3d segment, Capsule capsule) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(capsule, "capsule");
        return GeometryDistances.segmentToSegmentWithin(segment, capsule.centerline(), 0.0D, capsule.radius());
    }

    /** Returns whether a closed capsule and finite closed segment overlap or touch. */
    public static boolean intersects(Capsule capsule, Segment3d segment) { return intersects(segment, capsule); }

    private static boolean centersWithin(Vec3d first, Vec3d second, double firstRadius, double secondRadius) {
        double rawX = first.x() - second.x();
        double rawY = first.y() - second.y();
        double rawZ = first.z() - second.z();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, firstRadius, secondRadius)
                : maximumMagnitude(first.x(), first.y(), first.z(), second.x(), second.y(), second.z(), firstRadius, secondRadius);
        if (scale == 0.0D) return true;
        double dx = directRelativeCoordinates ? rawX / scale : GeometryDistances.normalizedDifference(first.x(), second.x(), scale);
        double dy = directRelativeCoordinates ? rawY / scale : GeometryDistances.normalizedDifference(first.y(), second.y(), scale);
        double dz = directRelativeCoordinates ? rawZ / scale : GeometryDistances.normalizedDifference(first.z(), second.z(), scale);
        double radius = firstRadius / scale + secondRadius / scale;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static boolean intersectsSlab(Vec3d start, Vec3d end, Vec3d min, Vec3d max) {
        return intersectsSlab(start.x(), start.y(), start.z(), end.x() - start.x(), end.y() - start.y(), end.z() - start.z(), min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    private static boolean intersectsSlab(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double halfX, double halfY, double halfZ) {
        return intersectsSlab(startX, startY, startZ, directionX, directionY, directionZ, -halfX, -halfY, -halfZ, halfX, halfY, halfZ);
    }

    private static boolean intersectsSlab(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double low = 0.0D;
        double high = 1.0D;
        double[] starts = {startX, startY, startZ};
        double[] directions = {directionX, directionY, directionZ};
        double[] minimums = {minX, minY, minZ};
        double[] maximums = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double direction = directions[axis];
            if (direction == 0.0D) {
                if (starts[axis] < minimums[axis] || starts[axis] > maximums[axis]) return false;
                continue;
            }
            double first = (minimums[axis] - starts[axis]) / direction;
            double second = (maximums[axis] - starts[axis]) / direction;
            low = Math.max(low, Math.min(first, second));
            high = Math.min(high, Math.max(first, second));
            if (low > high) return false;
        }
        return true;
    }

    private static double localCoordinate(Vec3d point, Vec3d center, double scale, Vec3d basis, double rawX, double rawY, double rawZ, boolean directRelativeCoordinates) {
        return directRelativeCoordinates ? dot(rawX / scale, rawY / scale, rawZ / scale, basis) : dot(GeometryDistances.normalizedDifference(point.x(), center.x(), scale), GeometryDistances.normalizedDifference(point.y(), center.y(), scale), GeometryDistances.normalizedDifference(point.z(), center.z(), scale), basis);
    }

    private static double localDifference(Vec3d end, Vec3d start, Vec3d delta, double scale, Vec3d basis, boolean directRelativeCoordinates) {
        return directRelativeCoordinates ? dot(delta.x() / scale, delta.y() / scale, delta.z() / scale, basis) : dot(GeometryDistances.normalizedDifference(end.x(), start.x(), scale), GeometryDistances.normalizedDifference(end.y(), start.y(), scale), GeometryDistances.normalizedDifference(end.z(), start.z(), scale), basis);
    }

    private static double dot(double x, double y, double z, Vec3d basis) { return x * basis.x() + y * basis.y() + z * basis.z(); }

    private static double maximumMagnitude(double... values) {
        double maximum = 0.0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }
}
