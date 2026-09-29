package dev.crlhitbox.internal.entity;

import dev.crlhitbox.internal.network.FullSnapshotOrdering;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generation allocation, snapshot acceptance ordering, and replica-state behavior.
 *
 * <p>The property suite uses the frozen seed {@code 0x5EED_2B03L} with 2,048 iterations and
 * compares the production classification against an independently written reference model.</p>
 */
class EntityHitboxReplicationPhase2BTest {
    private static final long SEED_ACCEPTANCE = 0x5EED_2B03L;
    private static final int PROPERTY_ITERATIONS = 2_048;

    @Test
    void firstAllocatedGenerationIsPositive() {
        assertTrue(HolderGenerationAllocator.allocate() > 0L);
    }

    @Test
    void allocationIsStrictlyMonotonicAndDistinct() {
        long first = HolderGenerationAllocator.allocate();
        long second = HolderGenerationAllocator.allocate();
        long third = HolderGenerationAllocator.allocate();

        assertEquals(3, Set.of(first, second, third).size(), "distinct generations");
        assertTrue(second > first, "monotonic allocation");
        assertTrue(third > second, "monotonic allocation");
    }

    @Test
    void nearExhaustionReturnsLastPositiveValueThenFailsWithoutWrapping() {
        AtomicLong counter = new AtomicLong(Long.MAX_VALUE - 1L);

        assertEquals(Long.MAX_VALUE, HolderGenerationAllocator.allocateFrom(counter));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> HolderGenerationAllocator.allocateFrom(counter));
        assertTrue(failure.getMessage().contains("exhausted"), failure.getMessage());
        assertEquals(Long.MAX_VALUE, counter.get(), "the counter must never wrap");
    }

    @Test
    void exhaustedAllocatorFailsBeforeReturningNonPositiveValue() {
        AtomicLong counter = new AtomicLong(Long.MAX_VALUE);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> HolderGenerationAllocator.allocateFrom(counter));
        assertTrue(failure.getMessage().contains("exhausted"), failure.getMessage());
    }

    @Test
    void concurrentAllocationStaysUniqueAndPositive() throws Exception {
        int threads = 8;
        int perThread = 256;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<List<Long>>> tasks = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                tasks.add(() -> {
                    List<Long> allocated = new ArrayList<>(perThread);
                    for (int index = 0; index < perThread; index++) {
                        allocated.add(HolderGenerationAllocator.allocate());
                    }
                    return allocated;
                });
            }
            Set<Long> observed = new HashSet<>();
            for (Future<List<Long>> future : executor.invokeAll(tasks)) {
                for (Long generation : future.get()) {
                    assertTrue(generation > 0L, "positive generation");
                    assertTrue(observed.add(generation), "duplicate generation " + generation);
                }
            }
            assertEquals(threads * perThread, observed.size(), "unique generations");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void firstSnapshotIsAcceptedWithoutPreviousState() {
        assertTrue(FullSnapshotOrdering.classify(false, 0L, 0L, 7L, 0L).isAccepted());
        assertEquals(FullSnapshotOrdering.FIRST, FullSnapshotOrdering.classify(false, 0L, 0L, 7L, 3L));
    }

    @Test
    void higherGenerationIsAcceptedEvenWithLowerRevision() {
        FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(true, 5L, 40L, 6L, 1L);

        assertEquals(FullSnapshotOrdering.NEWER_GENERATION, ordering);
        assertTrue(ordering.isAccepted(), "a newer incarnation replaces the old one");
    }

    @Test
    void lowerGenerationIsStaleEvenWithHigherRevision() {
        FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(true, 5L, 1L, 4L, 99L);

        assertEquals(FullSnapshotOrdering.STALE_GENERATION, ordering);
        assertFalse(ordering.isAccepted());
    }

    @Test
    void sameGenerationLowerRevisionIsStale() {
        FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(true, 5L, 9L, 5L, 8L);

        assertEquals(FullSnapshotOrdering.STALE_REVISION, ordering);
        assertFalse(ordering.isAccepted());
    }

    @Test
    void sameGenerationEqualRevisionIsAuthoritativeRepair() {
        FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(true, 5L, 9L, 5L, 9L);

        assertEquals(FullSnapshotOrdering.SAME_GENERATION_EQUAL_REVISION, ordering);
        assertTrue(ordering.isAccepted(), "equal generation/revision repairs client-local changes");
    }

    @Test
    void sameGenerationHigherRevisionIsAccepted() {
        FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(true, 5L, 9L, 5L, 10L);

        assertEquals(FullSnapshotOrdering.SAME_GENERATION_NEWER_REVISION, ordering);
        assertTrue(ordering.isAccepted());
    }

    @Test
    void negativeGenerationIsRejectedInsteadOfBeingReadAsAnUnsignedHugeValue() {
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(true, 5L, 0L, -1L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(true, 5L, 0L, Long.MIN_VALUE, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(true, Long.MIN_VALUE, 0L, 5L, 0L));
    }

    @Test
    void invalidIncomingValuesAreRejectedBeforeComparison() {
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(false, 0L, 0L, 0L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(false, 0L, 0L, 1L, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(true, 0L, 0L, 1L, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> FullSnapshotOrdering.classify(true, 1L, -1L, 1L, 0L));
    }

    @Test
    void freshReplicaStateHasOnePositiveLocalGenerationAndNoRemoteState() {
        EntityHitboxReplicaState state = EntityHitboxReplicaState.create();

        assertTrue(state.localGeneration() > 0L, "positive local generation");
        assertFalse(state.hasAcceptedRemote(), "no accepted remote snapshot");
        assertEquals(0L, state.acceptedRemoteGeneration(), "no accepted generation");
        assertEquals(0L, state.acceptedRemoteRevision(), "no accepted revision");
        assertEquals(0L, state.holderLocalRevisionAtLastInstall(), "no install recorded");
    }

    @Test
    void distinctProvidersReceiveDistinctReplicaStates() {
        EntityHitboxReplicaState first = EntityHitboxReplicaState.create();
        EntityHitboxReplicaState second = EntityHitboxReplicaState.create();

        assertNotEquals(first.localGeneration(), second.localGeneration(),
                "each provider incarnation receives its own generation");
        assertNotSame(first, second);
    }

    @Test
    void replicaStateRecordsOnlyExplicitSuccessfulInstalls() {
        EntityHitboxReplicaState state = EntityHitboxReplicaState.create();

        assertEquals(FullSnapshotOrdering.FIRST, state.classify(11L, 4L));
        state.recordAcceptedInstall(11L, 4L, 3L);

        assertTrue(state.hasAcceptedRemote());
        assertEquals(11L, state.acceptedRemoteGeneration());
        assertEquals(4L, state.acceptedRemoteRevision());
        assertEquals(3L, state.holderLocalRevisionAtLastInstall());
        assertEquals(FullSnapshotOrdering.STALE_REVISION, state.classify(11L, 3L));
        assertEquals(FullSnapshotOrdering.SAME_GENERATION_EQUAL_REVISION, state.classify(11L, 4L));
    }

    @Test
    void replicaStateRejectsInvalidRecordsAndKeepsPreviousState() {
        EntityHitboxReplicaState state = EntityHitboxReplicaState.create();
        state.recordAcceptedInstall(2L, 1L, 1L);

        assertThrows(IllegalArgumentException.class, () -> state.recordAcceptedInstall(0L, 1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> state.recordAcceptedInstall(2L, -1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> state.recordAcceptedInstall(2L, 1L, -1L));

        assertEquals(2L, state.acceptedRemoteGeneration(), "failed records must not mutate state");
        assertEquals(1L, state.acceptedRemoteRevision());
        assertEquals(1L, state.holderLocalRevisionAtLastInstall());
    }

    @Test
    void localTamperRepairKeepsWireRevisionAndHolderRevisionDistinct() {
        EntityHitboxReplicaState state = EntityHitboxReplicaState.create();
        state.recordAcceptedInstall(4L, 7L, 1L);

        // The client mutates its own holder locally, then the server reasserts generation/revision.
        long localHolderRevisionAfterTamper = 2L;
        assertEquals(FullSnapshotOrdering.SAME_GENERATION_EQUAL_REVISION, state.classify(4L, 7L));

        state.recordAcceptedInstall(4L, 7L, localHolderRevisionAfterTamper + 1L);
        assertEquals(7L, state.acceptedRemoteRevision(), "wire revision unchanged");
        assertEquals(3L, state.holderLocalRevisionAtLastInstall(),
                "holder-local revision at install is recorded separately");
    }

    @Test
    void acceptanceRuleMatchesIndependentReferenceForEveryGeneratedCase() {
        Random random = new Random(SEED_ACCEPTANCE);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            boolean hasAccepted = random.nextBoolean();
            long acceptedGeneration = 1L + random.nextInt(4);
            long acceptedRevision = random.nextInt(6);
            long incomingGeneration = 1L + random.nextInt(7);
            long incomingRevision = random.nextInt(8);
            String context = "seed=0x" + Long.toUnsignedString(SEED_ACCEPTANCE, 16).toUpperCase()
                    + "L, iteration=" + iteration
                    + ", hasAcceptedRemote=" + hasAccepted
                    + ", acceptedGeneration=" + acceptedGeneration
                    + ", acceptedRevision=" + acceptedRevision
                    + ", incomingGeneration=" + incomingGeneration
                    + ", incomingRevision=" + incomingRevision;

            boolean expectedAcceptance = referenceAccepts(hasAccepted, acceptedGeneration,
                    acceptedRevision, incomingGeneration, incomingRevision);
            FullSnapshotOrdering ordering = FullSnapshotOrdering.classify(hasAccepted,
                    acceptedGeneration, acceptedRevision, incomingGeneration, incomingRevision);

            assertEquals(expectedAcceptance, ordering.isAccepted(), context);
            assertEquals(expectedAcceptance, referenceAccepts(hasAccepted, acceptedGeneration,
                    acceptedRevision, incomingGeneration, incomingRevision), context);
        }
    }

    /**
     * Independent restatement of the frozen acceptance rule: a newer generation always wins, an
     * older generation is always stale, and equal generations compare by revision with equality
     * accepted as the authoritative repair.
     */
    private static boolean referenceAccepts(boolean hasAcceptedRemote, long acceptedGeneration,
            long acceptedRevision, long incomingGeneration, long incomingRevision) {
        if (!hasAcceptedRemote) return true;
        if (incomingGeneration != acceptedGeneration) {
            return incomingGeneration > acceptedGeneration;
        }
        return incomingRevision >= acceptedRevision;
    }
}
