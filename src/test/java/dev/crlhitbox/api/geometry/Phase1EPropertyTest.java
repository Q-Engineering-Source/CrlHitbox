package dev.crlhitbox.api.geometry;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fixed-seed Phase 1E properties; expected values never call placed production dispatch. */
class Phase1EPropertyTest {
    private static final long IDENTITY_AND_COMMON_SEED = 0x5EED_1E01L;
    private static final long BOX_ORACLE_SEED = 0x5EED_1E02L;
    private static final long COMPOSITE_SEED = 0x5EED_1E03L;
    private static final int ITERATIONS = 2048;

    @Test
    void identityCommonPlacementAndTypedKernelAgreementUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(IDENTITY_AND_COMMON_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Solid3d first = primitive(random, iteration & 3, new Vec3d(0.0D, 0.0D, 0.0D));
            Solid3d second = primitive(random, iteration >>> 2 & 3,
                    new Vec3d((iteration & 1) == 0 ? 0.25D : 8.0D, 0.5D, -0.25D));
            Segment3d segment = new Segment3d(new Vec3d(-4.0D, 0.25D, 0.0D), new Vec3d(4.0D, 0.25D, 0.0D));
            RigidTransform3d common = transform(random);
            PlacedSolid3d identityFirst = new PlacedSolid3d(first, RigidTransform3d.identity());
            PlacedSolid3d identitySecond = new PlacedSolid3d(second, RigidTransform3d.identity());
            PlacedSolid3d commonFirst = new PlacedSolid3d(first, common);
            PlacedSolid3d commonSecond = new PlacedSolid3d(second, common);
            boolean expected = Phase1CTestSupport.intersectsPrimitiveTyped(first, second);
            boolean expectedSegment = Phase1CTestSupport.intersectsSegmentPrimitiveTyped(segment, first);

            assertEquals(first.bounds(), identityFirst.bounds(), context(IDENTITY_AND_COMMON_SEED, iteration, "identity bounds", first, common, second, null, first.bounds(), identityFirst.bounds(), null));
            assertEquals(second.bounds(), identitySecond.bounds(), context(IDENTITY_AND_COMMON_SEED, iteration, "identity bounds second", second, common, first, null, second.bounds(), identitySecond.bounds(), null));
            assertBoolean(expected, GeometryIntersections.intersects(identityFirst, identitySecond), IDENTITY_AND_COMMON_SEED, iteration,
                    "identity placed primitive agreement", first, common, second, null, identityFirst.bounds() + "; " + identitySecond.bounds());
            assertBoolean(expected, GeometryIntersections.intersects(commonFirst, commonSecond), IDENTITY_AND_COMMON_SEED, iteration,
                    "common rigid placement invariance", first, common, second, null, commonFirst.bounds() + "; " + commonSecond.bounds());
            Segment3d parentSegment = Phase1ETestSupport.parentSegment(segment, common);
            assertBoolean(expectedSegment, GeometryIntersections.intersects(segment, identityFirst), IDENTITY_AND_COMMON_SEED, iteration,
                    "identity segment agreement", first, RigidTransform3d.identity(), null, segment, identityFirst.bounds().toString());
            assertBoolean(expectedSegment, GeometryIntersections.intersects(parentSegment, commonFirst), IDENTITY_AND_COMMON_SEED, iteration,
                    "common rigid segment invariance", first, common, null, parentSegment, commonFirst.bounds().toString());
            assertBoolean(GeometryIntersections.intersects(parentSegment, commonFirst), GeometryIntersections.intersects(commonFirst, parentSegment),
                    IDENTITY_AND_COMMON_SEED, iteration, "placed segment reverse overload", first, common, null, parentSegment, commonFirst.bounds().toString());
        }
    }

    @Test
    void rigidIntervalBoxSatBoundsSegmentSphereAndCapsuleOraclesUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(BOX_ORACLE_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Solid3d first = box(random, iteration & 1, new Vec3d(0.0D, 0.0D, 0.0D));
            Solid3d second = box(random, iteration >>> 1 & 1, new Vec3d((iteration & 1) == 0 ? 0.5D : 6.0D, 0.25D, -0.5D));
            RigidTransform3d firstTransform = transform(random);
            RigidTransform3d secondTransform = transform(random);
            PlacedSolid3d placedFirst = new PlacedSolid3d(first, firstTransform);
            PlacedSolid3d placedSecond = new PlacedSolid3d(second, secondTransform);
            Phase1ETestSupport.BoxOracle firstBox = Phase1ETestSupport.boxOracle(first, firstTransform);
            Phase1ETestSupport.BoxOracle secondBox = Phase1ETestSupport.boxOracle(second, secondTransform);
            boolean boxExpected = Phase1ETestSupport.intersectsBoxes(firstBox, secondBox);
            Phase1ETestSupport.assertBoundsContain(firstBox, placedFirst.bounds(), context(BOX_ORACLE_SEED, iteration, "first rigid-box corner containment", first, firstTransform, second, null, null, placedFirst.bounds(), "corner oracle"));
            Phase1ETestSupport.assertBoundsContain(secondBox, placedSecond.bounds(), context(BOX_ORACLE_SEED, iteration, "second rigid-box corner containment", second, secondTransform, first, null, null, placedSecond.bounds(), "corner oracle"));
            assertBoolean(boxExpected, GeometryIntersections.intersects(placedFirst, placedSecond), BOX_ORACLE_SEED, iteration,
                    "15-axis rigid interval box SAT", first, firstTransform, second, null, placedFirst.bounds() + "; " + placedSecond.bounds());

            Segment3d localSegment = new Segment3d(new Vec3d(-3.0D, -0.25D, 0.25D), new Vec3d(3.0D, 0.75D, -0.25D));
            Segment3d parentSegment = Phase1ETestSupport.parentSegment(localSegment, firstTransform);
            boolean segmentExpected = Phase1ETestSupport.segmentIntersectsBox(parentSegment, firstBox);
            assertBoolean(segmentExpected, GeometryIntersections.intersects(parentSegment, placedFirst), BOX_ORACLE_SEED, iteration,
                    "parent segment to rigid box slab oracle", first, firstTransform, null, parentSegment, placedFirst.bounds().toString());

            Sphere sphere = new Sphere(new Vec3d((iteration & 1) == 0 ? 0.25D : 4.0D, 0.0D, 0.0D), 0.75D);
            RigidTransform3d sphereTransform = transform(random);
            boolean sphereExpected = Phase1ETestSupport.sphereIntersectsBox(sphere, sphereTransform, firstBox);
            assertBoolean(sphereExpected, GeometryIntersections.intersects(new PlacedSolid3d(sphere, sphereTransform), placedFirst), BOX_ORACLE_SEED, iteration,
                    "sphere to rigid box local point-distance oracle", sphere, sphereTransform, first, null, placedFirst.bounds().toString());

            Capsule capsule = new Capsule(new Segment3d(new Vec3d(-2.0D, 0.5D, 0.0D), new Vec3d(2.0D, 0.5D, 0.0D)), 0.5D);
            RigidTransform3d capsuleTransform = transform(random);
            boolean capsuleExpected = Phase1ETestSupport.capsuleIntersectsBox(capsule, capsuleTransform, firstBox);
            assertBoolean(capsuleExpected, GeometryIntersections.intersects(placedFirst, new PlacedSolid3d(capsule, capsuleTransform)), BOX_ORACLE_SEED, iteration,
                    "capsule to rigid box derivative-bisection oracle", first, firstTransform, capsule, null, placedFirst.bounds().toString());
        }
    }

    @Test
    void compositeSegmentDuplicatePermutationAndStructuralPropertiesUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(COMPOSITE_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Solid3d left = primitive(random, iteration & 3, new Vec3d(-3.0D, 0.0D, 0.0D));
            Solid3d middle = primitive(random, iteration >>> 2 & 3, new Vec3d(0.0D, 0.0D, 0.0D));
            Solid3d right = primitive(random, iteration >>> 4 & 3, new Vec3d(3.0D, 0.0D, 0.0D));
            Composite ordered = new Composite(List.of(left, middle, right));
            Composite duplicated = new Composite(List.of(left, middle, middle, right));
            Composite permuted = new Composite(List.of(right, left, middle));
            Solid3d other = primitive(random, iteration >>> 6 & 3, new Vec3d((iteration & 1) == 0 ? 0.5D : 12.0D, 0.0D, 0.0D));
            RigidTransform3d placement = transform(random);
            RigidTransform3d otherPlacement = transform(random);
            PlacedSolid3d placedOrdered = new PlacedSolid3d(ordered, placement);
            PlacedSolid3d placedDuplicated = new PlacedSolid3d(duplicated, placement);
            PlacedSolid3d placedPermuted = new PlacedSolid3d(permuted, placement);
            PlacedSolid3d placedOther = new PlacedSolid3d(other, otherPlacement);
            boolean expected = Phase1ETestSupport.intersectsPlacedSolid(ordered, placement, other, otherPlacement);
            boolean cartesianExpected = Phase1ETestSupport.intersectsPlacedSolid(ordered, placement, permuted, placement);
            Segment3d parentSegment = Phase1ETestSupport.parentSegment(
                    new Segment3d(new Vec3d(-5.0D, 0.0D, 0.0D), new Vec3d(5.0D, 0.0D, 0.0D)), placement);
            boolean segmentExpected = Phase1ETestSupport.intersectsSegmentPlaced(parentSegment, ordered, placement);

            assertBoolean(expected, GeometryIntersections.intersects(placedOrdered, placedOther), COMPOSITE_SEED, iteration,
                    "composite leaf-union oracle", ordered, placement, other, null, placedOrdered.bounds() + "; " + placedOther.bounds());
            assertBoolean(cartesianExpected, GeometryIntersections.intersects(placedOrdered, placedPermuted), COMPOSITE_SEED, iteration,
                    "composite Cartesian-product oracle", ordered, placement, permuted, null, placedOrdered.bounds() + "; " + placedPermuted.bounds());
            assertBoolean(segmentExpected, GeometryIntersections.intersects(parentSegment, placedOrdered), COMPOSITE_SEED, iteration,
                    "segment composite leaf-union oracle", ordered, placement, null, parentSegment, placedOrdered.bounds().toString());
            assertBoolean(GeometryIntersections.intersects(placedOrdered, placedOther), GeometryIntersections.intersects(placedDuplicated, placedOther),
                    COMPOSITE_SEED, iteration, "duplicate insertion query preservation", duplicated, placement, other, null, placedDuplicated.bounds().toString());
            assertBoolean(GeometryIntersections.intersects(placedOrdered, placedOther), GeometryIntersections.intersects(placedPermuted, placedOther),
                    COMPOSITE_SEED, iteration, "permutation query preservation", permuted, placement, other, null, placedPermuted.bounds().toString());
            assertEquals(placedOrdered.bounds(), placedDuplicated.bounds(), context(COMPOSITE_SEED, iteration, "duplicate bounds preservation", ordered, placement, duplicated, null, placedOrdered.bounds(), placedDuplicated.bounds(), null));
            assertEquals(placedOrdered.bounds(), placedPermuted.bounds(), context(COMPOSITE_SEED, iteration, "permutation bounds preservation", ordered, placement, permuted, null, placedOrdered.bounds(), placedPermuted.bounds(), null));
            assertEquals(placedOrdered, new PlacedSolid3d(ordered, placement), context(COMPOSITE_SEED, iteration, "exact structural placement equality", ordered, placement, null, null, placedOrdered, null, null));
        }
    }

    private static void assertBoolean(boolean expected, boolean actual, long seed, int iteration, String operation,
            Solid3d local, RigidTransform3d transform, Solid3d second, Segment3d segment, String bounds) {
        assertEquals(expected, actual, context(seed, iteration, operation, local, transform, second, segment, expected, actual, bounds));
    }

    private static String context(long seed, int iteration, String operation, Solid3d local, RigidTransform3d transform,
            Solid3d second, Segment3d segment, Object expected, Object actual, String intermediate) {
        return "seed=" + seed + ", iteration=" + iteration + ", operation=" + operation
                + ", localSolidOrCanonicalLeaves=" + local + ", localToParent=" + transform
                + ", secondPlacementOrSegment=" + (segment != null ? segment : second)
                + ", parentFrameBounds=" + intermediate + ", queryOrder=" + operation
                + ", expected=" + expected + ", actual=" + actual
                + ", relevantLocalParentIntermediate=" + intermediate
                + ", comparatorPolicy=exact booleans; vector comparisons are scale-aware max(256 ULP(scale), scale * 8 ulp(1.0)) with strict zero and no absolute floor";
    }

    private static RigidTransform3d transform(SplittableRandom random) {
        return new RigidTransform3d(new Rotation3d(
                random.nextDouble(-0.7D, 0.7D), random.nextDouble(-0.7D, 0.7D),
                random.nextDouble(-0.7D, 0.7D), 1.0D + random.nextDouble(-0.4D, 0.4D)),
                new Vec3d(random.nextDouble(-10.0D, 10.0D), random.nextDouble(-10.0D, 10.0D), random.nextDouble(-10.0D, 10.0D)));
    }

    private static Solid3d box(SplittableRandom random, int kind, Vec3d center) {
        return kind == 0
                ? new Aabb(center.subtract(new Vec3d(1.5D, 0.75D, 0.5D)), center.add(new Vec3d(1.5D, 0.75D, 0.5D)))
                : new Obb(center, new Vec3d(1.25D, 0.8D, 0.55D), new Rotation3d(
                        random.nextDouble(-0.5D, 0.5D), random.nextDouble(-0.5D, 0.5D), random.nextDouble(-0.5D, 0.5D), 1.0D));
    }

    private static Solid3d primitive(SplittableRandom random, int kind, Vec3d center) {
        return switch (kind) {
            case 0 -> new Aabb(center.subtract(new Vec3d(1.0D, 0.75D, 0.5D)), center.add(new Vec3d(1.0D, 0.75D, 0.5D)));
            case 1 -> new Sphere(center, 1.0D);
            case 2 -> new Obb(center, new Vec3d(1.0D, 0.75D, 0.5D), new Rotation3d(
                    random.nextDouble(-0.4D, 0.4D), random.nextDouble(-0.4D, 0.4D), random.nextDouble(-0.4D, 0.4D), 1.0D));
            case 3 -> new Capsule(new Segment3d(center.subtract(new Vec3d(0.75D, 0.0D, 0.0D)), center.add(new Vec3d(0.75D, 0.0D, 0.0D))), 0.4D);
            default -> throw new AssertionError("unknown primitive kind " + kind);
        };
    }
}
