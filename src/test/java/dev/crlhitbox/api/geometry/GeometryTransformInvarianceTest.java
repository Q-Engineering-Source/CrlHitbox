package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Deterministic rigid-motion invariance checks for every supported intersection pair. */
class GeometryTransformInvarianceTest {
    private static final Vec3d TRANSLATION = new Vec3d(5.0D, -7.0D, 11.0D);
    private static final Rotation3d QUARTER_TURN_Z = new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D);

    @Test
    void everySupportedPairPreservesOverlapUnderTranslationAndRigidRotation() {
        Aabb aabb = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Aabb overlappingAabb = new Aabb(new Vec3d(0.25D, -0.5D, -0.5D), new Vec3d(2.0D, 0.5D, 0.5D));
        Sphere sphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Sphere overlappingSphere = new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 1.0D);
        Obb obb = new Obb(new Vec3d(0.5D, 0.0D, 0.0D), new Vec3d(0.75D, 0.5D, 0.25D), new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D));
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 0.5D);
        Capsule overlappingCapsule = new Capsule(new Segment3d(new Vec3d(-1.0D, 0.75D, 0.0D), new Vec3d(1.0D, 0.75D, 0.0D)), 0.5D);
        Segment3d segment = new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D));

        assertInvariant(aabb, overlappingAabb, "Aabb/Aabb");
        assertInvariant(sphere, overlappingSphere, "Sphere/Sphere");
        assertInvariant(sphere, aabb, "Sphere/Aabb");
        assertInvariant(sphere, obb, "Sphere/Obb");
        assertInvariant(capsule, sphere, "Capsule/Sphere");
        assertInvariant(capsule, overlappingCapsule, "Capsule/Capsule");
        assertInvariant(segment, sphere, "Segment/Sphere");
        assertInvariant(segment, aabb, "Segment/Aabb");
        assertInvariant(segment, obb, "Segment/Obb");
        assertInvariant(segment, capsule, "Segment/Capsule");
    }

    @Test
    void rigidTransformValueCrossChecksExistingRepresentativeInvariance() {
        RigidTransform3d transform = new RigidTransform3d(QUARTER_TURN_Z, TRANSLATION);
        Sphere sphere = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Capsule capsule = new Capsule(
                new Segment3d(new Vec3d(-1.0D, 0.5D, 0.0D), new Vec3d(1.0D, 0.5D, 0.0D)),
                0.75D);
        boolean expected = GeometryIntersections.intersects(sphere, capsule);
        Solid3d transformedSphere = Phase1DTestSupport.transformSolid(transform, sphere);
        Solid3d transformedCapsule = Phase1DTestSupport.transformSolid(transform, capsule);

        assertEquals(expected, GeometryIntersections.intersects(transformedSphere, transformedCapsule));
    }

    private static void assertInvariant(Aabb first, Aabb second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Sphere first, Sphere second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Sphere first, Aabb second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Sphere first, Obb second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Capsule first, Sphere second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Capsule first, Capsule second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Segment3d first, Sphere second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Segment3d first, Aabb second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Segment3d first, Obb second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertInvariant(Segment3d first, Capsule second, String pair) {
        boolean expected = GeometryIntersections.intersects(first, second);
        assertExpectedOverlap(expected, pair);
        assertPair(expected, first, second, pair + " original");
        assertPair(expected, translate(first), translate(second), pair + " translated");
        assertPair(expected, rotate(first), rotate(second), pair + " rotated");
    }

    private static void assertPair(boolean expected, Aabb first, Aabb second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " forward"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " reverse"); }
    private static void assertPair(boolean expected, Sphere first, Sphere second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " forward"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " reverse"); }
    private static void assertPair(boolean expected, Sphere first, Aabb second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " sphere/aabb"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " aabb/sphere"); }
    private static void assertPair(boolean expected, Sphere first, Obb second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " sphere/obb"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " obb/sphere"); }
    private static void assertPair(boolean expected, Capsule first, Sphere second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " capsule/sphere"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " sphere/capsule"); }
    private static void assertPair(boolean expected, Capsule first, Capsule second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " forward"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " reverse"); }
    private static void assertPair(boolean expected, Segment3d first, Sphere second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " segment/sphere"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " sphere/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Aabb second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " segment/aabb"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " aabb/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Obb second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " segment/obb"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " obb/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Capsule second, String context) { assertEquals(expected, GeometryIntersections.intersects(first, second), context + " segment/capsule"); assertEquals(expected, GeometryIntersections.intersects(second, first), context + " capsule/segment"); }
    private static void assertExpectedOverlap(boolean expected, String pair) { assertEquals(true, expected, pair + " expected proper overlap"); }

    private static Aabb translate(Aabb box) { return new Aabb(box.min().add(TRANSLATION), box.max().add(TRANSLATION)); }
    private static Sphere translate(Sphere sphere) { return new Sphere(sphere.center().add(TRANSLATION), sphere.radius()); }
    private static Segment3d translate(Segment3d segment) { return new Segment3d(segment.start().add(TRANSLATION), segment.end().add(TRANSLATION)); }
    private static Capsule translate(Capsule capsule) { return new Capsule(translate(capsule.centerline()), capsule.radius()); }
    private static Obb translate(Obb obb) { return new Obb(obb.center().add(TRANSLATION), obb.halfExtents(), obb.orientation()); }
    private static Sphere rotate(Sphere sphere) { return new Sphere(QUARTER_TURN_Z.rotate(sphere.center()), sphere.radius()); }
    private static Segment3d rotate(Segment3d segment) { return new Segment3d(QUARTER_TURN_Z.rotate(segment.start()), QUARTER_TURN_Z.rotate(segment.end())); }
    private static Capsule rotate(Capsule capsule) { return new Capsule(rotate(capsule.centerline()), capsule.radius()); }
    private static Obb rotate(Obb obb) { return new Obb(QUARTER_TURN_Z.rotate(obb.center()), obb.halfExtents(), compose(QUARTER_TURN_Z, obb.orientation())); }

    private static Aabb rotate(Aabb box) {
        Vec3d min = null;
        Vec3d max = null;
        for (int signs = 0; signs < 8; signs++) {
            Vec3d corner = new Vec3d((signs & 1) == 0 ? box.min().x() : box.max().x(), (signs & 2) == 0 ? box.min().y() : box.max().y(), (signs & 4) == 0 ? box.min().z() : box.max().z());
            Vec3d rotated = QUARTER_TURN_Z.rotate(corner);
            min = min == null ? rotated : min.min(rotated);
            max = max == null ? rotated : max.max(rotated);
        }
        return new Aabb(min, max);
    }

    private static Rotation3d compose(Rotation3d first, Rotation3d second) {
        return new Rotation3d(
                first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(),
                first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(),
                first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(),
                first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z()
        );
    }
}
