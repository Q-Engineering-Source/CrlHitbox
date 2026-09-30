package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.GeometryIntersections;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Iterative dispatcher for two collider snapshots that share one parent frame.
 *
 * <p>The dispatcher handles the disabled/empty cases first. When both sides are plain leaves it takes
 * an allocation-free fast path straight to the typed narrow phase, because the narrow phase is
 * already the exact decision and a bounds test would only repeat it. When a compound is involved it
 * switches to an explicit stack, applies conservative negative pruning with the snapshots'
 * parent-frame bounds, and expands compounds iteratively so a user-controlled nesting depth cannot
 * overflow the JVM stack. A bounds overlap never returns {@code true}; only a typed narrow phase may.</p>
 *
 * <p>Each frame carries the transform from a snapshot's parent frame to the shared root frame, and an
 * identity transform is never composed, so the entity or world transform is neither stacked twice nor
 * rebuilt per query.</p>
 */
final class ColliderQueryDispatcher {
    private static final RigidTransform3d IDENTITY = RigidTransform3d.identity();

    private ColliderQueryDispatcher() {
    }

    /**
     * Returns whether two snapshots intersect.
     *
     * @param first the first snapshot, expressed in the shared parent frame
     * @param second the second snapshot, expressed in the same shared parent frame
     */
    static boolean intersects(ColliderSnapshot first, ColliderSnapshot second) {
        if (!first.enabled() || !second.enabled()) {
            return false;
        }
        if (!(first instanceof CompoundColliderSnapshot)
                && !(second instanceof CompoundColliderSnapshot)) {
            return leavesIntersect(first, IDENTITY, second, IDENTITY);
        }
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(first, IDENTITY, second, IDENTITY));
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            if (!frame.first().enabled() || !frame.second().enabled()) {
                continue;
            }
            if (boundsAreDisjoint(frame)) {
                continue;
            }
            if (frame.first() instanceof CompoundColliderSnapshot compound) {
                RigidTransform3d childToRoot = worldOf(compound, frame.firstToRoot());
                for (int index = compound.childCount() - 1; index >= 0; index--) {
                    stack.push(new Frame(compound.child(index), childToRoot,
                            frame.second(), frame.secondToRoot()));
                }
                continue;
            }
            if (frame.second() instanceof CompoundColliderSnapshot compound) {
                RigidTransform3d childToRoot = worldOf(compound, frame.secondToRoot());
                for (int index = compound.childCount() - 1; index >= 0; index--) {
                    stack.push(new Frame(frame.first(), frame.firstToRoot(),
                            compound.child(index), childToRoot));
                }
                continue;
            }
            if (leavesIntersect(frame.first(), frame.firstToRoot(),
                    frame.second(), frame.secondToRoot())) {
                return true;
            }
        }
        return false;
    }

    /** One pending pair: a snapshot plus the transform from its parent frame to the shared root. */
    private record Frame(
            ColliderSnapshot first,
            RigidTransform3d firstToRoot,
            ColliderSnapshot second,
            RigidTransform3d secondToRoot
    ) {
    }

    /** Returns whether the parent-frame bounds prove separation; bounds never prove a hit. */
    private static boolean boundsAreDisjoint(Frame frame) {
        Optional<Aabb> firstBounds = frame.first().bounds();
        Optional<Aabb> secondBounds = frame.second().bounds();
        if (firstBounds.isEmpty() || secondBounds.isEmpty()) {
            return true;
        }
        return !solidsIntersect(firstBounds.get(), frame.firstToRoot(),
                secondBounds.get(), frame.secondToRoot());
    }

    /** Runs the narrow phase for one pair of non-compound snapshots. */
    private static boolean leavesIntersect(
            ColliderSnapshot first,
            RigidTransform3d firstToRoot,
            ColliderSnapshot second,
            RigidTransform3d secondToRoot
    ) {
        if (first instanceof SolidColliderSnapshot firstSolid
                && second instanceof SolidColliderSnapshot secondSolid) {
            return solidsIntersect(firstSolid.solid(), worldOf(firstSolid, firstToRoot),
                    secondSolid.solid(), worldOf(secondSolid, secondToRoot));
        }
        if (first instanceof RayColliderSnapshot firstRay
                && second instanceof SolidColliderSnapshot secondSolid) {
            return rayIntersectsSolid(firstRay.ray(), worldOf(firstRay, firstToRoot),
                    secondSolid.solid(), worldOf(secondSolid, secondToRoot));
        }
        if (first instanceof SolidColliderSnapshot firstSolid
                && second instanceof RayColliderSnapshot secondRay) {
            return rayIntersectsSolid(secondRay.ray(), worldOf(secondRay, secondToRoot),
                    firstSolid.solid(), worldOf(firstSolid, firstToRoot));
        }
        if (first instanceof RayColliderSnapshot firstRay
                && second instanceof RayColliderSnapshot secondRay) {
            return RayRayPredicate.intersects(
                    transformSegment(firstRay.ray().asSegment(), worldOf(firstRay, firstToRoot)),
                    transformSegment(secondRay.ray().asSegment(), worldOf(secondRay, secondToRoot)));
        }
        throw new IllegalStateException("unsupported collider snapshot pair: "
                + first.getClass().getName() + " / " + second.getClass().getName());
    }

    /** Composes a snapshot placement with the frame it sits in, skipping identity work entirely. */
    private static RigidTransform3d worldOf(ColliderSnapshot snapshot, RigidTransform3d toRoot) {
        RigidTransform3d placement = snapshot.localToParent();
        return toRoot == IDENTITY || toRoot.equals(IDENTITY)
                ? placement
                : placement.andThen(toRoot);
    }

    /** Tests two solids, reusing the frozen typed kernel for the identity fast path. */
    private static boolean solidsIntersect(
            Solid3d first,
            RigidTransform3d firstWorld,
            Solid3d second,
            RigidTransform3d secondWorld
    ) {
        boolean firstIdentity = firstWorld == IDENTITY || firstWorld.equals(IDENTITY);
        boolean secondIdentity = secondWorld == IDENTITY || secondWorld.equals(IDENTITY);
        if (firstIdentity && secondIdentity) {
            return GeometryIntersections.intersects(first, second);
        }
        return GeometryIntersections.intersects(
                new PlacedSolid3d(first, firstWorld), new PlacedSolid3d(second, secondWorld));
    }

    /** Tests one finite ray against one solid, both placed into the shared root frame. */
    private static boolean rayIntersectsSolid(
            Ray3d ray,
            RigidTransform3d rayWorld,
            Solid3d solid,
            RigidTransform3d solidWorld
    ) {
        Segment3d segment = transformSegment(ray.asSegment(), rayWorld);
        if (solidWorld == IDENTITY || solidWorld.equals(IDENTITY)) {
            return GeometryIntersections.intersects(segment, solid);
        }
        return GeometryIntersections.intersects(segment, new PlacedSolid3d(solid, solidWorld));
    }

    private static Segment3d transformSegment(Segment3d segment, RigidTransform3d transform) {
        if (transform == IDENTITY || transform.equals(IDENTITY)) {
            return segment;
        }
        return new Segment3d(
                transform.transformPoint(segment.start()),
                transform.transformPoint(segment.end()));
    }
}
