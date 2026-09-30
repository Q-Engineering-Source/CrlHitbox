package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.GeometryDistances;
import dev.crlhitbox.api.geometry.Segment3d;

/**
 * Conservative closed-set predicate for two finite segments, shared by the public ray-ray query and
 * the collider dispatcher.
 *
 * <p>The predicate reports a shared point when the two segments have an exactly equal endpoint or
 * when the computed closest distance between them is exactly zero, and reports no intersection
 * otherwise. It therefore never promotes a near miss into a hit and introduces no tolerance; a
 * geometric intersection that binary64 arithmetic cannot resolve exactly is reported conservatively
 * as no intersection.</p>
 */
final class RayRayPredicate {
    private RayRayPredicate() {
    }

    /** Returns whether the two closed segments share at least one point. */
    static boolean intersects(Segment3d first, Segment3d second) {
        if (sharesAnEndpoint(first, second)) {
            return true;
        }
        return GeometryDistances.segmentToSegmentSquared(first, second) == 0.0D;
    }

    private static boolean sharesAnEndpoint(Segment3d first, Segment3d second) {
        return first.start().equals(second.start())
                || first.start().equals(second.end())
                || first.end().equals(second.start())
                || first.end().equals(second.end());
    }
}
