package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract matrix for every explicitly supported closed-set intersection pair. */
class GeometryIntersectionMatrixTest {
    private static final Vec3d ORIGIN = new Vec3d(0.0D, 0.0D, 0.0D);
    private static final Aabb UNIT_AABB = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
    private static final Obb UNIT_OBB = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());

    @Test
    void aabbAabbCoversFaceEdgeCornerOverlapContainmentAndPointDegeneracy() {
        assertPair(false, UNIT_AABB, new Aabb(new Vec3d(1.1D, -1.0D, -1.0D), new Vec3d(2.0D, 1.0D, 1.0D)), "separated");
        assertPair(true, UNIT_AABB, new Aabb(new Vec3d(1.0D, -1.0D, -1.0D), new Vec3d(2.0D, 1.0D, 1.0D)), "face touch");
        assertPair(true, UNIT_AABB, new Aabb(new Vec3d(1.0D, 1.0D, -1.0D), new Vec3d(2.0D, 2.0D, 1.0D)), "edge touch");
        assertPair(true, UNIT_AABB, new Aabb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(2.0D, 2.0D, 2.0D)), "corner touch");
        assertPair(true, UNIT_AABB, new Aabb(new Vec3d(0.5D, -0.5D, -0.5D), new Vec3d(2.0D, 0.5D, 0.5D)), "proper overlap");
        assertPair(true, UNIT_AABB, new Aabb(new Vec3d(-0.5D, -0.5D, -0.5D), new Vec3d(0.5D, 0.5D, 0.5D)), "contained");
        assertPair(true, UNIT_AABB, pointAabb(1.0D, 1.0D, 1.0D), "point degeneracy");
    }

    @Test
    void sphereSphereCoversAllClosedSetRelationsAndPointDegeneracy() {
        Sphere unit = new Sphere(ORIGIN, 1.0D);
        assertPair(false, unit, new Sphere(new Vec3d(2.1D, 0.0D, 0.0D), 1.0D), "separated");
        assertPair(true, unit, new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D), "tangent");
        assertPair(true, unit, new Sphere(new Vec3d(1.5D, 0.0D, 0.0D), 1.0D), "proper overlap");
        assertPair(true, unit, new Sphere(ORIGIN, 0.5D), "contained");
        assertPair(true, unit, new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 0.0D), "point degeneracy");
    }

    @Test
    void sphereAabbCoversFaceTangentOverlapContainmentAndPointDegeneracy() {
        Sphere unit = new Sphere(ORIGIN, 1.0D);
        assertPair(false, unit, new Aabb(new Vec3d(1.1D, -0.1D, -0.1D), new Vec3d(2.0D, 0.1D, 0.1D)), "separated");
        assertPair(true, unit, pointAabb(1.0D, 0.0D, 0.0D), "tangent point box");
        assertPair(true, unit, new Aabb(new Vec3d(0.5D, -0.5D, -0.5D), new Vec3d(2.0D, 0.5D, 0.5D)), "proper overlap");
        assertPair(true, new Sphere(ORIGIN, 3.0D), UNIT_AABB, "box contained by sphere");
        assertPair(true, new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 0.0D), UNIT_AABB, "zero-radius sphere point");
    }

    @Test
    void sphereObbCoversTangentOverlapContainmentAndPointDegeneracy() {
        Sphere unit = new Sphere(ORIGIN, 1.0D);
        assertPair(false, unit, new Obb(new Vec3d(2.1D, 0.0D, 0.0D), ORIGIN, Rotation3d.identity()), "separated");
        assertPair(true, unit, new Obb(new Vec3d(1.0D, 0.0D, 0.0D), ORIGIN, Rotation3d.identity()), "tangent point obb");
        assertPair(true, unit, new Obb(new Vec3d(1.5D, 0.0D, 0.0D), new Vec3d(1.0D, 0.25D, 0.25D), Rotation3d.identity()), "proper overlap");
        assertPair(true, new Sphere(ORIGIN, 3.0D), UNIT_OBB, "contained obb");
        assertPair(true, new Sphere(new Vec3d(1.0D, 1.0D, 1.0D), 0.0D), UNIT_OBB, "zero-radius boundary point");
    }

    @Test
    void capsuleSphereCoversSideAndEndpointTangencyOverlapContainmentAndDegeneracy() {
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 1.0D);
        assertPair(false, capsule, new Sphere(new Vec3d(0.0D, 2.1D, 0.0D), 1.0D), "separated");
        assertPair(true, capsule, new Sphere(new Vec3d(0.0D, 3.0D, 0.0D), 2.0D), "side tangent");
        assertPair(true, capsule, new Sphere(new Vec3d(3.0D, 0.0D, 0.0D), 1.0D), "endpoint tangent");
        assertPair(true, capsule, new Sphere(new Vec3d(0.0D, 1.5D, 0.0D), 1.0D), "proper overlap");
        assertPair(true, capsule, new Sphere(ORIGIN, 0.25D), "contained");
        assertPair(true, new Capsule(new Segment3d(ORIGIN, ORIGIN), 1.0D), new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D), "zero-length capsule sphere equivalence");
    }

    @Test
    void capsuleCapsuleCoversRelationsAndZeroRadiusSegmentEquivalence() {
        Capsule first = new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 1.0D);
        assertPair(false, first, new Capsule(new Segment3d(new Vec3d(-1.0D, 3.1D, 0.0D), new Vec3d(1.0D, 3.1D, 0.0D)), 1.0D), "separated");
        assertPair(true, first, new Capsule(new Segment3d(new Vec3d(-1.0D, 2.0D, 0.0D), new Vec3d(1.0D, 2.0D, 0.0D)), 1.0D), "side tangent");
        assertPair(true, first, new Capsule(new Segment3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D)), 1.0D), "endpoint tangent");
        assertPair(true, first, new Capsule(new Segment3d(new Vec3d(0.0D, 0.5D, 0.0D), new Vec3d(2.0D, 0.5D, 0.0D)), 1.0D), "proper overlap");
        assertPair(true, first, new Capsule(new Segment3d(new Vec3d(-0.5D, 0.0D, 0.0D), new Vec3d(0.5D, 0.0D, 0.0D)), 0.25D), "contained");
        assertPair(true, new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 0.0D), new Capsule(new Segment3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D)), 0.0D), "zero-radius segments touch");
    }

    @Test
    void segmentSphereCoversEndpointAndInteriorTangencyOverlapContainmentAndPointDegeneracy() {
        Segment3d segment = new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        assertPair(false, segment, new Sphere(new Vec3d(0.0D, 1.1D, 0.0D), 0.0D), "separated");
        assertPair(true, segment, new Sphere(new Vec3d(0.0D, 1.0D, 0.0D), 1.0D), "side tangent");
        assertPair(true, segment, new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D), "endpoint tangent");
        assertPair(true, segment, new Sphere(new Vec3d(0.0D, 0.5D, 0.0D), 1.0D), "proper overlap");
        assertPair(true, segment, new Sphere(ORIGIN, 2.0D), "contained");
        assertPair(true, new Segment3d(ORIGIN, ORIGIN), new Sphere(ORIGIN, 0.0D), "point segment and point sphere");
    }

    @Test
    void segmentAabbCoversEnterLeaveParallelFaceAndAllPointCases() {
        assertPair(false, new Segment3d(new Vec3d(-2.0D, 2.0D, 0.0D), new Vec3d(2.0D, 2.0D, 0.0D)), UNIT_AABB, "parallel outside");
        assertPair(true, new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D)), UNIT_AABB, "enter leave");
        assertPair(true, new Segment3d(new Vec3d(-1.0D, -1.0D, 0.0D), new Vec3d(1.0D, -1.0D, 0.0D)), UNIT_AABB, "on face");
        assertPair(true, new Segment3d(new Vec3d(-0.5D, 0.0D, 0.0D), new Vec3d(0.5D, 0.0D, 0.0D)), UNIT_AABB, "contained");
        assertPair(true, new Segment3d(ORIGIN, ORIGIN), UNIT_AABB, "point strictly inside");
        assertPair(true, new Segment3d(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(1.0D, 1.0D, 1.0D)), UNIT_AABB, "point on corner");
        assertPair(false, new Segment3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D)), UNIT_AABB, "point outside");
    }

    @Test
    void segmentObbUsesEquivalentLocalSlabAndCoversDegeneracy() {
        Rotation3d rotation = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        Obb obb = new Obb(new Vec3d(3.0D, -2.0D, 5.0D), new Vec3d(1.0D, 2.0D, 1.0D), rotation);
        Segment3d localCrossing = new Segment3d(new Vec3d(-3.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));
        Segment3d localOutside = new Segment3d(new Vec3d(-3.0D, 3.0D, 0.0D), new Vec3d(3.0D, 3.0D, 0.0D));
        assertPair(GeometryIntersections.intersects(localCrossing, new Aabb(new Vec3d(-1.0D, -2.0D, -1.0D), new Vec3d(1.0D, 2.0D, 1.0D))), new Segment3d(obb.localToWorld(localCrossing.start()), obb.localToWorld(localCrossing.end())), obb, "local slab equivalence crossing");
        assertPair(GeometryIntersections.intersects(localOutside, new Aabb(new Vec3d(-1.0D, -2.0D, -1.0D), new Vec3d(1.0D, 2.0D, 1.0D))), new Segment3d(obb.localToWorld(localOutside.start()), obb.localToWorld(localOutside.end())), obb, "local slab equivalence outside");
        assertPair(true, new Segment3d(obb.center(), obb.center()), obb, "point contained");
        assertPair(false, new Segment3d(obb.localToWorld(new Vec3d(2.0D, 0.0D, 0.0D)), obb.localToWorld(new Vec3d(2.0D, 0.0D, 0.0D))), obb, "point separated");
    }

    @Test
    void segmentCapsuleCoversEndpointAndSideTangencyOverlapContainmentAndPointDegeneracy() {
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 1.0D);
        assertPair(false, new Segment3d(new Vec3d(-1.0D, 1.1D, 0.0D), new Vec3d(1.0D, 1.1D, 0.0D)), capsule, "separated");
        assertPair(true, new Segment3d(new Vec3d(-1.0D, 1.0D, 0.0D), new Vec3d(1.0D, 1.0D, 0.0D)), capsule, "side tangency");
        assertPair(true, new Segment3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D)), capsule, "endpoint tangency");
        assertPair(true, new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D)), capsule, "proper overlap");
        assertPair(true, new Segment3d(new Vec3d(-0.5D, 0.0D, 0.0D), new Vec3d(0.5D, 0.0D, 0.0D)), capsule, "contained");
        assertPair(true, new Segment3d(ORIGIN, ORIGIN), new Capsule(new Segment3d(ORIGIN, ORIGIN), 0.0D), "point degeneracy");
    }

    private static Aabb pointAabb(double x, double y, double z) { Vec3d point = new Vec3d(x, y, z); return new Aabb(point, point); }
    private static void assertPair(boolean expected, Aabb first, Aabb second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " forward"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " reversed"); }
    private static void assertPair(boolean expected, Sphere first, Sphere second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " forward"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " reversed"); }
    private static void assertPair(boolean expected, Sphere first, Aabb second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " sphere/aabb"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " aabb/sphere"); }
    private static void assertPair(boolean expected, Sphere first, Obb second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " sphere/obb"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " obb/sphere"); }
    private static void assertPair(boolean expected, Capsule first, Sphere second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " capsule/sphere"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " sphere/capsule"); }
    private static void assertPair(boolean expected, Capsule first, Capsule second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " forward"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " reversed"); }
    private static void assertPair(boolean expected, Segment3d first, Sphere second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " segment/sphere"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " sphere/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Aabb second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " segment/aabb"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " aabb/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Obb second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " segment/obb"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " obb/segment"); }
    private static void assertPair(boolean expected, Segment3d first, Capsule second, String relation) { assertResult(expected, GeometryIntersections.intersects(first, second), relation + " segment/capsule"); assertResult(expected, GeometryIntersections.intersects(second, first), relation + " capsule/segment"); }
    private static void assertResult(boolean expected, boolean actual, String message) { if (expected) assertTrue(actual, message); else assertFalse(actual, message); }
}
