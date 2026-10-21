package dev.crlhitbox.api.geometry.contact;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Exact finite-profile capsule/AABB contact; a result does not authorize world movement. */
public final class CapsuleAabbContact3d {
    private CapsuleAabbContact3d() { }

    public record ContactAccuracy(double maxClearanceWidthMeters,
                                  double maxWitnessErrorMeters, double maxPathWidthMeters) {
        public ContactAccuracy {
            positive(maxClearanceWidthMeters, "clearance width");
            positive(maxWitnessErrorMeters, "witness error");
            positive(maxPathWidthMeters, "path width");
        }
    }

    public record QueryLimits(int maxExactBits, int maxExactOps, int maxStaticEvaluations,
                              int maxCastQueries, int maxCastEvaluations, int maxCandidateUnits,
                              int maxRefinementSteps) {
        public QueryLimits {
            // All zero denotes observation without work cutoffs. Existing explicit
            // bounded callers retain their contract; mixed zero profiles are invalid.
            if (maxExactBits != 0 || maxExactOps != 0 || maxStaticEvaluations != 0
                    || maxCastQueries != 0 || maxCastEvaluations != 0
                    || maxCandidateUnits != 0 || maxRefinementSteps != 0) {
                bounded(maxExactBits, 16384, "exact bits");
                bounded(maxExactOps, 100000, "exact operations");
                bounded(maxStaticEvaluations, 256, "static evaluations");
                bounded(maxCastQueries, 128, "cast queries");
                bounded(maxCastEvaluations, 9344, "cast evaluations");
                bounded(maxCandidateUnits, 150000, "candidate units");
                bounded(maxRefinementSteps, 128, "refinement steps");
            }
        }

        /** Collect real Work without rejecting a query because of its work counters. */
        public static QueryLimits recordingOnly() { return new QueryLimits(0, 0, 0, 0, 0, 0, 0); }

        public boolean recordsOnly() { return maxExactBits == 0; }
    }

    public enum Relation { SEPARATED, TOUCHING, OVERLAPPING, UNRESOLVED }
    public enum Reason { UNSUPPORTED_DOMAIN, NUMERICAL_ENCLOSURE, EXACT_WORK_LIMIT,
        ACCURACY_UNREACHABLE, NO_CERTIFIED_HIT, NO_PREFIX_CERTIFICATE,
        START_DIRECTION_UNPROVED, ROUNDED_INSTALL_UNSAFE, CORRECTION_PATH_UNPROVED }

