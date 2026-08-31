package dev.crlhitbox.api.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Independent, test-only numerical references for Phase 1B segment/box queries. */
final class Phase1BTestSupport {
    // Independent bisection/local-transform and normalized active-set paths can
    // accumulate different last-bit rounding; keep a local ULP bound with no
    // absolute floor so subnormal and wrong-active-interval defects remain visible.
    private static final double DISTANCE_ULP_FACTOR = 512.0D;
    private static final double DISTANCE_RELATIVE_FACTOR = 1.0E-12D;

    private Phase1BTestSupport() {
    }

    static double segmentToAabbSquaredByDerivativeBisection(Segment3d segment, Aabb box) {
        double low = 0.0D;
        double high = 1.0D;
        double best = residualSquared(segment, box, low);
        best = Math.min(best, residualSquared(segment, box, high));
        for (int iteration = 0; iteration < 160; iteration++) {
            double middle = low + (high - low) * 0.5D;
            if (middle == low || middle == high) break;
            best = Math.min(best, residualSquared(segment, box, middle));
            if (derivative(segment, box, middle) < 0.0D) low = middle;
            else high = middle;
        }
        double middle = low + (high - low) * 0.5D;
        double[] candidates = {low, high, middle, Math.nextDown(low), Math.nextUp(low), Math.nextDown(high), Math.nextUp(high)};
        for (double candidate : candidates) {
            if (candidate >= 0.0D && candidate <= 1.0D) best = Math.min(best, residualSquared(segment, box, candidate));
        }
        return best;
    }

    static double segmentToObbSquaredByLocalOracle(Segment3d segment, Obb box) {
        Vec3d center = box.center();
        Rotation3d inverse = box.orientation().inverse();
        Segment3d local = new Segment3d(
                inverse.rotate(segment.start().subtract(center)),
                inverse.rotate(segment.end().subtract(center))
        );
        Vec3d half = box.halfExtents();
        return segmentToAabbSquaredByDerivativeBisection(local, new Aabb(half.multiply(-1.0D), half));
    }

    static boolean obbObbSatByCornerProjection(Obb first, Obb second) {
        Vec3d[] firstCorners = obbCorners(first);
        Vec3d[] secondCorners = obbCorners(second);
        Vec3d[] firstAxes = {first.orientation().basisX(), first.orientation().basisY(), first.orientation().basisZ()};
        Vec3d[] secondAxes = {second.orientation().basisX(), second.orientation().basisY(), second.orientation().basisZ()};
        for (Vec3d axis : firstAxes) if (separates(firstCorners, secondCorners, axis.x(), axis.y(), axis.z())) return false;
        for (Vec3d axis : secondAxes) if (separates(firstCorners, secondCorners, axis.x(), axis.y(), axis.z())) return false;
        for (Vec3d left : firstAxes) for (Vec3d right : secondAxes) {
            double x = Math.fma(left.y(), right.z(), -left.z() * right.y());
            double y = Math.fma(left.z(), right.x(), -left.x() * right.z());
            double z = Math.fma(left.x(), right.y(), -left.y() * right.x());
            if (separates(firstCorners, secondCorners, x, y, z)) return false;
        }
        return true;
    }

    static boolean aabbObbSatByCornerProjection(Aabb first, Obb second) {
        Vec3d[] firstCorners = aabbCorners(first);
        Vec3d[] secondCorners = obbCorners(second);
        Vec3d[] obbAxes = {second.orientation().basisX(), second.orientation().basisY(), second.orientation().basisZ()};
        if (separates(firstCorners, secondCorners, 1.0D, 0.0D, 0.0D)
                || separates(firstCorners, secondCorners, 0.0D, 1.0D, 0.0D)
                || separates(firstCorners, secondCorners, 0.0D, 0.0D, 1.0D)) return false;
        for (Vec3d axis : obbAxes) if (separates(firstCorners, secondCorners, axis.x(), axis.y(), axis.z())) return false;
        for (Vec3d axis : obbAxes) {
            if (separates(firstCorners, secondCorners, 0.0D, -axis.z(), axis.y())
                    || separates(firstCorners, secondCorners, axis.z(), 0.0D, -axis.x())
                    || separates(firstCorners, secondCorners, -axis.y(), axis.x(), 0.0D)) return false;
        }
        return true;
    }

