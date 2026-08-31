package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentBoxDistancePhase1BTest {
    @Test
    void segmentToAabbFindsTheStrictInteriorActiveSetMinimum() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Segment3d segment = new Segment3d(new Vec3d(-2.0D, 3.0D, 0.0D), new Vec3d(2.0D, 3.0D, 0.0D));

        assertEquals(4.0D, GeometryDistances.segmentToAabbSquared(segment, box));
    }

    @Test
    void segmentToAabbTreatsFaceEdgeAndCornerContactAsCanonicalZero() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        assertCanonicalZero(GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(-2.0D, 1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 0.0D)), box));
        assertCanonicalZero(GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(-2.0D, 1.0D, 1.0D), new Vec3d(2.0D, 1.0D, 1.0D)), box));
        assertCanonicalZero(GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(-2.0D, -2.0D, -2.0D), new Vec3d(1.0D, 1.0D, 1.0D)), box));
    }

    @Test
    void segmentToAabbSupportsDegenerateSegmentAndDegenerateBoxes() {
        Segment3d point = new Segment3d(new Vec3d(3.0D, 2.0D, 1.0D), new Vec3d(3.0D, 2.0D, 1.0D));
        Aabb plane = new Aabb(new Vec3d(-1.0D, -1.0D, 1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Aabb pointBox = new Aabb(new Vec3d(1.0D, 2.0D, 1.0D), new Vec3d(1.0D, 2.0D, 1.0D));
        assertEquals(5.0D, GeometryDistances.segmentToAabbSquared(point, plane));
        assertEquals(4.0D, GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(-1.0D, 4.0D, 1.0D), new Vec3d(3.0D, 4.0D, 1.0D)), pointBox));
    }

    @Test
    void segmentToAabbIsOrderInvariantAndHandlesExtremeAnalyticCases() {
        Aabb box = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Segment3d forward = new Segment3d(new Vec3d(-2.0D, 3.0D, 0.0D), new Vec3d(2.0D, 3.0D, 0.0D));
        Segment3d reverse = new Segment3d(forward.end(), forward.start());
        assertEquals(9.0D, GeometryDistances.segmentToAabbSquared(forward, box));
        assertEquals(GeometryDistances.segmentToAabbSquared(forward, box), GeometryDistances.segmentToAabbSquared(reverse, box));
        assertEquals(Double.POSITIVE_INFINITY, GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D)), box));
        assertEquals(Double.MIN_VALUE * Double.MIN_VALUE, GeometryDistances.segmentToAabbSquared(new Segment3d(new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D)), box));
    }

    @Test
    void segmentToObbUsesTheSameTrueLocalSegmentBoxDistance() {
        Obb box = new Obb(new Vec3d(4.0D, -3.0D, 2.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Segment3d segment = new Segment3d(box.localToWorld(new Vec3d(-2.0D, 3.0D, 0.0D)), box.localToWorld(new Vec3d(2.0D, 3.0D, 0.0D)));

        GeometryTestSupport.assertClose(1.0D, GeometryDistances.segmentToObbSquared(segment, box));
    }

    @Test
    void segmentToObbAcceptsReconstructedBoundaryWitnessAndDegenerateObb() {
        Obb box = new Obb(new Vec3d(-0.7082650730663893D, -6.731064378241012D, -9.70759371599838D), new Vec3d(7.52805839645041D, 3.4219451865795945D, 0.25667266740287853D), new Rotation3d(-0.005971366638466002D, -0.627596208544786D, 0.16834333991315123D, 0.7600972712143912D));
        Vec3d corner = box.localToWorld(new Vec3d(-box.halfExtents().x(), -box.halfExtents().y(), -box.halfExtents().z()));
        assertTrue(box.contains(corner));
        assertCanonicalZero(GeometryDistances.segmentToObbSquared(new Segment3d(corner, corner), box));
        Obb point = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        assertEquals(4.0D, GeometryDistances.segmentToObbSquared(new Segment3d(new Vec3d(-1.0D, 2.0D, 0.0D), new Vec3d(1.0D, 2.0D, 0.0D)), point));
    }

    @Test
    void segmentToObbIsInvariantUnderEquivalentQuaternionSignAndRigidTransform() {
        Rotation3d rotation = new Rotation3d(0.2D, -0.3D, 0.4D, 0.5D);
        Rotation3d negative = new Rotation3d(-0.2D, 0.3D, -0.4D, -0.5D);
        Obb box = new Obb(new Vec3d(1.0D, -2.0D, 3.0D), new Vec3d(1.0D, 2.0D, 3.0D), rotation);
        Obb equivalent = new Obb(box.center(), box.halfExtents(), negative);
        Segment3d segment = new Segment3d(new Vec3d(-4.0D, 2.0D, 5.0D), new Vec3d(6.0D, -1.0D, -2.0D));
        double original = GeometryDistances.segmentToObbSquared(segment, box);
        GeometryTestSupport.assertClose(original, GeometryDistances.segmentToObbSquared(segment, equivalent));
        Vec3d translation = new Vec3d(11.0D, -7.0D, 5.0D);
        GeometryTestSupport.assertClose(original, GeometryDistances.segmentToObbSquared(translate(segment, translation), new Obb(box.center().add(translation), box.halfExtents(), box.orientation())));
    }

    @Test
    void segmentToObbPreservesSmallLocalGapBesideHugeCommonTranslation() {
        double shift = Double.MAX_VALUE;
        Obb point = new Obb(new Vec3d(1.0E-16D, shift, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Segment3d segment = new Segment3d(new Vec3d(0.0D, shift, 0.0D), new Vec3d(0.0D, shift, 0.0D));
        double result = GeometryDistances.segmentToObbSquared(segment, point);
        assertTrue(result > 0.0D);
        GeometryTestSupport.assertClose(1.0E-32D, result);
    }

    @Test
    void perAxisScalingRetainsTinyTransverseGapBesideMaximalSegmentDirection() {
        Segment3d segment = new Segment3d(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(1.0E-16D, 0.0D, Double.MAX_VALUE));
        Aabb line = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, Double.MAX_VALUE));
        Obb equivalentLine = new Obb(new Vec3d(0.0D, 0.0D, Double.MAX_VALUE / 2.0D), new Vec3d(0.0D, 0.0D, Double.MAX_VALUE / 2.0D), Rotation3d.identity());
        double expected = 1.0E-32D;

        assertEquals(expected, GeometryDistances.segmentToAabbSquared(segment, line), Math.ulp(expected) * 4.0D);
        assertEquals(expected, GeometryDistances.segmentToObbSquared(segment, equivalentLine), Math.ulp(expected) * 4.0D);
        assertFalse(GeometryIntersections.intersects(segment, line));
        assertFalse(GeometryIntersections.intersects(segment, equivalentLine));
    }

    @Test
    void obbProjectionScalesActualTinyBasisProductsRatherThanRawHugeComponents() {
        Rotation3d rotation = new Rotation3d(0.0D, 0.0D, Double.MIN_VALUE, 1.0D);
        Obb box = new Obb(new Vec3d(0.0D, Double.MAX_VALUE / 2.0D, 0.0D), new Vec3d(0.0D, Double.MAX_VALUE / 2.0D, 0.0D), rotation);
        Segment3d segment = new Segment3d(new Vec3d(1.0E-14D, 0.0D, 0.0D), new Vec3d(1.0E-14D, Double.MAX_VALUE, 0.0D));
        double expected = Phase1BTestSupport.segmentToObbSquaredByLocalOracle(segment, box);
        double actual = GeometryDistances.segmentToObbSquared(segment, box);

        assertTrue(!Double.isNaN(actual));
        Phase1BTestSupport.assertDistanceClose(expected, actual, "tiny-basis local Aabb oracle, expected=" + expected + ", actual=" + actual);

        Capsule worldCapsule = new Capsule(segment, 9.2E-15D);
        Rotation3d inverse = box.orientation().inverse();
        Capsule localCapsule = new Capsule(new Segment3d(inverse.rotate(segment.start().subtract(box.center())), inverse.rotate(segment.end().subtract(box.center()))), worldCapsule.radius());
        Vec3d half = box.halfExtents();
        Aabb localBox = new Aabb(half.multiply(-1.0D), half);
        assertTrue(GeometryIntersections.intersects(localCapsule, localBox), "independent local capsule/Aabb reference");
        assertTrue(GeometryIntersections.intersects(worldCapsule, box), "world capsule/Obb must agree with its local Aabb representation");
    }

    private static Segment3d translate(Segment3d segment, Vec3d translation) { return new Segment3d(segment.start().add(translation), segment.end().add(translation)); }
    private static void assertCanonicalZero(double value) { assertEquals(0.0D, value); assertEquals(0L, Double.doubleToRawLongBits(value)); }
}
