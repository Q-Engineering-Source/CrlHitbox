package dev.crlhitbox.api.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Independent, test-only references for Phase 1E placed-query properties. */
final class Phase1ETestSupport {
    private static final double ULP_FACTOR = 256.0D;
    private static final double RELATIVE_FACTOR = 8.0D * Math.ulp(1.0D);

    private Phase1ETestSupport() {
    }

    static BoxOracle boxOracle(Solid3d solid, RigidTransform3d placement) {
        if (solid instanceof Aabb box) {
            return new BoxOracle(box.min(), box.max(), placement.translation(),
                    placement.rotation().basisX(), placement.rotation().basisY(), placement.rotation().basisZ(),
                    directlyTransformedCorners(box.min(), box.max(), placement));
        }
        Obb box = (Obb) solid;
        Vec3d half = box.halfExtents();
        // This is observable intrinsic-pose composition: map the local origin and each intrinsic
        // direction through the placement. It intentionally does not use RigidIntervalBox or andThen.
        return new BoxOracle(half.negate(), half,
                parentPoint(box.center(), placement),
                parentVector(box.orientation().basisX(), placement),
                parentVector(box.orientation().basisY(), placement),
                parentVector(box.orientation().basisZ(), placement),
                directlyTransformedObbCorners(box, placement));
    }

    static boolean intersectsBoxes(BoxOracle first, BoxOracle second) {
        for (Vec3d axis : first.axes()) if (separates(first, second, axis)) return false;
        for (Vec3d axis : second.axes()) if (separates(first, second, axis)) return false;
        for (Vec3d left : first.axes()) for (Vec3d right : second.axes()) {
            Vec3d cross = cross(left, right);
            if (!exactZero(cross) && separates(first, second, cross)) return false;
        }
        return true;
    }

    static boolean segmentIntersectsBox(Segment3d parentSegment, BoxOracle box) {
        Segment3d local = new Segment3d(box.inversePoint(parentSegment.start()), box.inversePoint(parentSegment.end()));
        return slabIntersects(local, box.localBounds());
    }

    static boolean sphereIntersectsBox(Sphere sphere, RigidTransform3d placement, BoxOracle box) {
        Vec3d local = box.inversePoint(parentPoint(sphere.center(), placement));
        return pointToAabbSquared(local, box.localBounds()) <= sphere.radius() * sphere.radius();
    }

    static boolean capsuleIntersectsBox(Capsule capsule, RigidTransform3d placement, BoxOracle box) {
        Segment3d local = new Segment3d(
                box.inversePoint(parentPoint(capsule.centerline().start(), placement)),
                box.inversePoint(parentPoint(capsule.centerline().end(), placement)));
        Aabb localBounds = box.localBounds();
        double squared = Phase1BTestSupport.segmentToAabbSquaredByDerivativeBisection(local, localBounds);
        return squared <= capsule.radius() * capsule.radius();
    }

    static boolean intersectsPlacedPrimitive(Solid3d first, RigidTransform3d firstTransform, Solid3d second, RigidTransform3d secondTransform) {
        boolean firstBox = first instanceof Aabb || first instanceof Obb;
        boolean secondBox = second instanceof Aabb || second instanceof Obb;
        if (firstBox && secondBox) return intersectsBoxes(boxOracle(first, firstTransform), boxOracle(second, secondTransform));
        if (firstBox) return boxIntersectsPrimitive(boxOracle(first, firstTransform), second, secondTransform);
        if (secondBox) return boxIntersectsPrimitive(boxOracle(second, secondTransform), first, firstTransform);
        return Phase1CTestSupport.intersectsPrimitiveTyped(materialize(first, firstTransform), materialize(second, secondTransform));
    }

    static boolean intersectsPlacedSolid(Solid3d first, RigidTransform3d firstTransform, Solid3d second, RigidTransform3d secondTransform) {
        if (first instanceof Composite left) {
            if (second instanceof Composite right) {
                for (int i = 0; i < left.childCount(); i++) for (int j = 0; j < right.childCount(); j++) {
                    if (intersectsPlacedPrimitive(left.child(i), firstTransform, right.child(j), secondTransform)) return true;
                }
                return false;
            }
            for (int i = 0; i < left.childCount(); i++) {
                if (intersectsPlacedPrimitive(left.child(i), firstTransform, second, secondTransform)) return true;
            }
            return false;
        }
        if (second instanceof Composite right) {
            for (int i = 0; i < right.childCount(); i++) {
                if (intersectsPlacedPrimitive(first, firstTransform, right.child(i), secondTransform)) return true;
            }
            return false;
        }
        return intersectsPlacedPrimitive(first, firstTransform, second, secondTransform);
    }