    static void assertDistanceClose(double expected, double actual, String message) {
        if (Double.isNaN(expected) || Double.isNaN(actual) || expected < 0.0D || actual < 0.0D) {
            fail(message + ", distances must be nonnegative and non-NaN: expected=" + expected + ", actual=" + actual);
        }
        if (Double.isInfinite(expected) || Double.isInfinite(actual)) {
            assertEquals(expected, actual, message);
            return;
        }
        double magnitude = Math.max(Math.abs(expected), Math.abs(actual));
        double tolerance = Math.max(
                Math.ulp(magnitude) * DISTANCE_ULP_FACTOR,
                magnitude * DISTANCE_RELATIVE_FACTOR
        );
        if (tolerance == 0.0D) tolerance = Double.MIN_VALUE;
        assertEquals(expected, actual, tolerance, message);
    }

    private static double derivative(Segment3d segment, Aabb box, double parameter) {
        Vec3d start = segment.start();
        Vec3d delta = segment.delta();
        return derivativeAxis(Math.fma(delta.x(), parameter, start.x()), delta.x(), box.min().x(), box.max().x())
                + derivativeAxis(Math.fma(delta.y(), parameter, start.y()), delta.y(), box.min().y(), box.max().y())
                + derivativeAxis(Math.fma(delta.z(), parameter, start.z()), delta.z(), box.min().z(), box.max().z());
    }

    private static double derivativeAxis(double value, double delta, double min, double max) {
        if (value < min) return 2.0D * (value - min) * delta;
        if (value > max) return 2.0D * (value - max) * delta;
        return 0.0D;
    }

    private static double residualSquared(Segment3d segment, Aabb box, double parameter) {
        Vec3d start = segment.start();
        Vec3d delta = segment.delta();
        double x = outside(Math.fma(delta.x(), parameter, start.x()), box.min().x(), box.max().x());
        double y = outside(Math.fma(delta.y(), parameter, start.y()), box.min().y(), box.max().y());
        double z = outside(Math.fma(delta.z(), parameter, start.z()), box.min().z(), box.max().z());
        return x * x + y * y + z * z;
    }

    private static double outside(double value, double min, double max) { return value < min ? value - min : value > max ? value - max : 0.0D; }

    private static boolean separates(Vec3d[] first, Vec3d[] second, double x, double y, double z) {
        double maximum = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (maximum == 0.0D) return false;
        x /= maximum;
        y /= maximum;
        z /= maximum;
        double firstLow = Double.POSITIVE_INFINITY;
        double firstHigh = Double.NEGATIVE_INFINITY;
        double secondLow = Double.POSITIVE_INFINITY;
        double secondHigh = Double.NEGATIVE_INFINITY;
        for (Vec3d corner : first) {
            double projection = Math.fma(corner.x(), x, Math.fma(corner.y(), y, corner.z() * z));
            firstLow = Math.min(firstLow, projection);
            firstHigh = Math.max(firstHigh, projection);
        }
        for (Vec3d corner : second) {
            double projection = Math.fma(corner.x(), x, Math.fma(corner.y(), y, corner.z() * z));
            secondLow = Math.min(secondLow, projection);
            secondHigh = Math.max(secondHigh, projection);
        }
        return firstLow > secondHigh || secondLow > firstHigh;
    }

    private static Vec3d[] aabbCorners(Aabb box) {
        Vec3d min = box.min();
        Vec3d max = box.max();
        Vec3d[] corners = new Vec3d[8];
        for (int signs = 0; signs < 8; signs++) corners[signs] = new Vec3d(
                (signs & 1) == 0 ? min.x() : max.x(),
                (signs & 2) == 0 ? min.y() : max.y(),
                (signs & 4) == 0 ? min.z() : max.z());
        return corners;
    }

    private static Vec3d[] obbCorners(Obb box) {
        Vec3d half = box.halfExtents();
        Vec3d x = box.orientation().basisX();
        Vec3d y = box.orientation().basisY();
        Vec3d z = box.orientation().basisZ();
        Vec3d center = box.center();
        Vec3d[] corners = new Vec3d[8];
        for (int signs = 0; signs < 8; signs++) {
            double sx = (signs & 1) == 0 ? -half.x() : half.x();
            double sy = (signs & 2) == 0 ? -half.y() : half.y();
            double sz = (signs & 4) == 0 ? -half.z() : half.z();
            corners[signs] = new Vec3d(Math.fma(x.x(), sx, Math.fma(y.x(), sy, Math.fma(z.x(), sz, center.x()))), Math.fma(x.y(), sx, Math.fma(y.y(), sy, Math.fma(z.y(), sz, center.y()))), Math.fma(x.z(), sx, Math.fma(y.z(), sy, Math.fma(z.z(), sz, center.z()))));
        }
        return corners;
    }
}
