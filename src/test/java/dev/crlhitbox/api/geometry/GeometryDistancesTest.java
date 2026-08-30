package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryDistancesTest {
    @Test
    void pointToSegmentCoversEndpointsInteriorOutsideAndEveryDegenerateCombination() {
        Segment3d line = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(4.0D, 0.0D, 0.0D));
        assertEquals(0.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(0.0D, 0.0D, 0.0D), line), "start");
        assertEquals(0.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(4.0D, 0.0D, 0.0D), line), "end");
        assertEquals(0.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(2.0D, 0.0D, 0.0D), line), "interior");
        assertEquals(5.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(-1.0D, 2.0D, 0.0D), line), "before");
        assertEquals(5.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(5.0D, 2.0D, 0.0D), line), "after");
        Segment3d point = new Segment3d(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(1.0D, 2.0D, 3.0D));
        assertEquals(0.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(1.0D, 2.0D, 3.0D), point), "zero segment at point");
        assertEquals(9.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(1.0D, 5.0D, 3.0D), point), "zero segment away");

        Segment3d otherPoint = new Segment3d(new Vec3d(1.0D, 5.0D, 3.0D), new Vec3d(1.0D, 5.0D, 3.0D));
        assertEquals(9.0D, GeometryDistances.segmentToSegmentSquared(point, otherPoint), "both degenerate");
        assertEquals(4.0D, GeometryDistances.segmentToSegmentSquared(line, new Segment3d(new Vec3d(2.0D, 2.0D, 0.0D), new Vec3d(2.0D, 2.0D, 0.0D))), "right degenerate");
        assertEquals(4.0D, GeometryDistances.segmentToSegmentSquared(new Segment3d(new Vec3d(2.0D, 2.0D, 0.0D), new Vec3d(2.0D, 2.0D, 0.0D)), line), "left degenerate");
    }

    @Test
    void distanceResultsAreCanonicalNonnegativeAndOverflowIsExplicitInfinity() {
        Segment3d line = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Obb obb = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D), Rotation3d.identity());
        double[] results = {
                GeometryDistances.pointToSegmentSquared(new Vec3d(0.0D, 0.0D, 0.0D), line),
                GeometryDistances.segmentToSegmentSquared(line, line),
                GeometryDistances.pointToAabbSquared(new Vec3d(0.0D, 0.0D, 0.0D), box),
                GeometryDistances.pointToObbSquared(new Vec3d(0.0D, 0.0D, 0.0D), obb)
        };
        for (double result : results) {
            assertTrue(!Double.isNaN(result) && result >= 0.0D, "valid distance must be nonnegative and non-NaN: " + result);
            assertEquals(0L, Double.doubleToRawLongBits(result) >>> 63, "valid zero must be canonical +0");
        }
        assertEquals(Double.POSITIVE_INFINITY, GeometryDistances.pointToSegmentSquared(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), new Segment3d(new Vec3d(-Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(-Double.MAX_VALUE, 0.0D, 0.0D))));
    }
    @Test
    void pointToSegmentUsesFiniteProjectionAndAcceptsZeroLengthSegments() {
        Segment3d segment = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(4.0D, 0.0D, 0.0D));
        assertEquals(0.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(2.0D, 0.0D, 0.0D), segment));
        assertEquals(5.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(-1.0D, 2.0D, 0.0D), segment));
        assertEquals(5.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(5.0D, 2.0D, 0.0D), segment));
        assertEquals(4.0D, GeometryDistances.pointToSegmentSquared(new Vec3d(1.0D, 2.0D, 2.0D), new Segment3d(new Vec3d(1.0D, 2.0D, 0.0D), new Vec3d(1.0D, 2.0D, 0.0D))));
    }

    @Test
    void segmentDistanceHandlesSkewParallelAndDegenerateSegments() {
        Segment3d first = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(4.0D, 0.0D, 0.0D));
        Segment3d crossing = new Segment3d(new Vec3d(2.0D, -1.0D, 0.0D), new Vec3d(2.0D, 1.0D, 0.0D));
        Segment3d parallel = new Segment3d(new Vec3d(0.0D, 3.0D, 0.0D), new Vec3d(4.0D, 3.0D, 0.0D));
        Segment3d skew = new Segment3d(new Vec3d(2.0D, -1.0D, 2.0D), new Vec3d(2.0D, 1.0D, 2.0D));
        Segment3d point = new Segment3d(new Vec3d(0.0D, 3.0D, 0.0D), new Vec3d(0.0D, 3.0D, 0.0D));

        assertEquals(0.0D, GeometryDistances.segmentToSegmentSquared(first, crossing));
        assertEquals(9.0D, GeometryDistances.segmentToSegmentSquared(first, parallel));
        assertEquals(4.0D, GeometryDistances.segmentToSegmentSquared(first, skew));
        assertEquals(9.0D, GeometryDistances.segmentToSegmentSquared(first, point));
        assertEquals(0.0D, GeometryDistances.segmentToSegmentSquared(first, new Segment3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D))));
    }

    @Test
    void nearParallelSegmentsThatCrossInTheirInteriorsHaveZeroDistance() {
        Segment3d first = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Segment3d second = new Segment3d(new Vec3d(0.0D, -5.0E-9D, 0.0D), new Vec3d(1.0D, 5.0E-9D, 0.0D));
        assertEquals(0.0D, GeometryDistances.segmentToSegmentSquared(first, second));
    }

    @Test
    void segmentDistanceUsesStableInteriorCandidateAndAllBoundaryCandidates() {
        Segment3d horizontal = new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Segment3d illConditionedInteriorCrossing = new Segment3d(new Vec3d(0.0D, -1.0E-8D, 0.0D), new Vec3d(1.0D, 1.0E-8D, 0.0D));
        Segment3d endpointMinimum = new Segment3d(new Vec3d(2.0D, 1.0D, 0.0D), new Vec3d(3.0D, 1.0D, 0.0D));

        assertEquals(0.0D, GeometryDistances.segmentToSegmentSquared(horizontal, illConditionedInteriorCrossing));
        assertEquals(2.0D, GeometryDistances.segmentToSegmentSquared(horizontal, endpointMinimum));
    }

    @Test
    void aabbAndObbDistancesUseClosedBoundaries() {
        Aabb box = new Aabb(new Vec3d(-1.0D, -1.0D, -1.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        Obb obb = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 1.0D), new Rotation3d(0.0D, 0.0D, Math.sqrt(0.5D), Math.sqrt(0.5D)));
        assertEquals(0.0D, GeometryDistances.pointToAabbSquared(new Vec3d(1.0D, 0.0D, 0.0D), box));
        assertEquals(0.0D, GeometryDistances.pointToAabbSquared(new Vec3d(0.0D, 0.0D, 0.0D), box));
        assertEquals(0.0D, GeometryDistances.pointToAabbSquared(new Vec3d(1.0D, 1.0D, 1.0D), box));
        assertEquals(9.0D, GeometryDistances.pointToAabbSquared(new Vec3d(4.0D, 0.0D, 0.0D), box));
        assertEquals(0.0D, GeometryDistances.pointToObbSquared(new Vec3d(0.0D, 1.0D, 0.0D), obb));
        GeometryTestSupport.assertClose(1.0D, GeometryDistances.pointToObbSquared(new Vec3d(3.0D, 0.0D, 0.0D), obb));
    }

    @Test
    void obbDistanceUsesRelativeScaleAndRecognizesExactlyReconstructedCorners() {
        double shift = Double.MAX_VALUE;
        Obb unshiftedPoint = new Obb(new Vec3d(1.0E-16D, 0.0D, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        Obb shiftedPoint = new Obb(new Vec3d(1.0E-16D, shift, 0.0D), new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity());
        double unshiftedDistance = GeometryDistances.pointToObbSquared(new Vec3d(0.0D, 0.0D, 0.0D), unshiftedPoint);
        double shiftedDistance = GeometryDistances.pointToObbSquared(new Vec3d(0.0D, shift, 0.0D), shiftedPoint);

        assertTrue(unshiftedDistance > 0.0D);
        assertEquals(unshiftedDistance, shiftedDistance);

        Obb obb = reviewerCornerObb();
        Vec3d corner = obb.localToWorld(new Vec3d(-obb.halfExtents().x(), -obb.halfExtents().y(), -obb.halfExtents().z()));
        assertTrue(obb.contains(corner));
        assertEquals(0.0D, GeometryDistances.pointToObbSquared(corner, obb));
    }

    private static Obb reviewerCornerObb() {
        return new Obb(
                new Vec3d(-0.7082650730663893D, -6.731064378241012D, -9.70759371599838D),
                new Vec3d(7.52805839645041D, 3.4219451865795945D, 0.25667266740287853D),
                new Rotation3d(-0.005971366638466002D, -0.627596208544786D, 0.16834333991315123D, 0.7600972712143912D)
        );
    }
}