    static boolean intersectsSegmentPlaced(Segment3d segment, Solid3d solid, RigidTransform3d transform) {
        if (solid instanceof Composite composite) {
            for (int index = 0; index < composite.childCount(); index++) {
                if (intersectsSegmentPlaced(segment, composite.child(index), transform)) return true;
            }
            return false;
        }
        if (solid instanceof Aabb || solid instanceof Obb) return segmentIntersectsBox(segment, boxOracle(solid, transform));
        return Phase1CTestSupport.intersectsSegmentPrimitiveTyped(segment, materialize(solid, transform));
    }

    static Solid3d materialize(Solid3d local, RigidTransform3d placement) {
        return switch (local) {
            case Sphere sphere -> new Sphere(parentPoint(sphere.center(), placement), sphere.radius());
            case Capsule capsule -> new Capsule(new Segment3d(
                    parentPoint(capsule.centerline().start(), placement),
                    parentPoint(capsule.centerline().end(), placement)), capsule.radius());
            case Aabb ignored -> throw new IllegalArgumentException("Aabb materialization is intentionally unsupported by this oracle");
            case Obb ignored -> throw new IllegalArgumentException("Obb materialization is intentionally unsupported by this oracle");
            case Composite ignored -> throw new IllegalArgumentException("Composite must be enumerated by the oracle");
        };
    }

    static Vec3d parentPoint(Vec3d local, RigidTransform3d placement) {
        return combine(placement.translation(), placement.rotation().basisX(), placement.rotation().basisY(), placement.rotation().basisZ(), local);
    }

    static Vec3d parentVector(Vec3d local, RigidTransform3d placement) {
        return combine(new Vec3d(0.0D, 0.0D, 0.0D), placement.rotation().basisX(), placement.rotation().basisY(), placement.rotation().basisZ(), local);
    }

    static Segment3d parentSegment(Segment3d local, RigidTransform3d placement) {
        return new Segment3d(parentPoint(local.start(), placement), parentPoint(local.end(), placement));
    }

    static void assertBoundsContain(BoxOracle box, Aabb bounds, String message) {
        for (Vec3d corner : box.directCorners()) {
            if (!bounds.contains(corner)) fail(message + ", uncontainedParentCorner=" + corner + ", bounds=" + bounds);
        }
    }

    static void assertVectorClose(Vec3d expected, Vec3d actual, String message) {
        assertClose(expected.x(), actual.x(), message + ", component=x");
        assertClose(expected.y(), actual.y(), message + ", component=y");
        assertClose(expected.z(), actual.z(), message + ", component=z");
    }

    private static boolean boxIntersectsPrimitive(BoxOracle box, Solid3d primitive, RigidTransform3d transform) {
        return switch (primitive) {
            case Sphere sphere -> sphereIntersectsBox(sphere, transform, box);
            case Capsule capsule -> capsuleIntersectsBox(capsule, transform, box);
            case Aabb ignored -> throw new AssertionError("box was not classified");
            case Obb ignored -> throw new AssertionError("box was not classified");
            case Composite ignored -> throw new AssertionError("Composite must be flattened");
        };
    }

    private static boolean slabIntersects(Segment3d segment, Aabb box) {
        double low = 0.0D;
        double high = 1.0D;
        double[] start = {segment.start().x(), segment.start().y(), segment.start().z()};
        double[] delta = {segment.delta().x(), segment.delta().y(), segment.delta().z()};
        double[] min = {box.min().x(), box.min().y(), box.min().z()};
        double[] max = {box.max().x(), box.max().y(), box.max().z()};
        for (int axis = 0; axis < 3; axis++) {
            if (delta[axis] == 0.0D) {
                if (start[axis] < min[axis] || start[axis] > max[axis]) return false;
                continue;
            }
            double first = (min[axis] - start[axis]) / delta[axis];
            double second = (max[axis] - start[axis]) / delta[axis];
            low = Math.max(low, Math.min(first, second));
            high = Math.min(high, Math.max(first, second));
            if (low > high) return false;
        }
        return true;
    }

    private static double pointToAabbSquared(Vec3d point, Aabb box) {
        double x = outside(point.x(), box.min().x(), box.max().x());
        double y = outside(point.y(), box.min().y(), box.max().y());
        double z = outside(point.z(), box.min().z(), box.max().z());
        return x * x + y * y + z * z;
    }

    private static double outside(double value, double min, double max) {
        return value < min ? value - min : value > max ? value - max : 0.0D;
    }

    private static boolean separates(BoxOracle first, BoxOracle second, Vec3d rawAxis) {
        double maximum = Math.max(Math.abs(rawAxis.x()), Math.max(Math.abs(rawAxis.y()), Math.abs(rawAxis.z())));
        if (maximum == 0.0D) return false;
        Vec3d axis = new Vec3d(rawAxis.x() / maximum, rawAxis.y() / maximum, rawAxis.z() / maximum);
        double[] firstInterval = project(first.corners(), axis);
        double[] secondInterval = project(second.corners(), axis);
        return firstInterval[0] > secondInterval[1] || secondInterval[0] > firstInterval[1];
    }