    public record Interval(double lower, double upper) {
        public Interval {
            if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower > upper)
                throw new IllegalArgumentException("invalid finite interval");
            if (lower == 0.0) lower = 0.0;
            if (upper == 0.0) upper = 0.0;
        }
    }

    public enum Face { X_MIN, X_MAX, Y_MIN, Y_MAX, Z_MIN, Z_MAX }
    public record Feature(Set<Face> activeFaces) {
        public Feature { activeFaces = Set.copyOf(Objects.requireNonNull(activeFaces, "activeFaces")); }
    }
    public record FeatureAttribution(Set<Feature> possibilities, boolean complete) {
        public FeatureAttribution { possibilities = Set.copyOf(Objects.requireNonNull(possibilities, "possibilities")); }
    }
    public record Witness(Interval segmentParameter, Interval segmentX, Interval segmentY,
                          Interval segmentZ, Interval boxX, Interval boxY, Interval boxZ,
                          FeatureAttribution feature) {
        public Witness {
            Objects.requireNonNull(segmentParameter); Objects.requireNonNull(segmentX);
            Objects.requireNonNull(segmentY); Objects.requireNonNull(segmentZ);
            Objects.requireNonNull(boxX); Objects.requireNonNull(boxY);
            Objects.requireNonNull(boxZ); Objects.requireNonNull(feature);
        }
    }
    public record NormalEstimate(Interval x, Interval y, Interval z,
                                 double maximumAngularErrorRadians) {
        public NormalEstimate {
            Objects.requireNonNull(x); Objects.requireNonNull(y); Objects.requireNonNull(z);
            if (!Double.isFinite(maximumAngularErrorRadians) || maximumAngularErrorRadians < 0)
                throw new IllegalArgumentException("invalid angular error");
        }
    }
    public record Work(int exactAttempts, int exactOps, int peakExactBits,
                       int staticAttempts, int staticEvaluations, int staticCandidateUnits,
                       int castAttempts, int castQueries, int castEvaluations,
                       int castCandidateUnits, int totalCandidateUnits, int refinementSteps) { }
    public record ContactResult(Relation relation, Optional<Interval> signedClearanceMeters,
                                Optional<Witness> witness, Optional<NormalEstimate> normal,
                                Optional<Reason> reason, Work work) {
        public ContactResult {
            Objects.requireNonNull(relation); Objects.requireNonNull(signedClearanceMeters);
            Objects.requireNonNull(witness); Objects.requireNonNull(normal);
            Objects.requireNonNull(reason); Objects.requireNonNull(work);
            if ((relation == Relation.UNRESOLVED) != reason.isPresent())
                throw new IllegalArgumentException("reason applicability");
        }
    }
    public record BoundarySearchHint(double preferredLowFraction, double preferredHighFraction) {
        public BoundarySearchHint {
            fraction(preferredLowFraction); fraction(preferredHighFraction);
            if (preferredLowFraction > preferredHighFraction) throw new IllegalArgumentException("unordered hint");
        }
    }
    public record StoredInstallCandidate(double fraction, Vec3d acceptedStartCenter,
                                         Vec3d acceptedStoredCenter, Vec3d acceptedHalfLine,
                                         Capsule actualStoredCapsule) {
        public StoredInstallCandidate {
            CapsuleAabbContact3d.fraction(fraction); Objects.requireNonNull(acceptedStartCenter);
            Objects.requireNonNull(acceptedStoredCenter); Objects.requireNonNull(acceptedHalfLine);
            Objects.requireNonNull(actualStoredCapsule);
        }
    }
    public enum PrefixKind { CLEAR_SEPARATED, NONPENETRATING }
    public record PrefixReceipt(PrefixKind kind, double endFraction) {
        public PrefixReceipt { Objects.requireNonNull(kind); fraction(endFraction); }
    }
    public enum BoundaryKind { SEPARATED_FULL, NONPENETRATING_FULL,
        BLOCKED_AT_START, CERTIFIED_BOUNDARY, INITIAL_OVERLAP, UNRESOLVED }
    public record InstalledPoseReceipt(double fraction, Vec3d storedCenter, Vec3d halfLine,
                                       Capsule storedCapsule, ContactResult storedContact) {
        public InstalledPoseReceipt {
            CapsuleAabbContact3d.fraction(fraction); Objects.requireNonNull(storedCenter); Objects.requireNonNull(halfLine);
            Objects.requireNonNull(storedCapsule); Objects.requireNonNull(storedContact);
        }
    }
    public record BoundaryResult(BoundaryKind kind, Relation startRelation,
                                 Optional<PrefixReceipt> prefix,
                                 Optional<Interval> firstObstructionBracket,
                                 Optional<InstalledPoseReceipt> installation,
                                 Optional<Reason> reason, Work work) {
        public BoundaryResult {
            Objects.requireNonNull(kind); Objects.requireNonNull(startRelation);
            Objects.requireNonNull(prefix); Objects.requireNonNull(firstObstructionBracket);
            Objects.requireNonNull(installation); Objects.requireNonNull(reason); Objects.requireNonNull(work);
            if ((kind == BoundaryKind.UNRESOLVED) != reason.isPresent())
                throw new IllegalArgumentException("reason applicability");
        }
    }

    /** A sufficient point-capsule proof for the original translation through {@code endFraction}. */
    public record OriginalPrefixReceipt(Capsule startCapsule, Vec3d originalDelta,
                                        double endFraction, Aabb obstacle,
                                        StoredInstallCandidate candidate, PrefixReceipt prefix,
                                        InstalledPoseReceipt installation) {
        public OriginalPrefixReceipt {
            Objects.requireNonNull(startCapsule); Objects.requireNonNull(originalDelta);
            Objects.requireNonNull(obstacle); Objects.requireNonNull(candidate);
            Objects.requireNonNull(prefix); Objects.requireNonNull(installation);
            if (!Double.isFinite(endFraction) || endFraction <= 0 || endFraction > 1
                    || Double.doubleToRawLongBits(endFraction) != Double.doubleToRawLongBits(candidate.fraction())
                    || Double.doubleToRawLongBits(endFraction) != Double.doubleToRawLongBits(prefix.endFraction())
                    || Double.doubleToRawLongBits(endFraction) != Double.doubleToRawLongBits(installation.fraction())
                    || prefix.kind() != PrefixKind.NONPENETRATING
                    || installation.storedCapsule() != candidate.actualStoredCapsule()
                    || installation.storedContact().relation() != Relation.TOUCHING)
                throw new IllegalArgumentException("inconsistent original-prefix receipt");
        }
    }

    /** A missing proof is an explicit failure; no installed pose is authorized. */
    public record OriginalPrefixResult(Relation startRelation, Optional<OriginalPrefixReceipt> proof,
                                       Optional<Reason> reason, Work work) {
        public OriginalPrefixResult {
            Objects.requireNonNull(startRelation); Objects.requireNonNull(proof);
            Objects.requireNonNull(reason); Objects.requireNonNull(work);
            if (proof.isPresent() == reason.isPresent())
                throw new IllegalArgumentException("proof/reason applicability");
        }
    }

    /**
     * A closed nonpenetrating original-translation prefix for an elongated capsule.
     * The receipt binds every geometry, install-identity, accuracy, limit and work input
     * used by the proof; its fraction is the caller's actual {@code t}, never an inferred 1.
     * The proof covers the exact ideal path, the canonical round-to-nearest installed path,
     * and the ideal-to-stored endpoint bridge by one common radius-expanded AABB face.
     */
    public record ElongatedOriginalPrefixReceipt(Capsule startCapsule, Vec3d originalDelta,
            double endFraction, Aabb obstacle, ContactAccuracy accuracy, QueryLimits limits,
            StoredInstallCandidate candidate, PrefixReceipt prefix,
            InstalledPoseReceipt installation, Work work) {
        public ElongatedOriginalPrefixReceipt {
            Objects.requireNonNull(startCapsule); Objects.requireNonNull(originalDelta);
            Objects.requireNonNull(obstacle); Objects.requireNonNull(accuracy);
            Objects.requireNonNull(limits); Objects.requireNonNull(candidate);
            Objects.requireNonNull(prefix); Objects.requireNonNull(installation);
            Objects.requireNonNull(work);
            fraction(endFraction);
            if (Double.doubleToRawLongBits(endFraction)
                        != Double.doubleToRawLongBits(candidate.fraction())
                    || Double.doubleToRawLongBits(endFraction)
                        != Double.doubleToRawLongBits(prefix.endFraction())
                    || Double.doubleToRawLongBits(endFraction)
                        != Double.doubleToRawLongBits(installation.fraction())
                    || prefix.kind() != PrefixKind.NONPENETRATING
                    || installation.storedCapsule() != candidate.actualStoredCapsule()
                    || installation.storedCenter() != candidate.acceptedStoredCenter()
                    || installation.halfLine() != candidate.acceptedHalfLine()
                    || (installation.storedContact().relation() != Relation.SEPARATED
                        && installation.storedContact().relation() != Relation.TOUCHING)
                    || installation.storedContact().witness().isEmpty()
                    || !work.equals(installation.storedContact().work()))
                throw new IllegalArgumentException("inconsistent elongated-prefix receipt");
        }
    }

    /** A missing elongated-capsule prefix proof is an explicit failure with accumulated work. */
    public record ElongatedOriginalPrefixResult(Relation startRelation,
            Optional<ElongatedOriginalPrefixReceipt> proof, Optional<Reason> reason, Work work) {
        public ElongatedOriginalPrefixResult {
            Objects.requireNonNull(startRelation); Objects.requireNonNull(proof);
            Objects.requireNonNull(reason); Objects.requireNonNull(work);
            if (proof.isPresent() == reason.isPresent())
                throw new IllegalArgumentException("proof/reason applicability");
            if (proof.isPresent() && !proof.orElseThrow().work().equals(work))
                throw new IllegalArgumentException("receipt work mismatch");
        }
    }

    public static ContactResult query(Capsule capsule, Aabb obstacle,
                                      ContactAccuracy accuracy, QueryLimits limits) {
        Objects.requireNonNull(capsule); Objects.requireNonNull(obstacle);
        Objects.requireNonNull(accuracy); Objects.requireNonNull(limits);
        return CapsuleAabbContactMath.query(capsule, obstacle, accuracy, limits);
    }

    public static BoundaryResult refineTranslationBoundary(Capsule capsule, Vec3d delta,
            Aabb obstacle, BoundarySearchHint hint, StoredInstallCandidate installation,
            ContactAccuracy accuracy, QueryLimits limits) {
        Objects.requireNonNull(capsule); Objects.requireNonNull(delta); Objects.requireNonNull(obstacle);
        Objects.requireNonNull(hint); Objects.requireNonNull(installation);
        Objects.requireNonNull(accuracy); Objects.requireNonNull(limits);
        return CapsuleAabbContactMath.refine(capsule, delta, obstacle, hint, installation, accuracy, limits);
    }

    /**
     * Proves the original path {@code start + originalDelta * s}, {@code 0 <= s <= endFraction},
     * and its bounded rounding bridge to a touching stored point capsule. It does not prove the
     * remainder of {@code originalDelta} and does not reinterpret rounded {@code delta * t}
     * as a new full translation.
     */
    public static OriginalPrefixResult proveOriginalTranslationPrefix(Capsule start,
            Vec3d originalDelta, double endFraction, Aabb obstacle,
            StoredInstallCandidate installation, ContactAccuracy accuracy, QueryLimits limits) {
        Objects.requireNonNull(start); Objects.requireNonNull(originalDelta);
        Objects.requireNonNull(obstacle); Objects.requireNonNull(installation);
        Objects.requireNonNull(accuracy); Objects.requireNonNull(limits);
        return CapsuleAabbContactMath.proveOriginalPrefix(start,originalDelta,endFraction,
                obstacle,installation,accuracy,limits);
    }

    /**
     * Proves an elongated capsule's original fixed-translation path through {@code endFraction}
     * and the actual stored installation. It conservatively requires the start endpoints, exact
     * ideal endpoints at that fraction, and both actual stored endpoints to remain outside one
     * common radius-expanded AABB face. This single-face proof covers three distinct paths:
     * the exact ideal translation; UG's round-to-nearest sequence
     * {@code fl(delta*s) -> fl(startCenter+offset) -> fl(center +/- fixedHalfLine)} for every
     * {@code s} in the closed interval; and the terminal ideal-to-stored endpoint bridge.
     * IEEE-754 round-to-nearest is coordinatewise monotone for each fixed operation operand, so
     * every actual endpoint is between its verified start and stored values. Unsupported or
     * inconclusive cases fail closed and retain cumulative work.
     */
    public static ElongatedOriginalPrefixResult proveOriginalCapsuleTranslationPrefix(Capsule start,
            Vec3d originalDelta, double endFraction, Aabb obstacle,
            StoredInstallCandidate installation, ContactAccuracy accuracy, QueryLimits limits) {
        Objects.requireNonNull(start); Objects.requireNonNull(originalDelta);
        Objects.requireNonNull(obstacle); Objects.requireNonNull(installation);
        Objects.requireNonNull(accuracy); Objects.requireNonNull(limits);
        return CapsuleAabbContactMath.proveOriginalCapsulePrefix(start,originalDelta,endFraction,
                obstacle,installation,accuracy,limits);
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(name);
    }
    private static void bounded(int value, int ceiling, String name) {
        if (value <= 0 || value > ceiling) throw new IllegalArgumentException(name);
    }
    private static void fraction(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("fraction");
    }
}
