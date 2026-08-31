package dev.crlhitbox.api.geometry;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase1DPropertyTest {
    private static final long MAPPING_SEED = 0x5EED_1D01L;
    private static final long COMPOSITION_SEED = 0x5EED_1D02L;
    private static final long GEOMETRY_SEED = 0x5EED_1D03L;
    private static final int ITERATIONS = 2048;

    @Test
    void pointVectorAndInverseMappingsAgreeWithMatrixOracleForFixedSeed() {
        SplittableRandom random = new SplittableRandom(MAPPING_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            RigidTransform3d transform = transform(random);
            Vec3d point = vector(random, -10.0D, 10.0D);
            Vec3d vector = iteration % 17 == 0
                    ? new Vec3d(0.0D, 0.0D, 0.0D)
                    : vector(random, -10.0D, 10.0D);

            Vec3d expectedPoint = Phase1DTestSupport.matrixTransformPoint(transform.rotation(), transform.translation(), point);
            Vec3d actualPoint = transform.transformPoint(point);
            assertVectorProperty(MAPPING_SEED, iteration, transform, point, "transformPoint matrix oracle",
                    expectedPoint, actualPoint, "matrixPoint=" + expectedPoint);

            Vec3d expectedVector = Phase1DTestSupport.matrixTransformVector(transform.rotation(), vector);
            Vec3d actualVector = transform.transformVector(vector);
            assertVectorProperty(MAPPING_SEED, iteration, transform, vector, "transformVector matrix oracle",
                    expectedVector, actualVector, "matrixVector=" + expectedVector);

            Vec3d expectedInversePoint = Phase1DTestSupport.matrixInverseTransformPoint(transform.rotation(), transform.translation(), point);
            Vec3d actualInversePoint = transform.inverseTransformPoint(point);
            assertVectorProperty(MAPPING_SEED, iteration, transform, point, "inverseTransformPoint transpose oracle",
                    expectedInversePoint, actualInversePoint, "relative=" + point.subtract(transform.translation()));

            Vec3d expectedInverseVector = Phase1DTestSupport.matrixInverseTransformVector(transform.rotation(), vector);
            Vec3d actualInverseVector = transform.inverseTransformVector(vector);
            assertVectorProperty(MAPPING_SEED, iteration, transform, vector, "inverseTransformVector transpose oracle",
                    expectedInverseVector, actualInverseVector, "matrixInverseVector=" + expectedInverseVector);

            Vec3d pointRoundTrip = transform.inverseTransformPoint(actualPoint);
            assertVectorProperty(MAPPING_SEED, iteration, transform, point, "inverseTransformPoint(transformPoint(point))",
                    point, pointRoundTrip, "forwardPoint=" + actualPoint);

            Vec3d vectorRoundTrip = transform.inverseTransformVector(actualVector);
            assertVectorProperty(MAPPING_SEED, iteration, transform, vector, "inverseTransformVector(transformVector(vector))",
                    vector, vectorRoundTrip, "forwardVector=" + actualVector);

            Vec3d inverseValuePoint = transform.inverse().transformPoint(point);
            assertVectorProperty(MAPPING_SEED, iteration, transform, point, "inverse value/direct inverse point",
                    actualInversePoint, inverseValuePoint, "directInverse=" + actualInversePoint);
        }
    }

    @Test
    void compositionOrderSequentialMappingAndInverseCompositionUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(COMPOSITION_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            RigidTransform3d first = transform(random);
            RigidTransform3d after = transform(random);
            RigidTransform3d third = transform(random);
            Vec3d point = vector(random, -8.0D, 8.0D);
            Vec3d vector = vector(random, -8.0D, 8.0D);
            RigidTransform3d composed = first.andThen(after);

            Vec3d firstPoint = first.transformPoint(point);
            Vec3d expectedPoint = after.transformPoint(firstPoint);
            Vec3d actualPoint = composed.transformPoint(point);
            assertVectorProperty(COMPOSITION_SEED, iteration, composed, point, "a.andThen(b) point = b(a(point))",
                    expectedPoint, actualPoint, "aPoint=" + firstPoint + ", bAfterA=" + expectedPoint);

            Vec3d firstVector = first.transformVector(vector);
            Vec3d expectedVector = after.transformVector(firstVector);
            Vec3d actualVector = composed.transformVector(vector);
            assertVectorProperty(COMPOSITION_SEED, iteration, composed, vector, "a.andThen(b) vector = b(a(vector))",
                    expectedVector, actualVector, "aVector=" + firstVector + ", bAfterA=" + expectedVector);

            Vec3d matrixPoint = Phase1DTestSupport.matrixTransformPoint(composed.rotation(), composed.translation(), point);
            assertVectorProperty(COMPOSITION_SEED, iteration, composed, point, "composed matrix point cross-check",
                    matrixPoint, actualPoint, "sequential=" + expectedPoint);

            Vec3d expectedTranslation = Phase1DTestSupport.matrixTransformVector(after.rotation(), first.translation())
                    .add(after.translation());
            assertVectorProperty(COMPOSITION_SEED, iteration, composed, first.translation(), "composed translation Rb(ta)+tb",
                    expectedTranslation, composed.translation(), "RbTa=" + Phase1DTestSupport.matrixTransformVector(after.rotation(), first.translation()));

            RigidTransform3d expectedInverse = after.inverse().andThen(first.inverse());
            RigidTransform3d actualInverse = composed.inverse();
            assertVectorProperty(COMPOSITION_SEED, iteration, actualInverse, point, "inverse composition point",
                    expectedInverse.transformPoint(point), actualInverse.transformPoint(point),
                    "expectedInverse=" + expectedInverse);

            RigidTransform3d leftGrouped = first.andThen(after).andThen(third);
            RigidTransform3d rightGrouped = first.andThen(after.andThen(third));
            assertVectorProperty(COMPOSITION_SEED, iteration, leftGrouped, point, "binary64 observational associativity point",
                    leftGrouped.transformPoint(point), rightGrouped.transformPoint(point),
                    "left=" + leftGrouped + ", right=" + rightGrouped);
            assertVectorProperty(COMPOSITION_SEED, iteration, leftGrouped, vector, "binary64 observational associativity vector",
                    leftGrouped.transformVector(vector), rightGrouped.transformVector(vector),
                    "left=" + leftGrouped + ", right=" + rightGrouped);
        }
    }

    @Test
    void existingGeometryQueriesPreserveControlledRigidMotionForFixedSeed() {
        SplittableRandom random = new SplittableRandom(GEOMETRY_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            RigidTransform3d common = transform(random);
            Vec3d center = dyadicCenter(random);
            boolean overlapping = (iteration & 1) == 0;
            Vec3d otherCenter = overlapping ? center : center.add(new Vec3d(12.0D, 0.0D, 0.0D));
            Solid3d first = primitiveAt(random, iteration & 3, center);
            Solid3d second = primitiveAt(random, iteration >>> 2 & 3, otherCenter);
            Segment3d segment = new Segment3d(
                    center.add(new Vec3d(-3.0D, 0.0D, 0.0D)),
                    center.add(new Vec3d(3.0D, 0.0D, 0.0D)));
            Composite firstComposite = new Composite(List.of(
                    primitiveAt(random, 1, center.add(new Vec3d(-20.0D, 0.0D, 0.0D))),
                    first,
                    first));
            Composite secondComposite = new Composite(List.of(
                    second,
                    primitiveAt(random, 3, center.add(new Vec3d(30.0D, 0.0D, 0.0D)))));

            Solid3d transformedFirst = Phase1DTestSupport.transformSolid(common, first);
            Solid3d transformedSecond = Phase1DTestSupport.transformSolid(common, second);
            Segment3d transformedSegment = Phase1DTestSupport.transformSegment(common, segment);
            Composite transformedFirstComposite = (Composite) Phase1DTestSupport.transformSolid(common, firstComposite);
            Composite transformedSecondComposite = (Composite) Phase1DTestSupport.transformSolid(common, secondComposite);

            assertBooleanProperty(GEOMETRY_SEED, iteration, common, "primitive/primitive",
                    GeometryIntersections.intersects(first, second),
                    GeometryIntersections.intersects(transformedFirst, transformedSecond),
                    "first=" + first + ", second=" + second,
                    "transformedFirst=" + transformedFirst + ", transformedSecond=" + transformedSecond);

            assertBooleanProperty(GEOMETRY_SEED, iteration, common, "segment/primitive",
                    GeometryIntersections.intersects(segment, second),
                    GeometryIntersections.intersects(transformedSegment, transformedSecond),
                    "segment=" + segment + ", second=" + second,
                    "transformedSegment=" + transformedSegment + ", transformedSecond=" + transformedSecond);

            assertBooleanProperty(GEOMETRY_SEED, iteration, common, "composite/primitive",
                    GeometryIntersections.intersects((Solid3d) firstComposite, second),
                    GeometryIntersections.intersects((Solid3d) transformedFirstComposite, transformedSecond),
                    "firstComposite=" + firstComposite + ", second=" + second,
                    "transformedComposite=" + transformedFirstComposite + ", transformedSecond=" + transformedSecond);

            boolean transformedCompositeActual = GeometryIntersections.intersects(
                    (Solid3d) transformedFirstComposite,
                    (Solid3d) transformedSecondComposite);
            assertBooleanProperty(GEOMETRY_SEED, iteration, common, "composite/composite",
                    GeometryIntersections.intersects((Solid3d) firstComposite, (Solid3d) secondComposite),
                    transformedCompositeActual,
                    "firstComposite=" + firstComposite + ", secondComposite=" + secondComposite,
                    "transformedFirst=" + transformedFirstComposite + ", transformedSecond=" + transformedSecondComposite);
            boolean transformedCompositeOracle = Phase1CTestSupport.intersectsCompositeCompositeOracle(
                    transformedFirstComposite,
                    transformedSecondComposite);
            assertEquals(
                    transformedCompositeOracle,
                    transformedCompositeActual,
                    failure(GEOMETRY_SEED, iteration, common, "transformed Cartesian oracle",
                            transformedCompositeOracle, transformedCompositeActual,
                            "transformedFirst=" + transformedFirstComposite,
                            "transformedSecond=" + transformedSecondComposite));

            assertBooleanProperty(GEOMETRY_SEED, iteration, common, "segment/composite",
                    GeometryIntersections.intersects(segment, (Solid3d) firstComposite),
                    GeometryIntersections.intersects(transformedSegment, (Solid3d) transformedFirstComposite),
                    "segment=" + segment + ", composite=" + firstComposite,
                    "transformedSegment=" + transformedSegment + ", transformedComposite=" + transformedFirstComposite);

            Vec3d point = center.add(new Vec3d(0.0D, 3.0D, 4.0D));
            Vec3d transformedPoint = common.transformPoint(point);
            double expectedPointSegment = GeometryDistances.pointToSegmentSquared(point, segment);
            double actualPointSegment = GeometryDistances.pointToSegmentSquared(transformedPoint, transformedSegment);
            Phase1DTestSupport.assertClose(expectedPointSegment, actualPointSegment,
                    Math.max(Math.abs(expectedPointSegment), Math.abs(actualPointSegment)),
                    failure(GEOMETRY_SEED, iteration, common, "point/segment squared distance",
                            expectedPointSegment, actualPointSegment,
                            "point=" + point + ", segment=" + segment,
                            "transformedPoint=" + transformedPoint + ", transformedSegment=" + transformedSegment));
        }
    }

    private static void assertVectorProperty(
            long seed,
            int iteration,
            RigidTransform3d transform,
            Vec3d input,
            String operation,
            Vec3d expected,
            Vec3d actual,
            String intermediate
    ) {
        Phase1DTestSupport.assertVectorClose(expected, actual,
                failure(seed, iteration, transform, operation, expected, actual,
                        "input=" + input, intermediate));
    }

    private static void assertBooleanProperty(
            long seed,
            int iteration,
            RigidTransform3d transform,
            String operation,
            boolean expected,
            boolean actual,
            String input,
            String intermediate
    ) {
        assertEquals(expected, actual, failure(seed, iteration, transform, operation, expected, actual, input, intermediate));
    }

    private static String failure(
            long seed,
            int iteration,
            RigidTransform3d transform,
            String operation,
            Object expected,
            Object actual,
            String input,
            String intermediate
    ) {
        return "seed=" + seed
                + ", iteration=" + iteration
                + ", transform=" + transform
                + ", operation=" + operation
                + ", " + input
                + ", expected=" + expected
                + ", actual=" + actual
                + ", sequentialIntermediate={" + intermediate + "}"
                + ", tolerancePolicy=scale-only(max(256 ULP, scale * 8 * ulp(1.0))), no absolute floor";
    }

    private static RigidTransform3d transform(SplittableRandom random) {
        return new RigidTransform3d(rotation(random), vector(random, -10.0D, 10.0D));
    }

    private static Rotation3d rotation(SplittableRandom random) {
        return new Rotation3d(
                random.nextDouble(-1.0D, 1.0D),
                random.nextDouble(-1.0D, 1.0D),
                random.nextDouble(-1.0D, 1.0D),
                1.0D + random.nextDouble(-0.5D, 0.5D));
    }

    private static Vec3d vector(SplittableRandom random, double minimum, double maximum) {
        return new Vec3d(
                random.nextDouble(minimum, maximum),
                random.nextDouble(minimum, maximum),
                random.nextDouble(minimum, maximum));
    }

    private static Vec3d dyadicCenter(SplittableRandom random) {
        return new Vec3d(
                random.nextInt(-16, 17) * 0.5D,
                random.nextInt(-16, 17) * 0.5D,
                random.nextInt(-16, 17) * 0.5D);
    }

    private static Solid3d primitiveAt(SplittableRandom random, int kind, Vec3d center) {
        return switch (kind) {
            case 0 -> new Aabb(
                    center.subtract(new Vec3d(1.0D, 1.5D, 0.5D)),
                    center.add(new Vec3d(1.0D, 1.5D, 0.5D)));
            case 1 -> new Sphere(center, 1.25D);
            case 2 -> new Obb(center, new Vec3d(1.0D, 1.5D, 0.75D), rotation(random));
            case 3 -> new Capsule(new Segment3d(
                    center.add(new Vec3d(-1.0D, 0.0D, 0.0D)),
                    center.add(new Vec3d(1.0D, 0.0D, 0.0D))), 0.75D);
            default -> throw new AssertionError("unknown primitive kind " + kind);
        };
    }
}
