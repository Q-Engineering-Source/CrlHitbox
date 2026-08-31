package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapsuleBoxPhase1BTest {
    @Test
    void capsuleAndAabbUseTheWholeCenterlineRatherThanEndpointSpheres() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Capsule crossing = new Capsule(new Segment3d(new Vec3d(-3.0D, 2.0D, 0.0D), new Vec3d(3.0D, 2.0D, 0.0D)), 1.0D);
        Capsule separated = new Capsule(new Segment3d(new Vec3d(-3.0D, 2.1D, 0.0D), new Vec3d(3.0D, 2.1D, 0.0D)), 1.0D);

        assertTrue(GeometryIntersections.intersects(crossing, box));
        assertTrue(GeometryIntersections.intersects(box, crossing));
        assertFalse(GeometryIntersections.intersects(separated, box));
    }

    @Test
    void capsuleAndObbUseTheWholeCenterlineInObbLocalCoordinates() {
        Obb box = new Obb(new Vec3d(2.0D, -3.0D, 1.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Capsule tangent = new Capsule(new Segment3d(box.localToWorld(new Vec3d(-3.0D, 3.0D, 0.0D)), box.localToWorld(new Vec3d(3.0D, 3.0D, 0.0D))), 1.0D);

        assertTrue(GeometryIntersections.intersects(tangent, box));
        assertTrue(GeometryIntersections.intersects(box, tangent));
    }

    @Test
    void capsuleBoxFaceEdgeAndCornerTangenciesAreClosed() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        assertTrue(GeometryIntersections.intersects(new Capsule(new Segment3d(new Vec3d(-2.0D, 2.0D, 0.0D), new Vec3d(2.0D, 2.0D, 0.0D)), 1.0D), box));
        assertTrue(GeometryIntersections.intersects(new Capsule(new Segment3d(new Vec3d(-2.0D, 2.0D, 1.0D), new Vec3d(2.0D, 2.0D, 1.0D)), 1.0D), box));
        assertTrue(GeometryIntersections.intersects(new Capsule(new Segment3d(new Vec3d(2.0D, 1.0D, 1.0D), new Vec3d(2.0D, 1.0D, 1.0D)), 1.0D), box));
    }

    @Test
    void capsuleBoxesSupportPlaneLinePointAndContainment() {
        Aabb plane = new Aabb(new Vec3d(-1.0D, -1.0D, 0.0D), new Vec3d(1.0D, 1.0D, 0.0D));
        Aabb line = new Aabb(new Vec3d(0.0D, 0.0D, -1.0D), new Vec3d(0.0D, 0.0D, 1.0D));
        Aabb point = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Capsule contained = new Capsule(new Segment3d(new Vec3d(-0.5D, 0.0D, 0.0D), new Vec3d(0.5D, 0.0D, 0.0D)), 0.0D);
        Capsule aroundLine = new Capsule(new Segment3d(new Vec3d(-2.0D, 1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 0.0D)), 1.0D);
        assertTrue(GeometryIntersections.intersects(contained, plane));
        assertTrue(GeometryIntersections.intersects(aroundLine, line));
        assertTrue(GeometryIntersections.intersects(new Capsule(new Segment3d(new Vec3d(-2.0D, 1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 0.0D)), 1.0D), point));
    }

    @Test
    void capsuleRadiusZeroAndPointCenterlineMatchSegmentAndSphereQueries() {
        Aabb aabb = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Obb obb = new Obb(new Vec3d(2.0D, 1.0D, -3.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        Segment3d segment = new Segment3d(new Vec3d(-3.0D, 1.0D, 0.0D), new Vec3d(3.0D, 1.0D, 0.0D));
        Capsule zeroRadius = new Capsule(segment, 0.0D);
        Sphere sphere = new Sphere(new Vec3d(3.0D, 1.0D, -3.0D), 0.5D);
        Capsule pointCapsule = new Capsule(new Segment3d(sphere.center(), sphere.center()), sphere.radius());
        assertEquals(GeometryIntersections.intersects(segment, aabb), GeometryIntersections.intersects(zeroRadius, aabb));
        assertEquals(GeometryIntersections.intersects(segment, obb), GeometryIntersections.intersects(zeroRadius, obb));
        assertEquals(GeometryIntersections.intersects(sphere, aabb), GeometryIntersections.intersects(pointCapsule, aabb));
        assertEquals(GeometryIntersections.intersects(sphere, obb), GeometryIntersections.intersects(pointCapsule, obb));
    }

    @Test
    void capsuleBoxesPreserveTranslationScalingAndRigidObbTransforms() {
        Obb box = new Obb(new Vec3d(2.0D, -3.0D, 1.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.2D, -0.3D, 0.4D, 0.5D));
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-3.0D, 1.0D, 0.0D), new Vec3d(4.0D, 1.0D, 0.0D)), 0.75D);
        boolean expected = GeometryIntersections.intersects(capsule, box);
        Vec3d translation = new Vec3d(8.0D, -7.0D, 5.0D);
        assertEquals(expected, GeometryIntersections.intersects(translate(capsule, translation), new Obb(box.center().add(translation), box.halfExtents(), box.orientation())));
        double scale = 2.0D;
        assertEquals(expected, GeometryIntersections.intersects(scale(capsule, scale), new Obb(box.center().multiply(scale), box.halfExtents().multiply(scale), box.orientation())));
        Rotation3d quarter = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        assertEquals(expected, GeometryIntersections.intersects(rotate(capsule, quarter), new Obb(quarter.rotate(box.center()), box.halfExtents(), compose(quarter, box.orientation()))));
    }

    @Test
    void capsuleBoxRadiusComparisonAvoidsSubnormalAndSquaredOverflowErrors() {
        Capsule subnormal = new Capsule(new Segment3d(new Vec3d(3.0D * Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(3.0D * Double.MIN_VALUE, 0.0D, 0.0D)), Double.MIN_VALUE);
        Aabb originPoint = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D));
        Obb originObbPoint = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Capsule hugeButSeparated = new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D)), Double.MAX_VALUE / 2.0D);
        Aabb farPoint = new Aabb(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));
        assertFalse(GeometryIntersections.intersects(subnormal, originPoint));
        assertFalse(GeometryIntersections.intersects(subnormal, originObbPoint));
        assertFalse(GeometryIntersections.intersects(hugeButSeparated, farPoint));
    }

    @Test
    void capsuleBoxesRetainTinyTransverseGapBesideMaximalCenterlineDirection() {
        Segment3d centerline = new Segment3d(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(1.0E-16D, 0.0D, Double.MAX_VALUE));
        Capsule capsule = new Capsule(centerline, 5.0E-17D);
        Aabb line = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, Double.MAX_VALUE));
        Obb equivalentLine = new Obb(new Vec3d(0.0D, 0.0D, Double.MAX_VALUE / 2.0D), new Vec3d(0.0D, 0.0D, Double.MAX_VALUE / 2.0D), Rotation3d.identity());

        assertFalse(GeometryIntersections.intersects(capsule, line));
        assertFalse(GeometryIntersections.intersects(capsule, equivalentLine));
    }

    private static Capsule translate(Capsule capsule, Vec3d translation) { return new Capsule(new Segment3d(capsule.centerline().start().add(translation), capsule.centerline().end().add(translation)), capsule.radius()); }
    private static Capsule scale(Capsule capsule, double scale) { return new Capsule(new Segment3d(capsule.centerline().start().multiply(scale), capsule.centerline().end().multiply(scale)), capsule.radius() * scale); }
    private static Capsule rotate(Capsule capsule, Rotation3d rotation) { return new Capsule(new Segment3d(rotation.rotate(capsule.centerline().start()), rotation.rotate(capsule.centerline().end())), capsule.radius()); }
    private static Rotation3d compose(Rotation3d first, Rotation3d second) { return new Rotation3d(first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(), first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(), first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(), first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z()); }
}
