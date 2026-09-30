package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice B acceptance: mutable shape colliders, atomic invalid updates, no-op equality and revisions. */
class MutableColliderTest {
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);

    @Test
    void everyShapeStartsEnabledAtRevisionZeroWithIdentityPlacement() {
        for (MutableCollider collider : allShapes()) {
            assertEquals(0L, collider.revision(), collider.getClass().getSimpleName());
            assertTrue(collider.enabled(), collider.getClass().getSimpleName());
            assertEquals(RigidTransform3d.identity(), collider.localToParent(),
                    collider.getClass().getSimpleName());
            assertTrue(collider.snapshot().enabled(), collider.getClass().getSimpleName());
            assertTrue(collider.snapshot().bounds().isPresent(),
                    collider.getClass().getSimpleName());
        }
    }

    @Test
    void effectiveSettersAdvanceTheRevisionOnceAndPublishANewSnapshot() {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        ColliderSnapshot before = sphere.snapshot();

        assertTrue(sphere.setRadius(2.0D));

        assertEquals(1L, sphere.revision());
        assertEquals(2.0D, sphere.radius(), 0.0D);
        assertNotEquals(before, sphere.snapshot());
        assertEquals(new Sphere(ZERO, 2.0D), ((SolidColliderSnapshot) sphere.snapshot()).solid());
    }

    @Test
    void equalUpdatesAreNoOpsThatReuseTheCachedSnapshot() {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        ColliderSnapshot before = sphere.snapshot();

        assertFalse(sphere.setRadius(1.0D));
        assertFalse(sphere.setShape(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        assertFalse(sphere.setEnabled(true));
        assertFalse(sphere.setLocalToParent(RigidTransform3d.identity()));

        assertEquals(0L, sphere.revision(), "a no-op never advances the revision");
        assertSame(before, sphere.snapshot(), "a no-op reuses the cached snapshot");
    }

    @Test
    void invalidUpdatesThrowAndKeepThePreviousStateSnapshotAndRevision() {
        MutableAabbCollider box = new MutableAabbCollider(ZERO, new Vec3d(1.0D, 1.0D, 1.0D));
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        MutableObbCollider obb = new MutableObbCollider(
                ZERO, new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        MutableCapsuleCollider capsule = new MutableCapsuleCollider(
                ZERO, 2.0D, 0.5D, Rotation3d.identity());

        assertThrows(IllegalArgumentException.class,
                () -> box.setBounds(new Vec3d(1.0D, 0.0D, 0.0D), ZERO), "inverted interval");
        assertThrows(IllegalArgumentException.class,
                () -> sphere.setRadius(-1.0D), "negative radius");
        assertThrows(IllegalArgumentException.class,
                () -> sphere.setRadius(Double.NaN), "NaN radius");
        assertThrows(IllegalArgumentException.class,
                () -> obb.setHalfExtents(new Vec3d(-1.0D, 1.0D, 1.0D)), "negative extent");
        assertThrows(IllegalArgumentException.class,
                () -> capsule.setCenterlineLength(-1.0D), "negative centerline length");
        assertThrows(IllegalArgumentException.class,
                () -> capsule.setRadius(Double.POSITIVE_INFINITY), "infinite radius");
        assertThrows(NullPointerException.class,
                () -> sphere.setCenter(null), "null center");
        assertThrows(NullPointerException.class,
                () -> capsule.setOrientation(null), "null orientation");

        for (MutableCollider collider : new MutableCollider[] {box, sphere, obb, capsule}) {
            assertEquals(0L, collider.revision(),
                    collider.getClass().getSimpleName() + " must keep its revision");
            assertTrue(collider.snapshot().bounds().isPresent(),
                    collider.getClass().getSimpleName() + " must keep a valid snapshot");
        }
        assertEquals(new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)),
                ((SolidColliderSnapshot) box.snapshot()).solid());
    }

    @Test
    void setShapeIsAtomicWhenOnlyOneArgumentIsInvalid() {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        ColliderSnapshot before = sphere.snapshot();

        assertThrows(IllegalArgumentException.class,
                () -> sphere.setShape(new Vec3d(5.0D, 0.0D, 0.0D), -1.0D));

        assertEquals(0L, sphere.revision());
        assertEquals(ZERO, sphere.center(), "the valid argument of a failed setShape is not applied");
        assertSame(before, sphere.snapshot());
    }

    @Test
    void publishedSnapshotsStayStableAfterLaterMutations() {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        ColliderSnapshot first = sphere.snapshot();

        sphere.setShape(new Vec3d(10.0D, 0.0D, 0.0D), 4.0D);

        assertEquals(new Sphere(ZERO, 1.0D), ((SolidColliderSnapshot) first).solid(),
                "an earlier snapshot is immutable");
        assertEquals(new Sphere(new Vec3d(10.0D, 0.0D, 0.0D), 4.0D),
                ((SolidColliderSnapshot) sphere.snapshot()).solid());
        assertEquals(1L, sphere.revision());
    }

    @Test
    void disablingAndEnablingChangesQueryVisibilityWithoutLosingTheShape() {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);

        assertTrue(sphere.setEnabled(false));
        assertEquals(1L, sphere.revision());
        assertTrue(sphere.snapshot().bounds().isEmpty(), "a disabled collider is the empty set");
        assertEquals(new Sphere(ZERO, 1.0D), ((SolidColliderSnapshot) sphere.snapshot()).solid(),
                "disabling does not discard the shape");

        assertTrue(sphere.setEnabled(true));
        assertEquals(2L, sphere.revision());
        assertTrue(sphere.snapshot().bounds().isPresent());
    }

    @Test
    void placementChangesBoundsWithoutTouchingTheShape() {
        MutableAabbCollider box = new MutableAabbCollider(ZERO, new Vec3d(1.0D, 1.0D, 1.0D));
        RigidTransform3d moved =
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(5.0D, 0.0D, 0.0D));

        assertTrue(box.setLocalToParent(moved));

        Aabb bounds = box.snapshot().bounds().orElseThrow();
        assertEquals(5.0D, bounds.min().x(), 0.0D);
        assertEquals(6.0D, bounds.max().x(), 0.0D);
        assertEquals(new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)),
                ((SolidColliderSnapshot) box.snapshot()).solid(),
                "placement does not rewrite the local shape");
        assertEquals(1L, box.revision());
    }

    @Test
    void obbShapeOrientationIsIndependentOfTheColliderPlacement() {
        MutableObbCollider obb = new MutableObbCollider(
                ZERO, new Vec3d(1.0D, 0.5D, 0.5D), Rotation3d.identity());
        Rotation3d quarterTurn = new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D);

        assertTrue(obb.setOrientation(quarterTurn));

        assertEquals(quarterTurn, obb.orientation());
        assertEquals(RigidTransform3d.identity(), obb.localToParent(),
                "the shape orientation never becomes the collider placement");
        assertEquals(new Obb(ZERO, new Vec3d(1.0D, 0.5D, 0.5D), quarterTurn),
                ((SolidColliderSnapshot) obb.snapshot()).solid());
    }

    @Test
    void capsuleLengthIsCenterlineLengthOnTheLocalYAxis() {
        MutableCapsuleCollider capsule = new MutableCapsuleCollider(
                new Vec3d(0.0D, 1.0D, 0.0D), 2.0D, 0.5D, Rotation3d.identity());
        Capsule solid = (Capsule) ((SolidColliderSnapshot) capsule.snapshot()).solid();

        assertEquals(new Vec3d(0.0D, 0.0D, 0.0D), solid.centerline().start());
        assertEquals(new Vec3d(0.0D, 2.0D, 0.0D), solid.centerline().end());
        assertEquals(2.0D, solid.centerlineLength(), 1.0E-12D);
        assertEquals(3.0D, solid.exteriorLength(), 1.0E-12D,
                "the exterior length is centerline length plus two radii");
    }

    @Test
    void zeroLengthCapsuleDegeneratesToASphereAndZeroRadiusToItsCenterline() {
        MutableCapsuleCollider point = new MutableCapsuleCollider(
                ZERO, 0.0D, 0.5D, Rotation3d.identity());
        Capsule pointSolid = (Capsule) ((SolidColliderSnapshot) point.snapshot()).solid();
        assertEquals(pointSolid.centerline().start(), pointSolid.centerline().end(),
                "a zero centerline length is a point centerline");

        assertTrue(point.setRadius(0.0D));
        Capsule segmentSolid = (Capsule) ((SolidColliderSnapshot) point.snapshot()).solid();
        assertEquals(0.0D, segmentSolid.radius(), 0.0D);
        assertEquals(1L, point.revision());
    }

    @Test
    void rotatingTheCapsuleAxisMovesItsEndpointsWithoutChangingLength() {
        MutableCapsuleCollider capsule = new MutableCapsuleCollider(
                ZERO, 2.0D, 0.25D, Rotation3d.identity());
        Rotation3d quarterTurn = new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D);

        assertTrue(capsule.setOrientation(quarterTurn));

        Capsule solid = (Capsule) ((SolidColliderSnapshot) capsule.snapshot()).solid();
        assertEquals(2.0D, solid.centerlineLength(), 1.0E-12D);
        assertEquals(0.0D, solid.centerline().start().y(), 1.0E-12D,
                "a quarter turn about Z moves the local +Y axis off the Y axis");
        assertEquals(1.0D, Math.abs(solid.centerline().start().x()), 1.0E-12D);
    }

    @Test
    void revisionOverflowIsRejectedWithoutChangingAnyState() throws Exception {
        MutableSphereCollider sphere = new MutableSphereCollider(ZERO, 1.0D);
        Field revisionField = AbstractMutableCollider.class.getDeclaredField("revision");
        revisionField.setAccessible(true);
        revisionField.setLong(sphere, Long.MAX_VALUE);
        ColliderSnapshot before = sphere.snapshot();

        assertThrows(IllegalStateException.class, () -> sphere.setRadius(3.0D));
        assertThrows(IllegalStateException.class, () -> sphere.setEnabled(false));
        assertThrows(IllegalStateException.class, () -> sphere.setLocalToParent(
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D))));

        assertEquals(Long.MAX_VALUE, sphere.revision(), "the revision never wraps");
        assertEquals(1.0D, sphere.radius(), 0.0D, "the shape is unchanged");
        assertTrue(sphere.enabled(), "the enable flag is unchanged");
        assertEquals(RigidTransform3d.identity(), sphere.localToParent());
        assertSame(before, sphere.snapshot());
    }

    @Test
    void boundsMatchTheExistingPlacedSolidSemanticsForEachShape() {
        for (MutableCollider collider : allShapes()) {
            Solid3d solid = ((SolidColliderSnapshot) collider.snapshot()).solid();
            Aabb expected = new PlacedSolid3d(solid, RigidTransform3d.identity()).bounds();
            assertEquals(expected, collider.snapshot().bounds().orElseThrow(),
                    collider.getClass().getSimpleName()
                            + " must reuse the frozen placement semantics");
        }
    }

    private static MutableCollider[] allShapes() {
        return new MutableCollider[] {
                new MutableAabbCollider(ZERO, new Vec3d(1.0D, 1.0D, 1.0D)),
                new MutableSphereCollider(ZERO, 1.0D),
                new MutableObbCollider(ZERO, new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity()),
                new MutableCapsuleCollider(ZERO, 2.0D, 0.5D, Rotation3d.identity())};
    }
}
