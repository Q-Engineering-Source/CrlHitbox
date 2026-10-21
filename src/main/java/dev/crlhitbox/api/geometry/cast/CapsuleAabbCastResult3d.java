package dev.crlhitbox.api.geometry.cast;

import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Immutable evidence for one bounded translation request, never a physical-TOI receipt. */
public final class CapsuleAabbCastResult3d {
    public enum Kind {
        CLEAR_SEPARATED, CLEAR_INITIAL_OUTWARD, CLEAR_INITIAL_TANGENT,
        HIT_AFTER_CLEAR, HIT_INITIAL_INWARD, HIT_INITIAL_NEAR_BAND, INITIAL_OVERLAP, UNRESOLVED
    }

    public enum StartRelation {
        SEPARATED_OUTSIDE_BAND, NEAR_CONTACT, OVERLAP, SEPARATED_BAND_UNRESOLVED, AMBIGUOUS_START, UNCLASSIFIED
    }

    public enum UnresolvedReason {
        UNSUPPORTED_DOMAIN, ARITHMETIC_UNCERTAINTY, AMBIGUOUS_START, NO_VALID_AXIS,
        NO_PROGRESS, ADVANCEMENT_LIMIT, ENDPOINT_AMBIGUOUS
    }

    public enum NormalSource { CURRENT_WITNESS, LAST_SEPARATED }

    /** Endpoints bracket contact-band entry, not exact physical first contact. */
    public record BandBracket(double lastOutsideBandFraction, double insideContactBandFraction,
                              double contactClearanceUpper, double bandEntryPathWidth, boolean refinementLimited) {
        public BandBracket {
            lastOutsideBandFraction = fraction(lastOutsideBandFraction);
            insideContactBandFraction = fraction(insideContactBandFraction);
            positive(contactClearanceUpper, "contact band");
            nonnegative(bandEntryPathWidth, "path width");
            bandEntryPathWidth = bandEntryPathWidth == 0.0 ? 0.0 : bandEntryPathWidth;
            if (!(lastOutsideBandFraction < insideContactBandFraction)) throw new IllegalArgumentException("unordered band bracket");
        }
    }

    /** Direction estimate only: not a separating certificate, contact manifold or MTV. */
    public record NormalEstimate(Vec3d direction, double sourceFraction, NormalSource source) {
        public NormalEstimate {
            Objects.requireNonNull(direction, "direction");
            Objects.requireNonNull(source, "source");
            sourceFraction = fraction(sourceFraction);
            CastEnclosure length = CastEnclosure.norm(CastEnclosure.point(direction.x()),
                    CastEnclosure.point(direction.y()), CastEnclosure.point(direction.z()));
            if (length.lower() < 1.0 - 0x1.0p-48 || length.upper() > 1.0 + 0x1.0p-48) {
                throw new IllegalArgumentException("normal estimate is not numerically normalized");
            }
        }
    }

    public record QueryScale(double epsilon, double contactBandWidth, double penetrationSlop) {
        public QueryScale {
            positive(epsilon, "epsilon");
            positive(contactBandWidth, "contactBandWidth");
            if (epsilon > 1.0 / 4096.0 || contactBandWidth < 8.0 * epsilon || penetrationSlop != contactBandWidth) {
                throw new IllegalArgumentException("inconsistent query scale");
            }
        }
    }

    /** Counts attempted evaluations/advances/refinements, including a failing attempt. */
    public record WorkCounts(int evaluationCount, int advancementAttempts, int refinementAttempts) {
        public WorkCounts {
            if (evaluationCount < 0 || evaluationCount > 73 || advancementAttempts < 0 || advancementAttempts > 32
                    || refinementAttempts < 0 || refinementAttempts > 40
                    || evaluationCount > 1 + advancementAttempts + refinementAttempts) {
                throw new IllegalArgumentException("work bound exceeded");
            }
        }
    }

    private final Kind kind;
    private final StartRelation startRelation;
    private final OptionalDouble safePrefixEnd;
    private final Optional<BandBracket> bandBracket;
    private final Optional<NormalEstimate> normalEstimate;
    private final Optional<QueryScale> scale;
    private final Optional<UnresolvedReason> reason;
    private final WorkCounts work;

