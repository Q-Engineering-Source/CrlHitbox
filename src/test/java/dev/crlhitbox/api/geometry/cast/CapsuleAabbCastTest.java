package dev.crlhitbox.api.geometry.cast;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.Kind.*;

class CapsuleAabbCastTest {
    private static final Vec3d ZERO = new Vec3d(0, 0, 0);
    private static final Aabb UNIT_BOX = new Aabb(new Vec3d(0, 0, 0), new Vec3d(1, 1, 1));

    @Test
    void ordinaryPlaneTangencyUsesTheWholeRequestSlopAndNeverInventsASeparatedPrefix() {
        var result = CapsuleAabbCast3d.cast(pointCapsule(-0.25, 0.5, 0.5, 0.25), new Vec3d(0, 0.125, 0), UNIT_BOX);
        assertEquals(CLEAR_INITIAL_TANGENT, result.kind());
        assertEquals(CapsuleAabbCastResult3d.StartRelation.NEAR_CONTACT, result.startRelation());
        assertFalse(result.safePrefixEnd().isPresent());
        assertTrue(result.bandBracket().isEmpty());
        assertTrue(result.reason().isEmpty());
        assertEquals(new Vec3d(-1, 0, 0), result.normalEstimate().orElseThrow().direction());
        assertEquals(0.0, result.normalEstimate().orElseThrow().sourceFraction());
        assertEquals(new CapsuleAabbCastResult3d.WorkCounts(1, 0, 0), result.work());
    }

    private static Capsule pointCapsule(double x, double y, double z, double radius) {
        var center = new Vec3d(x, y, z);
        return new Capsule(new Segment3d(center, center), radius);
    }

    @Test
    void cornerStartWithinSlopWithoutWholeRequestCertificateStopsAsNearBandHit() {
        double h = 0x1.6a09e667a5fddp3;
        var result = CapsuleAabbCast3d.cast(pointCapsule(-h, -h, 0.5, 16), new Vec3d(16, -16, 0), UNIT_BOX);
        assertEquals(HIT_INITIAL_NEAR_BAND, result.kind());
        assertEquals(CapsuleAabbCastResult3d.StartRelation.NEAR_CONTACT, result.startRelation());
        assertEquals(new CapsuleAabbCastResult3d.WorkCounts(1, 0, 0), result.work());
        assertTrue(result.safePrefixEnd().isEmpty());
        assertTrue(result.bandBracket().isEmpty());
        assertTrue(result.reason().isEmpty());
        assertEquals(0.0, result.normalEstimate().orElseThrow().sourceFraction());
        var scale = result.scale().orElseThrow();
        BigDecimal squaredDistance = new BigDecimal(h).pow(2).multiply(BigDecimal.TWO);
        assertTrue(squaredDistance.compareTo(new BigDecimal(16).subtract(new BigDecimal(scale.penetrationSlop())).pow(2)) >= 0);
        assertTrue(squaredDistance.compareTo(new BigDecimal(16).add(new BigDecimal(scale.contactBandWidth())).pow(2)) <= 0);
    }

    @Test
    void touchingInwardAndOutwardAreTerminalInitialResults() {
        var capsule = pointCapsule(-0.25, 0.5, 0.5, 0.25);
        var inward = CapsuleAabbCast3d.cast(capsule, new Vec3d(1, 0, 0), UNIT_BOX);
        var outward = CapsuleAabbCast3d.cast(capsule, new Vec3d(-1, 0, 0), UNIT_BOX);
        assertEquals(HIT_INITIAL_INWARD, inward.kind());
        assertEquals(CLEAR_INITIAL_OUTWARD, outward.kind());
        for (var result : new CapsuleAabbCastResult3d[]{inward, outward}) {
            assertTrue(result.safePrefixEnd().isEmpty());
            assertTrue(result.bandBracket().isEmpty());
            assertEquals(new CapsuleAabbCastResult3d.WorkCounts(1, 0, 0), result.work());
        }
    }

    @Test
    void shallowNegativeGapUsesSlopButDeepPenetrationDoesNotOfferAnMtv() {
        var shallow = CapsuleAabbCast3d.cast(pointCapsule(-0.25 + 0x1.0p-31, 0.5, 0.5, 0.25),
                new Vec3d(0, 0.125, 0), UNIT_BOX);
        var deep = CapsuleAabbCast3d.cast(pointCapsule(-0.25 + 0x1.0p-29, 0.5, 0.5, 0.25),
                new Vec3d(-1, 0, 0), UNIT_BOX);
        assertEquals(CLEAR_INITIAL_TANGENT, shallow.kind());
        assertTrue(shallow.safePrefixEnd().isEmpty());
        assertEquals(INITIAL_OVERLAP, deep.kind());
        assertEquals(CapsuleAabbCastResult3d.StartRelation.OVERLAP, deep.startRelation());
        assertTrue(deep.normalEstimate().isEmpty());
        assertTrue(deep.safePrefixEnd().isEmpty());
    }

