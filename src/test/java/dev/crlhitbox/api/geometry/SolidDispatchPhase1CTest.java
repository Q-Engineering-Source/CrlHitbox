package dev.crlhitbox.api.geometry;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolidDispatchPhase1CTest {
    @Test
    void genericPrimitiveDispatchMatchesAll16OrderedKernelsForHitsAndMisses() {
        Solid3d[] near = primitivesAt(0.0D);
        Solid3d[] far = primitivesAt(20.0D);
        for (Solid3d first : near) {
            for (int kind = 0; kind < near.length; kind++) {
                assertGenericAgreement(first, near[kind], "overlap kind " + kind);
                assertGenericAgreement(first, far[kind], "separated kind " + kind);
            }
        }
    }

    @Test
    void genericPrimitiveDispatchPreservesClosedTangencyAndDegeneracy() {
        Aabb unit = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Solid3d[] touching = {
                new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D),
                new Obb(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D), Rotation3d.identity()),
                new Capsule(new Segment3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D)), 1.0D)
        };
        for (Solid3d second : touching) {
            assertTrue(GeometryIntersections.intersects((Solid3d) unit, second));
            assertTrue(GeometryIntersections.intersects(second, (Solid3d) unit));
        }

        Solid3d[] points = {
                pointBox(0.0D),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D),
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity()),
                new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D)), 0.0D)
        };
        for (Solid3d first : points) for (Solid3d second : points) assertGenericAgreement(first, second, "point degeneracy");
    }

    @Test
    void compositePrimitiveFindsFirstMiddleAndFinalLeafHits() {
        Solid3d hit = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Solid3d missLeft = pointBox(-10.0D);
        Solid3d missRight = pointBox(10.0D);
        Solid3d query = pointBox(0.0D);
        for (Composite composite : List.of(
                new Composite(List.of(hit, missLeft, missRight)),
                new Composite(List.of(missLeft, hit, missRight)),
                new Composite(List.of(missLeft, missRight, hit)))) {
            assertEquals(Phase1CTestSupport.intersectsSolidOracle(composite, query), GeometryIntersections.intersects((Solid3d) composite, query));
            assertEquals(Phase1CTestSupport.intersectsSolidOracle(query, composite), GeometryIntersections.intersects(query, (Solid3d) composite));
        }
    }

    @Test
    void compositePrimitiveRejectsDisjointRootsAndOverlappingRootFalsePositives() {
        Composite spanningMiss = new Composite(List.of(pointBox(-2.0D), pointBox(2.0D)));
        Solid3d gapPoint = pointBox(0.0D);
        Composite disjoint = new Composite(List.of(pointBox(20.0D), pointBox(30.0D)));

        assertFalse(GeometryIntersections.intersects((Solid3d) spanningMiss, gapPoint), "root bounds overlap is not a hit");
        assertFalse(GeometryIntersections.intersects(gapPoint, (Solid3d) spanningMiss), "reverse root bounds overlap is not a hit");
        assertFalse(GeometryIntersections.intersects((Solid3d) disjoint, gapPoint), "disjoint root bounds reject");
    }

    @Test
    void compositePrimitivePreservesTouchingContainmentDuplicatesAndReverseOrder() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Sphere tangent = new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D);
        Composite composite = new Composite(List.of(pointBox(-20.0D), box, box));

        assertTrue(GeometryIntersections.intersects((Solid3d) composite, (Solid3d) tangent));
        assertTrue(GeometryIntersections.intersects((Solid3d) tangent, (Solid3d) composite));
        assertEquals(
                Phase1CTestSupport.intersectsCompositePrimitiveOracle(composite, tangent),
                GeometryIntersections.intersects((Solid3d) composite, (Solid3d) tangent));
    }

    @Test
    void compositeCompositeFindsAUniqueNonInitialLeafPair() {
        Composite first = new Composite(List.of(pointBox(-10.0D), pointBox(0.0D), pointBox(10.0D)));
        Composite second = new Composite(List.of(pointBox(30.0D), pointBox(10.0D), pointBox(40.0D)));
        boolean expected = Phase1CTestSupport.intersectsCompositeCompositeOracle(first, second);

        assertTrue(expected);
        assertEquals(expected, GeometryIntersections.intersects((Solid3d) first, (Solid3d) second));
        assertEquals(expected, GeometryIntersections.intersects((Solid3d) second, (Solid3d) first));
    }

    @Test
    void compositeCompositeNeverPromotesOverlappingRootBoundsToIntersection() {
        Composite first = new Composite(List.of(pointBox(-2.0D), pointBox(2.0D)));
        Composite second = new Composite(List.of(pointBox(-1.0D), pointBox(1.0D)));

        assertTrue(GeometryIntersections.intersects(first.bounds(), second.bounds()), "test requires overlapping roots");
        assertFalse(Phase1CTestSupport.intersectsCompositeCompositeOracle(first, second));
        assertFalse(GeometryIntersections.intersects((Solid3d) first, (Solid3d) second));
    }

    @Test
    void childPermutationAndDuplicateInsertionPreserveGeometricQueries() {
        Solid3d a = pointBox(-4.0D);
        Solid3d b = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Solid3d c = pointBox(6.0D);
        Solid3d query = pointBox(0.0D);
        Composite ordered = new Composite(List.of(a, b, c));
        Composite permuted = new Composite(List.of(c, a, b));
        Composite duplicated = new Composite(List.of(a, b, b, c));

        boolean expected = GeometryIntersections.intersects((Solid3d) ordered, query);
        assertEquals(expected, GeometryIntersections.intersects((Solid3d) permuted, query));
        assertEquals(expected, GeometryIntersections.intersects((Solid3d) duplicated, query));
    }

    @Test
    void oneChildCompositeIsQueryEquivalentToEveryPrimitiveKind() {
        Solid3d query = new Sphere(new Vec3d(0.5D, 0.0D, 0.0D), 0.75D);
        for (Solid3d leaf : primitivesAt(0.0D)) {
            Composite composite = new Composite(List.of(leaf));
            assertEquals(Phase1CTestSupport.intersectsPrimitiveTyped(leaf, query), GeometryIntersections.intersects((Solid3d) composite, query));
            assertEquals(Phase1CTestSupport.intersectsPrimitiveTyped(query, leaf), GeometryIntersections.intersects(query, (Solid3d) composite));
        }
    }

    @Test
    void genericSolidDispatchRejectsNullArguments() {
        Solid3d solid = pointBox(0.0D);
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((Solid3d) null, solid));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects(solid, (Solid3d) null));
    }

    @Test
    void genericSegmentDispatchMatchesEveryTypedPrimitiveKernelInBothOrders() {
        Segment3d hit = new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D));
        Segment3d miss = new Segment3d(new Vec3d(40.0D, 0.0D, 0.0D), new Vec3d(42.0D, 0.0D, 0.0D));
        for (Solid3d solid : primitivesAt(0.0D)) {
            for (Segment3d segment : List.of(hit, miss)) {
                boolean expected = Phase1CTestSupport.intersectsSegmentPrimitiveTyped(segment, solid);
                assertEquals(expected, GeometryIntersections.intersects(segment, solid));
                assertEquals(expected, GeometryIntersections.intersects(solid, segment));
            }
        }
    }

    @Test
    void segmentCompositeFindsFirstMiddleAndFinalLeafHits() {
        Solid3d hit = pointBox(0.0D);
        Solid3d missLeft = pointBox(-10.0D);
        Solid3d missRight = pointBox(10.0D);
        Segment3d segment = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        for (Composite composite : List.of(
                new Composite(List.of(hit, missLeft, missRight)),
                new Composite(List.of(missLeft, hit, missRight)),
                new Composite(List.of(missLeft, missRight, hit)))) {
            boolean expected = Phase1CTestSupport.intersectsSegmentCompositeOracle(segment, composite);
            assertTrue(expected);
            assertEquals(expected, GeometryIntersections.intersects(segment, (Solid3d) composite));
            assertEquals(expected, GeometryIntersections.intersects((Solid3d) composite, segment));
        }
    }

    @Test
    void segmentCompositeRejectsDisjointRootsAndOverlappingRootFalsePositives() {
        Segment3d gapPoint = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Composite spanningMiss = new Composite(List.of(
                pointBox(-2.0D),
                new Aabb(new Vec3d(2.0D, 1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 0.0D))));
        Composite disjoint = new Composite(List.of(pointBox(20.0D), pointBox(30.0D)));

        assertTrue(GeometryIntersections.intersects(gapPoint, spanningMiss.bounds()), "test requires segment/root overlap");
        assertFalse(GeometryIntersections.intersects(gapPoint, (Solid3d) spanningMiss));
        assertFalse(GeometryIntersections.intersects((Solid3d) spanningMiss, gapPoint));
        assertFalse(GeometryIntersections.intersects(gapPoint, (Solid3d) disjoint));
    }

    @Test
    void segmentCompositePreservesTouchingZeroLengthAndRadiusZeroBehavior() {
        Aabb unit = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Capsule zeroRadius = new Capsule(new Segment3d(new Vec3d(3.0D, 0.0D, 0.0D), new Vec3d(5.0D, 0.0D, 0.0D)), 0.0D);
        Composite composite = new Composite(List.of(pointBox(-20.0D), unit, zeroRadius));
        Segment3d boundaryPoint = new Segment3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Segment3d endpointTouch = new Segment3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));

        assertTrue(GeometryIntersections.intersects(boundaryPoint, (Solid3d) composite));
        assertTrue(GeometryIntersections.intersects(endpointTouch, (Solid3d) composite));
        assertEquals(
                Phase1CTestSupport.intersectsSegmentCompositeOracle(endpointTouch, composite),
                GeometryIntersections.intersects((Solid3d) composite, endpointTouch));
    }

    @Test
    void deepAndWideCompositeSegmentQueriesRemainIterative() {
        Sphere leaf = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Composite deep = new Composite(List.of(leaf));
        for (int round = 0; round < 4096; round++) deep = new Composite(List.of(deep));
        Segment3d origin = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Solid3d originSolid = pointBox(0.0D);
        assertTrue(GeometryIntersections.intersects((Solid3d) deep, originSolid));
        assertTrue(GeometryIntersections.intersects(originSolid, (Solid3d) deep));
        assertTrue(GeometryIntersections.intersects(origin, (Solid3d) deep));
        assertTrue(GeometryIntersections.intersects((Solid3d) deep, origin));

        List<Solid3d> leaves = new java.util.ArrayList<>(8192);
        for (int index = 0; index < 8192; index++) leaves.add(pointBox(index));
        Composite wide = new Composite(leaves);
        for (double x : new double[] {0.0D, 4096.0D, 8191.0D}) {
            Segment3d query = new Segment3d(new Vec3d(x, 0.0D, 0.0D), new Vec3d(x, 0.0D, 0.0D));
            assertTrue(GeometryIntersections.intersects((Solid3d) wide, (Solid3d) pointBox(x)), "wide solid hit x=" + x);
            assertTrue(GeometryIntersections.intersects(query, (Solid3d) wide), "wide hit x=" + x);
        }
        Segment3d miss = new Segment3d(new Vec3d(4096.5D, 0.0D, 0.0D), new Vec3d(4096.5D, 0.0D, 0.0D));
        assertFalse(GeometryIntersections.intersects((Solid3d) wide, (Solid3d) pointBox(4096.5D)));
        assertFalse(GeometryIntersections.intersects(miss, (Solid3d) wide));
    }

    @Test
    void genericSegmentDispatchRejectsNullArguments() {
        Segment3d segment = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Solid3d solid = pointBox(0.0D);
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((Segment3d) null, solid));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects(segment, (Solid3d) null));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((Solid3d) null, segment));
    }

    private static void assertGenericAgreement(Solid3d first, Solid3d second, String context) {
        String firstValue = first.toString();
        String secondValue = second.toString();
        Aabb firstBounds = first.bounds();
        Aabb secondBounds = second.bounds();
        boolean expected = Phase1CTestSupport.intersectsPrimitiveTyped(first, second);
        assertEquals(expected, GeometryIntersections.intersects(first, second), context + ", first=" + first + ", second=" + second);
        assertEquals(firstValue, first.toString(), context + ", first value mutation");
        assertEquals(secondValue, second.toString(), context + ", second value mutation");
        assertEquals(firstBounds, first.bounds(), context + ", first bounds mutation");
        assertEquals(secondBounds, second.bounds(), context + ", second bounds mutation");
    }

    private static Solid3d[] primitivesAt(double x) {
        Vec3d center = new Vec3d(x, 0.0D, 0.0D);
        return new Solid3d[] {
                new Aabb(new Vec3d(x - 1.0D, -1.0D, -1.0D), new Vec3d(x + 1.0D, 1.0D, 1.0D)),
                new Sphere(center, 1.0D),
                new Obb(center, new Vec3d(1.0D, 1.0D, 1.0D), new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D)),
                new Capsule(new Segment3d(new Vec3d(x - 1.0D, 0.0D, 0.0D), new Vec3d(x + 1.0D, 0.0D, 0.0D)), 0.5D)
        };
    }

    private static Aabb pointBox(double x) {
        Vec3d point = new Vec3d(x, 0.0D, 0.0D);
        return new Aabb(point, point);
    }
}
