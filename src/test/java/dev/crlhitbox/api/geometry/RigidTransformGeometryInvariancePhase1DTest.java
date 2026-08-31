package dev.crlhitbox.api.geometry;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RigidTransformGeometryInvariancePhase1DTest {
    private static final RigidTransform3d COMMON = new RigidTransform3d(
            new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
            new Vec3d(5.0D, -7.0D, 11.0D));

    @Test
    void everyPrimitiveKindPreservesGenericPrimitiveQueries() {
        Solid3d[] primitives = {
                controlledAabb(0.0D),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.5D),
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.1D, 0.2D, -0.1D, 0.9D)),
                new Capsule(new Segment3d(new Vec3d(-1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D)), 0.75D)
        };
        Solid3d near = new Sphere(new Vec3d(0.5D, 0.0D, 0.0D), 1.0D);
        Solid3d far = new Sphere(new Vec3d(20.0D, 0.0D, 0.0D), 1.0D);
        for (Solid3d primitive : primitives) {
            assertInvariant(primitive, near, "near " + primitive.getClass().getSimpleName());
            assertInvariant(primitive, far, "far " + primitive.getClass().getSimpleName());
        }
    }

    @Test
    void segmentPrimitiveQueriesPreserveRigidMotion() {
        Segment3d segment = new Segment3d(new Vec3d(-3.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));
        for (Solid3d primitive : List.of(
                controlledAabb(0.0D),
                new Sphere(new Vec3d(0.0D, 0.5D, 0.0D), 1.0D),
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), Rotation3d.identity()),
                new Capsule(new Segment3d(new Vec3d(-1.0D, 0.5D, 0.0D), new Vec3d(1.0D, 0.5D, 0.0D)), 1.0D))) {
            boolean expected = GeometryIntersections.intersects(segment, primitive);
            Segment3d transformedSegment = Phase1DTestSupport.transformSegment(COMMON, segment);
            Solid3d transformedPrimitive = Phase1DTestSupport.transformSolid(COMMON, primitive);
            assertEquals(expected, GeometryIntersections.intersects(transformedSegment, transformedPrimitive), primitive.toString());
        }
    }

    @Test
    void compositePrimitiveQueriesPreserveRigidMotionAndLeafOracleAgreement() {
        Composite composite = new Composite(List.of(
                controlledAabb(-6.0D),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                new Capsule(new Segment3d(new Vec3d(5.0D, 0.0D, 0.0D), new Vec3d(7.0D, 0.0D, 0.0D)), 0.5D)));
        Solid3d primitive = new Obb(new Vec3d(0.5D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        boolean expected = GeometryIntersections.intersects((Solid3d) composite, primitive);
        Composite transformedComposite = (Composite) Phase1DTestSupport.transformSolid(COMMON, composite);
        Solid3d transformedPrimitive = Phase1DTestSupport.transformSolid(COMMON, primitive);
        boolean actual = GeometryIntersections.intersects((Solid3d) transformedComposite, transformedPrimitive);

        assertEquals(expected, actual);
        assertEquals(Phase1CTestSupport.intersectsSolidOracle(transformedComposite, transformedPrimitive), actual);
    }

    @Test
    void compositeCompositeQueriesPreserveRigidMotionAndCartesianOracleAgreement() {
        Composite first = new Composite(List.of(
                controlledAabb(-5.0D),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D)));
        Composite second = new Composite(List.of(
                new Capsule(new Segment3d(new Vec3d(0.0D, -2.0D, 0.0D), new Vec3d(0.0D, 2.0D, 0.0D)), 0.5D),
                controlledAabb(7.0D)));
        boolean expected = GeometryIntersections.intersects((Solid3d) first, (Solid3d) second);
        Composite transformedFirst = (Composite) Phase1DTestSupport.transformSolid(COMMON, first);
        Composite transformedSecond = (Composite) Phase1DTestSupport.transformSolid(COMMON, second);
        boolean actual = GeometryIntersections.intersects((Solid3d) transformedFirst, (Solid3d) transformedSecond);

        assertEquals(expected, actual);
        assertEquals(Phase1CTestSupport.intersectsCompositeCompositeOracle(transformedFirst, transformedSecond), actual);
    }

    @Test
    void segmentCompositeQueriesPreserveRigidMotionAndLeafOracleAgreement() {
        Segment3d segment = new Segment3d(new Vec3d(-3.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));
        Composite composite = new Composite(List.of(
                controlledAabb(-8.0D),
                new Sphere(new Vec3d(0.0D, 0.5D, 0.0D), 1.0D),
                new Capsule(new Segment3d(new Vec3d(8.0D, 0.0D, 0.0D), new Vec3d(9.0D, 0.0D, 0.0D)), 0.5D)));
        boolean expected = GeometryIntersections.intersects(segment, (Solid3d) composite);
        Segment3d transformedSegment = Phase1DTestSupport.transformSegment(COMMON, segment);
        Composite transformedComposite = (Composite) Phase1DTestSupport.transformSolid(COMMON, composite);
        boolean actual = GeometryIntersections.intersects(transformedSegment, (Solid3d) transformedComposite);

        assertEquals(expected, actual);
        assertEquals(Phase1CTestSupport.intersectsSegmentCompositeOracle(transformedSegment, transformedComposite), actual);
    }

    @Test
    void squaredDistanceQueriesPreserveRigidMotionForModerateInputs() {
        Vec3d point = new Vec3d(2.0D, 3.0D, 4.0D);
        Segment3d first = new Segment3d(new Vec3d(-2.0D, 0.0D, 0.0D), new Vec3d(2.0D, 0.0D, 0.0D));
        Segment3d second = new Segment3d(new Vec3d(0.0D, -2.0D, 3.0D), new Vec3d(0.0D, 2.0D, 3.0D));
        Aabb box = controlledAabb(0.0D);
        Obb transformedBox = (Obb) Phase1DTestSupport.transformSolid(COMMON, box);
        Segment3d transformedFirst = Phase1DTestSupport.transformSegment(COMMON, first);
        Segment3d transformedSecond = Phase1DTestSupport.transformSegment(COMMON, second);
        Vec3d transformedPoint = COMMON.transformPoint(point);

        assertDistanceInvariant(
                GeometryDistances.pointToSegmentSquared(point, first),
                GeometryDistances.pointToSegmentSquared(transformedPoint, transformedFirst),
                "point/segment");
        assertDistanceInvariant(
                GeometryDistances.segmentToSegmentSquared(first, second),
                GeometryDistances.segmentToSegmentSquared(transformedFirst, transformedSecond),
                "segment/segment");
        assertDistanceInvariant(
                GeometryDistances.pointToAabbSquared(point, box),
                GeometryDistances.pointToObbSquared(transformedPoint, transformedBox),
                "point/aabb-as-controlled-obb");
        assertDistanceInvariant(
                GeometryDistances.segmentToAabbSquared(first, box),
                GeometryDistances.segmentToObbSquared(transformedFirst, transformedBox),
                "segment/aabb-as-controlled-obb");
    }

    @Test
    void translationOnlyAabbUsesDirectExactEndpointMapping() {
        Aabb box = new Aabb(new Vec3d(-2.0D, -1.0D, 0.0D), new Vec3d(4.0D, 3.0D, 6.0D));
        RigidTransform3d translation = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(8.0D, -4.0D, 2.0D));

        assertEquals(
                new Aabb(new Vec3d(6.0D, -5.0D, 2.0D), new Vec3d(12.0D, -1.0D, 8.0D)),
                Phase1DTestSupport.translateAabb(translation, box));
    }

    @Test
    void controlledDyadicAabbRotationIsTestOnlyIdentityOrientedObbMapping() {
        Aabb box = controlledAabb(0.0D);
        Sphere sphere = new Sphere(new Vec3d(0.5D, 0.0D, 0.0D), 0.75D);
        boolean expected = GeometryIntersections.intersects(box, sphere);
        Solid3d transformedBox = Phase1DTestSupport.transformSolid(COMMON, box);
        Solid3d transformedSphere = Phase1DTestSupport.transformSolid(COMMON, sphere);

        assertTrue(transformedBox instanceof Obb);
        assertEquals(expected, GeometryIntersections.intersects(transformedBox, transformedSphere));
    }

    @Test
    void compositeTestTransformationPreservesOrderAndDuplicates() {
        Sphere duplicate = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D);
        Composite composite = new Composite(List.of(duplicate, controlledAabb(4.0D), duplicate));
        Composite transformed = (Composite) Phase1DTestSupport.transformSolid(COMMON, composite);

        assertEquals(3, transformed.childCount());
        assertTrue(transformed.child(0) instanceof Sphere);
        assertTrue(transformed.child(1) instanceof Obb);
        assertTrue(transformed.child(2) instanceof Sphere);
        assertEquals(transformed.child(0), transformed.child(2));
        assertFalse(transformed.child(0) instanceof Composite);
    }

    private static void assertInvariant(Solid3d first, Solid3d second, String context) {
        boolean expected = GeometryIntersections.intersects(first, second);
        Solid3d transformedFirst = Phase1DTestSupport.transformSolid(COMMON, first);
        Solid3d transformedSecond = Phase1DTestSupport.transformSolid(COMMON, second);
        assertEquals(expected, GeometryIntersections.intersects(transformedFirst, transformedSecond), context);
    }

    private static void assertDistanceInvariant(double expected, double actual, String context) {
        Phase1DTestSupport.assertClose(expected, actual, Math.max(Math.abs(expected), Math.abs(actual)), context);
    }

    private static Aabb controlledAabb(double centerX) {
        return new Aabb(
                new Vec3d(centerX - 2.0D, -1.0D, -1.0D),
                new Vec3d(centerX + 2.0D, 1.0D, 1.0D));
    }
}
