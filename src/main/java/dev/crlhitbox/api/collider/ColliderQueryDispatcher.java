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
 * <p>The dispatcher handles the disabled/empty cases first, then applies conservative negative
 * pruning with the snapshots' parent-frame bounds, and only then runs a real narrow phase. A bounds
 * overlap never returns {@code true}; only a typed narrow phase may.</p>
 *
 * <p>Compounds are expanded with an explicit stack rather than recursion, so a user-controlled
 * nesting depth cannot overflow the JVM stack, and each expansion composes the child placement with
 * the parent placement exactly once (child parent frame to root). Each frame carries the transform
 * from a snapshot's parent frame to the shared root frame, so the entity or world transform is never
 * stacked twice.</p>
 */
final class ColliderQueryDispatcher {
    private ColliderQueryDispatcher() {
    }

    /**
     * Returns whether two snapshots intersect.
     *
     * @param first the first snapshot, expressed in the shared parent frame
     * @param second the second snapshot, expressed in the same shared parent frame
     */
    static boolean intersects(ColliderSnapshot first, ColliderSnapshot second) {
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(
                first, RigidTransform3d.identity(), second, RigidTransform3d.identity()));
        while (!stack.isEmpty()) {
            Frame frame = stack.pop();
            if (!frame.first().enabled() || !frame.second().enabled()) {
                continue;
            }
            if (boundsAreDisjoint(frame)) {
                continue;
            }
            if (frame.first() instanceof CompoundColliderSnapshot compound) {
                RigidTransform3d childToRoot = compound.localToParent().andThen(frame.firstToRoot());
                for (int index = compound.childCount() - 1; index >= 0; index--) {
                    stack.push(new Frame(compound.child(index), childToRoot,
                            frame.second(), frame.secondToRoot()));
                }
                continue;
            }
            if (frame.second() instanceof CompoundColliderSnapshot compound) {
                RigidTransform3d childToRoot = compound.localToParent().andThen(frame.secondToRoot());
                for (int index = compound.childCount() - 1; index >= 0; index--) {
                    stack.push(new Frame(frame.first(), frame.firstToRoot(),
                            compound.child(index), childToRoot));
                }
                continue;
            }
            if (leafIntersects(frame)) {
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
    private static boolean leafIntersects(Frame frame) {
        RigidTransform3d firstWorld = frame.first().localToParent().andThen(frame.firstToRoot());
        RigidTransform3d secondWorld = frame.second().localToParent().andThen(frame.secondToRoot());
        if (frame.first() instanceof SolidColliderSnapshot first
                && frame.second() instanceof SolidColliderSnapshot second) {
            return solidsIntersect(first.solid(), firstWorld, second.solid(), secondWorld);
        }
        if (frame.first() instanceof RayColliderSnapshot first
                && frame.second() instanceof SolidColliderSnapshot second) {
            return rayIntersectsSolid(first.ray(), firstWorld, second.solid(), secondWorld);
        }
        if (frame.first() instanceof SolidColliderSnapshot first
                && frame.second() instanceof RayColliderSnapshot second) {
            return rayIntersectsSolid(second.ray(), secondWorld, first.solid(), firstWorld);
        }
        if (frame.first() instanceof RayColliderSnapshot first
                && frame.second() instanceof RayColliderSnapshot second) {
            return RayRayPredicate.intersects(
                    transformSegment(first.ray().asSegment(), firstWorld),
                    transformSegment(second.ray().asSegment(), secondWorld));
        }
        throw new IllegalStateException("unsupported collider snapshot pair: "
                + frame.first().getClass().getName() + " / " + frame.second().getClass().getName());
    }

    /** Tests two solids, reusing the frozen typed kernel for the identity fast path. */
    private static boolean solidsIntersect(
            Solid3d first,
            RigidTransform3d firstWorld,
            Solid3d second,
            RigidTransform3d secondWorld
    ) {
        boolean firstIdentity = firstWorld.equals(RigidTransform3d.identity());
        boolean secondIdentity = secondWorld.equals(RigidTransform3d.identity());
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
        if (solidWorld.equals(RigidTransform3d.identity())) {
            return GeometryIntersections.intersects(segment, solid);
        }
        return GeometryIntersections.intersects(segment, new PlacedSolid3d(solid, solidWorld));
    }

    private static Segment3d transformSegment(Segment3d segment, RigidTransform3d transform) {
        if (transform.equals(RigidTransform3d.identity())) {
            return segment;
        }
        return new Segment3d(
                transform.transformPoint(segment.start()),
                transform.transformPoint(segment.end()));
    }
}
