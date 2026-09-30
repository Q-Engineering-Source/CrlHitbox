package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice C acceptance: the mutable ray collider and the finite-ray query pairs. */
class RayColliderTest {
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);
    private static final Vec3d POSITIVE_X = new Vec3d(1.0D, 0.0D, 0.0D);

    @Test
    void rayColliderStartsEnabledAtRevisionZeroWithIdentityPlacement() {
        MutableRayCollider ray = new MutableRayCollider(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D);

        assertEquals(0L, ray.revision());
        assertTrue(ray.enabled());
        assertEquals(RigidTransform3d.identity(), ray.localToParent());
        assertEquals(new Vec3d(-3.0D, 0.0D, 0.0D), ray.origin());
        assertEquals(POSITIVE_X, ray.direction());
        assertEquals(5.0D, ray.length(), 0.0D);
        assertEquals(new Vec3d(2.0D, 0.0D, 0.0D), ray.end());
        assertInstanceOf(RayColliderSnapshot.class, ray.snapshot());
        assertEquals(new Ray3d(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D),
                ((RayColliderSnapshot) ray.snapshot()).ray());
        assertEquals(ray.snapshot().bounds().orElseThrow(),
                ((RayColliderSnapshot) ray.snapshot()).ray().bounds());
    }

    @Test
    void raySettersAdvanceTheRevisionOnceAndEqualUpdatesAreNoOps() {
        MutableRayCollider ray = new MutableRayCollider(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D);
        ColliderSnapshot before = ray.snapshot();

        assertTrue(ray.setLength(2.0D));
        assertEquals(1L, ray.revision());
        assertEquals(2.0D, ray.length(), 0.0D);
        assertFalse(ray.setLength(2.0D), "an equal length is a no-op");
        assertFalse(ray.setOrigin(new Vec3d(-3.0D, 0.0D, 0.0D)));
        assertFalse(ray.setDirection(new Vec3d(2.0D, 0.0D, 0.0D)),
                "a proportional direction normalizes to the same unit value");
        assertEquals(1L, ray.revision(), "no-ops never advance the revision");
        assertTrue(before != ray.snapshot(), "the first update published a new snapshot");
    }

    @Test
    void invalidRayUpdatesKeepThePreviousState() {
        MutableRayCollider ray = new MutableRayCollider(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D);
        ColliderSnapshot before = ray.snapshot();

        assertThrows(IllegalArgumentException.class, () -> ray.setDirection(ZERO));
        assertThrows(IllegalArgumentException.class, () -> ray.setLength(-1.0D));
        assertThrows(IllegalArgumentException.class, () -> ray.setLength(Double.NaN));
        assertThrows(NullPointerException.class, () -> ray.setOrigin(null));
        assertThrows(IllegalArgumentException.class,
                () -> ray.setShape(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), POSITIVE_X,
                        Double.MAX_VALUE),
                "an unrepresentable end point is rejected");

        assertEquals(0L, ray.revision());
        assertEquals(5.0D, ray.length(), 0.0D);
        assertEquals(new Vec3d(-3.0D, 0.0D, 0.0D), ray.origin());
        assertEquals(before, ray.snapshot());
    }

    @Test
    void disabledRayIsTheEmptySetAndPlacementMovesItsBounds() {
        MutableRayCollider ray = new MutableRayCollider(ZERO, POSITIVE_X, 2.0D);

        assertTrue(ray.setEnabled(false));
        assertTrue(ray.snapshot().bounds().isEmpty());
        assertTrue(ray.setEnabled(true));

        assertTrue(ray.setLocalToParent(
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D))));
        Aabb bounds = ray.snapshot().bounds().orElseThrow();
        assertEquals(10.0D, bounds.min().x(), 0.0D);
        assertEquals(12.0D, bounds.max().x(), 0.0D);
    }

    @Test
    void finiteRayLengthClipsTheQueryAgainstEverySolidShape() {
        SolidShapeCases cases = new SolidShapeCases();

        assertTrue(ColliderQueries.intersects(cases.reaching, cases.aabb), "reaching the box");
        assertFalse(ColliderQueries.intersects(cases.tooShort, cases.aabb),
                "a ray that stops before the box does not hit it");
        assertTrue(ColliderQueries.intersects(cases.reaching, cases.sphere));
        assertFalse(ColliderQueries.intersects(cases.tooShort, cases.sphere));
        assertTrue(ColliderQueries.intersects(cases.reaching, cases.obb));
        assertFalse(ColliderQueries.intersects(cases.tooShort, cases.obb));
        assertTrue(ColliderQueries.intersects(cases.reaching, cases.capsule));
        assertFalse(ColliderQueries.intersects(cases.tooShort, cases.capsule));
    }

    @Test
    void tangentialContactStillCountsAsAnIntersection() {
        Aabb box = new Aabb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D));
        Ray3d touchingCorner = new Ray3d(new Vec3d(-1.0D, 0.0D, 0.0D), POSITIVE_X, 1.0D);
        Sphere sphere = new Sphere(ZERO, 1.0D);
        Ray3d tangent = new Ray3d(new Vec3d(-3.0D, 1.0D, 0.0D), POSITIVE_X, 5.0D);

        assertTrue(ColliderQueries.intersects(touchingCorner, box),
                "the closed ray ends exactly on the closed box corner");
        assertTrue(ColliderQueries.intersects(tangent, sphere),
                "a tangent touch is an intersection under the closed-set rule");
    }

    @Test
    void rotatingTheShapeChangesWhetherTheRayHitsIt() {
        Obb flat = new Obb(ZERO, new Vec3d(1.0D, 0.5D, 0.5D), Rotation3d.identity());
        Obb upright = new Obb(ZERO, new Vec3d(1.0D, 0.5D, 0.5D),
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D));
        Ray3d high = new Ray3d(new Vec3d(-3.0D, 0.9D, 0.0D), POSITIVE_X, 5.0D);

        assertFalse(ColliderQueries.intersects(high, flat), "0.9 is outside the flat half extent");
        assertTrue(ColliderQueries.intersects(high, upright),
                "the quarter turn brings 0.9 inside the box");
    }

    @Test
    void placedSolidsUseTheParentFrameOfThePlacement() {
        PlacedSolid3d placedSphere = new PlacedSolid3d(
                new Sphere(ZERO, 1.0D),
                new RigidTransform3d(Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D)));
        Ray3d reaching = new Ray3d(new Vec3d(7.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D);
        Ray3d tooShort = new Ray3d(new Vec3d(7.0D, 0.0D, 0.0D), POSITIVE_X, 1.5D);

        assertTrue(ColliderQueries.intersects(reaching, placedSphere));
        assertFalse(ColliderQueries.intersects(tooShort, placedSphere),
                "the ray stops well before the placed sphere's closed bound");
    }

    @Test
    void crossingRaysIntersect() {
        Ray3d horizontal = new Ray3d(ZERO, POSITIVE_X, 2.0D);
        Ray3d vertical = new Ray3d(new Vec3d(1.0D, -1.0D, 0.0D), new Vec3d(0.0D, 1.0D, 0.0D), 2.0D);

        assertTrue(ColliderQueries.intersects(horizontal, vertical));
        assertTrue(ColliderQueries.intersects(vertical, horizontal), "the predicate is symmetric");
    }

    @Test
    void collinearOverlapIntersectsAndCollinearSeparationDoesNot() {
        Ray3d first = new Ray3d(ZERO, POSITIVE_X, 2.0D);
        Ray3d overlapping = new Ray3d(new Vec3d(1.0D, 0.0D, 0.0D), POSITIVE_X, 2.0D);
        Ray3d separated = new Ray3d(new Vec3d(3.0D, 0.0D, 0.0D), POSITIVE_X, 1.0D);

        assertTrue(ColliderQueries.intersects(first, overlapping));
        assertFalse(ColliderQueries.intersects(first, separated));
    }

    @Test
    void sharedEndpointIntersectsWithoutAnyDistanceComputation() {
        Ray3d first = new Ray3d(ZERO, POSITIVE_X, 1.0D);
        Ray3d second = new Ray3d(new Vec3d(1.0D, 0.0D, 0.0D), POSITIVE_X, 1.0D);
        Ray3d reverse = new Ray3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(-1.0D, 0.0D, 0.0D), 1.0D);

        assertTrue(ColliderQueries.intersects(first, second));
        assertTrue(ColliderQueries.intersects(first, reverse));
    }

    @Test
    void parallelRaysThatOnlyMeetOnTheirExtensionsDoNotIntersect() {
        Ray3d first = new Ray3d(ZERO, POSITIVE_X, 1.0D);
        Ray3d offsetParallel = new Ray3d(new Vec3d(0.0D, 1.0D, 0.0D), POSITIVE_X, 1.0D);
        Ray3d extendedOnly = new Ray3d(new Vec3d(2.0D, -1.0D, 0.0D), new Vec3d(0.0D, 1.0D, 0.0D), 2.0D);

        assertFalse(ColliderQueries.intersects(first, offsetParallel),
                "parallel rays at different offsets never meet");
        assertFalse(ColliderQueries.intersects(first, extendedOnly),
                "the crossing lies beyond the first ray's finite length");
    }

    @Test
    void nearlyTouchingRaysAreNotTreatedAsIntersecting() {
        Ray3d first = new Ray3d(ZERO, POSITIVE_X, 2.0D);
        Ray3d nearlyTouching = new Ray3d(new Vec3d(0.0D, 1.0E-9D, 0.0D), POSITIVE_X, 2.0D);

        assertFalse(ColliderQueries.intersects(first, nearlyTouching),
                "a near miss is never reported as an intersection; there is no tolerance");
    }

    @Test
    void zeroLengthRaysBehaveAsPoints() {
        Ray3d pointOnSegment = new Ray3d(new Vec3d(1.0D, 0.0D, 0.0D), POSITIVE_X, 0.0D);
        Ray3d pointOffSegment = new Ray3d(new Vec3d(1.0D, 0.5D, 0.0D), POSITIVE_X, 0.0D);
        Ray3d host = new Ray3d(ZERO, POSITIVE_X, 2.0D);

        assertTrue(ColliderQueries.intersects(pointOnSegment, host));
        assertFalse(ColliderQueries.intersects(pointOffSegment, host));
    }

    @Test
    void extremeScalesKeepTheRayRayPredicateReliable() {
        Ray3d tiny = new Ray3d(ZERO, POSITIVE_X, Double.MIN_NORMAL);
        Ray3d farCrossing = new Ray3d(
                new Vec3d(1.0D, -1.0D, 0.0D), new Vec3d(0.0D, 1.0D, 0.0D), 2.0D);
        Ray3d huge = new Ray3d(ZERO, POSITIVE_X, 1.0E150D);
        Ray3d hugeSeparated = new Ray3d(
                new Vec3d(0.0D, 1.0E150D, 0.0D), POSITIVE_X, 1.0E150D);

        assertFalse(ColliderQueries.intersects(tiny, farCrossing),
                "a ray far shorter than the gap never reaches the other ray");
        assertFalse(ColliderQueries.intersects(huge, hugeSeparated),
                "widely separated parallel rays never meet");
    }

    @Test
    void rayQueriesRejectNullArguments() {
        Ray3d ray = new Ray3d(ZERO, POSITIVE_X, 1.0D);
        assertThrows(NullPointerException.class, () -> ColliderQueries.intersects(ray, (Sphere) null));
        assertThrows(NullPointerException.class,
                () -> ColliderQueries.intersects(ray, (PlacedSolid3d) null));
        assertThrows(NullPointerException.class, () -> ColliderQueries.intersects(ray, (Ray3d) null));
        assertThrows(NullPointerException.class,
                () -> ColliderQueries.intersects(null, new Sphere(ZERO, 1.0D)));
    }

    /** Fixed local shapes plus one reaching and one too-short ray along the positive X axis. */
    private static final class SolidShapeCases {
        private final Ray3d reaching = new Ray3d(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 5.0D);
        private final Ray3d tooShort = new Ray3d(new Vec3d(-3.0D, 0.0D, 0.0D), POSITIVE_X, 1.0D);
        private final Aabb aabb = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D),
                new Vec3d(1.0D, 1.0D, 1.0D));
        private final Sphere sphere = new Sphere(ZERO, 1.0D);
        private final Obb obb = new Obb(ZERO, new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        private final Capsule capsule = new Capsule(
                new Segment3d(new Vec3d(0.0D, -1.0D, 0.0D), new Vec3d(0.0D, 1.0D, 0.0D)), 0.5D);
    }
}
