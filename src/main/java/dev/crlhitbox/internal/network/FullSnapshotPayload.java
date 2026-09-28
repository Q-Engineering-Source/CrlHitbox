package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.entity.EntityHitboxSnapshot;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/**
 * Immutable decoded or to-be-encoded full-snapshot payload state.
 *
 * <p>The value owns defensive copies of its ordered entry arrays and retains no Entity, World,
 * holder, buffer, capability, or transport reference. It is internal protocol state: it is not
 * stable public API and is never exposed from {@code dev.crlhitbox.api.entity}.</p>
 *
 * <p>The carried server revision is the source holder revision used for stale-message ordering. It
 * is deliberately distinct from the local revision of any snapshot rebuilt by
 * {@link #toSnapshot()}, which is a temporary holder's own mutation counter.</p>
 */
final class FullSnapshotPayload {
    private final int dimensionId;
    private final int entityId;
    private final long uuidMost;
    private final long uuidLeast;
    private final long holderGeneration;
    private final long serverRevision;
    private final ResourceLocation[] ids;
    private final PlacedSolid3d[] placements;

    private FullSnapshotPayload(
            int dimensionId,
            int entityId,
            long uuidMost,
            long uuidLeast,
            long holderGeneration,
            long serverRevision,
            ResourceLocation[] ids,
            PlacedSolid3d[] placements
    ) {
        this.dimensionId = dimensionId;
        this.entityId = entityId;
        this.uuidMost = uuidMost;
        this.uuidLeast = uuidLeast;
        this.holderGeneration = holderGeneration;
        this.serverRevision = serverRevision;
        this.ids = ids.clone();
        this.placements = placements.clone();
    }

    /**
     * Captures one server-side holder snapshot with its entity identity and provider generation.
     *
     * <p>The capture is immutable, so the resulting payload stays self-consistent when the source
     * holder is mutated afterwards. Protocol validation happens in the encoder, which fails before
     * any transport call when the captured state is unsendable.</p>
     */
    static FullSnapshotPayload fromSnapshot(
            int dimensionId,
            int entityId,
            long uuidMost,
            long uuidLeast,
            long holderGeneration,
            EntityHitboxSnapshot snapshot
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        int entryCount = snapshot.size();
        ResourceLocation[] ids = new ResourceLocation[entryCount];
        PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
        for (int index = 0; index < entryCount; index++) {
            ids[index] = Objects.requireNonNull(snapshot.id(index), "snapshot id");
            placements[index] = Objects.requireNonNull(snapshot.placement(index), "snapshot placement");
        }
        return new FullSnapshotPayload(
                dimensionId, entityId, uuidMost, uuidLeast, holderGeneration, snapshot.revision(),
                ids, placements);
    }

    /** Creates the payload produced by a completed, fully validated decode. */
    static FullSnapshotPayload decoded(
            int dimensionId,
            int entityId,
            long uuidMost,
            long uuidLeast,
            long holderGeneration,
            long serverRevision,
            ResourceLocation[] ids,
            PlacedSolid3d[] placements
    ) {
        return new FullSnapshotPayload(
                dimensionId, entityId, uuidMost, uuidLeast, holderGeneration, serverRevision,
                ids, placements);
    }

    int dimensionId() {
        return dimensionId;
    }

    /** Returns the nonnegative runtime entity ID. */
    int entityId() {
        return entityId;
    }

    long uuidMost() {
        return uuidMost;
    }

    long uuidLeast() {
        return uuidLeast;
    }

    /** Returns the positive provider generation of the source holder. */
    long holderGeneration() {
        return holderGeneration;
    }

    /** Returns the source holder revision used for stale-message ordering. */
    long serverRevision() {
        return serverRevision;
    }

    /** Returns the number of ordered holder entries. */
    int size() {
        return ids.length;
    }

    ResourceLocation id(int index) {
        return ids[index];
    }

    PlacedSolid3d placement(int index) {
        return placements[index];
    }

    /**
     * Rebuilds an immutable snapshot of the decoded ordered contents through a temporary holder.
     *
     * <p>The returned snapshot's revision is the temporary holder's local mutation counter, not the
     * transmitted server revision. Wire ordering must always compare {@link #serverRevision()}.</p>
     */
    EntityHitboxSnapshot toSnapshot() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        for (int index = 0; index < ids.length; index++) {
            holder.put(ids[index], placements[index]);
        }
        return holder.snapshot();
    }

    @Override
    public String toString() {
        return "FullSnapshotPayload[dimensionId=" + dimensionId
                + ", entityId=" + entityId
                + ", uuidMost=" + uuidMost
                + ", uuidLeast=" + uuidLeast
                + ", holderGeneration=" + holderGeneration
                + ", serverRevision=" + serverRevision
                + ", size=" + ids.length + "]";
    }
}
