package dev.crlhitbox.internal.network;

/**
 * Classification of one inbound full snapshot against the replica state already accepted.
 *
 * <p>This is the frozen Phase 2B acceptance rule, expressed without any Entity, holder, or world
 * dependency so that it is directly testable. A higher generation replaces the previous incarnation
 * even when the incoming source revision is lower; an equal generation/revision is accepted
 * deliberately, because it is the authoritative repair message for a client-local mutation.
 * Comparisons never use unsigned arithmetic.</p>
 *
 * <p>The type is internal protocol state, not stable public API: it is visible outside its package
 * only because the internal replica state and the client installation seam live in a sibling
 * internal package.</p>
 */
public enum FullSnapshotOrdering {
    /** No remote snapshot has been accepted yet. */
    FIRST,
    /** The incoming generation is higher, so the previous incarnation is replaced. */
    NEWER_GENERATION,
    /** Same generation and same revision: authoritative reassertion and local-tamper repair. */
    SAME_GENERATION_EQUAL_REVISION,
    /** Same generation and a higher revision. */
    SAME_GENERATION_NEWER_REVISION,
    /** The incoming generation is lower than the accepted one. */
    STALE_GENERATION,
    /** Same generation with a lower revision. */
    STALE_REVISION;

    /** Returns whether this classification may be installed. */
    public boolean isAccepted() {
        return this != STALE_GENERATION && this != STALE_REVISION;
    }

    /**
     * Classifies an inbound snapshot.
     *
     * @param hasAcceptedRemote whether replica state already holds an accepted remote snapshot
     * @param acceptedGeneration the accepted remote generation (ignored when none was accepted)
     * @param acceptedRevision the accepted remote revision (ignored when none was accepted)
     * @param incomingGeneration the positive incoming generation
     * @param incomingRevision the nonnegative incoming revision
     */
    public static FullSnapshotOrdering classify(            boolean hasAcceptedRemote,
            long acceptedGeneration,
            long acceptedRevision,
            long incomingGeneration,
            long incomingRevision
    ) {
        if (incomingGeneration <= 0L) {
            throw new IllegalArgumentException(
                    "incoming generation must be positive: " + incomingGeneration);
        }
        if (incomingRevision < 0L) {
            throw new IllegalArgumentException(
                    "incoming revision must be nonnegative: " + incomingRevision);
        }
        if (!hasAcceptedRemote) {
            return FIRST;
        }
        if (acceptedGeneration <= 0L) {
            throw new IllegalArgumentException(
                    "accepted generation must be positive: " + acceptedGeneration);
        }
        if (acceptedRevision < 0L) {
            throw new IllegalArgumentException(
                    "accepted revision must be nonnegative: " + acceptedRevision);
        }
        if (incomingGeneration > acceptedGeneration) {
            return NEWER_GENERATION;
        }
        if (incomingGeneration < acceptedGeneration) {
            return STALE_GENERATION;
        }
        if (incomingRevision < acceptedRevision) {
            return STALE_REVISION;
        }
        if (incomingRevision == acceptedRevision) {
            return SAME_GENERATION_EQUAL_REVISION;
        }
        return SAME_GENERATION_NEWER_REVISION;
    }
}
