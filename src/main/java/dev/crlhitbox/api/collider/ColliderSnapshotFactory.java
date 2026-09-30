package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal construction support shared by the collider snapshots.
 *
 * <p>It performs the validation and one-time bounds precomputation that every snapshot needs, and it
 * routes all placement arithmetic through the frozen geometry package so this layer never duplicates
 * projection policy.</p>
 */
final class ColliderSnapshotFactory {
    private ColliderSnapshotFactory() {
    }

    static Solid3d requireSolid(Solid3d solid) {
        return Objects.requireNonNull(solid, "solid");
    }

    static Ray3d requireRay(Ray3d ray) {
        return Objects.requireNonNull(ray, "ray");
    }

    static RigidTransform3d requireTransform(RigidTransform3d transform) {
        return Objects.requireNonNull(transform, "localToParent");
    }

    /** Returns one solid's local conservative bounds. */
    static Aabb localBoundsOf(Solid3d solid) {
        return solid.bounds();
    }

    /** Copies a child list defensively, rejecting {@code null} lists and {@code null} children. */
    static ColliderSnapshot[] copyChildren(List<? extends ColliderSnapshot> children) {
        Objects.requireNonNull(children, "children");
        ColliderSnapshot[] copy = children.toArray(ColliderSnapshot[]::new);
        for (int index = 0; index < copy.length; index++) {
            Objects.requireNonNull(copy[index], "children[" + index + "]");
        }
        return copy;
    }

    /**
     * Returns the coordinate-wise union of the children's parent-frame bounds, or {@code null} when no
     * child contributes bounds (the empty set).
     */
    static Aabb unionOfChildren(ColliderSnapshot[] children) {
        Aabb union = null;
        for (ColliderSnapshot child : children) {
            Optional<Aabb> childBounds = child.bounds();
            if (childBounds.isEmpty()) {
                continue;
            }
            union = union == null ? childBounds.get() : unionOf(union, childBounds.get());
        }
        return union;
    }

    /**
     * Places local bounds into the parent frame, or returns {@code null} for the empty set.
     *
     * @throws IllegalArgumentException when the placement cannot represent finite bounds
     */
    static Aabb placeBounds(Aabb localBounds, RigidTransform3d localToParent, boolean enabled) {
        if (!enabled || localBounds == null) {
            return null;
        }
        return new PlacedSolid3d(localBounds, localToParent).bounds();
    }

    private static Aabb unionOf(Aabb first, Aabb second) {
        return new Aabb(first.min().min(second.min()), first.max().max(second.max()));
    }
}
