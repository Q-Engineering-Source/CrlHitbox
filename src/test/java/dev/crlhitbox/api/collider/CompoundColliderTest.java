package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice D acceptance: editable compounds and the unified snapshot query entry. */
class CompoundColliderTest {
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);

    @Test
    void emptyCompoundIsLegalAndIntersectsNothing() {
        MutableCompoundCollider compound = new MutableCompoundCollider();

        assertEquals(0, compound.childCount());
        assertEquals(0L, compound.revision());
        assertTrue(compound.snapshot().bounds().isEmpty(), "an empty compound is the empty set");
        assertFalse(compound.clearChildren(), "clearing an empty compound is a no-op");
        assertFalse(ColliderQueries.intersects(compound.snapshot(), sphereAt(ZERO)));
        assertFalse(ColliderQueries.intersects(sphereAt(ZERO), compound.snapshot()));
    }

    @Test
    void editingOperationsAdvanceTheRevisionOnceAndPreserveOrder() {
        MutableCompoundCollider compound = new MutableCompoundCollider();
        ColliderSnapshot first = sphereAt(ZERO);
        ColliderSnapshot second = sphereAt(new Vec3d(5.0D, 0.0D, 0.0D));

        compound.addChild(first);
        assertEquals(1L, compound.revision());
        compound.addChild(second);
        assertEquals(2L, compound.revision());
        assertEquals(2, compound.childCount());
        assertEquals(first, compound.child(0));
        assertEquals(second, compound.child(1));

        assertFalse(compound.setChild(0, first), "replacing with an equal child is a no-op");
        assertEquals(2L, compound.revision());

        ColliderSnapshot replacement = sphereAt(new Vec3d(0.0D, 3.0D, 0.0D));
        assertTrue(compound.setChild(0, replacement));
        assertEquals(3L, compound.revision());
        assertEquals(replacement, compound.child(0));

        ColliderSnapshot removed = compound.removeChild(0);
        assertEquals(replacement, removed);
        assertEquals(4L, compound.revision());
        assertEquals(1, compound.childCount());
        assertEquals(second, compound.child(0), "removal keeps the remaining order");
    }

    @Test
    void replaceAndClearPublishInOneStep() {
        MutableCompoundCollider compound = new MutableCompoundCollider();
        compound.addChild(sphereAt(ZERO));
        ColliderSnapshot before = compound.snapshot();

        List<ColliderSnapshot> next = List.of(
                sphereAt(new Vec3d(1.0D, 0.0D, 0.0D)),
                sphereAt(new Vec3d(2.0D, 0.0D, 0.0D)),
                sphereAt(new Vec3d(3.0D, 0.0D, 0.0D)));
        assertTrue(compound.replaceChildren(next));
        assertEquals(2L, compound.revision(), "a bulk replacement is one effective change");
        assertEquals(3, compound.childCount());
        assertNotSame(before, compound.snapshot());
        assertEquals(next.get(1), compound.child(1));

        assertTrue(compound.clearChildren());
        assertEquals(3L, compound.revision());
        assertEquals(0, compound.childCount());
        assertTrue(compound.snapshot().bounds().isEmpty());
        assertFalse(compound.clearChildren(), "clearing again is a no-op");
    }

    @Test
    void invalidEditsLeaveChildrenSnapshotAndRevisionUntouched() {
        MutableCompoundCollider compound = new MutableCompoundCollider();
        compound.addChild(sphereAt(ZERO));
        ColliderSnapshot before = compound.snapshot();
        long revision = compound.revision();

        assertThrows(IndexOutOfBoundsException.class, () -> compound.child(1));
        assertThrows(IndexOutOfBoundsException.class, () -> compound.child(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> compound.setChild(1, sphereAt(ZERO)));
        assertThrows(IndexOutOfBoundsException.class, () -> compound.removeChild(5));
        assertThrows(NullPointerException.class, () -> compound.addChild(null));
        assertThrows(NullPointerException.class, () -> compound.setChild(0, null));
        assertThrows(NullPointerException.class, () -> compound.replaceChildren(null));
        assertThrows(NullPointerException.class,
                () -> compound.replaceChildren(Arrays.asList(sphereAt(ZERO), null)));

        assertEquals(revision, compound.revision());
        assertEquals(1, compound.childCount());
        assertSame(before, compound.snapshot(), "a rejected edit never republishes the snapshot");
    }

    @Test
    void compoundOwnsSnapshotsInsteadOfLiveMutableChildren() {
        MutableSphereCollider mutable = new MutableSphereCollider(ZERO, 1.0D);
        MutableCompoundCollider compound = new MutableCompoundCollider();
        compound.addChild(mutable.snapshot());

        mutable.setRadius(9.0D);

        assertEquals(new SolidColliderSnapshot(new Sphere(ZERO, 1.0D),
                RigidTransform3d.identity(), true), compound.child(0),
                "the compound still holds the published snapshot");
        assertFalse(ColliderQueries.intersects(compound.snapshot(),
                sphereAt(new Vec3d(5.0D, 0.0D, 0.0D))));

        compound.setChild(0, mutable.snapshot());
        assertTrue(ColliderQueries.intersects(compound.snapshot(),
                sphereAt(new Vec3d(5.0D, 0.0D, 0.0D))),
                "publishing the new snapshot makes the change visible");
    }

    @Test
    void compoundIntersectsWhenAnyLeafPairIntersects() {
        CompoundColliderSnapshot compound = new CompoundColliderSnapshot(List.of(
                sphereAt(new Vec3d(10.0D, 0.0D, 0.0D)),
                sphereAt(new Vec3d(20.0D, 0.0D, 0.0D))),
                RigidTransform3d.identity(), true);

        assertTrue(ColliderQueries.intersects(compound, sphereAt(new Vec3d(20.0D, 0.0D, 0.0D))));
        assertFalse(ColliderQueries.intersects(compound, sphereAt(new Vec3d(15.0D, 0.0D, 0.0D))));
    }

    @Test
    void compoundAgainstCompoundChecksLeafPairs() {
        CompoundColliderSnapshot left = new CompoundColliderSnapshot(List.of(
                sphereAt(new Vec3d(-5.0D, 0.0D, 0.0D)),
                sphereAt(new Vec3d(1.0D, 0.0D, 0.0D))),
                RigidTransform3d.identity(), true);
        CompoundColliderSnapshot right = new CompoundColliderSnapshot(List.of(
                sphereAt(new Vec3d(1.0D, 0.0D, 0.0D)),
                sphereAt(new Vec3d(30.0D, 0.0D, 0.0D))),
                RigidTransform3d.identity(), true);

        assertTrue(ColliderQueries.intersects(left, right));
        assertFalse(ColliderQueries.intersects(left, new CompoundColliderSnapshot(List.of(
                sphereAt(new Vec3d(50.0D, 0.0D, 0.0D))), RigidTransform3d.identity(), true)));
    }

    @Test
    void rayChildrenParticipateInCompoundQueries() {
        ColliderSnapshot ray = new RayColliderSnapshot(
                new Ray3d(new Vec3d(-5.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D), 10.0D),
                RigidTransform3d.identity(), true);
        CompoundColliderSnapshot withRay =
                new CompoundColliderSnapshot(List.of(ray), RigidTransform3d.identity(), true);

        assertTrue(ColliderQueries.intersects(withRay, sphereAt(ZERO)));
        assertFalse(ColliderQueries.intersects(withRay, sphereAt(new Vec3d(0.0D, 5.0D, 0.0D))));
    }

    @Test
    void disabledChildrenDoNotIntersect() {
        ColliderSnapshot disabledSphere = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), false);
        CompoundColliderSnapshot compound = new CompoundColliderSnapshot(
                List.of(disabledSphere), RigidTransform3d.identity(), true);

        assertFalse(ColliderQueries.intersects(compound, sphereAt(ZERO)));
        assertTrue(compound.bounds().isEmpty(), "no enabled leaf means the empty set");
    }

    @Test
    void nestedPlacementsComposeOncePerLevel() {
        RigidTransform3d inner = new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(2.0D, 0.0D, 0.0D));
        RigidTransform3d outer = new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D));
        ColliderSnapshot leaf = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), inner, true);
        CompoundColliderSnapshot nested = new CompoundColliderSnapshot(
                List.of(leaf), outer, true);

        assertTrue(ColliderQueries.intersects(nested, sphereAt(new Vec3d(12.0D, 0.0D, 0.0D))),
                "the leaf sits at x = 12, so the placement is not stacked twice");
        assertFalse(ColliderQueries.intersects(nested, sphereAt(new Vec3d(22.0D, 0.0D, 0.0D))));
    }

    @Test
    void deeplyNestedCompoundsAreTraversedIteratively() {
        ColliderSnapshot nested = sphereAt(ZERO);
        for (int depth = 0; depth < 64; depth++) {
            nested = new CompoundColliderSnapshot(
                    List.of(nested), RigidTransform3d.identity(), true);
        }

        assertTrue(ColliderQueries.intersects(nested, sphereAt(ZERO)),
                "a deep tree must not overflow the stack");
        assertFalse(ColliderQueries.intersects(nested, sphereAt(new Vec3d(100.0D, 0.0D, 0.0D))));
    }

    @Test
    void overlappingBoundsNeverProduceAFalsePositive() {
        ColliderSnapshot first = sphereAt(ZERO);
        ColliderSnapshot diagonal = sphereAt(new Vec3d(1.9D, 1.9D, 0.0D));
        Aabb firstBounds = first.bounds().orElseThrow();
        Aabb secondBounds = diagonal.bounds().orElseThrow();

        assertTrue(firstBounds.min().x() <= secondBounds.max().x()
                        && secondBounds.min().x() <= firstBounds.max().x(),
                "the fixture bounds really do overlap");

        assertFalse(ColliderQueries.intersects(first, diagonal),
                "only the narrow phase may report an intersection");
    }

    @Test
    void compoundIsUsableAsAChildSnapshotOfAnotherCompound() {
        MutableCompoundCollider inner = new MutableCompoundCollider();
        inner.addChild(sphereAt(new Vec3d(4.0D, 0.0D, 0.0D)));
        MutableCompoundCollider outer = new MutableCompoundCollider();
        outer.addChild(inner.snapshot());

        assertTrue(ColliderQueries.intersects(outer.snapshot(), sphereAt(new Vec3d(4.0D, 0.0D, 0.0D))));
        assertFalse(ColliderQueries.intersects(outer.snapshot(), sphereAt(ZERO)));

        inner.addChild(sphereAt(ZERO));
        assertFalse(ColliderQueries.intersects(outer.snapshot(), sphereAt(ZERO)),
                "the outer compound still holds the earlier inner snapshot");
        outer.replaceChildren(List.of(inner.snapshot()));
        assertTrue(ColliderQueries.intersects(outer.snapshot(), sphereAt(ZERO)));
    }

    @Test
    void replaceChildrenCopiesTheListDefensively() {
        MutableCompoundCollider compound = new MutableCompoundCollider();
        List<ColliderSnapshot> source = new ArrayList<>();
        source.add(sphereAt(ZERO));
        compound.replaceChildren(source);

        source.clear();
        source.add(sphereAt(new Vec3d(9.0D, 0.0D, 0.0D)));

        assertEquals(1, compound.childCount());
        assertInstanceOf(SolidColliderSnapshot.class, compound.child(0));
        assertEquals(new Sphere(ZERO, 1.0D),
                ((SolidColliderSnapshot) compound.child(0)).solid());
    }

    private static ColliderSnapshot sphereAt(Vec3d center) {
        return new SolidColliderSnapshot(new Sphere(center, 1.0D),
                RigidTransform3d.identity(), true);
    }
}
