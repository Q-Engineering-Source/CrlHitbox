package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryIntersectionsTest {
    @Test
    void aabbAndSpherePairsAreClosedAndSymmetric() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Aabb faceTouching = new Aabb(new Vec3d(1.0D, -2.0D, -2.0D), new Vec3d(2.0D, 2.0D, 2.0D));
        Aabb separated = new Aabb(new Vec3d(1.1D, -1.0D, -1.0D), new Vec3d(2.0D, 1.0D, 1.0D));
        Sphere tangent = new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D);
        Sphere contained = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Sphere otherTangent = new Sphere(new Vec3d(4.0D, 0.0D, 0.0D), 2.0D);

        assertTrue(GeometryIntersections.intersects(box, faceTouching));
        assertFalse(GeometryIntersections.intersects(box, separated));
        assertTrue(GeometryIntersections.intersects(tangent, box));
        assertTrue(GeometryIntersections.intersects(box, tangent));
        assertTrue(GeometryIntersections.intersects(contained, box));
        assertTrue(GeometryIntersections.intersects(box, contained));
        assertTrue(GeometryIntersections.intersects(tangent, otherTangent));
    }

    @Test
    void obbAndCapsuleSolidPairsHandleTouchingContainmentAndExtremeSquaredOverflow() {
        Obb obb = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Sphere tangent = new Sphere(new Vec3d(3.0D, 0.0D, 0.0D), 1.0D);
        Sphere inside = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-3.0D, 0.0D, 0.0D), new Vec3d(-1.0D, 0.0D, 0.0D)), 1.0D);
        Capsule touchingCapsule = new Capsule(new Segment3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(4.0D, 0.0D, 0.0D)), 1.0D);
        Sphere extremePoint = new Sphere(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), 0.0D);
        Capsule largeButSeparated = new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D)), Double.MAX_VALUE / 2.0D);

        assertTrue(GeometryIntersections.intersects(tangent, obb));
        assertTrue(GeometryIntersections.intersects(obb, tangent));
        assertTrue(GeometryIntersections.intersects(inside, obb));
        assertTrue(GeometryIntersections.intersects(capsule, new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D)));
        assertTrue(GeometryIntersections.intersects(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D), capsule));
        assertTrue(GeometryIntersections.intersects(capsule, touchingCapsule));
        assertFalse(GeometryIntersections.intersects(largeButSeparated, extremePoint));
    }

    @Test
    void finiteSegmentPairsUseClosedSlabsAndLocalObbCoordinates() {
        Segment3d enteringBox = new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D));
        Segment3d outsideParallel = new Segment3d(new Vec3d(-2.0D, 2.0D, 0.0D), new Vec3d(2.0D, 2.0D, 0.0D));
        Segment3d faceSegment = new Segment3d(new Vec3d(-1.0D, -1.0D, 0.0D), new Vec3d(1.0D, -1.0D, 0.0D));
        Segment3d pointOutside = new Segment3d(new Vec3d(3.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));
        Sphere sphere = new Sphere(new Vec3d(0.0D, 1.0D, 0.0D), 1.0D);
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Obb obb = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(0.0D, 1.0D, 0.0D), new Vec3d(0.0D, 2.0D, 0.0D)), 1.0D);

        assertTrue(GeometryIntersections.intersects(enteringBox, sphere));
        assertTrue(GeometryIntersections.intersects(sphere, enteringBox));
        assertTrue(GeometryIntersections.intersects(enteringBox, box));
        assertTrue(GeometryIntersections.intersects(box, enteringBox));
        assertFalse(GeometryIntersections.intersects(outsideParallel, box));
        assertTrue(GeometryIntersections.intersects(faceSegment, box));
        assertFalse(GeometryIntersections.intersects(pointOutside, box));
        assertTrue(GeometryIntersections.intersects(enteringBox, obb));
        assertTrue(GeometryIntersections.intersects(obb, enteringBox));
        assertTrue(GeometryIntersections.intersects(enteringBox, capsule));
        assertTrue(GeometryIntersections.intersects(capsule, enteringBox));
    }

    @Test
    void scaleNormalizedComparisonsHandleSquaredOverflowAndUnderflow() {
        Sphere huge = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), Double.MAX_VALUE);
        Aabb farPoint = new Aabb(new Vec3d(Double.MAX_VALUE / 2.0D, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE / 2.0D, 0.0D, 0.0D));
        Sphere originPoint = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Aabb subnormalPoint = new Aabb(new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D));
        Segment3d subnormalSegment = new Segment3d(new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D));

        assertTrue(GeometryIntersections.intersects(huge, farPoint));
        assertFalse(GeometryIntersections.intersects(originPoint, subnormalPoint));
        assertFalse(GeometryIntersections.intersects(originPoint, subnormalSegment));
    }

    @Test
    void normalizedRadiusComparisonsRetainSubnormalSeparationAndExtremeObbContainment() {
        Segment3d originPoint = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Capsule subnormalCapsule = new Capsule(new Segment3d(new Vec3d(3.0D * Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(3.0D * Double.MIN_VALUE, 0.0D, 0.0D)), Double.MIN_VALUE);
        Sphere subnormalSphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), Double.MIN_VALUE);
        Obb rotated = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Segment3d insideThenExtreme = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, 0.0D));

        assertFalse(GeometryIntersections.intersects(originPoint, subnormalCapsule));
        assertFalse(GeometryIntersections.intersects(subnormalCapsule, subnormalSphere));
        assertTrue(GeometryIntersections.intersects(insideThenExtreme, rotated));
    }

    @Test
    void relativeScalingPreservesTranslationEquivalenceForSmallSeparatedShapes() {
        double shift = Double.MAX_VALUE;
        Sphere unshiftedSphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0E-17D);
        Sphere shiftedSphere = new Sphere(new Vec3d(0.0D, shift, 0.0D), 1.0E-17D);
        Aabb unshiftedPointBox = new Aabb(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(1.0E-16D, 0.0D, 0.0D));
        Aabb shiftedPointBox = new Aabb(new Vec3d(1.0E-16D, shift, 0.0D), new Vec3d(1.0E-16D, shift, 0.0D));
        Sphere unshiftedPointSphere = new Sphere(new Vec3d(1.0E-16D, 0.0D, 0.0D), 0.0D);
        Sphere shiftedPointSphere = new Sphere(new Vec3d(1.0E-16D, shift, 0.0D), 0.0D);
        Obb unshiftedPointObb = new Obb(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Obb shiftedPointObb = new Obb(new Vec3d(1.0E-16D, shift, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Segment3d unshiftedPointSegment = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Segment3d shiftedPointSegment = new Segment3d(new Vec3d(0.0D, shift, 0.0D), new Vec3d(0.0D, shift, 0.0D));

        assertFalse(GeometryIntersections.intersects(unshiftedSphere, unshiftedPointBox));
        assertFalse(GeometryIntersections.intersects(shiftedSphere, shiftedPointBox));
        assertFalse(GeometryIntersections.intersects(unshiftedSphere, unshiftedPointSphere));
        assertFalse(GeometryIntersections.intersects(shiftedSphere, shiftedPointSphere));
        assertFalse(GeometryIntersections.intersects(unshiftedSphere, unshiftedPointObb));
        assertFalse(GeometryIntersections.intersects(shiftedSphere, shiftedPointObb));
        assertFalse(GeometryIntersections.intersects(unshiftedPointSegment, unshiftedPointObb));
        assertFalse(GeometryIntersections.intersects(shiftedPointSegment, shiftedPointObb));
    }

    @Test
    void exactlyReconstructedObbCornerIsClosedForSphereAndSegmentQueries() {
        Obb obb = reviewerCornerObb();
        Vec3d corner = obb.localToWorld(new Vec3d(-obb.halfExtents().x(), -obb.halfExtents().y(), -obb.halfExtents().z()));

        assertTrue(obb.contains(corner));
        assertTrue(GeometryIntersections.intersects(new Sphere(corner, 0.0D), obb));
        assertTrue(GeometryIntersections.intersects(new Segment3d(corner, corner), obb));
    }

    private static Obb reviewerCornerObb() {
        return new Obb(
                new Vec3d(-0.7082650730663893D, -6.731064378241012D, -9.70759371599838D),
                new Vec3d(7.52805839645041D, 3.4219451865795945D, 0.25667266740287853D),
                new Rotation3d(-0.005971366638466002D, -0.627596208544786D, 0.16834333991315123D, 0.7600972712143912D)
        );
    }
}
