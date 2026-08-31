package dev.crlhitbox.api.geometry;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedSolid3dPhase1ETest {
    @Test
    void constructorRetainsImmutableValuesRejectsNullsAndPreservesIdentityBounds() {
        Solid3d local = new Aabb(
                new Vec3d(1.0D, -2.0D, 3.0D),
                new Vec3d(Math.nextUp(1.0D), 4.0D, 5.0D));
        RigidTransform3d identity = RigidTransform3d.identity();
        PlacedSolid3d placed = new PlacedSolid3d(local, identity);

        assertSame(local, placed.localSolid());
        assertSame(identity, placed.localToParent());
        assertEquals(local.bounds(), placed.bounds());
        assertThrows(NullPointerException.class, () -> new PlacedSolid3d(null, identity));
        assertThrows(NullPointerException.class, () -> new PlacedSolid3d(local, null));
    }

    @Test
    void identityBoundsExactlyPreserveEverySolidKindAndComposite() {
        Solid3d[] solids = {
                new Aabb(new Vec3d(1.0D, -2.0D, 3.0D), new Vec3d(Math.nextUp(1.0D), 4.0D, 5.0D)),
                new Sphere(new Vec3d(2.0D, 3.0D, 4.0D), 1.5D),
                new Obb(new Vec3d(2.0D, 3.0D, 4.0D), new Vec3d(1.0D, 2.0D, 3.0D), new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D)),
                new Capsule(new Segment3d(new Vec3d(-2.0D, 0.0D, 1.0D), new Vec3d(3.0D, 4.0D, 5.0D)), 0.75D)
        };
        Composite composite = new Composite(List.of(solids));
        for (Solid3d solid : List.of(solids[0], solids[1], solids[2], solids[3], composite)) {
            assertEquals(solid.bounds(), new PlacedSolid3d(solid, RigidTransform3d.identity()).bounds(), solid.toString());
        }
    }

    @Test
    void placedAabbPureTranslationMapsStoredEndpointsExactly() {
        Aabb local = new Aabb(new Vec3d(-2.0D, -1.0D, 3.0D), new Vec3d(4.0D, 5.0D, 7.0D));
        RigidTransform3d placement = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(8.0D, -4.0D, 2.0D));

        assertEquals(
                new Aabb(new Vec3d(6.0D, -5.0D, 5.0D), new Vec3d(12.0D, 1.0D, 9.0D)),
                new PlacedSolid3d(local, placement).bounds());
    }

    @Test
    void placedAabbQuarterTurnHasExpectedParentExtrema() {
        Aabb local = new Aabb(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(4.0D, 5.0D, 6.0D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(10.0D, 20.0D, 30.0D));
        Aabb bounds = new PlacedSolid3d(local, placement).bounds();

        Phase1DTestSupport.assertVectorClose(new Vec3d(5.0D, 21.0D, 33.0D), bounds.min(), "z90 placed Aabb min");
        Phase1DTestSupport.assertVectorClose(new Vec3d(8.0D, 24.0D, 36.0D), bounds.max(), "z90 placed Aabb max");
    }

    @Test
    void placedAabbArbitraryRotationBoundsContainEveryDirectCornerWitness() {
        Aabb local = new Aabb(new Vec3d(-2.0D, -1.0D, -3.0D), new Vec3d(4.0D, 5.0D, 7.0D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        PlacedSolid3d placed = new PlacedSolid3d(local, placement);

        assertAabbCornerWitnessesContained(local, placement, placed.bounds());
        assertEquals(placed.bounds(), placed.bounds(), "eager deterministic bounds");
    }

    @Test
    void identityAabbBoundsPreserveAdjacentRepresentableEndpoints() {
        double minimum = 1.0D;
        double maximum = Math.nextUp(minimum);
        Aabb local = new Aabb(
                new Vec3d(minimum, -1.0D, -1.0D),
                new Vec3d(maximum, 1.0D, 1.0D));

        assertEquals(local, new PlacedSolid3d(local, RigidTransform3d.identity()).bounds());
    }

    @Test
    void identityAabbBoundsPreserveSubnormalWidth() {
        Aabb local = new Aabb(
                new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D));

        assertEquals(local, new PlacedSolid3d(local, RigidTransform3d.identity()).bounds());
    }

    @Test
    void identityAabbBoundsPreserveLargeSameSignEndpoints() {
        double minimum = Double.MAX_VALUE / 4.0D;
        double maximum = Math.nextUp(minimum);
        Aabb local = new Aabb(
                new Vec3d(minimum, minimum, minimum),
                new Vec3d(maximum, maximum, maximum));

        assertEquals(local, new PlacedSolid3d(local, RigidTransform3d.identity()).bounds());
    }

    @Test
    void identityAabbBoundsPreservePlaneLineAndPointDegeneracies() {
        Aabb plane = new Aabb(new Vec3d(-1.0D, -2.0D, 3.0D), new Vec3d(4.0D, 5.0D, 3.0D));
        Aabb line = new Aabb(new Vec3d(2.0D, -2.0D, 3.0D), new Vec3d(2.0D, 5.0D, 3.0D));
        Aabb point = new Aabb(new Vec3d(2.0D, 3.0D, 4.0D), new Vec3d(2.0D, 3.0D, 4.0D));

        assertEquals(plane, new PlacedSolid3d(plane, RigidTransform3d.identity()).bounds());
        assertEquals(line, new PlacedSolid3d(line, RigidTransform3d.identity()).bounds());
        assertEquals(point, new PlacedSolid3d(point, RigidTransform3d.identity()).bounds());
    }

    @Test
    void unrepresentablePlacedAabbBoundFailsDuringConstruction() {
        Vec3d point = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);
        Aabb local = new Aabb(point, point);
        RigidTransform3d placement = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));

        assertThrows(IllegalArgumentException.class, () -> new PlacedSolid3d(local, placement));
    }

    @Test
    void placedSphereTransformsCenterPreservesRadiusAndIgnoresRotationForExtent() {
        Sphere local = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 1.5D);
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Sphere expected = new Sphere(placement.transformPoint(local.center()), local.radius());

        assertEquals(expected.bounds(), new PlacedSolid3d(local, placement).bounds());
    }

    @Test
    void placedZeroRadiusSphereHasPointBounds() {
        Sphere local = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 0.0D);
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Vec3d parentPoint = placement.transformPoint(local.center());

        assertEquals(new Aabb(parentPoint, parentPoint), new PlacedSolid3d(local, placement).bounds());
    }

    @Test
    void placedSphereBoundsOverflowFailsConstruction() {
        Sphere local = new Sphere(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), 0.0D);
        RigidTransform3d placement = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));

        assertThrows(IllegalArgumentException.class, () -> new PlacedSolid3d(local, placement));
    }

    @Test
    void placedCapsuleTransformsCenterlineAndPreservesRadialExtrema() {
        Capsule local = new Capsule(
                new Segment3d(new Vec3d(-2.0D, 1.0D, 3.0D), new Vec3d(4.0D, 1.0D, 3.0D)),
                0.75D);
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Capsule expected = new Capsule(
                new Segment3d(
                        placement.transformPoint(local.centerline().start()),
                        placement.transformPoint(local.centerline().end())),
                local.radius());

        assertEquals(expected.bounds(), new PlacedSolid3d(local, placement).bounds());
    }

    @Test
    void placedCapsulePreservesZeroRadiusAndZeroLengthDegeneracies() {
        Vec3d point = new Vec3d(1.0D, 2.0D, 3.0D);
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.0D, 1.0D, 0.0D, 1.0D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Capsule zeroRadius = new Capsule(new Segment3d(point, point.add(new Vec3d(2.0D, 0.0D, 0.0D))), 0.0D);
        Capsule pointCapsule = new Capsule(new Segment3d(point, point), 1.25D);

        Capsule expectedZeroRadius = new Capsule(
                new Segment3d(
                        placement.transformPoint(zeroRadius.centerline().start()),
                        placement.transformPoint(zeroRadius.centerline().end())),
                0.0D);
        Capsule expectedPoint = new Capsule(
                new Segment3d(placement.transformPoint(point), placement.transformPoint(point)),
                1.25D);
        assertEquals(expectedZeroRadius.bounds(), new PlacedSolid3d(zeroRadius, placement).bounds());
        assertEquals(expectedPoint.bounds(), new PlacedSolid3d(pointCapsule, placement).bounds());
    }

    @Test
    void placedCapsuleBoundsOverflowFailsConstruction() {
        Vec3d point = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);
        Capsule local = new Capsule(new Segment3d(point, point), 0.0D);
        RigidTransform3d placement = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));

        assertThrows(IllegalArgumentException.class, () -> new PlacedSolid3d(local, placement));
    }

    @Test
    void placedObbBoundsContainAllIntrinsicThenPlacementCornerWitnesses() {
        Obb local = new Obb(
                new Vec3d(1.0D, 2.0D, 3.0D),
                new Vec3d(2.0D, 1.0D, 3.0D),
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Aabb bounds = new PlacedSolid3d(local, placement).bounds();

        for (int signs = 0; signs < 8; signs++) {
            Vec3d half = local.halfExtents();
            Vec3d localCorner = new Vec3d(
                    (signs & 1) == 0 ? -half.x() : half.x(),
                    (signs & 2) == 0 ? -half.y() : half.y(),
                    (signs & 4) == 0 ? -half.z() : half.z());
            Vec3d parentCorner = placement.transformPoint(local.localToWorld(localCorner));
            assertEquals(true, bounds.contains(parentCorner), "corner=" + signs + ", parent=" + parentCorner + ", bounds=" + bounds);
        }
    }

    @Test
    void placedObbBoundsRespectQuaternionSignsAndDegenerateExtents() {
        Vec3d center = new Vec3d(1.0D, 2.0D, 3.0D);
        Vec3d half = new Vec3d(0.0D, 2.0D, 0.0D);
        Rotation3d rotation = new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D);
        Obb positive = new Obb(center, half, rotation);
        Obb negative = new Obb(center, half, new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));

        assertEquals(
                new PlacedSolid3d(positive, placement).bounds(),
                new PlacedSolid3d(negative, placement).bounds());
    }

    @Test
    void placedCompositeBoundsUnionAllCanonicalPrimitiveLeafBounds() {
        Solid3d aabb = new Aabb(new Vec3d(-6.0D, -1.0D, -1.0D), new Vec3d(-4.0D, 1.0D, 1.0D));
        Solid3d sphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Solid3d obb = new Obb(new Vec3d(4.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), Rotation3d.identity());
        Solid3d capsule = new Capsule(new Segment3d(new Vec3d(8.0D, 0.0D, 0.0D), new Vec3d(10.0D, 0.0D, 0.0D)), 0.5D);
        Composite local = new Composite(List.of(aabb, sphere, obb, capsule, sphere));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Aabb actual = new PlacedSolid3d(local, placement).bounds();

        Aabb expected = null;
        for (int index = 0; index < local.childCount(); index++) {
            Aabb leafBounds = new PlacedSolid3d(local.child(index), placement).bounds();
            expected = expected == null
                    ? leafBounds
                    : new Aabb(expected.min().min(leafBounds.min()), expected.max().max(leafBounds.max()));
        }
        assertEquals(expected, actual);
    }

    @Test
    void placedCompositeBoundsIgnoreOrderAndDuplicateMultiplicity() {
        Solid3d first = new Sphere(new Vec3d(-3.0D, 0.0D, 0.0D), 1.0D);
        Solid3d second = new Aabb(new Vec3d(2.0D, -1.0D, -1.0D), new Vec3d(4.0D, 1.0D, 1.0D));
        RigidTransform3d placement = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(5.0D, -7.0D, 11.0D));

        assertEquals(
                new PlacedSolid3d(new Composite(List.of(first, second)), placement).bounds(),
                new PlacedSolid3d(new Composite(List.of(second, first, first)), placement).bounds());
    }

    @Test
    void unrepresentablePlacedCompositeLeafFailsWholeConstruction() {
        Vec3d maximum = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);
        Composite local = new Composite(List.of(
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                new Aabb(maximum, maximum)));
        RigidTransform3d placement = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));

        assertThrows(IllegalArgumentException.class, () -> new PlacedSolid3d(local, placement));
    }

    @Test
    void exactStructuralValueSemanticsExcludeDerivedBounds() {
        Solid3d local = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 1.0D);
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(-0.0D, 5.0D, 6.0D));
        PlacedSolid3d first = new PlacedSolid3d(local, transform);
        PlacedSolid3d equivalent = new PlacedSolid3d(
                new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 1.0D),
                new RigidTransform3d(
                        new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D),
                        new Vec3d(0.0D, 5.0D, 6.0D)));
        PlacedSolid3d transitive = new PlacedSolid3d(local, transform);
        PlacedSolid3d differentSolid = new PlacedSolid3d(new Sphere(local.bounds().center(), 2.0D), transform);
        PlacedSolid3d differentTransform = new PlacedSolid3d(local, new RigidTransform3d(transform.rotation(), new Vec3d(1.0D, 5.0D, 6.0D)));

        assertEquals(first, equivalent);
        assertEquals(equivalent, first);
        assertEquals(equivalent, transitive);
        assertEquals(first, transitive);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertNotEquals(first, differentSolid);
        assertNotEquals(first, differentTransform);
        assertNotEquals(first, null);
        assertEquals(
                "PlacedSolid3d[localSolid=" + local + ", localToParent=" + transform + "]",
                first.toString());
    }

    @Test
    void identityPlacedAabbQueriesPreserveAdjacentSubnormalAndLargeStoredEndpoints() {
        double largeMinimum = Double.MAX_VALUE / 4.0D;
        double[][] intervals = {
                {1.0D, Math.nextUp(1.0D)},
                {0.0D, Double.MIN_VALUE},
                {largeMinimum, Math.nextUp(largeMinimum)}
        };
        for (double[] interval : intervals) {
            Aabb local = new Aabb(
                    new Vec3d(interval[0], 0.0D, 0.0D),
                    new Vec3d(interval[1], 0.0D, 0.0D));
            PlacedSolid3d placed = new PlacedSolid3d(local, RigidTransform3d.identity());
            for (double inside : interval) {
                Sphere witness = new Sphere(new Vec3d(inside, 0.0D, 0.0D), 0.0D);
                assertTrue(GeometryIntersections.intersects(placed, new PlacedSolid3d(witness, RigidTransform3d.identity())));
                Segment3d point = new Segment3d(witness.center(), witness.center());
                assertTrue(GeometryIntersections.intersects(point, placed));
                assertEquals(GeometryIntersections.intersects(point, local), GeometryIntersections.intersects(point, placed));
            }
            double below = Math.nextDown(interval[0]);
            double above = Math.nextUp(interval[1]);
            for (double outside : new double[] {below, above}) {
                Segment3d point = new Segment3d(new Vec3d(outside, 0.0D, 0.0D), new Vec3d(outside, 0.0D, 0.0D));
                assertFalse(GeometryIntersections.intersects(point, placed), "interval=[" + interval[0] + "," + interval[1] + "], outside=" + outside);
            }
            assertEquals(local, placed.bounds());
        }
    }

    @Test
    void identityPlacedAabbPlaneLineAndPointQueriesMatchTypedKernels() {
        List<Aabb> boxes = List.of(
                new Aabb(new Vec3d(-1.0D, -1.0D, 0.0D), new Vec3d(1.0D, 1.0D, 0.0D)),
                new Aabb(new Vec3d(0.0D, 0.0D, -1.0D), new Vec3d(0.0D, 0.0D, 1.0D)),
                new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D)));
        Segment3d origin = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        for (Aabb box : boxes) {
            PlacedSolid3d placed = new PlacedSolid3d(box, RigidTransform3d.identity());
            assertEquals(GeometryIntersections.intersects(origin, box), GeometryIntersections.intersects(origin, placed));
        }
    }

    private static void assertAabbCornerWitnessesContained(
            Aabb local,
            RigidTransform3d placement,
            Aabb parentBounds
    ) {
        for (int signs = 0; signs < 8; signs++) {
            Vec3d localCorner = new Vec3d(
                    (signs & 1) == 0 ? local.min().x() : local.max().x(),
                    (signs & 2) == 0 ? local.min().y() : local.max().y(),
                    (signs & 4) == 0 ? local.min().z() : local.max().z());
            Vec3d parentCorner = placement.transformPoint(localCorner);
            assertEquals(true, parentBounds.contains(parentCorner), "corner=" + signs + ", parent=" + parentCorner + ", bounds=" + parentBounds);
        }
    }
}
