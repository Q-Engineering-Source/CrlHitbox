package dev.crlhitbox.internal.entity;

import dev.crlhitbox.internal.network.FullSnapshotOrdering;

/**
 * Internal, non-persistent synchronization/replica state for one capability provider.
 *
 * <p>The state owns one immutable local generation plus the last accepted remote generation,
 * revision, and the receiving holder's local revision immediately after that install. It retains no
 * Entity, world, holder, snapshot, buffer, or transport reference, and it is never serialized. It is
 * intentionally unsynchronized: server-side capture and client-side installation both belong on the
 * owning logical game thread.</p>
 *
 * <p>The type is internal platform state, not stable public API. It exists as a public internal type
 * only so that the Forge capability provider and the client installation seam, which live in sibling
 * internal packages, can share exactly one owner object.</p>
 */
public final class EntityHitboxReplicaState {
    private final long localGeneration;
    private boolean hasAcceptedRemote;
    private long acceptedRemoteGeneration;
    private long acceptedRemoteRevision;
    private long holderLocalRevisionAtLastInstall;

    EntityHitboxReplicaState(long localGeneration) {
        if (localGeneration <= 0L) {
            throw new IllegalArgumentException(
                    "local generation must be positive: " + localGeneration);
        }
        this.localGeneration = localGeneration;
    }

    /** Creates one fresh state with a newly allocated process-local generation. */
    public static EntityHitboxReplicaState create() {
        return new EntityHitboxReplicaState(HolderGenerationAllocator.allocate());
    }

    /** Returns this provider incarnation's immutable positive generation. */
    public long localGeneration() {
        return localGeneration;
    }

    /** Returns whether a remote snapshot has already been accepted for this incarnation. */
    public boolean hasAcceptedRemote() {
        return hasAcceptedRemote;
    }

    /** Returns the accepted remote generation, or {@code 0} when none was accepted. */
    public long acceptedRemoteGeneration() {
        return acceptedRemoteGeneration;
    }

    /** Returns the accepted remote wire revision, or {@code 0} when none was accepted. */
    public long acceptedRemoteRevision() {
        return acceptedRemoteRevision;
    }

    /** Returns the receiving holder's local revision after the last successful install. */
    public long holderLocalRevisionAtLastInstall() {
        return holderLocalRevisionAtLastInstall;
    }

    /** Classifies an inbound snapshot without mutating this state. */
    public FullSnapshotOrdering classify(long incomingGeneration, long incomingRevision) {
        return FullSnapshotOrdering.classify(
                hasAcceptedRemote, acceptedRemoteGeneration, acceptedRemoteRevision,
                incomingGeneration, incomingRevision);
    }

    /**
     * Records one successfully installed authoritative snapshot.
     *
     * <p>Callers must only invoke this after the holder replacement succeeded; a rejected or failed
     * installation must leave this state untouched.</p>
     *
     * @param generation the accepted remote generation
     * @param revision the accepted remote wire revision
     * @param holderRevisionAtInstall the receiving holder's local revision after replacement
     */
    public void recordAcceptedInstall(long generation, long revision, long holderRevisionAtInstall) {
        if (generation <= 0L) {
            throw new IllegalArgumentException("accepted generation must be positive: " + generation);
        }
        if (revision < 0L) {
            throw new IllegalArgumentException("accepted revision must be nonnegative: " + revision);
        }
        if (holderRevisionAtInstall < 0L) {
            throw new IllegalArgumentException(
                    "holder revision at install must be nonnegative: " + holderRevisionAtInstall);
        }
        hasAcceptedRemote = true;
        acceptedRemoteGeneration = generation;
        acceptedRemoteRevision = revision;
        holderLocalRevisionAtLastInstall = holderRevisionAtInstall;
    }

    @Override
    public String toString() {
        return "EntityHitboxReplicaState[localGeneration=" + localGeneration
                + ", hasAcceptedRemote=" + hasAcceptedRemote
                + ", acceptedRemoteGeneration=" + acceptedRemoteGeneration
                + ", acceptedRemoteRevision=" + acceptedRemoteRevision
                + ", holderLocalRevisionAtLastInstall=" + holderLocalRevisionAtLastInstall + "]";
    }
}