    @Test
    void zeroDeltaClassifiesTheActualStartRatherThanAutomaticallyClearingIt() {
        assertEquals(CLEAR_SEPARATED, CapsuleAabbCast3d.cast(pointCapsule(-1, 0.5, 0.5, 0.25), ZERO, UNIT_BOX).kind());
        assertEquals(CLEAR_INITIAL_TANGENT, CapsuleAabbCast3d.cast(pointCapsule(-0.25, 0.5, 0.5, 0.25), ZERO, UNIT_BOX).kind());
        assertEquals(INITIAL_OVERLAP, CapsuleAabbCast3d.cast(pointCapsule(0.5, 0.5, 0.5, 0.25), ZERO, UNIT_BOX).kind());
    }

    @Test
    void zeroRadiusCoincidenceIsExplicitlyAmbiguousWithoutAnInventedNormalOrPrefix() {
        var pointBox = new Aabb(ZERO, ZERO);
        var result = CapsuleAabbCast3d.cast(pointCapsule(0, 0, 0, 0), ZERO, pointBox);
        assertEquals(UNRESOLVED, result.kind());
        assertEquals(CapsuleAabbCastResult3d.UnresolvedReason.NO_VALID_AXIS, result.reason().orElseThrow());
        assertTrue(result.safePrefixEnd().isEmpty());
        assertTrue(result.normalEstimate().isEmpty());
        assertEquals(1, result.work().evaluationCount());
    }

    @Test
    void zeroRadiusCrossingStillReportsAContactBandBracketWithoutNormalAtZeroDistance() {
        var result = CapsuleAabbCast3d.cast(pointCapsule(-1, 0, 0, 0), new Vec3d(2, 0, 0), new Aabb(ZERO, ZERO));
        assertEquals(HIT_AFTER_CLEAR, result.kind());
        var normal = result.normalEstimate().orElseThrow();
        assertEquals(CapsuleAabbCastResult3d.NormalSource.LAST_SEPARATED, normal.source());
        assertTrue(normal.sourceFraction() <= result.safePrefixEnd().orElseThrow());
        assertTrue(result.bandBracket().orElseThrow().insideContactBandFraction() <= 0.5);
    }

    @Test
    void closingButStillSeparatedEndpointProvesTheRemainderWithoutAdvancement() {
        var result = CapsuleAabbCast3d.cast(pointCapsule(-2, 0.5, 0.5, 0.25), new Vec3d(1, 0, 0), UNIT_BOX);
        assertEquals(CLEAR_SEPARATED, result.kind());
        assertEquals(1.0, result.safePrefixEnd().orElseThrow());
        assertEquals(new CapsuleAabbCastResult3d.WorkCounts(1, 0, 0), result.work());
    }

    @Test
    void unsupportedFiniteInputsDoNotTurnIntoAir() {
        Capsule[] capsules = {
                pointCapsule(0x1.0p25 + 1, 0, 0, 0.25), pointCapsule(-1, 0, 0, 65),
                pointCapsule(-513, 0, 0, 0.25),
                new Capsule(new Segment3d(new Vec3d(0, -65, 0), new Vec3d(0, 65, 0)), 0.25)
        };
        for (var capsule : capsules) assertUnsupported(CapsuleAabbCast3d.cast(capsule, ZERO, UNIT_BOX));
        assertUnsupported(CapsuleAabbCast3d.cast(pointCapsule(-1, 0, 0, 0.25), new Vec3d(65, 0, 0), UNIT_BOX));
        assertUnsupported(CapsuleAabbCast3d.cast(pointCapsule(-1, 0, 0, 0.25), ZERO,
                new Aabb(ZERO, new Vec3d(129, 1, 1))));
    }

    @Test
    void conservativeProfileEligibilityCanRejectAnExactInclusiveBoundary() {
        // Exact length 128 is legal mathematically; widened subtraction/norm cannot establish it.
        var exactLimit = new Capsule(new Segment3d(new Vec3d(0, -64, 0), new Vec3d(0, 64, 0)), 0.25);
        assertUnsupported(CapsuleAabbCast3d.cast(exactLimit, ZERO, UNIT_BOX));
        var slightlyInside = new Capsule(new Segment3d(new Vec3d(-1, -63, 0), new Vec3d(-1, 63, 0)), 0.25);
        assertEquals(CLEAR_SEPARATED, CapsuleAabbCast3d.cast(slightlyInside, ZERO, UNIT_BOX).kind());
    }

    @Test
    void invalidNullCallsFailFast() {
        var capsule = pointCapsule(-1, 0, 0, 0.25);
        assertThrows(NullPointerException.class, () -> CapsuleAabbCast3d.cast(null, ZERO, UNIT_BOX));
        assertThrows(NullPointerException.class, () -> CapsuleAabbCast3d.cast(capsule, null, UNIT_BOX));
        assertThrows(NullPointerException.class, () -> CapsuleAabbCast3d.cast(capsule, ZERO, null));
    }

