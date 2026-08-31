package dev.crlhitbox.api.geometry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/** Strict scale-derived assertions and independent Phase 1D test oracles. */
final class Phase1DTestSupport {
    private static final double ULP_FACTOR = 256.0D;
    private static final double RELATIVE_FACTOR = 8.0D * Math.ulp(1.0D);

    private Phase1DTestSupport() {
    }

    static void assertVectorClose(Vec3d expected, Vec3d actual, String context) {
        double referenceScale = maximumMagnitude(
                expected.x(), expected.y(), expected.z(),
                actual.x(), actual.y(), actual.z());
        assertClose(expected.x(), actual.x(), referenceScale, context + ", component=x");
        assertClose(expected.y(), actual.y(), referenceScale, context + ", component=y");
        assertClose(expected.z(), actual.z(), referenceScale, context + ", component=z");
    }

    static void assertClose(double expected, double actual, double referenceScale, String context) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) return;
        if (!Double.isFinite(expected) || !Double.isFinite(actual)) {
            fail(context + ", expected and actual must be finite: expected=" + expected + ", actual=" + actual);
        }
        double scale = maximumMagnitude(expected, actual, referenceScale);
        double tolerance = Math.max(Math.ulp(scale) * ULP_FACTOR, scale * RELATIVE_FACTOR);
        if (Math.abs(expected - actual) > tolerance) {
            fail(context + ", expected=" + expected + ", actual=" + actual
                    + ", scale=" + scale + ", tolerance=" + tolerance
                    + ", ulpFactor=" + ULP_FACTOR + ", relativeFactor=" + RELATIVE_FACTOR);
        }
    }

    static Vec3d matrixTransformPoint(Rotation3d rotation, Vec3d translation, Vec3d point) {
        return matrixTransformVector(rotation, point).add(translation);
    }

    static Vec3d matrixTransformVector(Rotation3d rotation, Vec3d vector) {
        Vec3d basisX = rotation.basisX();
        Vec3d basisY = rotation.basisY();
        Vec3d basisZ = rotation.basisZ();
        return new Vec3d(
                basisX.x() * vector.x() + basisY.x() * vector.y() + basisZ.x() * vector.z(),
                basisX.y() * vector.x() + basisY.y() * vector.y() + basisZ.y() * vector.z(),
                basisX.z() * vector.x() + basisY.z() * vector.y() + basisZ.z() * vector.z());
    }

    static Vec3d matrixInverseTransformPoint(Rotation3d rotation, Vec3d translation, Vec3d point) {
        return matrixInverseTransformVector(rotation, point.subtract(translation));
    }

    static Vec3d matrixInverseTransformVector(Rotation3d rotation, Vec3d vector) {
        Vec3d basisX = rotation.basisX();
        Vec3d basisY = rotation.basisY();
        Vec3d basisZ = rotation.basisZ();
        return new Vec3d(
                vector.x() * basisX.x() + vector.y() * basisX.y() + vector.z() * basisX.z(),
                vector.x() * basisY.x() + vector.y() * basisY.y() + vector.z() * basisY.z(),
                vector.x() * basisZ.x() + vector.y() * basisZ.y() + vector.z() * basisZ.z());
    }

    static Segment3d transformSegment(RigidTransform3d transform, Segment3d segment) {
        return new Segment3d(
                transform.transformPoint(segment.start()),
                transform.transformPoint(segment.end()));
    }

    static Solid3d transformSolid(RigidTransform3d transform, Solid3d solid) {
        return switch (solid) {
            case Aabb box -> transformControlledAabb(transform, box);
            case Sphere sphere -> new Sphere(transform.transformPoint(sphere.center()), sphere.radius());
            case Obb box -> {
                RigidTransform3d localPose = new RigidTransform3d(box.orientation(), box.center());
                RigidTransform3d transformedPose = localPose.andThen(transform);
                yield new Obb(transformedPose.translation(), box.halfExtents(), transformedPose.rotation());
            }
            case Capsule capsule -> new Capsule(transformSegment(transform, capsule.centerline()), capsule.radius());
            case Composite composite -> transformComposite(transform, composite);
        };
    }

    static Aabb translateAabb(RigidTransform3d translationOnly, Aabb box) {
        if (!translationOnly.rotation().equals(Rotation3d.identity())) {
            throw new IllegalArgumentException("translationOnly must have identity rotation");
        }
        return new Aabb(
                translationOnly.transformPoint(box.min()),
                translationOnly.transformPoint(box.max()));
    }

    private static Obb transformControlledAabb(RigidTransform3d transform, Aabb box) {
        // Test-only: callers supply moderate dyadic center/half-extents so this identity-oriented
        // Aabb re-encoding is exact. This is not a production Aabb projection contract.
        RigidTransform3d localPose = new RigidTransform3d(Rotation3d.identity(), box.center());
        RigidTransform3d transformedPose = localPose.andThen(transform);
        return new Obb(transformedPose.translation(), box.halfExtents(), transformedPose.rotation());
    }

    private static Composite transformComposite(RigidTransform3d transform, Composite composite) {
        List<Solid3d> leaves = new ArrayList<>(composite.childCount());
        for (int index = 0; index < composite.childCount(); index++) {
            leaves.add(transformSolid(transform, composite.child(index)));
        }
        return new Composite(leaves);
    }

    private static double maximumMagnitude(double... values) {
        double maximum = 0.0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }
}
