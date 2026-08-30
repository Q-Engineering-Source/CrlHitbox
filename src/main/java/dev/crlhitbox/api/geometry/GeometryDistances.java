package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** Pure double-precision squared-distance queries for the supported finite geometry values. */
public final class GeometryDistances {
    /**
     * A local relative threshold for the scaled segment-direction denominator.
     * It selects the cross-product numerator for a conditioned interior candidate and is not an overlap epsilon.
     */
    private static final double PARALLEL_DENOMINATOR_RELATIVE_FACTOR = 64.0D * Math.ulp(1.0D);

    private GeometryDistances() {
    }

    /** Returns the squared distance from {@code point} to the finite closed {@code segment}. */
    public static double pointToSegmentSquared(Vec3d point, Segment3d segment) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(segment, "segment");
        return pointToSegmentSquared(point.x() - segment.start().x(), point.y() - segment.start().y(), point.z() - segment.start().z(), segment.delta());
    }

    /** Returns the true minimum squared distance between two finite closed segments. */
    public static double segmentToSegmentSquared(Segment3d first, Segment3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        double rx = first.start().x() - second.start().x();
        double ry = first.start().y() - second.start().y();
        double rz = first.start().z() - second.start().z();
        if (!Double.isFinite(rx) || !Double.isFinite(ry) || !Double.isFinite(rz)) return Double.POSITIVE_INFINITY;
        Vec3d firstDelta = first.delta();
        Vec3d secondDelta = second.delta();
        double scale = maximumMagnitude(rx, ry, rz, firstDelta.x(), firstDelta.y(), firstDelta.z(), secondDelta.x(), secondDelta.y(), secondDelta.z());
        if (scale == 0.0D) return 0.0D;
        return scaledSquaredFromNormalized(segmentMinimumSquared(rx / scale, ry / scale, rz / scale,
                firstDelta.x() / scale, firstDelta.y() / scale, firstDelta.z() / scale,
                secondDelta.x() / scale, secondDelta.y() / scale, secondDelta.z() / scale), scale);
    }

    /** Returns the squared distance from {@code point} to the closed axis-aligned {@code box}. */
    public static double pointToAabbSquared(Vec3d point, Aabb box) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(box, "box");
        return squaredFromOffsets(outsideOffset(point.x(), box.min().x(), box.max().x()), outsideOffset(point.y(), box.min().y(), box.max().y()), outsideOffset(point.z(), box.min().z(), box.max().z()));
    }

    /** Returns the squared distance from {@code point} to the closed oriented {@code box}. */
    public static double pointToObbSquared(Vec3d point, Obb box) {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(box, "box");
        if (box.contains(point)) return 0.0D;
        Vec3d center = box.center();
        Vec3d half = box.halfExtents();
        double rawX = point.x() - center.x();
        double rawY = point.y() - center.y();
        double rawZ = point.z() - center.z();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, half.x(), half.y(), half.z())
                : maximumMagnitude(point.x(), point.y(), point.z(), center.x(), center.y(), center.z(), half.x(), half.y(), half.z());
        if (scale == 0.0D) return 0.0D;
        Vec3d basisX = box.orientation().basisX();
        Vec3d basisY = box.orientation().basisY();
        Vec3d basisZ = box.orientation().basisZ();
        double dx = directRelativeCoordinates ? rawX / scale : normalizedDifference(point.x(), center.x(), scale);
        double dy = directRelativeCoordinates ? rawY / scale : normalizedDifference(point.y(), center.y(), scale);
        double dz = directRelativeCoordinates ? rawZ / scale : normalizedDifference(point.z(), center.z(), scale);
        return scaledSquaredFromNormalized(normalizedOutsideSquared(
                dot(dx, dy, dz, basisX.x(), basisX.y(), basisX.z()),
                dot(dx, dy, dz, basisY.x(), basisY.y(), basisY.z()),
                dot(dx, dy, dz, basisZ.x(), basisZ.y(), basisZ.z()),
                half.x() / scale, half.y() / scale, half.z() / scale), scale);
    }

    static boolean pointToSegmentWithin(Vec3d point, Segment3d segment, double pointRadius, double segmentRadius) {
        double rawX = point.x() - segment.start().x();
        double rawY = point.y() - segment.start().y();
        double rawZ = point.z() - segment.start().z();
        Vec3d delta = segment.delta();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, delta.x(), delta.y(), delta.z(), pointRadius, segmentRadius)
                : maximumMagnitude(point.x(), point.y(), point.z(), segment.start().x(), segment.start().y(), segment.start().z(), segment.end().x(), segment.end().y(), segment.end().z(), pointRadius, segmentRadius);
        if (scale == 0.0D) return true;
        double rx = directRelativeCoordinates ? rawX / scale : normalizedDifference(point.x(), segment.start().x(), scale);
        double ry = directRelativeCoordinates ? rawY / scale : normalizedDifference(point.y(), segment.start().y(), scale);
        double rz = directRelativeCoordinates ? rawZ / scale : normalizedDifference(point.z(), segment.start().z(), scale);
        double dx = directRelativeCoordinates ? delta.x() / scale : normalizedDifference(segment.end().x(), segment.start().x(), scale);
        double dy = directRelativeCoordinates ? delta.y() / scale : normalizedDifference(segment.end().y(), segment.start().y(), scale);
        double dz = directRelativeCoordinates ? delta.z() / scale : normalizedDifference(segment.end().z(), segment.start().z(), scale);
        double directionSquared = dot(dx, dy, dz, dx, dy, dz);
        double parameter = directionSquared == 0.0D ? 0.0D : clamp(dot(rx, ry, rz, dx, dy, dz) / directionSquared);
        double ex;
        double ey;
        double ez;
        if (parameter == 0.0D) {
            ex = rx;
            ey = ry;
            ez = rz;
        } else if (parameter == 1.0D) {
            ex = directRelativeCoordinates ? (point.x() - segment.end().x()) / scale : normalizedDifference(point.x(), segment.end().x(), scale);
            ey = directRelativeCoordinates ? (point.y() - segment.end().y()) / scale : normalizedDifference(point.y(), segment.end().y(), scale);
            ez = directRelativeCoordinates ? (point.z() - segment.end().z()) / scale : normalizedDifference(point.z(), segment.end().z(), scale);
        } else {
            ex = rx - parameter * dx;
            ey = ry - parameter * dy;
            ez = rz - parameter * dz;
        }
        double radius = pointRadius / scale + segmentRadius / scale;
        return ex * ex + ey * ey + ez * ez <= radius * radius;
    }

    static boolean segmentToSegmentWithin(Segment3d first, Segment3d second, double firstRadius, double secondRadius) {
        // The four point/segment checks are the exact boundary minima of the
        // segment-parameter square and preserve closed tangency at its edges.
        if (pointToSegmentWithin(first.start(), second, firstRadius, secondRadius)
                || pointToSegmentWithin(first.end(), second, firstRadius, secondRadius)
                || pointToSegmentWithin(second.start(), first, secondRadius, firstRadius)
                || pointToSegmentWithin(second.end(), first, secondRadius, firstRadius)) {
            return true;
        }
        double rawX = first.start().x() - second.start().x();
        double rawY = first.start().y() - second.start().y();
        double rawZ = first.start().z() - second.start().z();
        Vec3d firstDelta = first.delta();
        Vec3d secondDelta = second.delta();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, firstDelta.x(), firstDelta.y(), firstDelta.z(), secondDelta.x(), secondDelta.y(), secondDelta.z(), firstRadius, secondRadius)
                : maximumMagnitude(first.start().x(), first.start().y(), first.start().z(), first.end().x(), first.end().y(), first.end().z(), second.start().x(), second.start().y(), second.start().z(), second.end().x(), second.end().y(), second.end().z(), firstRadius, secondRadius);
        if (scale == 0.0D) return true;
        double squared = segmentMinimumSquared(
                directRelativeCoordinates ? rawX / scale : normalizedDifference(first.start().x(), second.start().x(), scale), directRelativeCoordinates ? rawY / scale : normalizedDifference(first.start().y(), second.start().y(), scale), directRelativeCoordinates ? rawZ / scale : normalizedDifference(first.start().z(), second.start().z(), scale),
                directRelativeCoordinates ? firstDelta.x() / scale : normalizedDifference(first.end().x(), first.start().x(), scale), directRelativeCoordinates ? firstDelta.y() / scale : normalizedDifference(first.end().y(), first.start().y(), scale), directRelativeCoordinates ? firstDelta.z() / scale : normalizedDifference(first.end().z(), first.start().z(), scale),
                directRelativeCoordinates ? secondDelta.x() / scale : normalizedDifference(second.end().x(), second.start().x(), scale), directRelativeCoordinates ? secondDelta.y() / scale : normalizedDifference(second.end().y(), second.start().y(), scale), directRelativeCoordinates ? secondDelta.z() / scale : normalizedDifference(second.end().z(), second.start().z(), scale));
        double radius = firstRadius / scale + secondRadius / scale;
        return squared <= radius * radius;
    }

    static boolean pointToAabbWithin(Vec3d point, Aabb box, double radius) {
        double rawX = outsideOffset(point.x(), box.min().x(), box.max().x());
        double rawY = outsideOffset(point.y(), box.min().y(), box.max().y());
        double rawZ = outsideOffset(point.z(), box.min().z(), box.max().z());
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, radius)
                : maximumMagnitude(point.x(), point.y(), point.z(), box.min().x(), box.min().y(), box.min().z(), box.max().x(), box.max().y(), box.max().z(), radius);
        if (scale == 0.0D) return true;
        double x = directRelativeCoordinates ? rawX / scale : normalizedOutsideOffset(point.x(), box.min().x(), box.max().x(), scale);
        double y = directRelativeCoordinates ? rawY / scale : normalizedOutsideOffset(point.y(), box.min().y(), box.max().y(), scale);
        double z = directRelativeCoordinates ? rawZ / scale : normalizedOutsideOffset(point.z(), box.min().z(), box.max().z(), scale);
        double normalizedRadius = radius / scale;
        return x * x + y * y + z * z <= normalizedRadius * normalizedRadius;
    }

    static boolean pointToObbWithin(Vec3d point, Obb box, double radius) {
        if (box.contains(point)) return true;
        Vec3d center = box.center();
        Vec3d half = box.halfExtents();
        double rawX = point.x() - center.x();
        double rawY = point.y() - center.y();
        double rawZ = point.z() - center.z();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, half.x(), half.y(), half.z(), radius)
                : maximumMagnitude(point.x(), point.y(), point.z(), center.x(), center.y(), center.z(), half.x(), half.y(), half.z(), radius);
        if (scale == 0.0D) return true;
        Vec3d basisX = box.orientation().basisX();
        Vec3d basisY = box.orientation().basisY();
        Vec3d basisZ = box.orientation().basisZ();
        double dx = directRelativeCoordinates ? rawX / scale : normalizedDifference(point.x(), center.x(), scale);
        double dy = directRelativeCoordinates ? rawY / scale : normalizedDifference(point.y(), center.y(), scale);
        double dz = directRelativeCoordinates ? rawZ / scale : normalizedDifference(point.z(), center.z(), scale);
        double squared = normalizedOutsideSquared(dot(dx, dy, dz, basisX.x(), basisX.y(), basisX.z()), dot(dx, dy, dz, basisY.x(), basisY.y(), basisY.z()), dot(dx, dy, dz, basisZ.x(), basisZ.y(), basisZ.z()), half.x() / scale, half.y() / scale, half.z() / scale);
        double normalizedRadius = radius / scale;
        return squared <= normalizedRadius * normalizedRadius;
    }

    static double normalizedDifference(double first, double second, double scale) {
        double difference = first - second;
        return Double.isFinite(difference) ? difference / scale : first / scale - second / scale;
    }

    private static double segmentMinimumSquared(double rx, double ry, double rz, double ux, double uy, double uz, double vx, double vy, double vz) {
        double a = dot(ux, uy, uz, ux, uy, uz);
        double e = dot(vx, vy, vz, vx, vy, vz);
        if (a == 0.0D && e == 0.0D) return dot(rx, ry, rz, rx, ry, rz);
        double minimum = Double.POSITIVE_INFINITY;
        if (e != 0.0D) {
            minimum = Math.min(minimum, residualSquared(rx, ry, rz, ux, uy, uz, vx, vy, vz, 0.0D, clamp(dot(vx, vy, vz, rx, ry, rz) / e)));
            minimum = Math.min(minimum, residualSquared(rx, ry, rz, ux, uy, uz, vx, vy, vz, 1.0D, clamp(dot(vx, vy, vz, rx + ux, ry + uy, rz + uz) / e)));
        }
        if (a != 0.0D) {
            minimum = Math.min(minimum, residualSquared(rx, ry, rz, ux, uy, uz, vx, vy, vz, clamp(-dot(ux, uy, uz, rx, ry, rz) / a), 0.0D));
            minimum = Math.min(minimum, residualSquared(rx, ry, rz, ux, uy, uz, vx, vy, vz, clamp(dot(ux, uy, uz, vx - rx, vy - ry, vz - rz) / a), 1.0D));
        }
        if (a == 0.0D || e == 0.0D) return minimum;
        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double denominator = dot(nx, ny, nz, nx, ny, nz);
        if (denominator == 0.0D) return minimum;
        double s;
        double t;
        if (denominator <= PARALLEL_DENOMINATOR_RELATIVE_FACTOR * a * e) {
            s = -dot(ry * vz - rz * vy, rz * vx - rx * vz, rx * vy - ry * vx, nx, ny, nz) / denominator;
            t = -dot(ry * uz - rz * uy, rz * ux - rx * uz, rx * uy - ry * ux, nx, ny, nz) / denominator;
        } else {
            double b = dot(ux, uy, uz, vx, vy, vz);
            double c = dot(ux, uy, uz, rx, ry, rz);
            double f = dot(vx, vy, vz, rx, ry, rz);
            s = (b * f - c * e) / denominator;
            t = (a * f - b * c) / denominator;
        }
        return s >= 0.0D && s <= 1.0D && t >= 0.0D && t <= 1.0D ? Math.min(minimum, residualSquared(rx, ry, rz, ux, uy, uz, vx, vy, vz, s, t)) : minimum;
    }

    private static double pointToSegmentSquared(double rx, double ry, double rz, Vec3d delta) {
        if (!Double.isFinite(rx) || !Double.isFinite(ry) || !Double.isFinite(rz)) return Double.POSITIVE_INFINITY;
        double scale = maximumMagnitude(rx, ry, rz, delta.x(), delta.y(), delta.z());
        if (scale == 0.0D) return 0.0D;
        double sx = rx / scale;
        double sy = ry / scale;
        double sz = rz / scale;
        double dx = delta.x() / scale;
        double dy = delta.y() / scale;
        double dz = delta.z() / scale;
        double directionSquared = dot(dx, dy, dz, dx, dy, dz);
        double parameter = directionSquared == 0.0D ? 0.0D : clamp(dot(sx, sy, sz, dx, dy, dz) / directionSquared);
        return scaledSquared(sx - parameter * dx, sy - parameter * dy, sz - parameter * dz, scale);
    }

    private static double residualSquared(double rx, double ry, double rz, double ux, double uy, double uz, double vx, double vy, double vz, double s, double t) {
        double x = rx + ux * s - vx * t;
        double y = ry + uy * s - vy * t;
        double z = rz + uz * s - vz * t;
        return dot(x, y, z, x, y, z);
    }

    private static double normalizedOutsideSquared(double x, double y, double z, double halfX, double halfY, double halfZ) {
        double offsetX = outsideOffset(x, -halfX, halfX);
        double offsetY = outsideOffset(y, -halfY, halfY);
        double offsetZ = outsideOffset(z, -halfZ, halfZ);
        return dot(offsetX, offsetY, offsetZ, offsetX, offsetY, offsetZ);
    }

    private static double normalizedOutsideOffset(double value, double min, double max, double scale) {
        if (value < min) return normalizedDifference(min, value, scale);
        if (value > max) return normalizedDifference(value, max, scale);
        return 0.0D;
    }

    private static double outsideOffset(double value, double min, double max) {
        if (value < min) return min - value;
        if (value > max) return value - max;
        return 0.0D;
    }

    private static double squaredFromOffsets(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return Double.POSITIVE_INFINITY;
        double scale = maximumMagnitude(x, y, z);
        return scale == 0.0D ? 0.0D : scaledSquared(x / scale, y / scale, z / scale, scale);
    }

    private static double scaledSquared(double x, double y, double z, double scale) { return scaledSquaredFromNormalized(dot(x, y, z, x, y, z), scale); }

    private static double scaledSquaredFromNormalized(double squared, double scale) {
        if (scale == 0.0D || squared == 0.0D) return 0.0D;
        double result = squared * scale * scale;
        return result == 0.0D ? 0.0D : result;
    }

    private static double maximumMagnitude(double... values) {
        double maximum = 0.0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }

    private static double dot(double ax, double ay, double az, double bx, double by, double bz) { return ax * bx + ay * by + az * bz; }

    private static double clamp(double value) { return value <= 0.0D ? 0.0D : value >= 1.0D ? 1.0D : value; }
}