    CapsuleAabbCastResult3d(Kind kind, StartRelation startRelation, OptionalDouble safePrefixEnd,
                           BandBracket bracket, NormalEstimate normal, QueryScale scale,
                           UnresolvedReason reason, WorkCounts work) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.startRelation = Objects.requireNonNull(startRelation, "startRelation");
        this.safePrefixEnd = Objects.requireNonNull(safePrefixEnd, "safePrefixEnd");
        this.bandBracket = Optional.ofNullable(bracket);
        this.normalEstimate = Optional.ofNullable(normal);
        this.scale = Optional.ofNullable(scale);
        this.reason = Optional.ofNullable(reason);
        this.work = Objects.requireNonNull(work, "work");
        safePrefixEnd.ifPresent(CapsuleAabbCastResult3d::fraction);
        invariant((reason != null) == (kind == Kind.UNRESOLVED), "reason applicability");
        invariant((bracket != null) == (kind == Kind.HIT_AFTER_CLEAR), "bracket applicability");
        invariant(scale != null || kind == Kind.UNRESOLVED && !safePrefixEnd.isPresent(), "scale applicability");
        if (safePrefixEnd.isPresent()) invariant(startRelation == StartRelation.SEPARATED_OUTSIDE_BAND, "prefix start");
        switch (kind) {
            case CLEAR_SEPARATED -> invariant(safePrefixEnd.isPresent() && safePrefixEnd.getAsDouble() == 1.0, "clear prefix");
            case HIT_AFTER_CLEAR -> invariant(safePrefixEnd.isPresent()
                    && safePrefixEnd.getAsDouble() == bracket.lastOutsideBandFraction(), "hit prefix");
            case CLEAR_INITIAL_OUTWARD, CLEAR_INITIAL_TANGENT, HIT_INITIAL_INWARD, HIT_INITIAL_NEAR_BAND -> {
                invariant(startRelation == StartRelation.NEAR_CONTACT && !safePrefixEnd.isPresent(), "initial applicability");
                invariant(normal == null || normal.source() == NormalSource.CURRENT_WITNESS && normal.sourceFraction() == 0.0, "initial normal");
            }
            case INITIAL_OVERLAP -> invariant(startRelation == StartRelation.OVERLAP && !safePrefixEnd.isPresent() && normal == null, "overlap applicability");
            case UNRESOLVED -> invariant(normal == null || normal.source() == NormalSource.LAST_SEPARATED
                    && safePrefixEnd.isPresent() && normal.sourceFraction() <= safePrefixEnd.getAsDouble(), "unresolved normal");
        }
        if (bracket != null) {
            invariant(bracket.contactClearanceUpper() == scale.contactBandWidth(), "bracket scale");
            invariant(normal == null || normal.source() == NormalSource.CURRENT_WITNESS
                    && normal.sourceFraction() == bracket.insideContactBandFraction()
                    || normal.source() == NormalSource.LAST_SEPARATED
                    && normal.sourceFraction() <= bracket.lastOutsideBandFraction(), "bracket normal");
        }
    }

    public Kind kind() { return kind; }
    public StartRelation startRelation() { return startRelation; }
    public OptionalDouble safePrefixEnd() { return safePrefixEnd; }
    public Optional<BandBracket> bandBracket() { return bandBracket; }
    public Optional<NormalEstimate> normalEstimate() { return normalEstimate; }
    public Optional<QueryScale> scale() { return scale; }
    public Optional<UnresolvedReason> reason() { return reason; }
    public WorkCounts work() { return work; }

    private static double fraction(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) throw new IllegalArgumentException("invalid fraction");
        return value == 0.0 ? 0.0 : value;
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(name + " must be finite and positive");
    }

    private static void nonnegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(name + " must be finite and nonnegative");
    }

    private static void invariant(boolean condition, String name) {
        if (!condition) throw new IllegalStateException("result invariant: " + name);
    }
}
