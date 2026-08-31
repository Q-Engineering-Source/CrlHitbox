package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxSatPhase1BTest {
    @Test
    void obbPairsUseClosedSetSatForOverlapTangencyAndSeparation() {
        Obb unit = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        assertTrue(GeometryIntersections.intersects(unit, new Obb(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(0.5D, 0.5D, 0.5D), Rotation3d.identity())));
        assertTrue(GeometryIntersections.intersects(unit, new Obb(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity())));
        assertFalse(GeometryIntersections.intersects(unit, new Obb(new Vec3d(Math.nextUp(2.0D), 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity())));
    }

    @Test
    void obbPairsRejectStableCrossAxisOnlySeparation() {
        Obb first = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.25D, 0.25D), new Rotation3d(-0.10633411073500307D, 0.5688316182711847D, -0.6544304516323415D, 0.48666665277731D));
        Obb second = new Obb(new Vec3d(0.2894382783525451D, 1.4154584653368913D, 0.4663658762717766D), new Vec3d(1.0D, 0.25D, 0.25D), new Rotation3d(0.06773886951658335D, 0.7708174152524913D, -0.34353871857551327D, 0.532196492603604D));
        assertFalse(GeometryIntersections.intersects(first, second));
        assertFalse(Phase1BTestSupport.obbObbSatByCornerProjection(first, second));
    }

    @Test
    void obbPairsCoverRotatedNoCornerOverlapAndNearParallelOrientations() {
        Obb first = new Obb(new Vec3d(-0.25D, 0.1D, 0.2D), new Vec3d(1.2D, 0.7D, 0.4D), new Rotation3d(0.1D, -0.3D, 0.2D, 0.9D));
        Obb second = new Obb(new Vec3d(0.8D, 0.2D, 0.1D), new Vec3d(0.9D, 0.5D, 0.6D), new Rotation3d(0.100000000000001D, -0.300000000000002D, 0.200000000000003D, 0.900000000000004D));
        assertTrue(GeometryIntersections.intersects(first, second));
        assertEquals(Phase1BTestSupport.obbObbSatByCornerProjection(first, second), GeometryIntersections.intersects(first, second));
    }

    @Test
    void obbPairsAreSymmetricForCanonicalQuaternionSigns() {
        Obb first = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 0.5D), new Rotation3d(0.2D, -0.4D, 0.1D, 0.8D));
        Obb second = new Obb(new Vec3d(1.1D, -0.2D, 0.3D), new Vec3d(0.8D, 0.3D, 1.1D), new Rotation3d(-0.3D, 0.2D, 0.4D, 0.7D));
        Obb negatedQuaternion = new Obb(second.center(), second.halfExtents(), new Rotation3d(0.3D, -0.2D, -0.4D, -0.7D));
        assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(second, first));
        assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(first, negatedQuaternion));
    }

    @Test
    void obbPairsPreserveTranslationRotationAndPositiveScale() {
        Obb first = new Obb(new Vec3d(-1.0D, 0.5D, 0.25D), new Vec3d(1.0D, 0.7D, 0.2D), new Rotation3d(0.1D, 0.2D, 0.3D, 0.9D));
        Obb second = new Obb(new Vec3d(0.75D, 0.3D, 0.1D), new Vec3d(0.8D, 0.4D, 0.6D), new Rotation3d(-0.3D, 0.2D, 0.1D, 0.8D));
        boolean expected = GeometryIntersections.intersects(first, second);
        Vec3d translation = new Vec3d(4.0D, -3.0D, 2.0D);
        Rotation3d quarterTurn = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        assertEquals(expected, GeometryIntersections.intersects(translate(first, translation), translate(second, translation)));
        assertEquals(expected, GeometryIntersections.intersects(rotate(first, quarterTurn), rotate(second, quarterTurn)));
        assertEquals(expected, GeometryIntersections.intersects(scale(first, 2.5D), scale(second, 2.5D)));
    }

    @Test
    void obbPairsHandlePointLinePlaneHugeAndSubnormalExtents() {
        Obb point = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Obb line = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D), Rotation3d.identity());
        Obb plane = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 0.0D), Rotation3d.identity());
        assertTrue(GeometryIntersections.intersects(point, line));
        assertTrue(GeometryIntersections.intersects(point, plane));
        assertFalse(GeometryIntersections.intersects(point, new Obb(new Vec3d(Math.nextUp(1.0D), 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D), Rotation3d.identity())));
        double tiny = Double.MIN_VALUE;
        assertFalse(GeometryIntersections.intersects(new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(tiny, tiny, tiny), Rotation3d.identity()), new Obb(new Vec3d(4.0D * tiny, 0.0D, 0.0D), new Vec3d(tiny, tiny, tiny), Rotation3d.identity())));
        assertFalse(GeometryIntersections.intersects(new Obb(new Vec3d(1.0E300D, 0.0D, 0.0D), new Vec3d(1.0E290D, 1.0D, 1.0D), Rotation3d.identity()), new Obb(new Vec3d(1.0E300D + 3.0E290D, 0.0D, 0.0D), new Vec3d(1.0E290D, 1.0D, 1.0D), Rotation3d.identity())));
    }

    @Test
    void hugeZeroProjectedExtentCannotHideAnOrthogonalPointSeparation() {
        Obb longLine = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, Double.MAX_VALUE), Rotation3d.identity());
        Obb offsetPoint = new Obb(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Aabb offsetAabbPoint = new Aabb(offsetPoint.center(), offsetPoint.center());
        assertFalse(GeometryIntersections.intersects(longLine, offsetPoint));
        assertFalse(GeometryIntersections.intersects(offsetAabbPoint, longLine));
    }

    @Test
    void aabbObbPairsUseStoredEndpointsForTangenciesAndAdjacentValues() {
        Aabb first = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Obb touching = new Obb(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        Obb separated = new Obb(new Vec3d(Math.nextUp(2.0D), 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        assertTrue(GeometryIntersections.intersects(first, touching));
        assertFalse(GeometryIntersections.intersects(first, separated));
        assertEquals(GeometryIntersections.intersects(first, touching), GeometryIntersections.intersects(touching, first));
        double tiny = Double.MIN_VALUE;
        Aabb subnormal = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(tiny, tiny, tiny));
        assertFalse(GeometryIntersections.intersects(subnormal, new Obb(new Vec3d(3.0D * tiny, 0.0D, 0.0D), new Vec3d(tiny, tiny, tiny), Rotation3d.identity())));
    }

    @Test
    void aabbObbPairsAgreeWithIndependentCornerProjectionOracle() {
        Aabb first = new Aabb(new Vec3d(-1.3D, -0.4D, -0.9D), new Vec3d(0.6D, 1.2D, 0.8D));
        Obb second = new Obb(new Vec3d(0.5D, 0.75D, -0.1D), new Vec3d(0.9D, 0.4D, 0.6D), new Rotation3d(0.3D, -0.2D, 0.1D, 0.8D));
        boolean expected = Phase1BTestSupport.aabbObbSatByCornerProjection(first, second);
        assertEquals(expected, GeometryIntersections.intersects(first, second));
        assertEquals(expected, GeometryIntersections.intersects(second, first));
    }

    @Test
    void identityObbIsEquivalentToAabbIncludingDegenerateBoxes() {
        Aabb first = new Aabb(new Vec3d(-2.0D, 0.0D, -1.0D), new Vec3d(0.0D, 0.0D, 3.0D));
        Aabb second = new Aabb(new Vec3d(-0.5D, -0.25D, -0.5D), new Vec3d(1.0D, 0.25D, 1.0D));
        Obb identity = new Obb(second.center(), second.halfExtents(), Rotation3d.identity());
        assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(first, identity));
    }

    private static Obb translate(Obb box, Vec3d translation) { return new Obb(box.center().add(translation), box.halfExtents(), box.orientation()); }
    private static Obb scale(Obb box, double factor) { return new Obb(box.center().multiply(factor), box.halfExtents().multiply(factor), box.orientation()); }
    private static Obb rotate(Obb box, Rotation3d rotation) { return new Obb(rotation.rotate(box.center()), box.halfExtents(), compose(rotation, box.orientation())); }
    private static Rotation3d compose(Rotation3d first, Rotation3d second) { return new Rotation3d(first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(), first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(), first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(), first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z()); }
}