    @Test
    void aPositiveSeparatorWithAmbiguousBandRelationDoesNotInventAZeroPrefix() {
        var result = CapsuleAabbCast3d.cast(pointCapsule(-0x1.0000000dbe6ffp-2, 0.5, 0.5, 0.25), new Vec3d(0, 1, 0), UNIT_BOX);
        assertEquals(UNRESOLVED, result.kind());
        assertEquals(CapsuleAabbCastResult3d.StartRelation.SEPARATED_BAND_UNRESOLVED, result.startRelation());
        assertEquals(CapsuleAabbCastResult3d.UnresolvedReason.AMBIGUOUS_START, result.reason().orElseThrow());
        assertTrue(result.safePrefixEnd().isEmpty());
        assertTrue(result.normalEstimate().isEmpty());
    }

    @Test
    void unprovedNegativeStartSlopStaysUnresolved() {
        var result = CapsuleAabbCast3d.cast(pointCapsule(-0x1.ffffffe483202p-3, 0.5, 0.5, 0.25), new Vec3d(0, 1, 0), UNIT_BOX);
        assertEquals(UNRESOLVED, result.kind());
        assertEquals(CapsuleAabbCastResult3d.StartRelation.AMBIGUOUS_START, result.startRelation());
        assertTrue(result.safePrefixEnd().isEmpty());
        assertEquals(new CapsuleAabbCastResult3d.WorkCounts(1, 0, 0), result.work());
    }

    @Test
    void queryOwnedScratchCannotAlterRetainedResultsOrOtherConcurrentRequests() {
        var capsule = pointCapsule(-1, -0.125, 0.5, 0.25);
        var delta = new Vec3d(2, 0, 0);
        var original = CapsuleAabbCast3d.cast(capsule, delta, UNIT_BOX);
        var expected = resultFields(original);
        IntStream.range(0, 128).parallel().forEach(index -> {
            var unrelated = CapsuleAabbCast3d.cast(pointCapsule(-0.25, 0.5, 0.5, 0.25), new Vec3d(0, 0.125, 0), UNIT_BOX);
            assertEquals(CLEAR_INITIAL_TANGENT, unrelated.kind());
            assertEquals(expected, resultFields(CapsuleAabbCast3d.cast(capsule, delta, UNIT_BOX)));
        });
        assertEquals(expected, resultFields(original));
    }

    private static List<?> resultFields(CapsuleAabbCastResult3d value) {
        return List.of(value.kind(), value.startRelation(), value.safePrefixEnd(), value.bandBracket(),
                value.normalEstimate(), value.scale(), value.reason(), value.work());
    }

    private static void assertUnsupported(CapsuleAabbCastResult3d result) {
        assertEquals(UNRESOLVED, result.kind());
        assertEquals(CapsuleAabbCastResult3d.UnresolvedReason.UNSUPPORTED_DOMAIN, result.reason().orElseThrow());
        assertTrue(result.safePrefixEnd().isEmpty());
        assertTrue(result.normalEstimate().isEmpty());
        assertTrue(result.scale().isEmpty());
        assertEquals(new CapsuleAabbCastResult3d.WorkCounts(0, 0, 0), result.work());
    }

    @Test
    void twoUnitMotionFindsTheCylindricalMiddleAcrossAThinWall() {
        Capsule moving = new Capsule(new Segment3d(new Vec3d(0, -2, 0), new Vec3d(0, 2, 0)), 0.25);
        Aabb wall = new Aabb(new Vec3d(1, -0.5, -0.5), new Vec3d(1.01, 0.5, 0.5));
        var result = CapsuleAabbCast3d.cast(moving, new Vec3d(2, 0, 0), wall);
        assertEquals(CapsuleAabbCastResult3d.Kind.HIT_AFTER_CLEAR, result.kind());
        var bracket = result.bandBracket().orElseThrow();
        assertEquals(bracket.lastOutsideBandFraction(), result.safePrefixEnd().orElseThrow());
        assertTrue(bracket.lastOutsideBandFraction() < bracket.insideContactBandFraction());
        BigDecimal band = new BigDecimal(result.scale().orElseThrow().contactBandWidth());
        BigDecimal lowGap = new BigDecimal("0.75").subtract(new BigDecimal(bracket.lastOutsideBandFraction()).multiply(BigDecimal.valueOf(2)));
        BigDecimal highGap = new BigDecimal("0.75").subtract(new BigDecimal(bracket.insideContactBandFraction()).multiply(BigDecimal.valueOf(2)));
        assertTrue(lowGap.compareTo(band) > 0, "outside-band lower endpoint");
        assertTrue(highGap.compareTo(band) <= 0, "inside-band upper endpoint");
        assertTrue(result.work().evaluationCount() <= 73);
        assertTrue(result.work().advancementAttempts() > 0);
    }
}
