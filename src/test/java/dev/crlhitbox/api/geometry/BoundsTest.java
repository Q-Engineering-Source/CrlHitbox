package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundsTest {
    @Test
    void aabbSubnormalDerivedValuesFollowMidpointAndDifferenceContracts() {
        Aabb subnormal = new Aabb(new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(2.0D * Double.MIN_VALUE, 0.0D, 0.0D));
        assertEquals(2.0D * Double.MIN_VALUE, subnormal.center().x(), "midpoint rounds to the representable upper subnormal");
        assertEquals((subnormal.max().x() - subnormal.min().x()) / 2.0D, subnormal.halfExtents().x(), "half extent must follow the difference contract");
    }

    @Test
    void identityObbAndCapsuleBoundsContainRepresentativeAxialAndRadialExtrema() {
        Obb identity = new Obb(new Vec3d(3.0D, -2.0D, 1.0D), new Vec3d(2.0D, 4.0D, 6.0D), Rotation3d.identity());
        assertEquals(new Aabb(new Vec3d(1.0D, -6.0D, -5.0D), new Vec3d(5.0D, 2.0D, 7.0D)), identity.bounds());

        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-2.0D, 1.0D, 3.0D), new Vec3d(4.0D, 1.0D, 3.0D)), 2.0D);
        Aabb bounds = capsule.bounds();
        assertEquals(new Aabb(new Vec3d(-4.0D, -1.0D, 1.0D), new Vec3d(6.0D, 3.0D, 5.0D)), bounds);
        assertTrue(bounds.contains(new Vec3d(-4.0D, 1.0D, 3.0D)), "axial start extreme");
        assertTrue(bounds.contains(new Vec3d(6.0D, 1.0D, 3.0D)), "axial end extreme");
        assertTrue(bounds.contains(new Vec3d(0.0D, -1.0D, 3.0D)), "radial y extreme");
        assertTrue(bounds.contains(new Vec3d(0.0D, 1.0D, 5.0D)), "radial z extreme");
    }

    @Test
    void boundsAreStableAndTranslationCovariantForEveryPrimitive() {
        Vec3d translation = new Vec3d(7.0D, -11.0D, 13.0D);
        Aabb aabb = new Aabb(new Vec3d(-2.0D, -1.0D, 0.0D), new Vec3d(3.0D, 4.0D, 5.0D));
        Sphere sphere = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 0.5D);
        Segment3d segment = new Segment3d(new Vec3d(-2.0D, 1.0D, 0.0D), new Vec3d(4.0D, -3.0D, 2.0D));
        Capsule capsule = new Capsule(segment, 0.5D);
        Rotation3d rotation = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        Obb obb = new Obb(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(2.0D, 1.0D, 3.0D), rotation);

        assertBoundsStableAndTranslated(aabb, new Aabb(aabb.min().add(translation), aabb.max().add(translation)), translation);
        assertBoundsStableAndTranslated(sphere, new Sphere(sphere.center().add(translation), sphere.radius()), translation);
        assertBoundsStableAndTranslated(segment, new Segment3d(segment.start().add(translation), segment.end().add(translation)), translation);
        assertBoundsStableAndTranslated(capsule, new Capsule(new Segment3d(segment.start().add(translation), segment.end().add(translation)), capsule.radius()), translation);
        assertBoundsStableAndTranslated(obb, new Obb(obb.center().add(translation), obb.halfExtents(), obb.orientation()), translation);
    }

    private static void assertBoundsStableAndTranslated(Bounded3d primitive, Bounded3d translated, Vec3d translation) {
        Aabb first = primitive.bounds();
        assertEquals(first, primitive.bounds(), "repeated bounds must be deterministic for " + primitive);
        assertEquals(new Aabb(first.min().add(translation), first.max().add(translation)), translated.bounds(), "translation covariance for " + primitive);
    }
    @Test
    void aabbSphereSegmentAndCapsuleBoundsUseClosedSemantics() {
        Aabb box = new Aabb(new Vec3d(-1.0D, 0.0D, 2.0D), new Vec3d(3.0D, 4.0D, 6.0D));
        Sphere sphere = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 2.0D);
        Segment3d segment = new Segment3d(new Vec3d(5.0D, 1.0D, -2.0D), new Vec3d(1.0D, 3.0D, 4.0D));
        Capsule capsule = new Capsule(segment, 0.5D);

        assertSame(box, box.bounds());
        assertEquals(new Vec3d(1.0D, 2.0D, 4.0D), box.center());
        assertEquals(new Vec3d(2.0D, 2.0D, 2.0D), box.halfExtents());
        assertTrue(box.contains(new Vec3d(-1.0D, 0.0D, 2.0D)));
        assertFalse(box.contains(new Vec3d(3.0D, 4.1D, 6.0D)));
        assertEquals(new Aabb(new Vec3d(-1.0D, 0.0D, 1.0D), new Vec3d(3.0D, 4.0D, 5.0D)), sphere.bounds());
        assertEquals(new Aabb(new Vec3d(1.0D, 1.0D, -2.0D), new Vec3d(5.0D, 3.0D, 4.0D)), segment.bounds());
        assertEquals(new Aabb(new Vec3d(0.5D, 0.5D, -2.5D), new Vec3d(5.5D, 3.5D, 4.5D)), capsule.bounds());
    }

    @Test
    void obbBoundsUseAbsoluteRotationMatrixAndContainEveryIndependentCorner() {
        double halfSqrt = Math.sqrt(0.5D);
        Obb obb = new Obb(new Vec3d(10.0D, -2.0D, 5.0D), new Vec3d(2.0D, 1.0D, 3.0D), new Rotation3d(0.0D, 0.0D, halfSqrt, halfSqrt));
        Aabb bounds = obb.bounds();

        GeometryTestSupport.assertVectorClose(new Vec3d(9.0D, -4.0D, 2.0D), bounds.min());
        GeometryTestSupport.assertVectorClose(new Vec3d(11.0D, 0.0D, 8.0D), bounds.max());
        for (int signs = 0; signs < 8; signs++) {
            Vec3d localCorner = new Vec3d((signs & 1) == 0 ? -2.0D : 2.0D, (signs & 2) == 0 ? -1.0D : 1.0D, (signs & 4) == 0 ? -3.0D : 3.0D);
            assertTrue(bounds.contains(obb.localToWorld(localCorner)), "corner index " + signs);
        }
        assertTrue(obb.contains(obb.localToWorld(new Vec3d(2.0D, -1.0D, 3.0D))));
        assertFalse(obb.contains(new Vec3d(10.0D, 1.0D, 5.0D)));
        GeometryTestSupport.assertVectorClose(new Vec3d(2.0D, -1.0D, 3.0D), obb.worldToLocal(obb.localToWorld(new Vec3d(2.0D, -1.0D, 3.0D))));
    }
}
