package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.GeometryIntersections;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.Objects;

/**
 * Unified collider query entry point.
 *
 * <p>Every method is a pure query: it reads its arguments, never fires an event, applies no damage,
 * touches no world state, and logs nothing. The frozen geometry package remains the sole owner of the
 * narrow phases, and a ray is converted to its finite segment form only for the duration of one
 * query, so the published ray origin and direction are never rewritten.</p>
 *
 * <p>Both arguments of the snapshot entry must already be expressed in the same caller-defined parent
 * frame; the API cannot detect a caller's frame mismatch.</p>
 */
public final class ColliderQueries {
    private ColliderQueries() {
    }

    /**
     * Tests two collider snapshots in one shared parent frame.
     *
     * <p>Disabled snapshots and empty compounds represent the empty set and never intersect. Bounds
     * are used for conservative negative pruning only. Compound children may be solids, rays or
     * further compounds, and deep trees are traversed iteratively.</p>
     *
     * @return whether the two closed sets share at least one point
     */
    public static boolean intersects(ColliderSnapshot first, ColliderSnapshot second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return ColliderQueryDispatcher.intersects(first, second);
    }

    /**
     * Tests one finite ray against one solid.
     *
     * @return whether the closed ray set meets the closed solid
     */
    public static boolean intersects(Ray3d ray, Solid3d solid) {
        Objects.requireNonNull(ray, "ray");
        Objects.requireNonNull(solid, "solid");
        return GeometryIntersections.intersects(ray.asSegment(), solid);
    }

    /**
     * Tests one finite ray against one rigidly placed solid.
     *
     * @return whether the ray meets the placed solid in the placement's parent frame
     */
    public static boolean intersects(Ray3d ray, PlacedSolid3d solid) {
        Objects.requireNonNull(ray, "ray");
        Objects.requireNonNull(solid, "solid");
        return GeometryIntersections.intersects(ray.asSegment(), solid);
    }

    /**
     * Tests two finite rays for a shared point.
     *
     * <p>The predicate is conservative: it reports a shared endpoint or an exactly zero computed
     * closest distance and nothing else, so a near miss is never reported as an intersection and no
     * tolerance is introduced.</p>
     *
     * @return whether the two closed ray sets share at least one point
     */
    public static boolean intersects(Ray3d first, Ray3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return RayRayPredicate.intersects(first.asSegment(), second.asSegment());
    }
}
