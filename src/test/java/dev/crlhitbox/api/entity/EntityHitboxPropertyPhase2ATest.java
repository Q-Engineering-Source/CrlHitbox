package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.PlacedSolid3d;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntityHitboxPropertyPhase2ATest {
    private static final int ITERATIONS = 2_048;

    @Test
    void holderMutationTraceMatchesIndependentLinkedHashMapReferenceModel() {
        long seed = 0x5EED_2A01L;
        Random random = new Random(seed);
        EntityHitboxHolder holder = new EntityHitboxHolder();
        EntityHitboxPhase2ATestSupport.ReferenceHolderModel reference = new EntityHitboxPhase2ATestSupport.ReferenceHolderModel();
        List<String> operations = new ArrayList<>();

        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            ResourceLocation id = EntityHitboxPhase2ATestSupport.id(random);
            PlacedSolid3d placement = EntityHitboxPhase2ATestSupport.placement(random, iteration);
            int operation = random.nextInt(6);
            switch (operation) {
                case 0, 1 -> {
                    boolean expected = reference.put(id, placement);
                    boolean actual = holder.put(id, placement);
                    operations.add("put(" + id + ", " + placement + ")");
                    assertEquals(expected, actual, failure(seed, iteration, operations, id, placement, reference, holder));
                }
                case 2 -> {
                    boolean expected = reference.remove(id);
                    boolean actual = holder.remove(id);
                    operations.add("remove(" + id + ")");
                    assertEquals(expected, actual, failure(seed, iteration, operations, id, placement, reference, holder));
                }
                case 3 -> {
                    boolean expected = reference.clear();
                    boolean actual = holder.clear();
                    operations.add("clear()");
                    assertEquals(expected, actual, failure(seed, iteration, operations, id, placement, reference, holder));
                }
                case 4 -> {
                    Optional<PlacedSolid3d> expected = reference.find(id);
                    Optional<PlacedSolid3d> actual = holder.find(id);
                    operations.add("find(" + id + ")");
                    assertEquals(expected, actual, failure(seed, iteration, operations, id, placement, reference, holder));
                }
                case 5 -> {
                    EntityHitboxPhase2ATestSupport.ReferenceSnapshot expected = reference.snapshot();
                    EntityHitboxSnapshot actual = holder.snapshot();
                    operations.add("snapshot()");
                    assertSnapshotMatches(expected, actual, seed, iteration, operations, id, placement);
                }
                default -> throw new AssertionError("unreachable operation");
            }
            assertHolderMatches(reference, holder, seed, iteration, operations, id, placement);
        }
    }

    @Test
    void historicalSnapshotsRetainIndependentOrderValuesAndStructuralEquality() {
        long seed = 0x5EED_2A02L;
        Random random = new Random(seed);
        List<String> operations = new ArrayList<>();

        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            EntityHitboxHolder holder = new EntityHitboxHolder();
            EntityHitboxPhase2ATestSupport.ReferenceHolderModel reference = new EntityHitboxPhase2ATestSupport.ReferenceHolderModel();
            ResourceLocation firstId = EntityHitboxPhase2ATestSupport.id(random);
            ResourceLocation secondId = EntityHitboxPhase2ATestSupport.id(random);
            PlacedSolid3d first = EntityHitboxPhase2ATestSupport.placement(random, iteration);
            PlacedSolid3d second = EntityHitboxPhase2ATestSupport.placement(random, iteration + 1);
            operations.add("iteration=" + iteration + " put(" + firstId + ", " + first + ")");
            assertEquals(reference.put(firstId, first), holder.put(firstId, first), failure(seed, iteration, operations, firstId, first, reference, holder));
            operations.add("put(" + secondId + ", " + second + ")");
            assertEquals(reference.put(secondId, second), holder.put(secondId, second), failure(seed, iteration, operations, secondId, second, reference, holder));

            EntityHitboxPhase2ATestSupport.ReferenceSnapshot expectedCapture = reference.snapshot();
            EntityHitboxSnapshot historical = holder.snapshot();
            operations.add("capture()");
            assertSnapshotMatches(expectedCapture, historical, seed, iteration, operations, secondId, second);
            assertEquals(historical, holder.snapshot(), failure(seed, iteration, operations, secondId, second, reference, holder));
            assertEquals(historical.hashCode(), holder.snapshot().hashCode(), failure(seed, iteration, operations, secondId, second, reference, holder));

            ResourceLocation mutationId = random.nextBoolean() ? firstId : secondId;
            PlacedSolid3d replacement = EntityHitboxPhase2ATestSupport.placement(random, iteration + 5);
            switch (iteration % 3) {
                case 0 -> {
                    operations.add("replace(" + mutationId + ", " + replacement + ")");
                    assertEquals(reference.put(mutationId, replacement), holder.put(mutationId, replacement), failure(seed, iteration, operations, mutationId, replacement, reference, holder));
                }
                case 1 -> {
                    operations.add("remove(" + mutationId + ")");
                    assertEquals(reference.remove(mutationId), holder.remove(mutationId), failure(seed, iteration, operations, mutationId, replacement, reference, holder));
                }
                case 2 -> {
                    operations.add("clear()");
                    assertEquals(reference.clear(), holder.clear(), failure(seed, iteration, operations, mutationId, replacement, reference, holder));
                }
                default -> throw new AssertionError("unreachable snapshot mutation");
            }
            assertSnapshotMatches(expectedCapture, historical, seed, iteration, operations, mutationId, replacement);
            assertHolderMatches(reference, holder, seed, iteration, operations, mutationId, replacement);
        }
    }

    @Test
    void multipleHoldersRemainIsolatedAcrossNoOpsAndPreserveExactPlacements() {
        long seed = 0x5EED_2A03L;
        Random random = new Random(seed);
        EntityHitboxHolder[] holders = {new EntityHitboxHolder(), new EntityHitboxHolder()};
        EntityHitboxPhase2ATestSupport.ReferenceHolderModel[] references = {
                new EntityHitboxPhase2ATestSupport.ReferenceHolderModel(),
                new EntityHitboxPhase2ATestSupport.ReferenceHolderModel()
        };
        List<String> operations = new ArrayList<>();

        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            int target = random.nextInt(holders.length);
            int other = 1 - target;
            ResourceLocation id = EntityHitboxPhase2ATestSupport.id(random);
            PlacedSolid3d placement = EntityHitboxPhase2ATestSupport.placement(random, iteration);
            EntityHitboxSnapshot otherBefore = holders[other].snapshot();
            EntityHitboxPhase2ATestSupport.ReferenceSnapshot otherExpectedBefore = references[other].snapshot();

            boolean expectedChanged = references[target].put(id, placement);
            boolean actualChanged = holders[target].put(id, placement);
            operations.add("holder[" + target + "].put(" + id + ", " + placement + ")");
            assertEquals(expectedChanged, actualChanged, failure(seed, iteration, operations, id, placement, references[target], holders[target]));

            PlacedSolid3d stored = references[target].find(id).orElseThrow();
            long revisionBeforeNoOp = holders[target].revision();
            boolean expectedNoOp = references[target].put(id, stored);
            boolean actualNoOp = holders[target].put(id, stored);
            operations.add("holder[" + target + "].putSame(" + id + ")");
            assertFalse(expectedNoOp, failure(seed, iteration, operations, id, stored, references[target], holders[target]));
            assertFalse(actualNoOp, failure(seed, iteration, operations, id, stored, references[target], holders[target]));
            assertEquals(revisionBeforeNoOp, holders[target].revision(), failure(seed, iteration, operations, id, stored, references[target], holders[target]));
            assertSame(stored, holders[target].find(id).orElseThrow(), failure(seed, iteration, operations, id, stored, references[target], holders[target]));

            assertSnapshotMatches(otherExpectedBefore, otherBefore, seed, iteration, operations, id, placement);
            assertEquals(otherBefore, holders[other].snapshot(), failure(seed, iteration, operations, id, placement, references[other], holders[other]));
            assertHolderMatches(references[target], holders[target], seed, iteration, operations, id, stored);
            assertHolderMatches(references[other], holders[other], seed, iteration, operations, id, placement);
        }
    }

    private static void assertHolderMatches(
            EntityHitboxPhase2ATestSupport.ReferenceHolderModel expected,
            EntityHitboxHolder actual,
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation lookupId,
            PlacedSolid3d placement
    ) {
        EntityHitboxPhase2ATestSupport.ReferenceSnapshot expectedSnapshot = expected.snapshot();
        EntityHitboxSnapshot actualSnapshot = actual.snapshot();
        EntityHitboxPhase2ATestSupport.FailureContext context = context(
                seed, iteration, operations, lookupId, placement, expected, actual);
        assertEquals(expectedSnapshot.revision(), actual.revision(), context::describe);
        assertEquals(expectedSnapshot.ids().size(), actual.size(), context::describe);
        assertEquals(expectedSnapshot.ids().isEmpty(), actual.isEmpty(), context::describe);
        assertSnapshotMatches(expectedSnapshot, actualSnapshot, seed, iteration, operations, lookupId, placement);
        assertEquals(expected.find(lookupId), actual.find(lookupId), context::describe);
    }

    private static void assertSnapshotMatches(
            EntityHitboxPhase2ATestSupport.ReferenceSnapshot expected,
            EntityHitboxSnapshot actual,
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation lookupId,
            PlacedSolid3d placement
    ) {
        EntityHitboxPhase2ATestSupport.FailureContext context = snapshotContext(
                seed, iteration, operations, lookupId, placement, expected, actual);
        assertEquals(expected.revision(), actual.revision(), context::describe);
        assertEquals(expected.ids().size(), actual.size(), context::describe);
        assertEquals(expected.ids().isEmpty(), actual.isEmpty(), context::describe);
        for (int index = 0; index < expected.ids().size(); index++) {
            assertEquals(expected.ids().get(index), actual.id(index), context::describe);
            assertSame(expected.placements().get(index), actual.placement(index), context::describe);
        }
        assertEquals(expected.find(lookupId), actual.find(lookupId), context::describe);
    }

    private static EntityHitboxPhase2ATestSupport.FailureContext context(
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation id,
            PlacedSolid3d placement,
            EntityHitboxPhase2ATestSupport.ReferenceHolderModel expected,
            EntityHitboxHolder actual
    ) {
        EntityHitboxPhase2ATestSupport.ReferenceSnapshot expectedSnapshot = expected.snapshot();
        EntityHitboxSnapshot actualSnapshot = actual.snapshot();
        List<ResourceLocation> actualIds = new ArrayList<>(actualSnapshot.size());
        for (int index = 0; index < actualSnapshot.size(); index++) actualIds.add(actualSnapshot.id(index));
        return new EntityHitboxPhase2ATestSupport.FailureContext(
                seed,
                iteration,
                List.copyOf(operations),
                id,
                placement,
                expected.revision(),
                actual.revision(),
                expectedSnapshot.ids(),
                List.copyOf(actualIds),
                expected.find(id),
                actual.find(id));
    }

    private static EntityHitboxPhase2ATestSupport.FailureContext snapshotContext(
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation id,
            PlacedSolid3d placement,
            EntityHitboxPhase2ATestSupport.ReferenceSnapshot expected,
            EntityHitboxSnapshot actual
    ) {
        List<ResourceLocation> actualIds = new ArrayList<>(actual.size());
        for (int index = 0; index < actual.size(); index++) actualIds.add(actual.id(index));
        return new EntityHitboxPhase2ATestSupport.FailureContext(
                seed,
                iteration,
                List.copyOf(operations),
                id,
                placement,
                expected.revision(),
                actual.revision(),
                expected.ids(),
                List.copyOf(actualIds),
                expected.find(id),
                actual.find(id));
    }

    private static Supplier<String> failure(
            long seed,
            int iteration,
            List<String> operations,
            ResourceLocation id,
            PlacedSolid3d placement,
            EntityHitboxPhase2ATestSupport.ReferenceHolderModel expected,
            EntityHitboxHolder actual
    ) {
        return () -> context(seed, iteration, operations, id, placement, expected, actual).describe();
    }
}
