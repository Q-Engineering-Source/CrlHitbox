package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.placed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bounded pending full-snapshot store behavior, including the frozen seed {@code 0x5EED_2B04L}
 * property suite compared against an independently written reference model.
 */
class PendingFullSnapshotPhase2BTest {
    private static final long SEED_PENDING = 0x5EED_2B04L;
    private static final int PROPERTY_ITERATIONS = 2_048;

    @Test
    void missingTargetSnapshotIsRetainedAndAddressable() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        FullSnapshotMessage message = message(0, 11, 1L, 2L, 5L, 1L, 2);

        assertTrue(pending.offer(message, 0));

        PendingFullSnapshots.Key key = new PendingFullSnapshots.Key(0, 11, 1L, 2L);
        assertEquals(1, pending.size());
        assertEquals(message, pending.peek(key));
        assertEquals(List.of(key), pending.keysForDimension(0));
        assertTrue(pending.keysForDimension(7).isEmpty());
    }

    @Test
    void newerOrEqualSnapshotReplacesThePendingPayload() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        PendingFullSnapshots.Key key = new PendingFullSnapshots.Key(0, 5, 3L, 4L);

        assertTrue(pending.offer(message(0, 5, 3L, 4L, 2L, 1L, 1), 0));
        FullSnapshotMessage newer = message(0, 5, 3L, 4L, 2L, 2L, 3);
        assertTrue(pending.offer(newer, 10));
        assertEquals(newer, pending.peek(key), "a newer revision replaces the payload");

        FullSnapshotMessage repair = message(0, 5, 3L, 4L, 2L, 2L, 1);
        assertTrue(pending.offer(repair, 20));
        assertEquals(repair, pending.peek(key),
                "an equal generation/revision snapshot replaces the pending payload");
        assertEquals(1, pending.size(), "the key is never duplicated");
    }

    @Test
    void staleSnapshotIsDiscardedAndTheRetainedPayloadKept() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        PendingFullSnapshots.Key key = new PendingFullSnapshots.Key(0, 5, 3L, 4L);
        FullSnapshotMessage retained = message(0, 5, 3L, 4L, 4L, 9L, 2);
        pending.offer(retained, 0);

        assertFalse(pending.offer(message(0, 5, 3L, 4L, 4L, 8L, 1), 1),
                "a lower revision for the same generation is discarded");
        assertFalse(pending.offer(message(0, 5, 3L, 4L, 3L, 99L, 1), 2),
                "a lower generation is discarded even with a higher revision");
        assertEquals(retained, pending.peek(key));
    }

    @Test
    void newerGenerationReplacesTheRetainedPayloadEvenWithLowerRevision() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        PendingFullSnapshots.Key key = new PendingFullSnapshots.Key(0, 5, 3L, 4L);
        pending.offer(message(0, 5, 3L, 4L, 2L, 9L, 1), 0);

        FullSnapshotMessage newerIncarnation = message(0, 5, 3L, 4L, 3L, 1L, 2);
        assertTrue(pending.offer(newerIncarnation, 1));
        assertEquals(newerIncarnation, pending.peek(key));
    }

    @Test
    void byteAccountingFollowsReplacementAndRemoval() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        FullSnapshotMessage small = message(0, 5, 3L, 4L, 1L, 1L, 1);
        FullSnapshotMessage large = message(0, 5, 3L, 4L, 1L, 2L, 4);
        int smallBytes = EntityHitboxWireCodec.encodedSize(small.payload());
        int largeBytes = EntityHitboxWireCodec.encodedSize(large.payload());

        pending.offer(small, 0);
        assertEquals(smallBytes, pending.totalBytes());
        pending.offer(large, 0);
        assertEquals(largeBytes, pending.totalBytes(), "replacement re-accounts the bytes");
        pending.remove(new PendingFullSnapshots.Key(0, 5, 3L, 4L));
        assertEquals(0, pending.totalBytes());
        assertEquals(0, pending.size());
    }

    @Test
    void countCapacityEvictsTheOldestEntriesDeterministically() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        int overflow = 3;
        for (int index = 0; index < PendingFullSnapshots.MAX_PENDING_MESSAGES + overflow; index++) {
            assertTrue(pending.offer(message(0, index, 0L, index, 1L, 0L, 1), index));
        }

        assertEquals(PendingFullSnapshots.MAX_PENDING_MESSAGES, pending.size(),
                "the count limit is never exceeded");
        for (int index = 0; index < overflow; index++) {
            assertNull(pending.peek(new PendingFullSnapshots.Key(0, index, 0L, index)),
                    "entry " + index + " must have been evicted oldest-first");
        }
        assertNotNull(pending.peek(new PendingFullSnapshots.Key(0, overflow, 0L, overflow)));
    }

    @Test
    void byteCapacityEvictsOldestEntriesAndNeverExceedsTheBudget() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        String longPath = "p".repeat(1_014);
        ResourceLocation[] ids = new ResourceLocation[900];
        PlacedSolid3d[] placements = new PlacedSolid3d[900];
        for (int index = 0; index < ids.length; index++) {
            String suffix = Integer.toString(index);
            ids[index] = new ResourceLocation("crlhitbox",
                    longPath.substring(suffix.length()) + suffix);
            placements[index] = placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        }
        FullSnapshotMessage large = messageOf(0, 0, 0L, 0L, 1L, 0L, ids, placements);
        int singleBytes = EntityHitboxWireCodec.encodedSize(large.payload());
        assertTrue(singleBytes < FullSnapshotProtocol.MAX_MESSAGE_BYTES,
                "one legal message is far smaller than the pending byte budget");

        int accepted = 0;
        for (int index = 0; index < 32; index++) {
            FullSnapshotMessage candidate = messageOf(0, index, 0L, index, 1L, 0L, ids, placements);
            if (pending.offer(candidate, index)) accepted++;
            assertTrue(pending.totalBytes() <= PendingFullSnapshots.MAX_PENDING_ENCODED_BYTES,
                    "the byte budget is never exceeded");
        }

        assertTrue(accepted > 1, "several messages must fit inside the byte budget");
        assertTrue(pending.size() < 32, "older entries are evicted to respect the byte budget");
        assertTrue(pending.totalBytes() <= PendingFullSnapshots.MAX_PENDING_ENCODED_BYTES);
    }

    @Test
    void expiryRetainsOneHundredNinetyNineTicksAndRemovesAtTwoHundred() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        pending.offer(message(0, 1, 0L, 1L, 1L, 0L, 1), 1_000);

        assertEquals(0, pending.expire(1_000 + PendingFullSnapshots.PENDING_TTL_CLIENT_TICKS - 1),
                "an entry is retained while its age is below the tick limit");
        assertEquals(1, pending.size());
        assertEquals(1, pending.expire(1_000 + PendingFullSnapshots.PENDING_TTL_CLIENT_TICKS),
                "an entry expires as soon as its age reaches the tick limit");
        assertEquals(0, pending.size());
    }

    @Test
    void expiryOnlyRemovesExpiredEntries() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        pending.offer(message(0, 1, 0L, 1L, 1L, 0L, 1), 0);
        pending.offer(message(0, 2, 0L, 2L, 1L, 0L, 1), 150);

        assertEquals(1, pending.expire(200));
        assertEquals(1, pending.size());
        assertNotNull(pending.peek(new PendingFullSnapshots.Key(0, 2, 0L, 2L)),
                "the younger entry is retained");
    }

    @Test
    void dimensionRemovalAndClearDropOnlyTheIntendedEntries() {
        PendingFullSnapshots pending = new PendingFullSnapshots();
        pending.offer(message(0, 1, 0L, 1L, 1L, 0L, 1), 0);
        pending.offer(message(-1, 2, 0L, 2L, 1L, 0L, 1), 0);
        pending.offer(message(0, 3, 0L, 3L, 1L, 0L, 1), 0);

        assertEquals(2, pending.removeDimension(0), "only the unloaded dimension is dropped");
        assertEquals(1, pending.size());
        assertEquals(1, pending.clear(), "clear drops every remaining session entry");
        assertEquals(0, pending.size());
    }

    @Test
    void offerRejectsTransportInstancesWithoutADecodedPayload() {
        PendingFullSnapshots pending = new PendingFullSnapshots();

        assertThrows(IllegalArgumentException.class,
                () -> pending.offer(new FullSnapshotMessage(), 0));
        assertThrows(NullPointerException.class, () -> pending.offer(null, 0));
    }

    @Test
    void pendingStoreMatchesIndependentReferenceModelForEveryGeneratedOperation() {
        Random random = new Random(SEED_PENDING);
        PendingFullSnapshots store = new PendingFullSnapshots();
        ReferencePendingModel reference = new ReferencePendingModel();
        int tick = 0;

        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            tick += random.nextInt(3);
            int dimensionId = random.nextInt(3) - 1;
            int entityId = random.nextInt(6);
            long generation = 1L + random.nextInt(3);
            long revision = random.nextInt(4);
            String context = "seed=0x" + Long.toUnsignedString(SEED_PENDING, 16).toUpperCase()
                    + "L, iteration=" + iteration + ", tick=" + tick
                    + ", dimensionId=" + dimensionId + ", entityId=" + entityId
                    + ", generation=" + generation + ", revision=" + revision;
            int operation = random.nextInt(5);
            switch (operation) {
                case 0, 1, 2 -> {
                    FullSnapshotMessage message = message(dimensionId, entityId, 0L, entityId,
                            generation, revision, 1 + random.nextInt(3));
                    boolean storeAccepted = store.offer(message, tick);
                    boolean referenceAccepted = reference.offer(
                            new PendingKey(dimensionId, entityId, 0L, entityId),
                            generation, revision, tick,
                            EntityHitboxWireCodec.encodedSize(message.payload()));
                    assertEquals(referenceAccepted, storeAccepted, context);
                }
                case 3 -> assertEquals(reference.expire(tick, PendingFullSnapshots.PENDING_TTL_CLIENT_TICKS),
                        store.expire(tick), context);
                default -> assertEquals(reference.removeDimension(dimensionId),
                        store.removeDimension(dimensionId), context);
            }

            assertEquals(reference.size(), store.size(), context + ", size");
            assertEquals(reference.totalBytes(), store.totalBytes(), context + ", totalBytes");
            for (PendingKey key : reference.keys()) {
                assertNotNull(store.peek(new PendingFullSnapshots.Key(key.dimensionId(),
                        key.entityId(), key.uuidMost(), key.uuidLeast())), context + ", key " + key);
            }
        }
    }

    private static FullSnapshotMessage message(int dimensionId, int entityId, long uuidMost,
            long uuidLeast, long generation, long revision, int entries) {
        ResourceLocation[] ids = new ResourceLocation[entries];
        PlacedSolid3d[] placements = new PlacedSolid3d[entries];
        for (int index = 0; index < entries; index++) {
            ids[index] = new ResourceLocation("crlhitbox", "pending_" + index);
            placements[index] = placed(new Sphere(new Vec3d(index, 0.0D, 0.0D), 0.5D));
        }
        return messageOf(dimensionId, entityId, uuidMost, uuidLeast, generation, revision,
                ids, placements);
    }

    private static FullSnapshotMessage messageOf(int dimensionId, int entityId, long uuidMost,
            long uuidLeast, long generation, long revision, ResourceLocation[] ids,
            PlacedSolid3d[] placements) {
        return FullSnapshotMessage.of(FullSnapshotPayload.decoded(dimensionId, entityId, uuidMost,
                uuidLeast, generation, revision, ids, placements));
    }

    /** Key of the independent reference model. */
    private record PendingKey(int dimensionId, int entityId, long uuidMost, long uuidLeast) {
    }

    /** Independent restatement of the bounded-store rules, written without the production types. */
    private static final class ReferencePendingModel {
        private final Map<PendingKey, long[]> entries = new LinkedHashMap<>();
        private final Map<PendingKey, Integer> queuedTicks = new LinkedHashMap<>();
        private final Map<PendingKey, Integer> bytes = new LinkedHashMap<>();

        boolean offer(PendingKey key, long generation, long revision, int tick, int encodedBytes) {
            if (entries.containsKey(key)) {
                long[] accepted = entries.get(key);
                if (generation < accepted[0] || (generation == accepted[0] && revision < accepted[1])) {
                    return false;
                }
                entries.put(key, new long[] {generation, revision});
                queuedTicks.put(key, tick);
                bytes.put(key, encodedBytes);
                return true;
            }
            while (entries.size() >= PendingFullSnapshots.MAX_PENDING_MESSAGES
                    || totalBytes() + encodedBytes > PendingFullSnapshots.MAX_PENDING_ENCODED_BYTES) {
                PendingKey oldest = entries.keySet().iterator().next();
                entries.remove(oldest);
                queuedTicks.remove(oldest);
                bytes.remove(oldest);
            }
            entries.put(key, new long[] {generation, revision});
            queuedTicks.put(key, tick);
            bytes.put(key, encodedBytes);
            return true;
        }

        int expire(int tick, int ttl) {
            List<PendingKey> expired = new ArrayList<>();
            for (PendingKey key : entries.keySet()) {
                if (tick - queuedTicks.get(key) >= ttl) expired.add(key);
            }
            for (PendingKey key : expired) {
                entries.remove(key);
                queuedTicks.remove(key);
                bytes.remove(key);
            }
            return expired.size();
        }

        int removeDimension(int dimensionId) {
            List<PendingKey> removed = new ArrayList<>();
            for (PendingKey key : entries.keySet()) {
                if (key.dimensionId() == dimensionId) removed.add(key);
            }
            for (PendingKey key : removed) {
                entries.remove(key);
                queuedTicks.remove(key);
                bytes.remove(key);
            }
            return removed.size();
        }

        int size() {
            return entries.size();
        }

        int totalBytes() {
            int total = 0;
            for (int value : bytes.values()) total += value;
            return total;
        }

        List<PendingKey> keys() {
            return new ArrayList<>(entries.keySet());
        }
    }
}
