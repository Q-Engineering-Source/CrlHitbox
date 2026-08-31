package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedPrimitiveQueriesPhase1ETest {
    @Test void aabbAabbIdentityAgreement() { assertIdentityAgreement(0, 0); }
    @Test void aabbSphereIdentityAgreement() { assertIdentityAgreement(0, 1); }
    @Test void aabbObbIdentityAgreement() { assertIdentityAgreement(0, 2); }
    @Test void aabbCapsuleIdentityAgreement() { assertIdentityAgreement(0, 3); }
    @Test void sphereAabbIdentityAgreement() { assertIdentityAgreement(1, 0); }
    @Test void sphereSphereIdentityAgreement() { assertIdentityAgreement(1, 1); }
    @Test void sphereObbIdentityAgreement() { assertIdentityAgreement(1, 2); }
    @Test void sphereCapsuleIdentityAgreement() { assertIdentityAgreement(1, 3); }
    @Test void obbAabbIdentityAgreement() { assertIdentityAgreement(2, 0); }
    @Test void obbSphereIdentityAgreement() { assertIdentityAgreement(2, 1); }
    @Test void obbObbIdentityAgreement() { assertIdentityAgreement(2, 2); }
    @Test void obbCapsuleIdentityAgreement() { assertIdentityAgreement(2, 3); }
    @Test void capsuleAabbIdentityAgreement() { assertIdentityAgreement(3, 0); }
    @Test void capsuleSphereIdentityAgreement() { assertIdentityAgreement(3, 1); }
    @Test void capsuleObbIdentityAgreement() { assertIdentityAgreement(3, 2); }
    @Test void capsuleCapsuleIdentityAgreement() { assertIdentityAgreement(3, 3); }

    @Test
    void placedQueryRejectsNullArguments() {
        PlacedSolid3d placed = placed(solid(0, 0.0D, false), 0.0D, Rotation3d.identity());
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects((PlacedSolid3d) null, placed));
        assertThrows(NullPointerException.class, () -> GeometryIntersections.intersects(placed, (PlacedSolid3d) null));
    }

    @Test
    void independentlyTranslatedAabbsOverlapAndSeparateExactly() {
        Aabb unit = (Aabb) solid(0, 0.0D, false);
        PlacedSolid3d first = placed(unit, 10.0D, Rotation3d.identity());
        PlacedSolid3d overlapping = placed(unit, 11.5D, Rotation3d.identity());
        PlacedSolid3d separated = placed(unit, 13.0D, Rotation3d.identity());

        assertTrue(GeometryIntersections.intersects(first, overlapping));
        assertFalse(GeometryIntersections.intersects(first, separated));
    }

    @Test
    void translatedAabbFaceEdgeAndCornerContactsRemainClosed() {
        Aabb unit = (Aabb) solid(0, 0.0D, false);
        PlacedSolid3d first = placed(unit, new Vec3d(5.0D, 0.0D, 0.0D), Rotation3d.identity());
        for (Vec3d translation : new Vec3d[] {
                new Vec3d(7.0D, 0.0D, 0.0D),
                new Vec3d(7.0D, 2.0D, 0.0D),
                new Vec3d(7.0D, 2.0D, 2.0D)
        }) {
            assertTrue(GeometryIntersections.intersects(first, placed(unit, translation, Rotation3d.identity())), translation.toString());
        }
    }

    @Test
    void differentlyRotatedAabbAndObbUseExactRigidBoxQueries() {
        Aabb aabb = (Aabb) solid(0, 0.0D, false);
        Obb obb = (Obb) solid(2, 0.0D, false);
        PlacedSolid3d first = placed(aabb, new Vec3d(3.0D, -2.0D, 1.0D), new Rotation3d(0.2D, -0.1D, 0.3D, 0.9D));
        PlacedSolid3d overlapping = placed(obb, new Vec3d(3.5D, -2.0D, 1.0D), new Rotation3d(-0.1D, 0.3D, 0.2D, 0.8D));
        PlacedSolid3d separated = placed(obb, new Vec3d(12.0D, -2.0D, 1.0D), new Rotation3d(-0.1D, 0.3D, 0.2D, 0.8D));

        assertTrue(GeometryIntersections.intersects(first, overlapping));
        assertFalse(GeometryIntersections.intersects(first, separated));
    }

    @Test
    void placedSphereUsesTrueDistanceAgainstRotatedAabb() {
        Aabb localBox = (Aabb) solid(0, 0.0D, false);
        Sphere localSphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.75D);
        Rotation3d rotation = new Rotation3d(0.2D, -0.1D, 0.3D, 0.9D);
        PlacedSolid3d box = placed(localBox, new Vec3d(3.0D, -2.0D, 1.0D), rotation);
        PlacedSolid3d hit = placed(localSphere, new Vec3d(3.5D, -2.0D, 1.0D), rotation);
        PlacedSolid3d miss = placed(localSphere, new Vec3d(10.0D, -2.0D, 1.0D), rotation);

        assertTrue(GeometryIntersections.intersects(hit, box));
        assertTrue(GeometryIntersections.intersects(box, hit));
        assertFalse(GeometryIntersections.intersects(miss, box));
    }

    @Test
    void placedCapsuleUsesWholeCenterlineAgainstRotatedAabbAndObb() {
        Capsule capsule = new Capsule(
                new Segment3d(new Vec3d(-3.0D, 1.5D, 0.0D), new Vec3d(3.0D, 1.5D, 0.0D)),
                0.75D);
        Aabb aabb = (Aabb) solid(0, 0.0D, false);
        Obb obb = (Obb) solid(2, 0.0D, false);
        RigidTransform3d common = new RigidTransform3d(
                new Rotation3d(0.2D, -0.1D, 0.3D, 0.9D),
                new Vec3d(3.0D, -2.0D, 1.0D));

        assertTrue(GeometryIntersections.intersects(new PlacedSolid3d(capsule, common), new PlacedSolid3d(aabb, common)));
        assertTrue(GeometryIntersections.intersects(new PlacedSolid3d(capsule, common), new PlacedSolid3d(obb, common)));
    }

    @Test
    void commonPlacementPreservesCrossAxisOnlyBoxSeparation() {
        Obb firstLocal = new Obb(
                new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(1.0D, 0.25D, 0.25D),
                new Rotation3d(-0.10633411073500307D, 0.5688316182711847D, -0.6544304516323415D, 0.48666665277731D));
        Obb secondLocal = new Obb(
                new Vec3d(0.2894382783525451D, 1.4154584653368913D, 0.4663658762717766D),
                new Vec3d(1.0D, 0.25D, 0.25D),
                new Rotation3d(0.06773886951658335D, 0.7708174152524913D, -0.34353871857551327D, 0.532196492603604D));
        RigidTransform3d common = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));

        assertFalse(GeometryIntersections.intersects(firstLocal, secondLocal));
        assertFalse(GeometryIntersections.intersects(
                new PlacedSolid3d(firstLocal, common),
                new PlacedSolid3d(secondLocal, common)));
    }

    @Test
    void nearParallelNonzeroCrossAxesRemainTested() {
        Obb firstLocal = new Obb(
                new Vec3d(-0.25D, 0.1D, 0.2D),
                new Vec3d(1.2D, 0.7D, 0.4D),
                new Rotation3d(0.1D, -0.3D, 0.2D, 0.9D));
        Obb secondLocal = new Obb(
                new Vec3d(0.8D, 0.2D, 0.1D),
                new Vec3d(0.9D, 0.5D, 0.6D),
                new Rotation3d(0.100000000000001D, -0.300000000000002D, 0.200000000000003D, 0.900000000000004D));
        RigidTransform3d common = new RigidTransform3d(
                new Rotation3d(0.1D, 0.2D, -0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        boolean expected = GeometryIntersections.intersects(firstLocal, secondLocal);

        assertEquals(expected, GeometryIntersections.intersects(
                new PlacedSolid3d(firstLocal, common),
                new PlacedSolid3d(secondLocal, common)));
    }

    @Test
    void overlappingRootBoundsNeverBecomePositiveNarrowPhaseAuthority() {
        Sphere local = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        PlacedSolid3d first = placed(local, new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        PlacedSolid3d second = placed(local, new Vec3d(1.9D, 1.9D, 0.0D), Rotation3d.identity());

        assertTrue(GeometryIntersections.intersects(first.bounds(), second.bounds()));
        assertFalse(GeometryIntersections.intersects(first, second));
    }

    @Test
    void hugeCommonTranslationPreservesSmallRepresentablePointSeparation() {
        Aabb firstLocal = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Aabb secondLocal = new Aabb(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(1.0E-16D, 0.0D, 0.0D));
        RigidTransform3d common = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(0.0D, Double.MAX_VALUE, 0.0D));

        assertFalse(GeometryIntersections.intersects(
                new PlacedSolid3d(firstLocal, common),
                new PlacedSolid3d(secondLocal, common)));
    }

    @Test
    void equivalentQuaternionSignsProduceEquivalentPlacedQueries() {
        Aabb firstLocal = (Aabb) solid(0, 0.0D, false);
        Obb secondLocal = (Obb) solid(2, 0.0D, false);
        RigidTransform3d positive = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        RigidTransform3d negative = new RigidTransform3d(
                new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        PlacedSolid3d second = new PlacedSolid3d(secondLocal, positive);

        assertEquals(
                GeometryIntersections.intersects(new PlacedSolid3d(firstLocal, positive), second),
                GeometryIntersections.intersects(new PlacedSolid3d(firstLocal, negative), second));
    }

    @Test
    void zeroRadiusAndZeroCenterlineCapsulesAgreeWithSegmentAndSphereAgainstPlacedBox() {
        Aabb localBox = (Aabb) solid(0, 0.0D, false);
        Segment3d localSegment = new Segment3d(new Vec3d(-2.0D, 0.5D, 0.0D), new Vec3d(2.0D, 0.5D, 0.0D));
        Capsule zeroRadius = new Capsule(localSegment, 0.0D);
        Sphere localSphere = new Sphere(new Vec3d(0.0D, 0.5D, 0.0D), 0.75D);
        Capsule zeroCenterline = new Capsule(new Segment3d(localSphere.center(), localSphere.center()), localSphere.radius());
        RigidTransform3d common = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        PlacedSolid3d box = new PlacedSolid3d(localBox, common);
        Segment3d parentSegment = new Segment3d(
                common.transformPoint(localSegment.start()),
                common.transformPoint(localSegment.end()));

        assertEquals(
                GeometryIntersections.intersects(parentSegment, box),
                GeometryIntersections.intersects(new PlacedSolid3d(zeroRadius, common), box));
        assertEquals(
                GeometryIntersections.intersects(new PlacedSolid3d(localSphere, common), box),
                GeometryIntersections.intersects(new PlacedSolid3d(zeroCenterline, common), box));
    }

    @Test
    void relativeCapsuleBoxPathHandlesOverflowingParentEndpointDifference() {
        double half = Double.MAX_VALUE / 2.0D;
        Capsule localCapsule = new Capsule(
                new Segment3d(new Vec3d(-half, -half, 0.0D), new Vec3d(half, half, 0.0D)),
                0.0D);
        Aabb localPoint = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        RigidTransform3d common = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, Math.sin(Math.PI / 8.0D), Math.cos(Math.PI / 8.0D)),
                new Vec3d(0.0D, 0.0D, 0.0D));
        Vec3d parentStart = common.transformPoint(localCapsule.centerline().start());
        Vec3d parentEnd = common.transformPoint(localCapsule.centerline().end());

        assertFalse(Double.isFinite(parentEnd.y() - parentStart.y()), "test requires raw parent endpoint difference overflow");
        assertTrue(GeometryIntersections.intersects(
                new PlacedSolid3d(localCapsule, common),
                new PlacedSolid3d(localPoint, common)));
    }

    @Test
    void subnormalRotationUsesObservableInversePointMappingForDegenerateBoxContact() {
        double minimum = Double.MIN_VALUE;
        Vec3d localPoint = new Vec3d(minimum, 0.0D, 0.0D);
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, minimum, 1.0D),
                new Vec3d(0.0D, 0.0D, 0.0D));
        Vec3d parentPoint = transform.transformPoint(localPoint);
        PlacedSolid3d box = new PlacedSolid3d(new Aabb(localPoint, localPoint), transform);
        PlacedSolid3d sphere = new PlacedSolid3d(new Sphere(localPoint, 0.0D), transform);
        PlacedSolid3d capsule = new PlacedSolid3d(
                new Capsule(new Segment3d(localPoint, localPoint), 0.0D),
                transform);
        Segment3d pointSegment = new Segment3d(parentPoint, parentPoint);

        assertEquals(localPoint, transform.inverseTransformPoint(parentPoint));
        assertTrue(GeometryIntersections.intersects(box, sphere));
        assertTrue(GeometryIntersections.intersects(sphere, box));
        assertTrue(GeometryIntersections.intersects(capsule, box));
        assertTrue(GeometryIntersections.intersects(pointSegment, box));
        assertTrue(GeometryIntersections.intersects(box, pointSegment));
    }

    @Test
    void unrepresentableDirectInversePointFallsBackToRelativeEndpointProjection() {
        Vec3d localBoxPoint = new Vec3d(-Double.MAX_VALUE, 0.0D, 0.0D);
        RigidTransform3d boxTransform = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));
        PlacedSolid3d box = new PlacedSolid3d(new Aabb(localBoxPoint, localBoxPoint), boxTransform);
        Capsule reachingParentOrigin = new Capsule(
                new Segment3d(localBoxPoint, new Vec3d(0.0D, 0.0D, 0.0D)),
                0.0D);
        PlacedSolid3d capsule = new PlacedSolid3d(reachingParentOrigin, RigidTransform3d.identity());

        assertThrows(
                IllegalArgumentException.class,
                () -> boxTransform.inverseTransformPoint(reachingParentOrigin.centerline().start()));
        assertEquals(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D)), box.bounds());
        assertTrue(GeometryIntersections.intersects(capsule, box));
        assertTrue(GeometryIntersections.intersects(box, capsule));
    }

    private static void assertIdentityAgreement(int firstKind, int secondKind) {
        Solid3d[][] cases = {
                {solid(firstKind, 0.0D, false), solid(secondKind, 0.0D, false)},
                {solid(firstKind, 0.0D, false), solid(secondKind, 5.0D, false)},
                {solid(firstKind, 0.0D, false), solid(secondKind, 2.0D, false)},
                {solid(firstKind, 0.0D, true), solid(secondKind, 0.0D, true)}
        };
        for (int relation = 0; relation < cases.length; relation++) {
            Solid3d firstLocal = cases[relation][0];
            Solid3d secondLocal = cases[relation][1];
            PlacedSolid3d first = new PlacedSolid3d(firstLocal, RigidTransform3d.identity());
            PlacedSolid3d second = new PlacedSolid3d(secondLocal, RigidTransform3d.identity());
            boolean expected = Phase1CTestSupport.intersectsPrimitiveTyped(firstLocal, secondLocal);
            String context = "firstKind=" + firstKind + ", secondKind=" + secondKind
                    + ", relation=" + relation + ", first=" + firstLocal + ", second=" + secondLocal;

            assertEquals(expected, GeometryIntersections.intersects(first, second), context);
            assertEquals(expected, GeometryIntersections.intersects(second, first), context + ", reverse");
            assertEquals(firstLocal, first.localSolid(), context + ", first mutation");
            assertEquals(secondLocal, second.localSolid(), context + ", second mutation");
            assertEquals(firstLocal.bounds(), first.bounds(), context + ", first identity bounds");
            assertEquals(secondLocal.bounds(), second.bounds(), context + ", second identity bounds");
        }
    }

    private static Solid3d solid(int kind, double centerX, boolean degenerate) {
        Vec3d center = new Vec3d(centerX, 0.0D, 0.0D);
        return switch (kind) {
            case 0 -> degenerate
                    ? new Aabb(center, center)
                    : new Aabb(new Vec3d(centerX - 1.0D, -1.0D, -1.0D), new Vec3d(centerX + 1.0D, 1.0D, 1.0D));
            case 1 -> new Sphere(center, degenerate ? 0.0D : 1.0D);
            case 2 -> new Obb(
                    center,
                    degenerate ? new Vec3d(0.0D, 0.0D, 0.0D) : new Vec3d(1.0D, 1.0D, 1.0D),
                    Rotation3d.identity());
            case 3 -> degenerate
                    ? new Capsule(new Segment3d(center, center), 0.0D)
                    : new Capsule(
                            new Segment3d(
                                    new Vec3d(centerX - 0.5D, 0.0D, 0.0D),
                                    new Vec3d(centerX + 0.5D, 0.0D, 0.0D)),
                            0.5D);
            default -> throw new AssertionError("unknown kind " + kind);
        };
    }

    private static PlacedSolid3d placed(Solid3d local, double x, Rotation3d rotation) {
        return placed(local, new Vec3d(x, 0.0D, 0.0D), rotation);
    }

    private static PlacedSolid3d placed(Solid3d local, Vec3d translation, Rotation3d rotation) {
        return new PlacedSolid3d(local, new RigidTransform3d(rotation, translation));
    }
}
