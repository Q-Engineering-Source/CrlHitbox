package dev.crlhitbox.api.geometry;

import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase1BPropertyTest {
    private static final long SEED = 0x5EED_1B02L;
    private static final long CAPSULE_BOX_SEED = 0x5EED_1B03L;
    private static final long BOX_SAT_SEED = 0x5EED_1B01L;
    private static final int ITERATIONS = 2048;

    @Test
    void segmentBoxDistancesAgreeWithIndependentOraclesAndMetamorphicTransforms() {
        SplittableRandom random = new SplittableRandom(SEED);
        Rotation3d quarterTurn = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Segment3d segment = new Segment3d(vector(random), vector(random));
            Aabb aabb = aabb(random);
            Rotation3d rotation = rotation(random);
            Obb obb = new Obb(vector(random), half(random), rotation);
            Vec3d translation = vector(random);
            double scale = random.nextDouble(0.25D, 3.0D);
            String context = "seed=" + SEED + ", iteration=" + iteration + ", segment=" + segment + ", aabb=" + aabb + ", rotation=" + rotation + ", obb=" + obb + ", translation=" + translation + ", scale=" + scale;

            double aabbDistance = GeometryDistances.segmentToAabbSquared(segment, aabb);
            double obbDistance = GeometryDistances.segmentToObbSquared(segment, obb);
            Phase1BTestSupport.assertDistanceClose(Phase1BTestSupport.segmentToAabbSquaredByDerivativeBisection(segment, aabb), aabbDistance, context + ", oracle=aabb");
            Phase1BTestSupport.assertDistanceClose(Phase1BTestSupport.segmentToObbSquaredByLocalOracle(segment, obb), obbDistance, context + ", oracle=obb-local");
            Phase1BTestSupport.assertDistanceClose(aabbDistance, GeometryDistances.segmentToAabbSquared(new Segment3d(segment.end(), segment.start()), aabb), context + ", endpoint-order=aabb");
            Phase1BTestSupport.assertDistanceClose(obbDistance, GeometryDistances.segmentToObbSquared(new Segment3d(segment.end(), segment.start()), obb), context + ", endpoint-order=obb");
            Phase1BTestSupport.assertDistanceClose(aabbDistance, GeometryDistances.segmentToAabbSquared(translate(segment, translation), translate(aabb, translation)), context + ", translation=aabb");
            Phase1BTestSupport.assertDistanceClose(obbDistance, GeometryDistances.segmentToObbSquared(translate(segment, translation), new Obb(obb.center().add(translation), obb.halfExtents(), obb.orientation())), context + ", translation=obb");
            Phase1BTestSupport.assertDistanceClose(aabbDistance, GeometryDistances.segmentToAabbSquared(rotateQuarterTurn(segment), rotateQuarterTurn(aabb)), context + ", rigid-rotation=aabb");
            Phase1BTestSupport.assertDistanceClose(obbDistance, GeometryDistances.segmentToObbSquared(rotateQuarterTurn(segment), new Obb(quarterTurn.rotate(obb.center()), obb.halfExtents(), compose(quarterTurn, obb.orientation()))), context + ", rigid-rotation=obb");
            Phase1BTestSupport.assertDistanceClose(aabbDistance * scale * scale, GeometryDistances.segmentToAabbSquared(scale(segment, scale), scale(aabb, scale)), context + ", positive-scale=aabb");
            Phase1BTestSupport.assertDistanceClose(obbDistance * scale * scale, GeometryDistances.segmentToObbSquared(scale(segment, scale), new Obb(obb.center().multiply(scale), obb.halfExtents().multiply(scale), obb.orientation())), context + ", positive-scale=obb");
        }
    }

    @Test
    void capsuleBoxesAgreeWithIndependentDistanceOraclesAndMetamorphicContracts() {
        SplittableRandom random = new SplittableRandom(CAPSULE_BOX_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Segment3d centerline = new Segment3d(vector(random), vector(random));
            double radius = random.nextDouble(0.0D, 2.0D);
            Capsule capsule = new Capsule(centerline, radius);
            Aabb aabb = aabb(random);
            Rotation3d rotation = rotation(random);
            Obb obb = new Obb(vector(random), half(random), rotation);
            Vec3d translation = vector(random);
            double scale = random.nextDouble(0.25D, 3.0D);
            String context = "seed=" + CAPSULE_BOX_SEED + ", iteration=" + iteration + ", centerline=" + centerline + ", radius=" + radius + ", capsule=" + capsule + ", aabb=" + aabb + ", rotation=" + rotation + ", obb=" + obb + ", translation=" + translation + ", scale=" + scale;

            boolean aabbExpected = Phase1BTestSupport.segmentToAabbSquaredByDerivativeBisection(centerline, aabb) <= radius * radius;
            boolean obbExpected = Phase1BTestSupport.segmentToObbSquaredByLocalOracle(centerline, obb) <= radius * radius;
            assertEquals(aabbExpected, GeometryIntersections.intersects(capsule, aabb), context + ", oracle=aabb");
            assertEquals(obbExpected, GeometryIntersections.intersects(capsule, obb), context + ", oracle=obb-local");
            assertEquals(GeometryIntersections.intersects(capsule, aabb), GeometryIntersections.intersects(aabb, capsule), context + ", reverse=aabb");
            assertEquals(GeometryIntersections.intersects(capsule, obb), GeometryIntersections.intersects(obb, capsule), context + ", reverse=obb");
            Capsule zeroRadius = new Capsule(centerline, 0.0D);
            assertEquals(GeometryIntersections.intersects(centerline, aabb), GeometryIntersections.intersects(zeroRadius, aabb), context + ", radius-zero=aabb");
            assertEquals(GeometryIntersections.intersects(centerline, obb), GeometryIntersections.intersects(zeroRadius, obb), context + ", radius-zero=obb");
            Sphere sphere = new Sphere(vector(random), radius);
            Capsule pointCapsule = new Capsule(new Segment3d(sphere.center(), sphere.center()), sphere.radius());
            assertEquals(GeometryIntersections.intersects(sphere, aabb), GeometryIntersections.intersects(pointCapsule, aabb), context + ", point-capsule=aabb, sphere=" + sphere);
            assertEquals(GeometryIntersections.intersects(sphere, obb), GeometryIntersections.intersects(pointCapsule, obb), context + ", point-capsule=obb, sphere=" + sphere);
            assertEquals(GeometryIntersections.intersects(capsule, aabb), GeometryIntersections.intersects(translate(capsule, translation), translate(aabb, translation)), context + ", translation=aabb");
            assertEquals(GeometryIntersections.intersects(capsule, obb), GeometryIntersections.intersects(translate(capsule, translation), new Obb(obb.center().add(translation), obb.halfExtents(), obb.orientation())), context + ", translation=obb");
            assertEquals(GeometryIntersections.intersects(capsule, aabb), GeometryIntersections.intersects(scale(capsule, scale), scale(aabb, scale)), context + ", positive-scale=aabb");
            assertEquals(GeometryIntersections.intersects(capsule, obb), GeometryIntersections.intersects(scale(capsule, scale), new Obb(obb.center().multiply(scale), obb.halfExtents().multiply(scale), obb.orientation())), context + ", positive-scale=obb");
        }
    }

    @Test
    void boxSatAgreesWithIndependentCornerOracleAndMetamorphicContracts() {
        SplittableRandom random = new SplittableRandom(BOX_SAT_SEED);
        Rotation3d quarterTurn = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D));
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Obb first = new Obb(vector(random), boxHalf(random, iteration), rotation(random));
            Obb second = new Obb(vector(random), boxHalf(random, iteration + 1), rotation(random));
            Aabb aabb = aabbWithDegeneracy(random, iteration);
            Vec3d translation = vector(random);
            double scale = random.nextDouble(0.25D, 3.0D);
            String context = "seed=" + BOX_SAT_SEED + ", iteration=" + iteration + ", first=" + first + ", second=" + second + ", aabb=" + aabb + ", translation=" + translation + ", scale=" + scale;

            boolean obbExpected = Phase1BTestSupport.obbObbSatByCornerProjection(first, second);
            boolean aabbExpected = Phase1BTestSupport.aabbObbSatByCornerProjection(aabb, second);
            assertEquals(obbExpected, GeometryIntersections.intersects(first, second), context + ", oracle=obb/obb");
            assertEquals(aabbExpected, GeometryIntersections.intersects(aabb, second), context + ", oracle=aabb/obb");
            assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(second, first), context + ", symmetry=obb/obb");
            assertEquals(GeometryIntersections.intersects(aabb, second), GeometryIntersections.intersects(second, aabb), context + ", reverse=aabb/obb");
            Obb identityAabb = new Obb(aabb.center(), aabb.halfExtents(), Rotation3d.identity());
            assertEquals(GeometryIntersections.intersects(aabb, second), GeometryIntersections.intersects(identityAabb, second), context + ", identity-aabb-equivalence");
            assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(translateObb(first, translation), translateObb(second, translation)), context + ", translation=obb/obb");
            assertEquals(GeometryIntersections.intersects(aabb, second), GeometryIntersections.intersects(translate(aabb, translation), translateObb(second, translation)), context + ", translation=aabb/obb");
            assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(rotateObb(first, quarterTurn), rotateObb(second, quarterTurn)), context + ", rotation=obb/obb");
            assertEquals(GeometryIntersections.intersects(aabb, second), GeometryIntersections.intersects(rotateQuarterTurn(aabb), rotateObb(second, quarterTurn)), context + ", rotation=aabb/obb");
            assertEquals(GeometryIntersections.intersects(first, second), GeometryIntersections.intersects(scaleObb(first, scale), scaleObb(second, scale)), context + ", positive-scale=obb/obb");
            assertEquals(GeometryIntersections.intersects(aabb, second), GeometryIntersections.intersects(scale(aabb, scale), scaleObb(second, scale)), context + ", positive-scale=aabb/obb");
        }
    }

    private static Vec3d vector(SplittableRandom random) { return new Vec3d(random.nextDouble(-5.0D, 5.0D), random.nextDouble(-5.0D, 5.0D), random.nextDouble(-5.0D, 5.0D)); }
    private static Vec3d half(SplittableRandom random) { return new Vec3d(random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D), random.nextDouble(0.0D, 2.0D)); }
    private static Rotation3d rotation(SplittableRandom random) { return new Rotation3d(random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D), random.nextDouble(-1.0D, 1.0D)); }
    private static Aabb aabb(SplittableRandom random) { Vec3d center = vector(random); Vec3d half = half(random); return new Aabb(center.subtract(half), center.add(half)); }
    private static Aabb aabbWithDegeneracy(SplittableRandom random, int iteration) { Vec3d center = vector(random); Vec3d half = boxHalf(random, iteration); return new Aabb(center.subtract(half), center.add(half)); }
    private static Vec3d boxHalf(SplittableRandom random, int iteration) { return new Vec3d(iteration % 7 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D), iteration % 11 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D), iteration % 13 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D)); }
    private static Segment3d translate(Segment3d segment, Vec3d translation) { return new Segment3d(segment.start().add(translation), segment.end().add(translation)); }
    private static Aabb translate(Aabb box, Vec3d translation) { return new Aabb(box.min().add(translation), box.max().add(translation)); }
    private static Capsule translate(Capsule capsule, Vec3d translation) { return new Capsule(translate(capsule.centerline(), translation), capsule.radius()); }
    private static Segment3d scale(Segment3d segment, double scale) { return new Segment3d(segment.start().multiply(scale), segment.end().multiply(scale)); }
    private static Aabb scale(Aabb box, double scale) { return new Aabb(box.min().multiply(scale), box.max().multiply(scale)); }
    private static Capsule scale(Capsule capsule, double scale) { return new Capsule(scale(capsule.centerline(), scale), capsule.radius() * scale); }
    private static Obb translateObb(Obb box, Vec3d translation) { return new Obb(box.center().add(translation), box.halfExtents(), box.orientation()); }
    private static Obb scaleObb(Obb box, double scale) { return new Obb(box.center().multiply(scale), box.halfExtents().multiply(scale), box.orientation()); }
    private static Obb rotateObb(Obb box, Rotation3d rotation) { return new Obb(rotation.rotate(box.center()), box.halfExtents(), compose(rotation, box.orientation())); }
    private static Segment3d rotateQuarterTurn(Segment3d segment) { Rotation3d rotation = new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)); return new Segment3d(rotation.rotate(segment.start()), rotation.rotate(segment.end())); }
    private static Aabb rotateQuarterTurn(Aabb box) { return new Aabb(new Vec3d(-box.max().y(), box.min().x(), box.min().z()), new Vec3d(-box.min().y(), box.max().x(), box.max().z())); }
    private static Rotation3d compose(Rotation3d first, Rotation3d second) { return new Rotation3d(first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(), first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(), first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(), first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z()); }
}
