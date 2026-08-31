package dev.crlhitbox.api.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1CPropertyTest {
    private static final long GENERIC_PRIMITIVE_SEED = 0x5EED_1C01L;
    private static final long COMPOSITE_UNION_SEED = 0x5EED_1C02L;
    private static final long COMPOSITE_QUERY_SEED = 0x5EED_1C03L;
    private static final int ITERATIONS = 2048;

    @Test
    void genericPrimitiveDispatchAgreesWithTypedKernelsForFixedSeed() {
        SplittableRandom random = new SplittableRandom(GENERIC_PRIMITIVE_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Solid3d first = primitive(random, iteration & 3, iteration);
            Solid3d second = primitive(random, iteration >>> 2 & 3, iteration + 1);
            boolean expected = Phase1CTestSupport.intersectsPrimitiveTyped(first, second);
            boolean actual = GeometryIntersections.intersects(first, second);
            assertProperty(
                    GENERIC_PRIMITIVE_SEED,
                    iteration,
                    "first=" + first + ", second=" + second,
                    "[]",
                    "Solid3d/Solid3d",
                    "first,second",
                    expected,
                    actual,
                    first.bounds(),
                    second.bounds());

            boolean reverseExpected = Phase1CTestSupport.intersectsPrimitiveTyped(second, first);
            boolean reverseActual = GeometryIntersections.intersects(second, first);
            assertProperty(
                    GENERIC_PRIMITIVE_SEED,
                    iteration,
                    "first=" + first + ", second=" + second,
                    "[]",
                    "Solid3d/Solid3d",
                    "second,first",
                    reverseExpected,
                    reverseActual,
                    second.bounds(),
                    first.bounds());
            assertProperty(
                    GENERIC_PRIMITIVE_SEED,
                    iteration,
                    "first=" + first + ", second=" + second,
                    "[]",
                    "Solid3d/Solid3d",
                    "symmetry",
                    expected,
                    reverseActual,
                    first.bounds(),
                    second.bounds());
        }
    }

    @Test
    void compositeUnionFlattenBoundsAndTransformsUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(COMPOSITE_UNION_SEED);
        Rotation3d rigidRotation = new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            List<Solid3d> leaves = primitiveLeaves(random, iteration);
            Composite flat = new Composite(leaves);
            Composite nested = nestedComposite(leaves);
            Solid3d query = primitive(random, iteration + 2 & 3, iteration + 11);
            String canonical = Phase1CTestSupport.canonicalLeaves(flat);

            assertObjectProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    canonical,
                    "Composite/Composite value",
                    "nested,flat",
                    flat,
                    nested,
                    flat.bounds(),
                    nested.bounds());
            assertObjectProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    canonical,
                    "Composite bounds",
                    "nested,flat",
                    flat.bounds(),
                    nested.bounds(),
                    flat.bounds(),
                    nested.bounds());

            boolean expected = Phase1CTestSupport.intersectsSolidOracle(flat, query);
            boolean actual = GeometryIntersections.intersects((Solid3d) nested, query);
            assertProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    canonical,
                    "Solid3d/Solid3d",
                    "nested,query",
                    expected,
                    actual,
                    nested.bounds(),
                    query.bounds());

            List<Solid3d> reversedLeaves = new ArrayList<>(leaves);
            Collections.reverse(reversedLeaves);
            Composite permuted = new Composite(reversedLeaves);
            assertObjectProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    canonical,
                    "Composite bounds",
                    "permuted",
                    flat.bounds(),
                    permuted.bounds(),
                    flat.bounds(),
                    permuted.bounds());
            boolean permutedActual = GeometryIntersections.intersects((Solid3d) permuted, query);
            assertProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    Phase1CTestSupport.canonicalLeaves(permuted),
                    "Solid3d/Solid3d",
                    "permuted,query",
                    expected,
                    permutedActual,
                    permuted.bounds(),
                    query.bounds());

            List<Solid3d> duplicatedLeaves = new ArrayList<>(leaves);
            duplicatedLeaves.add(leaves.getFirst());
            Composite duplicated = new Composite(duplicatedLeaves);
            assertObjectProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    Phase1CTestSupport.canonicalLeaves(duplicated),
                    "Composite bounds",
                    "duplicate insertion",
                    flat.bounds(),
                    duplicated.bounds(),
                    flat.bounds(),
                    duplicated.bounds());
            boolean duplicatedActual = GeometryIntersections.intersects((Solid3d) duplicated, query);
            assertProperty(
                    COMPOSITE_UNION_SEED,
                    iteration,
                    "query=" + query,
                    Phase1CTestSupport.canonicalLeaves(duplicated),
                    "Solid3d/Solid3d",
                    "duplicated,query",
                    expected,
                    duplicatedActual,
                    duplicated.bounds(),
                    query.bounds());

            Vec3d translation = vector(random, -3.0D, 3.0D);
            assertTransformInvariant(COMPOSITE_UNION_SEED, iteration, flat, query,
                    transformComposite(flat, leaf -> translate(leaf, translation)),
                    translate(query, translation), "translation=" + translation, expected);

            assertTransformInvariant(COMPOSITE_UNION_SEED, iteration, flat, query,
                    transformComposite(flat, leaf -> rotate(leaf, rigidRotation)),
                    rotate(query, rigidRotation), "rotation=" + rigidRotation, expected);

            double scale = random.nextDouble(0.25D, 3.0D);
            assertTransformInvariant(COMPOSITE_UNION_SEED, iteration, flat, query,
                    transformComposite(flat, leaf -> scale(leaf, scale)),
                    scale(query, scale), "scale=" + scale, expected);

            for (int childIndex = 0; childIndex < flat.childCount(); childIndex++) {
                Solid3d child = flat.child(childIndex);
                String inputs = failure(
                        COMPOSITE_UNION_SEED,
                        iteration,
                        "child=" + child + ", childIndex=" + childIndex,
                        canonical,
                        "Composite.bounds",
                        "containment",
                        true,
                        flat.bounds().contains(child.bounds().min()) && flat.bounds().contains(child.bounds().max()),
                        flat.bounds(),
                        child.bounds());
                assertTrue(flat.bounds().contains(child.bounds().min()), inputs);
                assertTrue(flat.bounds().contains(child.bounds().max()), inputs);
            }
        }
    }

    @Test
    void compositeCartesianAndSegmentLeafOraclesUseFixedSeed() {
        SplittableRandom random = new SplittableRandom(COMPOSITE_QUERY_SEED);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            Composite first = randomComposite(random, iteration);
            Composite second = randomComposite(random, iteration + 17);
            Segment3d segment = randomSegment(random, iteration);
            String primitives = "first=" + first + ", second=" + second + ", segment=" + segment;
            String canonical = "first=" + Phase1CTestSupport.canonicalLeaves(first)
                    + ", second=" + Phase1CTestSupport.canonicalLeaves(second);

            boolean expected = Phase1CTestSupport.intersectsCompositeCompositeOracle(first, second);
            boolean actual = GeometryIntersections.intersects((Solid3d) first, (Solid3d) second);
            assertProperty(
                    COMPOSITE_QUERY_SEED,
                    iteration,
                    primitives,
                    canonical,
                    "Solid3d/Solid3d",
                    "first,second",
                    expected,
                    actual,
                    first.bounds(),
                    second.bounds());
            boolean reverseActual = GeometryIntersections.intersects((Solid3d) second, (Solid3d) first);
            assertProperty(
                    COMPOSITE_QUERY_SEED,
                    iteration,
                    primitives,
                    canonical,
                    "Solid3d/Solid3d",
                    "second,first",
                    expected,
                    reverseActual,
                    second.bounds(),
                    first.bounds());

            boolean segmentExpected = Phase1CTestSupport.intersectsSegmentCompositeOracle(segment, first);
            boolean segmentActual = GeometryIntersections.intersects(segment, (Solid3d) first);
            assertProperty(
                    COMPOSITE_QUERY_SEED,
                    iteration,
                    primitives,
                    canonical,
                    "Segment3d/Solid3d",
                    "segment,first",
                    segmentExpected,
                    segmentActual,
                    segment.bounds(),
                    first.bounds());
            boolean segmentReverseActual = GeometryIntersections.intersects((Solid3d) first, segment);
            assertProperty(
                    COMPOSITE_QUERY_SEED,
                    iteration,
                    primitives,
                    canonical,
                    "Solid3d/Segment3d",
                    "first,segment",
                    segmentExpected,
                    segmentReverseActual,
                    first.bounds(),
                    segment.bounds());

            Solid3d leaf = first.child(iteration % first.childCount());
            Composite oneChild = new Composite(List.of(leaf));
            boolean oneChildExpected = Phase1CTestSupport.intersectsPrimitiveTyped(leaf, second.child(0));
            boolean oneChildActual = GeometryIntersections.intersects((Solid3d) oneChild, second.child(0));
            assertProperty(
                    COMPOSITE_QUERY_SEED,
                    iteration,
                    "leaf=" + leaf + ", query=" + second.child(0),
                    Phase1CTestSupport.canonicalLeaves(oneChild),
                    "Solid3d/Solid3d",
                    "oneChild,query",
                    oneChildExpected,
                    oneChildActual,
                    oneChild.bounds(),
                    second.child(0).bounds());
        }
    }

    private static void assertTransformInvariant(
            long seed,
            int iteration,
            Composite original,
            Solid3d originalQuery,
            Composite transformed,
            Solid3d transformedQuery,
            String transform,
            boolean expected
    ) {
        boolean oracle = Phase1CTestSupport.intersectsSolidOracle(transformed, transformedQuery);
        assertProperty(seed, iteration, "query=" + originalQuery + ", transformedQuery=" + transformedQuery + ", " + transform,
                Phase1CTestSupport.canonicalLeaves(transformed), "Solid3d/Solid3d", "transformed,query",
                expected, oracle, transformed.bounds(), transformedQuery.bounds());
        boolean actual = GeometryIntersections.intersects((Solid3d) transformed, transformedQuery);
        assertProperty(seed, iteration, "query=" + originalQuery + ", transformedQuery=" + transformedQuery + ", " + transform,
                Phase1CTestSupport.canonicalLeaves(transformed), "Solid3d/Solid3d", "transformed,query generic",
                oracle, actual, transformed.bounds(), transformedQuery.bounds());
        for (int index = 0; index < transformed.childCount(); index++) {
            Solid3d child = transformed.child(index);
            boolean contained = transformed.bounds().contains(child.bounds().min())
                    && transformed.bounds().contains(child.bounds().max());
            assertProperty(seed, iteration, "original=" + original + ", child=" + child + ", " + transform,
                    Phase1CTestSupport.canonicalLeaves(transformed), "Composite.bounds", "transformed child " + index,
                    true, contained, transformed.bounds(), child.bounds());
        }
    }

    private static List<Solid3d> primitiveLeaves(SplittableRandom random, int iteration) {
        int count = 1 + iteration % 5;
        List<Solid3d> leaves = new ArrayList<>(count + 1);
        for (int index = 0; index < count; index++) {
            leaves.add(primitive(random, iteration + index & 3, iteration + index));
        }
        if (iteration % 7 == 0) leaves.add(leaves.getFirst());
        return leaves;
    }

    private static Composite nestedComposite(List<Solid3d> leaves) {
        if (leaves.size() == 1) return new Composite(List.of(new Composite(leaves)));
        int split = Math.max(1, leaves.size() / 2);
        return new Composite(List.of(
                new Composite(leaves.subList(0, split)),
                new Composite(leaves.subList(split, leaves.size()))));
    }

    private static Composite randomComposite(SplittableRandom random, int iteration) {
        List<Solid3d> leaves = primitiveLeaves(random, iteration);
        return iteration % 2 == 0 ? nestedComposite(leaves) : new Composite(leaves);
    }

    private static Solid3d primitive(SplittableRandom random, int kind, int iteration) {
        Vec3d center = vector(random, -8.0D, 8.0D);
        return switch (kind) {
            case 0 -> {
                Vec3d half = halfExtents(random, iteration);
                yield new Aabb(center.subtract(half), center.add(half));
            }
            case 1 -> new Sphere(center, iteration % 7 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D));
            case 2 -> new Obb(center, halfExtents(random, iteration), rotation(random));
            case 3 -> new Capsule(
                    iteration % 11 == 0 ? new Segment3d(center, center) : new Segment3d(center, vector(random, -8.0D, 8.0D)),
                    iteration % 5 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D));
            default -> throw new AssertionError("unknown primitive kind " + kind);
        };
    }

    private static Segment3d randomSegment(SplittableRandom random, int iteration) {
        Vec3d first = vector(random, -10.0D, 10.0D);
        return iteration % 13 == 0 ? new Segment3d(first, first) : new Segment3d(first, vector(random, -10.0D, 10.0D));
    }

    private static Vec3d halfExtents(SplittableRandom random, int iteration) {
        return new Vec3d(
                iteration % 7 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D),
                iteration % 11 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D),
                iteration % 13 == 0 ? 0.0D : random.nextDouble(0.0D, 2.0D));
    }

    private static Vec3d vector(SplittableRandom random, double minimum, double maximum) {
        return new Vec3d(
                random.nextDouble(minimum, maximum),
                random.nextDouble(minimum, maximum),
                random.nextDouble(minimum, maximum));
    }

    private static Rotation3d rotation(SplittableRandom random) {
        return new Rotation3d(
                random.nextDouble(-1.0D, 1.0D),
                random.nextDouble(-1.0D, 1.0D),
                random.nextDouble(-1.0D, 1.0D),
                1.0D + random.nextDouble(-0.5D, 0.5D));
    }

    private static Composite transformComposite(Composite composite, SolidTransform transform) {
        List<Solid3d> transformed = new ArrayList<>(composite.childCount());
        for (int index = 0; index < composite.childCount(); index++) {
            transformed.add(transform.apply(composite.child(index)));
        }
        return new Composite(transformed);
    }

    private static Solid3d translate(Solid3d solid, Vec3d translation) {
        return switch (solid) {
            case Aabb box -> new Aabb(box.min().add(translation), box.max().add(translation));
            case Sphere sphere -> new Sphere(sphere.center().add(translation), sphere.radius());
            case Obb box -> new Obb(box.center().add(translation), box.halfExtents(), box.orientation());
            case Capsule capsule -> new Capsule(
                    new Segment3d(capsule.centerline().start().add(translation), capsule.centerline().end().add(translation)),
                    capsule.radius());
            case Composite composite -> transformComposite(composite, leaf -> translate(leaf, translation));
        };
    }

    private static Solid3d rotate(Solid3d solid, Rotation3d rotation) {
        return switch (solid) {
            case Aabb box -> new Obb(rotation.rotate(box.center()), box.halfExtents(), rotation);
            case Sphere sphere -> new Sphere(rotation.rotate(sphere.center()), sphere.radius());
            case Obb box -> new Obb(rotation.rotate(box.center()), box.halfExtents(), compose(rotation, box.orientation()));
            case Capsule capsule -> new Capsule(
                    new Segment3d(rotation.rotate(capsule.centerline().start()), rotation.rotate(capsule.centerline().end())),
                    capsule.radius());
            case Composite composite -> transformComposite(composite, leaf -> rotate(leaf, rotation));
        };
    }

    private static Solid3d scale(Solid3d solid, double scale) {
        return switch (solid) {
            case Aabb box -> new Aabb(box.min().multiply(scale), box.max().multiply(scale));
            case Sphere sphere -> new Sphere(sphere.center().multiply(scale), sphere.radius() * scale);
            case Obb box -> new Obb(box.center().multiply(scale), box.halfExtents().multiply(scale), box.orientation());
            case Capsule capsule -> new Capsule(
                    new Segment3d(capsule.centerline().start().multiply(scale), capsule.centerline().end().multiply(scale)),
                    capsule.radius() * scale);
            case Composite composite -> transformComposite(composite, leaf -> scale(leaf, scale));
        };
    }

    private static Rotation3d compose(Rotation3d first, Rotation3d second) {
        return new Rotation3d(
                first.w() * second.x() + first.x() * second.w() + first.y() * second.z() - first.z() * second.y(),
                first.w() * second.y() - first.x() * second.z() + first.y() * second.w() + first.z() * second.x(),
                first.w() * second.z() + first.x() * second.y() - first.y() * second.x() + first.z() * second.w(),
                first.w() * second.w() - first.x() * second.x() - first.y() * second.y() - first.z() * second.z());
    }

    private static void assertProperty(
            long seed,
            int iteration,
            String primitives,
            String canonicalLeaves,
            String declaredStaticTypes,
            String queryOrder,
            boolean expected,
            boolean actual,
            Aabb firstBounds,
            Aabb secondBounds
    ) {
        assertEquals(expected, actual, failure(seed, iteration, primitives, canonicalLeaves, declaredStaticTypes,
                queryOrder, expected, actual, firstBounds, secondBounds));
    }

    private static void assertObjectProperty(
            long seed,
            int iteration,
            String primitives,
            String canonicalLeaves,
            String declaredStaticTypes,
            String queryOrder,
            Object expected,
            Object actual,
            Aabb firstBounds,
            Aabb secondBounds
    ) {
        assertEquals(expected, actual, failure(seed, iteration, primitives, canonicalLeaves, declaredStaticTypes,
                queryOrder, expected, actual, firstBounds, secondBounds));
    }

    private static String failure(
            long seed,
            int iteration,
            String primitives,
            String canonicalLeaves,
            String declaredStaticTypes,
            String queryOrder,
            Object expected,
            Object actual,
            Aabb firstBounds,
            Aabb secondBounds
    ) {
        return "seed=" + seed
                + ", iteration=" + iteration
                + ", generatedPrimitives={" + primitives + "}"
                + ", canonicalLeaves=" + canonicalLeaves
                + ", declaredStaticTypes=" + declaredStaticTypes
                + ", queryOrder=" + queryOrder
                + ", expected=" + expected
                + ", actual=" + actual
                + ", firstBounds=" + firstBounds
                + ", secondBounds=" + secondBounds;
    }

    @FunctionalInterface
    private interface SolidTransform {
        Solid3d apply(Solid3d solid);
    }
}
