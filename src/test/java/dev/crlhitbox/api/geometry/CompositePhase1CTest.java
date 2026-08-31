package dev.crlhitbox.api.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositePhase1CTest {
    @Test
    void onePrimitiveLeafFormsAStableSolidUnion() {
        Aabb leaf = new Aabb(new Vec3d(-2.0D, -1.0D, 3.0D), new Vec3d(4.0D, 5.0D, 7.0D));
        Solid3d solid = leaf;
        Composite composite = new Composite(List.of(solid));

        assertEquals(1, composite.childCount());
        assertSame(leaf, composite.child(0));
        assertEquals(leaf.bounds(), composite.bounds());
        assertSame(composite.bounds(), composite.bounds(), "bounds are eagerly stored and stable");
    }

    @Test
    void rejectsNullAndEmptyInputWithActionableFailures() {
        assertThrows(NullPointerException.class, () -> new Composite(null));
        assertThrows(IllegalArgumentException.class, () -> new Composite(List.of()));
    }

    @Test
    void rejectsANullChildAtItsOriginalInputIndex() {
        Aabb leaf = pointBox(0.0D, 0.0D, 0.0D);
        for (int nullIndex = 0; nullIndex < 3; nullIndex++) {
            List<Solid3d> children = new ArrayList<>(Arrays.asList(leaf, leaf, leaf));
            children.set(nullIndex, null);
            int expectedIndex = nullIndex;
            NullPointerException failure = assertThrows(NullPointerException.class, () -> new Composite(children));
            assertTrue(failure.getMessage().contains("children[" + expectedIndex + "]"), failure::getMessage);
        }
    }

    @Test
    void snapshotsTheCallerListAndPreservesStandardIndexFailures() {
        Aabb first = pointBox(1.0D, 2.0D, 3.0D);
        Sphere second = new Sphere(new Vec3d(4.0D, 5.0D, 6.0D), 1.0D);
        List<Solid3d> source = new ArrayList<>(List.of(first, second));
        Composite composite = new Composite(source);

        source.clear();

        assertEquals(2, composite.childCount());
        assertSame(first, composite.child(0));
        assertSame(second, composite.child(1));
        assertThrows(IndexOutOfBoundsException.class, () -> composite.child(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> composite.child(composite.childCount()));
    }

    @Test
    void flattensNestedCompositesAtEveryInputPositionInEncounterOrder() {
        Aabb a = pointBox(0.0D, 0.0D, 0.0D);
        Sphere b = new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 0.0D);
        Obb c = new Obb(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Capsule d = new Capsule(new Segment3d(new Vec3d(3.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D)), 0.0D);
        Composite composite = new Composite(List.of(
                new Composite(List.of(a)),
                b,
                new Composite(List.of(c, c)),
                new Composite(List.of(d))));

        assertCanonicalChildren(composite, a, b, c, c, d);
    }

    @Test
    void associativeNestingProducesTheSameCanonicalLeafSequence() {
        Aabb a = pointBox(0.0D, 0.0D, 0.0D);
        Sphere b = new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 0.0D);
        Obb c = new Obb(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());

        Composite rightGrouped = new Composite(List.of(a, new Composite(List.of(b, c))));
        Composite leftGrouped = new Composite(List.of(new Composite(List.of(a, b)), c));
        Composite flat = new Composite(List.of(a, b, c));

        assertCanonicalChildren(rightGrouped, a, b, c);
        assertCanonicalChildren(leftGrouped, a, b, c);
        assertCanonicalChildren(flat, a, b, c);
    }

    @Test
    void duplicatesAcrossNestingBoundariesRemainPrimitiveLeaves() {
        Aabb leaf = pointBox(1.0D, 2.0D, 3.0D);
        Composite composite = new Composite(List.of(leaf, new Composite(List.of(leaf, leaf)), leaf));

        assertCanonicalChildren(composite, leaf, leaf, leaf, leaf);
        for (int index = 0; index < composite.childCount(); index++) {
            assertFalse(composite.child(index) instanceof Composite, "canonical child index " + index);
        }
    }

    @Test
    void repeatedNestingFor4096RoundsNeverRetainsACompositeChild() {
        Sphere leaf = new Sphere(new Vec3d(1.0D, -2.0D, 3.0D), 0.5D);
        Composite nested = new Composite(List.of(leaf));
        for (int round = 0; round < 4096; round++) {
            nested = new Composite(List.of(nested));
        }

        assertEquals(1, nested.childCount());
        assertSame(leaf, nested.child(0));
        assertEquals(leaf.bounds(), nested.bounds());
    }

    @Test
    void boundsUseTheExactCoordinateExtremaOfEveryPrimitiveLeaf() {
        Aabb aabb = new Aabb(new Vec3d(-4.0D, -2.0D, -1.0D), new Vec3d(-3.0D, 0.0D, 1.0D));
        Sphere sphere = new Sphere(new Vec3d(5.0D, 6.0D, 7.0D), 1.0D);
        Obb obb = new Obb(new Vec3d(0.0D, 10.0D, -5.0D), new Vec3d(2.0D, 1.0D, 3.0D), Rotation3d.identity());
        Capsule capsule = new Capsule(new Segment3d(new Vec3d(-10.0D, 2.0D, 3.0D), new Vec3d(-8.0D, 4.0D, 5.0D)), 0.5D);
        Composite composite = new Composite(List.of(aabb, sphere, obb, capsule));

        assertEquals(
                new Aabb(new Vec3d(-10.5D, -2.0D, -8.0D), new Vec3d(6.0D, 11.0D, 8.0D)),
                composite.bounds());
    }

    @Test
    void boundsIgnoreGroupingOrderAndDuplicateMultiplicity() {
        Aabb left = new Aabb(new Vec3d(-5.0D, -4.0D, -3.0D), new Vec3d(-2.0D, -1.0D, 0.0D));
        Sphere right = new Sphere(new Vec3d(7.0D, 8.0D, 9.0D), 2.0D);
        Aabb expected = new Aabb(new Vec3d(-5.0D, -4.0D, -3.0D), new Vec3d(9.0D, 10.0D, 11.0D));

        assertEquals(expected, new Composite(List.of(left, right)).bounds());
        assertEquals(expected, new Composite(List.of(right, left)).bounds());
        assertEquals(expected, new Composite(List.of(left, left, new Composite(List.of(right, right)))).bounds());
    }

    @Test
    void boundsContainEveryDegenerateLeafBound() {
        List<Solid3d> leaves = List.of(
                pointBox(0.0D, 0.0D, 0.0D),
                new Aabb(new Vec3d(-2.0D, 1.0D, 3.0D), new Vec3d(2.0D, 1.0D, 3.0D)),
                new Sphere(new Vec3d(4.0D, -1.0D, 2.0D), 0.0D),
                new Obb(new Vec3d(-3.0D, 5.0D, -7.0D), new Vec3d(0.0D, 2.0D, 0.0D), Rotation3d.identity()),
                new Capsule(new Segment3d(new Vec3d(8.0D, 1.0D, 1.0D), new Vec3d(8.0D, 1.0D, 1.0D)), 0.0D));
        Composite composite = new Composite(leaves);

        for (Solid3d leaf : leaves) {
            assertTrue(composite.bounds().contains(leaf.bounds().min()), "minimum of " + leaf);
            assertTrue(composite.bounds().contains(leaf.bounds().max()), "maximum of " + leaf);
        }
    }

    @Test
    void boundsSelectExtremeFiniteAndSubnormalCoordinatesWithoutMargin() {
        Aabb negative = pointBox(-Double.MAX_VALUE, -Double.MIN_VALUE, 3.0D * Double.MIN_VALUE);
        Aabb positive = pointBox(Double.MAX_VALUE, Double.MIN_VALUE, -2.0D * Double.MIN_VALUE);
        Composite composite = new Composite(List.of(negative, positive));

        assertEquals(
                new Aabb(
                        new Vec3d(-Double.MAX_VALUE, -Double.MIN_VALUE, -2.0D * Double.MIN_VALUE),
                        new Vec3d(Double.MAX_VALUE, Double.MIN_VALUE, 3.0D * Double.MIN_VALUE)),
                composite.bounds());
    }

    @Test
    void nestedAndFlatValuesHaveExactStructuralEqualityHashAndString() {
        Aabb a = pointBox(0.0D, 0.0D, 0.0D);
        Sphere b = new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 0.5D);
        Capsule c = new Capsule(new Segment3d(new Vec3d(4.0D, 0.0D, 0.0D), new Vec3d(5.0D, 0.0D, 0.0D)), 0.25D);
        Composite nested = new Composite(List.of(a, new Composite(List.of(b, c))));
        Composite flat = new Composite(List.of(a, b, c));
        Composite equivalent = new Composite(List.of(new Composite(List.of(a, b)), c));

        assertEquals(nested, flat);
        assertEquals(flat, nested, "symmetry");
        assertEquals(flat, equivalent, "transitivity premise");
        assertEquals(nested, equivalent, "transitivity result");
        assertEquals(nested.hashCode(), flat.hashCode());
        assertEquals(nested.toString(), flat.toString());
        assertEquals("Composite[children=[" + a + ", " + b + ", " + c + "]]", flat.toString());
    }

    @Test
    void structuralIdentityRetainsOrderAndDuplicateMultiplicity() {
        Aabb a = pointBox(0.0D, 0.0D, 0.0D);
        Sphere b = new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 0.0D);
        Composite ordered = new Composite(List.of(a, b));
        Composite permuted = new Composite(List.of(b, a));
        Composite duplicated = new Composite(List.of(a, b, b));

        assertNotEquals(ordered, permuted);
        assertNotEquals(ordered, duplicated);
        assertEquals(ordered.bounds(), permuted.bounds());
        assertEquals(ordered.bounds(), duplicated.bounds());
    }

    @Test
    void exactLeafValuesRatherThanGeometricUnionControlEquality() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Sphere contained = new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D);
        Composite oneLeaf = new Composite(List.of(box));
        Composite sameGeometricUnion = new Composite(List.of(box, contained));

        assertNotEquals(oneLeaf, sameGeometricUnion);
        assertEquals(oneLeaf, oneLeaf, "reflexivity");
        assertNotEquals(oneLeaf, null);
        assertNotEquals(oneLeaf, box);
    }

    @Test
    void wideCompositePreserves8192LeavesWithoutExposingTheSourceList() {
        List<Solid3d> source = new ArrayList<>(8192);
        for (int index = 0; index < 8192; index++) {
            source.add(pointBox(index, -index, index % 7));
        }
        Solid3d first = source.getFirst();
        Solid3d middle = source.get(4096);
        Solid3d last = source.getLast();
        Composite composite = new Composite(source);

        source.clear();

        assertEquals(8192, composite.childCount());
        assertSame(first, composite.child(0));
        assertSame(middle, composite.child(4096));
        assertSame(last, composite.child(8191));
        assertEquals(new Vec3d(0.0D, -8191.0D, 0.0D), composite.bounds().min());
        assertEquals(new Vec3d(8191.0D, 0.0D, 6.0D), composite.bounds().max());
    }

    private static void assertCanonicalChildren(Composite composite, Solid3d... expected) {
        assertEquals(expected.length, composite.childCount());
        for (int index = 0; index < expected.length; index++) {
            assertSame(expected[index], composite.child(index), "child index " + index);
        }
    }

    private static Aabb pointBox(double x, double y, double z) {
        Vec3d point = new Vec3d(x, y, z);
        return new Aabb(point, point);
    }
}
