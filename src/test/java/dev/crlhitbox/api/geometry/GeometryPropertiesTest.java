package dev.crlhitbox.api.geometry;

import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryPropertiesTest {
    private static final int ITERATIONS = 1024;
    private static final long ROTATION_AND_BOUNDS_SEED = 0x5EED_1001L;
    private static final long DISTANCE_SEED = 0x5EED_1002L;
    private static final long INTERSECTION_SEED = 0x5EED_1003L;

    @Test
    void foundationBoundaryRegressionsRemainCorrect() {
        assertEquals(Double.MIN_VALUE, new Aabb(new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D), new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D)).center().x());
        assertEquals(new Rotation3d(1.0D, 2.0D, 3.0D, 4.0D), new Rotation3d(1.0D, 2.0D, 3.0D, 4.0D).inverse().inverse());
        assertEquals(1.0E200D, new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0E200D, 0.0D, 0.0D)), 0.0D).centerlineLength());
    }

    @Test
    void rotationInverseLengthAndObbCornerPropertiesUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(ROTATION_AND_BOUNDS_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Rotation3d rotation = rotation(random);
            Vec3d vector = vector(random);
            Vec3d half = new Vec3d(random.nextDouble(0.0D, 3.0D), random.nextDouble(0.0D, 3.0D), random.nextDouble(0.0D, 3.0D));
            Vec3d center = vector(random);
            Obb obb = new Obb(center, half, rotation);
            String inputs = failure(ROTATION_AND_BOUNDS_SEED, iteration, "rotation=" + rotation + ", vector=" + vector + ", center=" + center + ", half=" + half + ", obb=" + obb);
            GeometryTestSupport.assertVectorClose(vector, rotation.inverseRotate(rotation.rotate(vector)), inputs);
            GeometryTestSupport.assertClose(vector.lengthSquared(), rotation.rotate(vector).lengthSquared(), inputs);
            for (int signs = 0; signs < 8; signs++) {
                Vec3d corner = new Vec3d((signs & 1) == 0 ? -half.x() : half.x(), (signs & 2) == 0 ? -half.y() : half.y(), (signs & 4) == 0 ? -half.z() : half.z());
                assertTrue(obb.bounds().contains(obb.localToWorld(corner)), inputs + ", corner=" + corner);
            }
        }
    }

    @Test
    void segmentDistanceSymmetryOrderTranslationRotationAndScalingUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(DISTANCE_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Segment3d first = new Segment3d(vector(random), vector(random));
            Segment3d second = new Segment3d(vector(random), vector(random));
            double distance = GeometryDistances.segmentToSegmentSquared(first, second);
            String inputs = failure(DISTANCE_SEED, iteration, "first=" + first + ", second=" + second);
            GeometryTestSupport.assertClose(distance, GeometryDistances.segmentToSegmentSquared(second, first), inputs);
            GeometryTestSupport.assertClose(distance, GeometryDistances.segmentToSegmentSquared(new Segment3d(first.end(), first.start()), second), inputs);
            Vec3d translation = vector(random);
            GeometryTestSupport.assertClose(distance, GeometryDistances.segmentToSegmentSquared(translate(first, translation), translate(second, translation)), inputs + ", translation=" + translation);
            Rotation3d rotation = rotation(random);
            GeometryTestSupport.assertClose(distance, GeometryDistances.segmentToSegmentSquared(rotate(first, rotation), rotate(second, rotation)), inputs + ", rotation=" + rotation);
            double scale = random.nextDouble(0.25D, 3.0D);
            GeometryTestSupport.assertClose(distance * scale * scale, GeometryDistances.segmentToSegmentSquared(scale(first, scale), scale(second, scale)), inputs + ", scale=" + scale);
        }
    }

    @Test
    void symmetricIntersectionsLocalObbSlabAndDegenerateCapsulePropertiesUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(INTERSECTION_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Segment3d segment = new Segment3d(vector(random), vector(random));
            Sphere sphere = new Sphere(vector(random), random.nextDouble(0.0D, 2.0D));
            Aabb box = aabb(random);
            Rotation3d rotation = rotation(random);
            Obb obb = new Obb(vector(random), new Vec3d(random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D)), rotation);
            Capsule capsule = new Capsule(new Segment3d(vector(random), vector(random)), random.nextDouble(0.0D, 2.0D));
            String allInputs = failure(INTERSECTION_SEED, iteration, "segment=" + segment + ", sphere=" + sphere + ", box=" + box + ", rotation=" + rotation + ", obb=" + obb + ", capsule=" + capsule);
            assertEquals(GeometryIntersections.intersects(segment, sphere), GeometryIntersections.intersects(sphere, segment), allInputs + ", pair=segment/sphere");
            assertEquals(GeometryIntersections.intersects(segment, box), GeometryIntersections.intersects(box, segment), allInputs + ", pair=segment/aabb");
            assertEquals(GeometryIntersections.intersects(segment, obb), GeometryIntersections.intersects(obb, segment), allInputs + ", pair=segment/obb");
            assertEquals(GeometryIntersections.intersects(segment, capsule), GeometryIntersections.intersects(capsule, segment), allInputs + ", pair=segment/capsule");
            assertEquals(GeometryIntersections.intersects(sphere, box), GeometryIntersections.intersects(box, sphere), allInputs + ", pair=sphere/aabb");
            assertEquals(GeometryIntersections.intersects(sphere, obb), GeometryIntersections.intersects(obb, sphere), allInputs + ", pair=sphere/obb");
            assertEquals(GeometryIntersections.intersects(capsule, sphere), GeometryIntersections.intersects(sphere, capsule), allInputs + ", pair=capsule/sphere");
            Segment3d local = new Segment3d(vector(random), vector(random));
            Aabb localBox = aabb(random);
            Obb localObb = new Obb(vector(random), localBox.halfExtents(), rotation);
            Segment3d world = new Segment3d(localObb.localToWorld(local.start()), localObb.localToWorld(local.end()));
            Aabb centeredLocal = new Aabb(localBox.min().subtract(localBox.center()), localBox.max().subtract(localBox.center()));
            Obb matchingObb = new Obb(localObb.center(), centeredLocal.halfExtents(), rotation);
            Segment3d matchingWorld = new Segment3d(matchingObb.localToWorld(local.start()), matchingObb.localToWorld(local.end()));
            assertEquals(GeometryIntersections.intersects(local, centeredLocal), GeometryIntersections.intersects(matchingWorld, matchingObb), allInputs + ", local=" + local + ", localBox=" + localBox + ", localObb=" + localObb + ", centeredLocal=" + centeredLocal + ", matchingWorld=" + matchingWorld + ", matchingObb=" + matchingObb);
            Capsule pointCapsule = new Capsule(new Segment3d(sphere.center(), sphere.center()), sphere.radius());
            Sphere thirdSphere = new Sphere(vector(random), random.nextDouble(0.0D, 2.0D));
            Capsule thirdCapsule = new Capsule(new Segment3d(vector(random), vector(random)), random.nextDouble(0.0D, 2.0D));
            Segment3d thirdSegment = new Segment3d(vector(random), vector(random));
            String equivalenceInputs = allInputs + ", pointCapsule=" + pointCapsule + ", thirdSphere=" + thirdSphere + ", thirdCapsule=" + thirdCapsule + ", thirdSegment=" + thirdSegment;
            assertEquals(GeometryIntersections.intersects(sphere, thirdSphere), GeometryIntersections.intersects(pointCapsule, thirdSphere), equivalenceInputs + ", point-capsule/sphere equivalence");
            assertEquals(GeometryIntersections.intersects(thirdCapsule, sphere), GeometryIntersections.intersects(thirdCapsule, pointCapsule), equivalenceInputs + ", point-capsule/capsule equivalence");
            assertEquals(GeometryIntersections.intersects(thirdSegment, sphere), GeometryIntersections.intersects(thirdSegment, pointCapsule), equivalenceInputs + ", point-capsule/segment equivalence");

            Vec3d translation = vector(random);
            Rotation3d transform = rotation(random);
            assertEquals(GeometryIntersections.intersects(segment, capsule), GeometryIntersections.intersects(translate(segment, translation), translate(capsule, translation)), allInputs + ", translation=" + translation + ", pair=segment/capsule");
            assertEquals(GeometryIntersections.intersects(sphere, obb), GeometryIntersections.intersects(rotate(sphere, transform), rotate(obb, transform)), allInputs + ", transform=" + transform + ", pair=sphere/obb");
        }
    }

    private static Vec3d vector(SplittableRandom random) { return new Vec3d(random.nextDouble(-5.0D, 5.0D), random.nextDouble(-5.0D, 5.0D), random.nextDouble(-5.0D, 5.0D)); }
    private static Rotation3d rotation(SplittableRandom random) { return new Rotation3d(random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D)); }
    private static Aabb aabb(SplittableRandom random) { Vec3d center = vector(random); Vec3d half = new Vec3d(random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D)); return new Aabb(center.subtract(half), center.add(half)); }
    private static Segment3d translate(Segment3d segment, Vec3d translation) { return new Segment3d(segment.start().add(translation), segment.end().add(translation)); }
    private static Segment3d rotate(Segment3d segment, Rotation3d rotation) { return new Segment3d(rotation.rotate(segment.start()), rotation.rotate(segment.end())); }
    private static Sphere rotate(Sphere sphere, Rotation3d rotation) { return new Sphere(rotation.rotate(sphere.center()), sphere.radius()); }
    private static Obb rotate(Obb obb, Rotation3d rotation) { return new Obb(rotation.rotate(obb.center()), obb.halfExtents(), compose(rotation, obb.orientation())); }
    private static Capsule translate(Capsule capsule, Vec3d translation) { return new Capsule(translate(capsule.centerline(), translation), capsule.radius()); }
    private static Segment3d scale(Segment3d segment, double scale) { return new Segment3d(segment.start().multiply(scale), segment.end().multiply(scale)); }
    private static Rotation3d compose(Rotation3d first, Rotation3d second) {
        return new Rotation3d(
                first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(),
                first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(),
                first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(),
                first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z()
        );
    }
    private static String failure(long seed, int iteration, String inputs) { return "seed=" + seed + ", iteration=" + iteration + ", " + inputs; }
}
