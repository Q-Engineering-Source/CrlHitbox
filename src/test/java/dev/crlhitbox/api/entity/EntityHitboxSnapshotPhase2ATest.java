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

class EntityHitboxSnapshotPhase2ATest {
    @Test
    void findReturnsExactPlacementAndRejectsNullId() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation present = new ResourceLocation("crlhitbox", "present");
        ResourceLocation absent = new ResourceLocation("crlhitbox", "absent");
        PlacedSolid3d placement = pointPlacement(1.0D);
        holder.put(present, placement);
        EntityHitboxSnapshot snapshot = holder.snapshot();

        assertSame(placement, snapshot.find(present).orElseThrow());
        assertFalse(snapshot.find(absent).isPresent());
        assertThrows(NullPointerException.class, () -> snapshot.find(null));
    }

    @Test
    void structuralEqualityIncludesRevisionAndOrderedEntries() {
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        ResourceLocation temporaryId = new ResourceLocation("crlhitbox", "temporary");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);

        EntityHitboxSnapshot firstCapture = capture(firstId, first, secondId, second);
        EntityHitboxSnapshot equalCapture = capture(firstId, first, secondId, second);
        EntityHitboxSnapshot differentOrder = capture(secondId, second, firstId, first);
        EntityHitboxHolder differentRevisionHolder = new EntityHitboxHolder();
        differentRevisionHolder.put(temporaryId, pointPlacement(3.0D));
        differentRevisionHolder.remove(temporaryId);
        differentRevisionHolder.put(firstId, first);
        differentRevisionHolder.put(secondId, second);
        EntityHitboxSnapshot differentRevision = differentRevisionHolder.snapshot();

        assertEquals(firstCapture, equalCapture);
        assertEquals(firstCapture.hashCode(), equalCapture.hashCode());
        assertEquals(firstCapture.toString(), equalCapture.toString());
        assertNotEquals(firstCapture, differentOrder);
        assertNotEquals(firstCapture, differentRevision);
    }

    @Test
    void emptySnapshotRetainsTheCurrentRevision() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        holder.put(id, pointPlacement(1.0D));
        holder.clear();

        EntityHitboxSnapshot snapshot = holder.snapshot();
        assertTrue(snapshot.isEmpty());
        assertEquals(0, snapshot.size());
        assertEquals(2L, snapshot.revision());
    }

    @Test
    void oneEntrySnapshotRetainsExactIdAndPlacement() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        PlacedSolid3d placement = pointPlacement(1.0D);
        holder.put(id, placement);

        EntityHitboxSnapshot snapshot = holder.snapshot();
        assertEquals(id, snapshot.id(0));
        assertSame(placement, snapshot.placement(0));
    }

    @Test
    void multipleEntriesPreserveOrderedIdsAndPlacements() {
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxSnapshot snapshot = capture(firstId, first, secondId, second);

        assertEquals(firstId, snapshot.id(0));
        assertSame(first, snapshot.placement(0));
        assertEquals(secondId, snapshot.id(1));
        assertSame(second, snapshot.placement(1));
    }

    @Test
    void idUsesStandardNegativeIndexFailure() {
        EntityHitboxSnapshot snapshot = new EntityHitboxHolder().snapshot();

        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.id(-1));
    }

    @Test
    void placementUsesStandardSizeIndexFailure() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        holder.put(new ResourceLocation("crlhitbox", "core"), pointPlacement(1.0D));
        EntityHitboxSnapshot snapshot = holder.snapshot();

        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.placement(snapshot.size()));
    }

    @Test
    void laterHolderAdditionDoesNotChangeEarlierSnapshot() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        PlacedSolid3d first = pointPlacement(1.0D);
        holder.put(firstId, first);
        EntityHitboxSnapshot snapshot = holder.snapshot();

        holder.put(new ResourceLocation("crlhitbox", "second"), pointPlacement(2.0D));

        assertEquals(1, snapshot.size());
        assertEquals(firstId, snapshot.id(0));
        assertSame(first, snapshot.placement(0));
    }

    @Test
    void holderClearDoesNotChangeEarlierSnapshot() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        PlacedSolid3d placement = pointPlacement(1.0D);
        holder.put(id, placement);
        EntityHitboxSnapshot snapshot = holder.snapshot();

        holder.clear();

        assertEquals(1, snapshot.size());
        assertEquals(id, snapshot.id(0));
        assertSame(placement, snapshot.placement(0));
    }

    @Test
    void holderReplacementDoesNotChangeEarlierSnapshot() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation id = new ResourceLocation("crlhitbox", "core");
        PlacedSolid3d original = pointPlacement(1.0D);
        holder.put(id, original);
        EntityHitboxSnapshot snapshot = holder.snapshot();

        holder.put(id, pointPlacement(2.0D));

        assertSame(original, snapshot.placement(0));
    }

    @Test
    void equalityIsReflexiveSymmetricTransitiveAndHashConsistent() {
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxSnapshot a = capture(firstId, first, secondId, second);
        EntityHitboxSnapshot b = capture(firstId, first, secondId, second);
        EntityHitboxSnapshot c = capture(firstId, first, secondId, second);

        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(b, c);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentIdProducesStructuralInequality() {
        EntityHitboxSnapshot first = capture(
                new ResourceLocation("crlhitbox", "first"), pointPlacement(1.0D),
                new ResourceLocation("crlhitbox", "second"), pointPlacement(2.0D));
        EntityHitboxSnapshot second = capture(
                new ResourceLocation("crlhitbox", "changed"), pointPlacement(1.0D),
                new ResourceLocation("crlhitbox", "second"), pointPlacement(2.0D));

        assertNotEquals(first, second);
    }

    @Test
    void differentPlacementProducesStructuralInequality() {
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        EntityHitboxSnapshot first = capture(firstId, pointPlacement(1.0D), secondId, pointPlacement(2.0D));
        EntityHitboxSnapshot second = capture(firstId, pointPlacement(3.0D), secondId, pointPlacement(2.0D));

        assertNotEquals(first, second);
    }

    @Test
    void equalSnapshotsHaveDeterministicToString() {
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);

        assertEquals(
                capture(firstId, first, secondId, second).toString(),
                capture(firstId, first, secondId, second).toString());
    }

    private static EntityHitboxSnapshot capture(
            ResourceLocation firstId,
            PlacedSolid3d first,
            ResourceLocation secondId,
            PlacedSolid3d second
    ) {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        holder.put(firstId, first);
        holder.put(secondId, second);
        return holder.snapshot();
    }

    private static PlacedSolid3d pointPlacement(double x) {
        Vec3d point = new Vec3d(x, 0.0D, 0.0D);
        return new PlacedSolid3d(new Aabb(point, point), RigidTransform3d.identity());
    }
}