    private static double[] project(Vec3d[] corners, Vec3d axis) {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (Vec3d corner : corners) {
            double value = Math.fma(corner.x(), axis.x(), Math.fma(corner.y(), axis.y(), corner.z() * axis.z()));
            low = Math.min(low, value);
            high = Math.max(high, value);
        }
        return new double[] {low, high};
    }

    private static Vec3d combine(Vec3d origin, Vec3d x, Vec3d y, Vec3d z, Vec3d value) {
        return new Vec3d(
                Math.fma(x.x(), value.x(), Math.fma(y.x(), value.y(), Math.fma(z.x(), value.z(), origin.x()))),
                Math.fma(x.y(), value.x(), Math.fma(y.y(), value.y(), Math.fma(z.y(), value.z(), origin.y()))),
                Math.fma(x.z(), value.x(), Math.fma(y.z(), value.y(), Math.fma(z.z(), value.z(), origin.z()))));
    }

    private static Vec3d cross(Vec3d first, Vec3d second) {
        return new Vec3d(
                Math.fma(first.y(), second.z(), -first.z() * second.y()),
                Math.fma(first.z(), second.x(), -first.x() * second.z()),
                Math.fma(first.x(), second.y(), -first.y() * second.x()));
    }

    private static boolean exactZero(Vec3d value) {
        return value.x() == 0.0D && value.y() == 0.0D && value.z() == 0.0D;
    }

    private static Vec3d[] directlyTransformedCorners(Vec3d min, Vec3d max, RigidTransform3d placement) {
        Vec3d[] corners = new Vec3d[8];
        for (int signs = 0; signs < 8; signs++) corners[signs] = placement.transformPoint(new Vec3d(
                (signs & 1) == 0 ? min.x() : max.x(),
                (signs & 2) == 0 ? min.y() : max.y(),
                (signs & 4) == 0 ? min.z() : max.z()));
        return corners;
    }

    private static Vec3d[] directlyTransformedObbCorners(Obb box, RigidTransform3d placement) {
        Vec3d half = box.halfExtents();
        Vec3d[] corners = new Vec3d[8];
        for (int signs = 0; signs < 8; signs++) corners[signs] = placement.transformPoint(box.localToWorld(new Vec3d(
                (signs & 1) == 0 ? -half.x() : half.x(),
                (signs & 2) == 0 ? -half.y() : half.y(),
                (signs & 4) == 0 ? -half.z() : half.z())));
        return corners;
    }

    private static void assertClose(double expected, double actual, String message) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) return;
        if (!Double.isFinite(expected) || !Double.isFinite(actual)) {
            assertEquals(expected, actual, message);
            return;
        }
        double scale = Math.max(Math.abs(expected), Math.abs(actual));
        if (scale == 0.0D) {
            assertEquals(expected, actual, message + ", strictZero");
            return;
        }
        double tolerance = Math.max(Math.ulp(scale) * ULP_FACTOR, scale * RELATIVE_FACTOR);
        assertEquals(expected, actual, tolerance, message + ", tolerancePolicy=max(256 ULP(scale), scale * 8 ulp(1.0)), no absolute floor");
    }

    static final class BoxOracle {
        private final Vec3d localMin;
        private final Vec3d localMax;
        private final Vec3d origin;
        private final Vec3d basisX;
        private final Vec3d basisY;
        private final Vec3d basisZ;
        private final Vec3d[] directCorners;

        private BoxOracle(Vec3d localMin, Vec3d localMax, Vec3d origin, Vec3d basisX, Vec3d basisY, Vec3d basisZ, Vec3d[] directCorners) {
            this.localMin = localMin;
            this.localMax = localMax;
            this.origin = origin;
            this.basisX = basisX;
            this.basisY = basisY;
            this.basisZ = basisZ;
            this.directCorners = directCorners;
        }

        Aabb localBounds() {
            return new Aabb(localMin, localMax);
        }

        Vec3d inversePoint(Vec3d parent) {
            Vec3d relative = parent.subtract(origin);
            return new Vec3d(
                    Math.fma(relative.x(), basisX.x(), Math.fma(relative.y(), basisX.y(), relative.z() * basisX.z())),
                    Math.fma(relative.x(), basisY.x(), Math.fma(relative.y(), basisY.y(), relative.z() * basisY.z())),
                    Math.fma(relative.x(), basisZ.x(), Math.fma(relative.y(), basisZ.y(), relative.z() * basisZ.z())));
        }

        Vec3d[] axes() {
            return new Vec3d[] {basisX, basisY, basisZ};
        }

        Vec3d[] corners() {
            Vec3d[] result = new Vec3d[8];
            for (int signs = 0; signs < 8; signs++) {
                result[signs] = combine(origin, basisX, basisY, basisZ, new Vec3d(
                        (signs & 1) == 0 ? localMin.x() : localMax.x(),
                        (signs & 2) == 0 ? localMin.y() : localMax.y(),
                        (signs & 4) == 0 ? localMin.z() : localMax.z()));
            }
            return result;
        }

        Vec3d[] directCorners() {
            return directCorners.clone();
        }
    }
}
