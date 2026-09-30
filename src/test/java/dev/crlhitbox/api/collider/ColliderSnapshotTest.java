package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice A acceptance: snapshots, defensive ownership, placement bounds and the finite ray value. */
class ColliderSnapshotTest {
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);

    @Test
    void solidSnapshotComputesIdentityParentFrameBounds() {
        Aabb local = new Aabb(ZERO, new Vec3d(1.0D, 2.0D, 3.0D));
        SolidColliderSnapshot snapshot =
                new SolidColliderSnapshot(local, RigidTransform3d.identity(), true);

        assertEquals(local, snapshot.bounds().orElseThrow(), "identity placement keeps local bounds");
        assertEquals(local, snapshot.solid());
        assertEquals(RigidTransform3d.identity(), snapshot.localToParent());
        assertTrue(snapshot.enabled());
    }

    @Test
    void solidSnapshotPlacesBoundsThroughTranslation() {
        Aabb local = new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D));
        RigidTransform3d translated =
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D));
        SolidColliderSnapshot snapshot = new SolidColliderSnapshot(local, translated, true);

        Aabb placed = snapshot.bounds().orElseThrow();
        assertEquals(10.0D, placed.min().x(), 0.0D);
        assertEquals(11.0D, placed.max().x(), 0.0D);
        assertEquals(0.0D, placed.min().y(), 0.0D);
        assertEquals(1.0D, placed.max().y(), 0.0D);
    }

    @Test
    void solidSnapshotBoundsAreConservativeUnderRotation() {
        Sphere local = new Sphere(ZERO, 1.0D);
        RigidTransform3d rotated = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D), ZERO);
        Aabb placed = new SolidColliderSnapshot(local, rotated, true).bounds().orElseThrow();

        assertTrue(placed.min().x() <= -1.0D && placed.max().x() >= 1.0D, "rotation stays covered");
        assertTrue(placed.min().y() <= -1.0D && placed.max().y() >= 1.0D, "rotation stays covered");
        assertTrue(placed.min().z() <= -1.0D && placed.max().z() >= 1.0D, "rotation stays covered");
    }

    @Test
    void disabledSnapshotRepresentsTheEmptySet() {
        SolidColliderSnapshot solid = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), false);
        RayColliderSnapshot ray = new RayColliderSnapshot(
                new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), 1.0D),
                RigidTransform3d.identity(), false);

        assertTrue(solid.bounds().isEmpty(), "a disabled solid is the empty set");
        assertTrue(ray.bounds().isEmpty(), "a disabled ray is the empty set");
        assertFalse(solid.enabled());
    }

    @Test
    void snapshotsCompareStructurallyIncludingEnabledAndPlacement() {
        Sphere shape = new Sphere(ZERO, 1.0D);
        SolidColliderSnapshot first =
                new SolidColliderSnapshot(shape, RigidTransform3d.identity(), true);
        SolidColliderSnapshot equal =
                new SolidColliderSnapshot(new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), true);
        SolidColliderSnapshot disabled =
                new SolidColliderSnapshot(shape, RigidTransform3d.identity(), false);
        SolidColliderSnapshot moved = new SolidColliderSnapshot(shape,
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D)), true);

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, disabled, "enable state participates in equality");
        assertNotEquals(first, moved, "placement participates in equality");
    }

    @Test
    void raySnapshotKeepsOriginAndDirectionAndCoversItsEndpoints() {
        RayColliderSnapshot snapshot = new RayColliderSnapshot(
                new Ray3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D), 4.0D),
                RigidTransform3d.identity(), true);

        Aabb bounds = snapshot.bounds().orElseThrow();
        assertEquals(1.0D, snapshot.ray().origin().x(), 0.0D);
        assertEquals(1.0D, snapshot.ray().direction().x(), 0.0D);
        assertEquals(5.0D, snapshot.ray().end().x(), 0.0D);
        assertEquals(1.0D, bounds.min().x(), 0.0D);
        assertEquals(5.0D, bounds.max().x(), 0.0D);
    }

    @Test
    void raySegmentConversionDoesNotRewriteTheRay() {
        Ray3d ray = new Ray3d(new Vec3d(2.0D, 0.0D, 0.0D), new Vec3d(-1.0D, 0.0D, 0.0D), 2.0D);
        Segment3d segment = ray.asSegment();

        assertEquals(new Vec3d(2.0D, 0.0D, 0.0D), ray.origin(), "origin preserved");
        assertEquals(new Vec3d(0.0D, 0.0D, 0.0D), ray.end());
        assertTrue(segment.start().equals(ray.origin()) || segment.start().equals(ray.end()),
                "one segment endpoint is the ray origin");
        assertTrue(segment.end().equals(ray.origin()) || segment.end().equals(ray.end()),
                "the other segment endpoint is the ray end");
        assertEquals(new Vec3d(-1.0D, 0.0D, 0.0D), ray.direction(),
                "the canonical segment order never rewrites direction");
        assertEquals(new Vec3d(2.0D, 0.0D, 0.0D), ray.origin(), "the ray origin is unchanged");
    }

    @Test
    void rayNormalizationIsStableForExtremeFiniteDirections() {
        Ray3d tiny = new Ray3d(ZERO, new Vec3d(Double.MIN_NORMAL, 0.0D, 0.0D), 1.0D);
        Ray3d huge = new Ray3d(ZERO, new Vec3d(1.0E300D, 0.0D, 0.0D), 1.0D);

        assertEquals(1.0D, tiny.direction().x(), 0.0D);
        assertEquals(1.0D, huge.direction().x(), 0.0D);
        assertEquals(1.0D, tiny.length(), 0.0D);
    }

    @Test
    void rayZeroLengthIsAPoint() {
        Ray3d ray = new Ray3d(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(0.0D, 1.0D, 0.0D), 0.0D);

        assertEquals(ray.origin(), ray.end(), "a zero-length ray is its origin");
        assertEquals(ray.origin(), ray.bounds().min(), "degenerate bounds are the point");
        assertEquals(ray.origin(), ray.bounds().max());
    }

    @Test
    void rayRejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ray3d(ZERO, ZERO, 1.0D), "zero direction");
        assertThrows(IllegalArgumentException.class,
                () -> new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), -1.0D), "negative length");
        assertThrows(IllegalArgumentException.class,
                () -> new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), Double.NaN), "NaN length");
        assertThrows(IllegalArgumentException.class,
                () -> new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), Double.POSITIVE_INFINITY),
                "infinite length");
        assertThrows(NullPointerException.class, () -> new Ray3d(null, new Vec3d(1, 0, 0), 1.0D));
    }

    @Test
    void rayEndMustStayRepresentable() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ray3d(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D),
                        new Vec3d(1.0D, 0.0D, 0.0D), Double.MAX_VALUE),
                "an unrepresentable end point is rejected instead of silently saturating");
    }

    @Test
    void compoundCopiesChildrenDefensively() {
        List<ColliderSnapshot> children = new ArrayList<>();
        children.add(new SolidColliderSnapshot(new Sphere(ZERO, 1.0D),
                RigidTransform3d.identity(), true));
        CompoundColliderSnapshot compound =
                new CompoundColliderSnapshot(children, RigidTransform3d.identity(), true);
        children.clear();

        assertEquals(1, compound.childCount(), "later mutation of the source list has no effect");
        assertTrue(compound.child(0) instanceof SolidColliderSnapshot);
    }

    @Test
    void compoundBoundsAreTheUnionOfEnabledChildBounds() {
        ColliderSnapshot first = new SolidColliderSnapshot(
                new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity(), true);
        ColliderSnapshot second = new SolidColliderSnapshot(
                new Aabb(new Vec3d(5.0D, 0.0D, 0.0D), new Vec3d(6.0D, 1.0D, 1.0D)),
                RigidTransform3d.identity(), true);
        CompoundColliderSnapshot compound =
                new CompoundColliderSnapshot(List.of(first, second), RigidTransform3d.identity(), true);

        Aabb bounds = compound.bounds().orElseThrow();
        assertEquals(0.0D, bounds.min().x(), 0.0D);
        assertEquals(6.0D, bounds.max().x(), 0.0D);
    }

    @Test
    void compoundPlacementAppliesOnceToOneLevel() {
        ColliderSnapshot child = new SolidColliderSnapshot(
                new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot compound = new CompoundColliderSnapshot(List.of(child),
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D)), true);

        Aabb bounds = compound.bounds().orElseThrow();
        assertEquals(10.0D, bounds.min().x(), 0.0D, "the compound placement applies exactly once");
        assertEquals(11.0D, bounds.max().x(), 0.0D);
    }

    @Test
    void nestedCompoundPlacementComposesPerLevel() {
        ColliderSnapshot leaf = new SolidColliderSnapshot(
                new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity(), true);
        ColliderSnapshot inner = new CompoundColliderSnapshot(List.of(leaf),
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D)), true);
        CompoundColliderSnapshot outer = new CompoundColliderSnapshot(List.of(inner),
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D)), true);

        Aabb bounds = outer.bounds().orElseThrow();
        assertEquals(11.0D, bounds.min().x(), 0.0D);
        assertEquals(12.0D, bounds.max().x(), 0.0D);
    }

    @Test
    void compoundWithoutEnabledLeavesIsTheEmptySet() {
        ColliderSnapshot disabled = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), false);
        CompoundColliderSnapshot allDisabled =
                new CompoundColliderSnapshot(List.of(disabled), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot empty =
                new CompoundColliderSnapshot(List.of(), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot disabledCompound =
                new CompoundColliderSnapshot(List.of(disabled), RigidTransform3d.identity(), false);

        assertTrue(allDisabled.bounds().isEmpty(), "no enabled leaf means the empty set");
        assertTrue(empty.bounds().isEmpty(), "an empty compound means the empty set");
        assertTrue(disabledCompound.bounds().isEmpty(), "a disabled compound means the empty set");
        assertEquals(0, empty.childCount());
    }

    @Test
    void compoundPreservesInsertionOrderAndComparesStructurally() {
        ColliderSnapshot sphere = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), true);
        ColliderSnapshot box = new SolidColliderSnapshot(
                new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot first =
                new CompoundColliderSnapshot(List.of(sphere, box), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot same =
                new CompoundColliderSnapshot(List.of(sphere, box), RigidTransform3d.identity(), true);
        CompoundColliderSnapshot reordered =
                new CompoundColliderSnapshot(List.of(box, sphere), RigidTransform3d.identity(), true);

        assertTrue(first.child(0) instanceof SolidColliderSnapshot);
        assertEquals(sphere, first.child(0), "insertion order is preserved");
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, reordered, "child order participates in equality");
    }

    @Test
    void snapshotsRejectNullArguments() {
        assertThrows(NullPointerException.class,
                () -> new SolidColliderSnapshot(null, RigidTransform3d.identity(), true));
        assertThrows(NullPointerException.class,
                () -> new SolidColliderSnapshot(new Sphere(ZERO, 1.0D), null, true));
        assertThrows(NullPointerException.class,
                () -> new RayColliderSnapshot(null, RigidTransform3d.identity(), true));
        assertThrows(NullPointerException.class,
                () -> new CompoundColliderSnapshot(null, RigidTransform3d.identity(), true));
        assertThrows(NullPointerException.class,
                () -> new CompoundColliderSnapshot(
                        java.util.Arrays.asList(new SolidColliderSnapshot(
                                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), true), null),
                        RigidTransform3d.identity(), true));
    }

    @Test
    void snapshotBoundsNeverProveIntersectionByThemselves() {
        ColliderSnapshot left = new SolidColliderSnapshot(
                new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity(), true);
        ColliderSnapshot right = new SolidColliderSnapshot(
                new Aabb(new Vec3d(0.5D, 0.5D, 0.5D), new Vec3d(2.0D, 2.0D, 2.0D)),
                RigidTransform3d.identity(), true);

        Aabb firstBounds = left.bounds().orElseThrow();
        Aabb secondBounds = right.bounds().orElseThrow();
        assertTrue(firstBounds.min().x() <= secondBounds.max().x()
                        && secondBounds.min().x() <= firstBounds.max().x(),
                "bounds overlap here; a real query still has to decide the result");
    }

    @Test
    void snapshotBoundsAreEmptyOnlyForTheEmptySet() {
        ColliderSnapshot enabled = new SolidColliderSnapshot(
                new Obb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity()),
                RigidTransform3d.identity(), true);

        Optional<Aabb> bounds = enabled.bounds();
        assertTrue(bounds.isPresent(), "an enabled leaf with representable bounds must report bounds");
    }
}
