package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityHitboxHolderPhase2ATest {
    @Test
    void newHolderStartsEmptyAtRevisionZero() {
        EntityHitboxHolder holder = new EntityHitboxHolder();

        assertEquals(0L, holder.revision());
        assertEquals(0, holder.size());
        assertTrue(holder.isEmpty());
    }

    @Test
    void putNewEntryReturnsTrueAndFindReturnsTheExactPlacement() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        PlacedSolid3d placement = pointPlacement(1.0D);

        assertTrue(holder.put(id, placement));
        assertEquals(1L, holder.revision());
        assertEquals(1, holder.size());
        assertFalse(holder.isEmpty());
        assertSame(placement, holder.find(id).orElseThrow());
    }

    @Test
    void unequalReplacementPreservesEncounterOrderAndAdvancesOnce() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d replacement = pointPlacement(3.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, first);
        holder.put(secondId, second);

        assertTrue(holder.put(firstId, replacement));
        EntityHitboxSnapshot snapshot = holder.snapshot();
        assertEquals(3L, holder.revision());
        assertEquals(3L, snapshot.revision());
        assertEquals(2, snapshot.size());
        assertEquals(firstId, snapshot.id(0));
        assertSame(replacement, snapshot.placement(0));
        assertEquals(secondId, snapshot.id(1));
        assertSame(second, snapshot.placement(1));
    }

    @Test
    void removePresentAbsentAndReaddMovesIdToEnd() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, first);
        holder.put(secondId, second);

        assertTrue(holder.remove(firstId));
        assertFalse(holder.remove(firstId));
        assertEquals(3L, holder.revision());
        assertTrue(holder.put(firstId, first));
        EntityHitboxSnapshot snapshot = holder.snapshot();
        assertEquals(4L, snapshot.revision());
        assertEquals(secondId, snapshot.id(0));
        assertEquals(firstId, snapshot.id(1));
    }

    @Test
    void clearAdvancesOnceWhenNonemptyAndNeverResetsRevision() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        holder.put(new ResourceLocation("crlhitbox", "first"), pointPlacement(1.0D));
        holder.put(new ResourceLocation("crlhitbox", "second"), pointPlacement(2.0D));
        holder.put(new ResourceLocation("crlhitbox", "third"), pointPlacement(3.0D));

        assertTrue(holder.clear());
        assertEquals(4L, holder.revision());
        assertTrue(holder.isEmpty());
        assertFalse(holder.clear());
        assertEquals(4L, holder.revision());
        holder.put(new ResourceLocation("crlhitbox", "later"), pointPlacement(4.0D));
        assertTrue(holder.clear());
        assertEquals(6L, holder.revision());
    }

    @Test
    void putRejectsNullIdBeforeMutatingState() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        EntityHitboxSnapshot before = holder.snapshot();

        assertThrows(NullPointerException.class, () -> holder.put(null, pointPlacement(1.0D)));
        assertEquals(before, holder.snapshot());
    }

    @Test
    void putRejectsNullPlacementBeforeMutatingState() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        EntityHitboxSnapshot before = holder.snapshot();

        assertThrows(NullPointerException.class, () -> holder.put(id, null));
        assertEquals(before, holder.snapshot());
    }

    @Test
    void findAndRemoveRejectNullId() {
        EntityHitboxHolder holder = new EntityHitboxHolder();

        assertThrows(NullPointerException.class, () -> holder.find(null));
        assertThrows(NullPointerException.class, () -> holder.remove(null));
        assertEquals(0L, holder.revision());
    }

    @Test
    void identicalReplacementIsANoOp() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        PlacedSolid3d placement = pointPlacement(1.0D);
        holder.put(id, placement);

        assertFalse(holder.put(id, placement));
        assertFalse(holder.put(id, pointPlacement(1.0D)));
        assertEquals(1L, holder.revision());
        assertSame(placement, holder.find(id).orElseThrow());
    }

    @Test
    void absentRemovalDoesNotAdvanceRevision() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation present = new ResourceLocation("crlhitbox", "present");
        holder.put(present, pointPlacement(1.0D));

        assertFalse(holder.remove(new ResourceLocation("crlhitbox", "absent")));
        assertEquals(1L, holder.revision());
        assertEquals(1, holder.size());
    }

    @Test
    void differentIdsMayStoreEqualPlacementsWithoutDeduplication() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        PlacedSolid3d placement = pointPlacement(1.0D);
        ResourceLocation first = new ResourceLocation("crlhitbox", "first");
        ResourceLocation second = new ResourceLocation("crlhitbox", "second");

        assertTrue(holder.put(first, placement));
        assertTrue(holder.put(second, placement));
        assertEquals(2, holder.size());
        assertSame(placement, holder.find(first).orElseThrow());
        assertSame(placement, holder.find(second).orElseThrow());
    }

    @Test
    void everyReadMethodLeavesRevisionAndEntriesUnchanged() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        holder.put(id, pointPlacement(1.0D));
        EntityHitboxSnapshot before = holder.snapshot();

        holder.revision();
        holder.size();
        holder.isEmpty();
        holder.find(id);
        holder.snapshot();

        assertEquals(1L, holder.revision());
        assertEquals(before, holder.snapshot());
    }

    @Test
    void manyInsertionsPreserveExactEncounterOrder() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        for (int index = 0; index < 128; index++) {
            holder.put(new ResourceLocation("crlhitbox", "entry_" + index), pointPlacement(index));
        }

        EntityHitboxSnapshot snapshot = holder.snapshot();
        assertEquals(128L, snapshot.revision());
        assertEquals(128, snapshot.size());
        for (int index = 0; index < 128; index++) {
            assertEquals(new ResourceLocation("crlhitbox", "entry_" + index), snapshot.id(index));
            assertEquals(pointPlacement(index), snapshot.placement(index));
        }
    }

    @Test
    void holderRetainsObjectIdentityEquality() {
        EntityHitboxHolder first = new EntityHitboxHolder();
        EntityHitboxHolder second = new EntityHitboxHolder();

        assertEquals(first, first);
        assertNotEquals(first, second);
    }

    @Test
    void clearOnNeverUsedHolderIsANoOp() {
        EntityHitboxHolder holder = new EntityHitboxHolder();

        assertFalse(holder.clear());
        assertEquals(0L, holder.revision());
        assertTrue(holder.isEmpty());
    }

    @Test
    void replacingOneIdNeverChangesSizeOrOtherMappings() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, pointPlacement(1.0D));
        holder.put(secondId, second);

        assertTrue(holder.put(firstId, pointPlacement(3.0D)));
        assertEquals(2, holder.size());
        assertSame(second, holder.find(secondId).orElseThrow());
    }

    private static PlacedSolid3d pointPlacement(double x) {
        Vec3d point = new Vec3d(x, 0.0D, 0.0D);
        return new PlacedSolid3d(new Aabb(point, point), RigidTransform3d.identity());
    }
}
