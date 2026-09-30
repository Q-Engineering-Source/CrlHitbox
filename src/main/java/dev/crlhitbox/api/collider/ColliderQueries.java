package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.GeometryDistances;
import dev.crlhitbox.api.geometry.GeometryIntersections;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;

import java.util.Objects;

/**
 * Unified collider query entry point.
 *
 * <p>This slice provides the finite-ray pairs. Every method is a pure query: it reads its arguments,
 * allocates nothing observable, and never fires an event, applies damage, or touches the world. The
 * frozen geometry package remains the sole owner of the narrow phases, and the ray is converted to
 * its finite segment form only for the duration of one query, so the published ray origin and
 * direction are never rewritten.</p>
 */
public final class ColliderQueries {
    private ColliderQueries() {
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
     * <p>The predicate is conservative in the strict sense: it returns {@code true} for a shared
     * endpoint or when the computed closest distance between the two closed segments is exactly zero,
     * and {@code false} otherwise. A near miss is never reported as an intersection, so no tolerance
     * is introduced; a geometric intersection that binary64 arithmetic cannot resolve exactly is
     * reported as no intersection rather than guessed.</p>
     *
     * @return whether the two closed ray sets share at least one point
     */
    public static boolean intersects(Ray3d first, Ray3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Segment3d firstSegment = first.asSegment();
        Segment3d secondSegment = second.asSegment();
        if (sharesAnEndpoint(firstSegment, secondSegment)) {
            return true;
        }
        return GeometryDistances.segmentToSegmentSquared(firstSegment, secondSegment) == 0.0D;
    }

    private static boolean sharesAnEndpoint(Segment3d first, Segment3d second) {
        return first.start().equals(second.start())
                || first.start().equals(second.end())
                || first.end().equals(second.start())
                || first.end().equals(second.end());
    }
}
