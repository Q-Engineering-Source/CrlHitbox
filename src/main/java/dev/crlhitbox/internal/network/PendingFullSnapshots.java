package dev.crlhitbox.internal.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded client-side store for full snapshots whose target world or Entity is not yet available.
 *
 * <p>The store is keyed by dimension ID, runtime entity ID, and Entity UUID, keeps deterministic
 * insertion order, and never retains an Entity, world, holder, capability, network-handler, or
 * transport reference. Capacity is bounded by both a message count and a total encoded byte budget;
 * a single valid message can never exceed the byte budget because {@code MAX_MESSAGE_BYTES} is much
 * smaller. Entries expire after a fixed number of client ticks.</p>
 *
 * <p>For one key the same generation/revision stale-order rule as live installation applies: an
 * older incoming snapshot is discarded, and a newer or equal authoritative snapshot replaces the
 * retained payload. This is internal client state, not stable public API.</p>
 */
public final class PendingFullSnapshots {
    /** Maximum number of retained pending snapshots. */
    public static final int MAX_PENDING_MESSAGES = 256;

    /** Maximum total encoded bytes retained across all pending snapshots. */
    public static final int MAX_PENDING_ENCODED_BYTES = 16 * 1_048_576;

    /**
     * Maximum age in client ticks. An entry queued at tick {@code t} is retained while
     * {@code currentTick - t < 200} and expires as soon as the age reaches 200.
     */
    public static final int PENDING_TTL_CLIENT_TICKS = 200;

    private final Map<Key, Entry> pending = new LinkedHashMap<>();

    /** Identity of one pending target. */
    public record Key(int dimensionId, int entityId, long uuidMost, long uuidLeast) {
    }

    private record Entry(FullSnapshotMessage message, int encodedBytes, int queuedTick) {
    }

    /**
     * Retains one decoded snapshot, replacing or discarding an older one for the same key.
     *
     * @return whether the incoming snapshot is now retained
     */
    public boolean offer(FullSnapshotMessage message, int currentTick) {
        Objects.requireNonNull(message, "message");
        FullSnapshotPayload payload = message.payload();
        if (payload == null) {
            throw new IllegalArgumentException("pending full snapshot has no decoded payload");
        }
        int encodedBytes = EntityHitboxWireCodec.encodedSize(payload);
        Key key = new Key(payload.dimensionId(), payload.entityId(),
                payload.uuidMost(), payload.uuidLeast());
        Entry existing = pending.get(key);
        if (existing != null) {
            FullSnapshotPayload retained = existing.message().payload();
            FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(
                    true,
                    retained.holderGeneration(), retained.serverRevision(),
                    payload.holderGeneration(), payload.serverRevision());
            if (!ordering.isAccepted()) {
                return false;
            }
            pending.put(key, new Entry(message, encodedBytes, currentTick));
            return true;
        }
        evictOldestUntilFits(encodedBytes);
        pending.put(key, new Entry(message, encodedBytes, currentTick));
        return true;
    }

    /** Returns the retained keys for one dimension in deterministic insertion order. */
    public List<Key> keysForDimension(int dimensionId) {
        List<Key> keys = new ArrayList<>();
        for (Key key : pending.keySet()) {
            if (key.dimensionId() == dimensionId) {
                keys.add(key);
            }
        }
        return keys;
    }

    /** Returns the retained message for {@code key}, if any. */
    public FullSnapshotMessage peek(Key key) {
        Objects.requireNonNull(key, "key");
        Entry entry = pending.get(key);
        return entry == null ? null : entry.message();
    }

    /** Removes one retained message. */
    public boolean remove(Key key) {
        Objects.requireNonNull(key, "key");
        return pending.remove(key) != null;
    }

    /** Removes every entry whose age has reached the tick limit. */
    public int expire(int currentTick) {
        int removed = 0;
        Iterator<Map.Entry<Key, Entry>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (currentTick - entry.queuedTick() >= PENDING_TTL_CLIENT_TICKS) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /** Removes every entry targeting one dimension, as on that world's unload. */
    public int removeDimension(int dimensionId) {
        int removed = 0;
        Iterator<Key> iterator = pending.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().dimensionId() == dimensionId) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /** Drops every retained message, as on connection start or disconnection. */
    public int clear() {
        int removed = pending.size();
        pending.clear();
        return removed;
    }

    /** Returns the number of retained messages. */
    public int size() {
        return pending.size();
    }

    /** Returns the total encoded bytes accounted to retained messages. */
    public int totalBytes() {
        int total = 0;
        for (Entry entry : pending.values()) {
            total += entry.encodedBytes();
        }
        return total;
    }

    private void evictOldestUntilFits(int incomingBytes) {
        Iterator<Key> iterator = pending.keySet().iterator();
        while (iterator.hasNext()
                && (pending.size() >= MAX_PENDING_MESSAGES
                || totalBytes() + incomingBytes > MAX_PENDING_ENCODED_BYTES)) {
            iterator.next();
            iterator.remove();
        }
    }
}
