package dev.crlhitbox.internal.network;

import java.util.UUID;

/**
 * Failure to encode one server-side full snapshot inside the frozen protocol limits.
 *
 * <p>The explicit send path surfaces this exception to its caller; automatic tracking and lifecycle
 * handlers catch it, log one actionable error containing the entity identity, generation, revision,
 * entry count and reason, and send nothing. It is internal protocol state, not stable public
 * API.</p>
 */
public final class UnsendableFullSnapshotException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    private final int entityId;
    private final long uuidMost;
    private final long uuidLeast;
    private final long holderGeneration;
    private final long serverRevision;
    private final int entryCount;

    UnsendableFullSnapshotException(FullSnapshotPayload payload, RuntimeException cause) {
        super("cannot encode full snapshot for entity " + payload.entityId()
                + " (uuid " + new UUID(payload.uuidMost(), payload.uuidLeast()) + ")"
                + ", generation " + payload.holderGeneration()
                + ", revision " + payload.serverRevision()
                + ", entryCount " + payload.size()
                + ", dimension " + payload.dimensionId()
                + ": " + cause.getMessage(), cause);
        this.entityId = payload.entityId();
        this.uuidMost = payload.uuidMost();
        this.uuidLeast = payload.uuidLeast();
        this.holderGeneration = payload.holderGeneration();
        this.serverRevision = payload.serverRevision();
        this.entryCount = payload.size();
    }

    /** Returns the addressed runtime entity ID. */
    public int entityId() {
        return entityId;
    }

    /** Returns the addressed entity UUID. */
    public UUID entityUuid() {
        return new UUID(uuidMost, uuidLeast);
    }

    /** Returns the captured provider generation. */
    public long holderGeneration() {
        return holderGeneration;
    }

    /** Returns the captured source holder revision. */
    public long serverRevision() {
        return serverRevision;
    }

    /** Returns the captured entry count. */
    public int entryCount() {
        return entryCount;
    }
}
