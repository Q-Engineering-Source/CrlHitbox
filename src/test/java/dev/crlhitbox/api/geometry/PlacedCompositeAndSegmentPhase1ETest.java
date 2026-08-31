package dev.crlhitbox.api.geometry;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedCompositeAndSegmentPhase1ETest {
    @Test
    void identitySegmentQueriesAgreeWithEveryPrimitiveTypedKernelAndReverseOverload() {
        Segment3d segment = new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D));
        for (Solid3d local : primitiveLeaves()) {
            PlacedSolid3d placed = new PlacedSolid3d(local, RigidTransform3d.identity());
            boolean expected = Phase1CTestSupport.intersectsSegmentPrimitiveTyped(segment, local);
            assertEquals(expected, GeometryIntersections.intersects(segment, placed), local.toString());
            assertEquals(expected, GeometryIntersections.intersects(placed, segment), local.toString() + ", reverse");
        }
    }

    @Test
    void segmentPlacedQueryRejectsNullArguments() {
        Segment3d segment = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        PlacedSolid3d placed = new PlacedSolid3d(primitiveLeaves().getFirst(), RigidTransform3d.identity());
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((Segment3d) null, placed));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects(segment, (PlacedSolid3d) null));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((PlacedSolid3d) null, segment));
    }

    @Test
    void segmentCrossingTouchingParallelOutsideAndZeroLengthPlacedAabb() {
        Aabb local = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(5.0D, -3.0D, 2.0D));
        PlacedSolid3d placed = new PlacedSolid3d(local, placement);

        assertTrue(GeometryIntersections.intersects(parentSegment(placement, -2.0D, 0.0D, 2.0D, 0.0D), placed));
        assertTrue(GeometryIntersections.intersects(parentSegment(placement, -2.0D, 1.0D, 2.0D, 1.0D), placed));
        assertTrue(GeometryIntersections.intersects(parentSegment3d(placement, new Vec3d(-2.0D, 1.0D, 1.0D), new Vec3d(2.0D, 1.0D, 1.0D)), placed));
        assertFalse(GeometryIntersections.intersects(parentSegment(placement, -2.0D, 2.0D, 2.0D, 2.0D), placed));
        Vec3d inside = placement.transformPoint(new Vec3d(0.0D, 0.0D, 0.0D));
        Vec3d corner = placement.transformPoint(new Vec3d(1.0D, 1.0D, 1.0D));
        Vec3d outside = placement.transformPoint(new Vec3d(2.0D, 0.0D, 0.0D));
        assertTrue(GeometryIntersections.intersects(new Segment3d(inside, inside), placed));
        assertTrue(GeometryIntersections.intersects(new Segment3d(corner, corner), placed));
        assertFalse(GeometryIntersections.intersects(new Segment3d(outside, outside), placed));
    }

    @Test
    void segmentPlacedSphereAndCapsulePreserveZeroRadiusAndTouching() {
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(5.0D, -3.0D, 2.0D));
        Sphere pointSphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Capsule zeroRadius = new Capsule(
                new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)),
                0.0D);
        Vec3d parentOrigin = placement.transformPoint(new Vec3d(0.0D, 0.0D, 0.0D));
        Segment3d point = new Segment3d(parentOrigin, parentOrigin);

        assertTrue(GeometryIntersections.intersects(point, new PlacedSolid3d(pointSphere, placement)));
        assertTrue(GeometryIntersections.intersects(point, new PlacedSolid3d(zeroRadius, placement)));
    }

    @Test
    void placedCompositeFindsFirstMiddleAndFinalLeafHits() {
        Solid3d hit = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Solid3d left = pointAabb(-10.0D, 0.0D);
        Solid3d right = pointAabb(10.0D, 0.0D);
        PlacedSolid3d query = new PlacedSolid3d(pointAabb(0.0D, 0.0D), placement());
        for (Composite composite : List.of(
                new Composite(List.of(hit, left, right)),
                new Composite(List.of(left, hit, right)),
                new Composite(List.of(left, right, hit)))) {
            assertTrue(GeometryIntersections.intersects(new PlacedSolid3d(composite, placement()), query));
        }
    }

    @Test
    void placedCompositeCartesianProductFindsUniqueNonInitialPair() {
        Composite first = new Composite(List.of(pointAabb(-10.0D, 0.0D), pointAabb(0.0D, 0.0D), pointAabb(10.0D, 0.0D)));
        Composite second = new Composite(List.of(pointAabb(30.0D, 0.0D), pointAabb(10.0D, 0.0D), pointAabb(40.0D, 0.0D)));

        assertTrue(GeometryIntersections.intersects(
                new PlacedSolid3d(first, placement()),
                new PlacedSolid3d(second, placement())));
    }

    @Test
    void placedCompositeRootBoundsOverlapButEveryLeafPairMisses() {
        Composite first = new Composite(List.of(pointAabb(-2.0D, 0.0D), pointAabb(2.0D, 0.0D)));
        Composite second = new Composite(List.of(pointAabb(-1.0D, 0.0D), pointAabb(1.0D, 0.0D)));
        PlacedSolid3d placedFirst = new PlacedSolid3d(first, placement());
        PlacedSolid3d placedSecond = new PlacedSolid3d(second, placement());

        assertTrue(GeometryIntersections.intersects(placedFirst.bounds(), placedSecond.bounds()));
        assertFalse(GeometryIntersections.intersects(placedFirst, placedSecond));
    }

    @Test
    void segmentPlacedCompositeFindsFirstMiddleFinalAndRejectsGap() {
        Solid3d hit = pointAabb(0.0D, 0.0D);
        Solid3d left = pointAabb(-10.0D, 0.0D);
        Solid3d right = pointAabb(10.0D, 0.0D);
        RigidTransform3d placement = placement();
        Vec3d parentOrigin = placement.transformPoint(new Vec3d(0.0D, 0.0D, 0.0D));
        Segment3d point = new Segment3d(parentOrigin, parentOrigin);
        for (Composite composite : List.of(
                new Composite(List.of(hit, left, right)),
                new Composite(List.of(left, hit, right)),
                new Composite(List.of(left, right, hit)))) {
            PlacedSolid3d placed = new PlacedSolid3d(composite, placement);
            assertTrue(GeometryIntersections.intersects(point, placed));
            assertTrue(GeometryIntersections.intersects(placed, point));
        }
        Composite spanningMiss = new Composite(List.of(pointAabb(-2.0D, 0.0D), pointAabb(2.0D, 1.0D)));
        assertFalse(GeometryIntersections.intersects(point, new PlacedSolid3d(spanningMiss, placement)));
    }

    @Test
    void duplicateAndPermutationPreservePlacedCompositeQueriesAndBounds() {
        Solid3d a = pointAabb(-4.0D, 0.0D);
        Solid3d b = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Solid3d c = pointAabb(6.0D, 0.0D);
        PlacedSolid3d query = new PlacedSolid3d(pointAabb(0.0D, 0.0D), placement());
        PlacedSolid3d ordered = new PlacedSolid3d(new Composite(List.of(a, b, c)), placement());
        PlacedSolid3d permuted = new PlacedSolid3d(new Composite(List.of(c, a, b)), placement());
        PlacedSolid3d duplicated = new PlacedSolid3d(new Composite(List.of(a, b, b, c)), placement());

        boolean expected = GeometryIntersections.intersects(ordered, query);
        assertEquals(expected, GeometryIntersections.intersects(permuted, query));
        assertEquals(expected, GeometryIntersections.intersects(duplicated, query));
        assertEquals(ordered.bounds(), permuted.bounds());
        assertEquals(ordered.bounds(), duplicated.bounds());
    }

    @Test
    void widePlacedCompositeQueriesWithoutRecursiveHierarchy() {
        List<Solid3d> leaves = new ArrayList<>(8192);
        for (int index = 0; index < 8192; index++) leaves.add(pointAabb(index, 0.0D));
        PlacedSolid3d wide = new PlacedSolid3d(new Composite(leaves), placement());
        for (double x : new double[] {0.0D, 4096.0D, 8191.0D}) {
            PlacedSolid3d query = new PlacedSolid3d(pointAabb(x, 0.0D), placement());
            assertTrue(GeometryIntersections.intersects(wide, query), "x=" + x);
        }
        assertFalse(GeometryIntersections.intersects(
                wide,
                new PlacedSolid3d(pointAabb(4096.5D, 0.0D), placement())));
    }

    private static List<Solid3d> primitiveLeaves() {
        return List.of(
                new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity()),
                new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 0.5D));
    }

    private static Segment3d parentSegment(
            RigidTransform3d placement,
            double firstX,
            double firstY,
            double secondX,
            double secondY
    ) {
        return new Segment3d(
                placement.transformPoint(new Vec3d(firstX, firstY, 0.0D)),
                placement.transformPoint(new Vec3d(secondX, secondY, 0.0D)));
    }

    private static Segment3d parentSegment3d(RigidTransform3d placement, Vec3d first, Vec3d second) {
        return new Segment3d(placement.transformPoint(first), placement.transformPoint(second));
    }

    private static Aabb pointAabb(double x, double y) {
        Vec3d point = new Vec3d(x, y, 0.0D);
        return new Aabb(point, point);
    }

    private static RigidTransform3d placement() {
        return new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(5.0D, -3.0D, 2.0D));
    }
}
