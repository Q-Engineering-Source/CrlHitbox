package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityHitboxReplaceContentsPhase2BTest {
    @Test
    void replacesAnEmptyHolderWithTheSnapshotContentsInSnapshotOrder() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxSnapshot source = snapshot(
                31L,
                new ResourceLocation[] {firstId, secondId},
                new PlacedSolid3d[] {first, second});
        EntityHitboxHolder target = new EntityHitboxHolder();

        assertTrue(target.replaceContents(source));

        EntityHitboxSnapshot installed = target.snapshot();
        assertEquals(1L, target.revision());
        assertEquals(firstId, installed.id(0));
        assertSame(first, installed.placement(0));
        assertEquals(secondId, installed.id(1));
        assertSame(second, installed.placement(1));
    }

    @Test
    void rejectsANullSnapshotBeforeChangingTheHolder() {
        EntityHitboxHolder target = holder(id("first"), pointPlacement(1.0D));
        EntityHitboxSnapshot before = target.snapshot();

        assertThrows(NullPointerException.class, () -> target.replaceContents(null));

        assertEquals(before, target.snapshot());
    }

    @Test
    void replacingEmptyContentsIntoAnEmptyHolderIsANoOp() {
        EntityHitboxHolder target = new EntityHitboxHolder();

        assertFalse(target.replaceContents(snapshot(88L, new ResourceLocation[0], new PlacedSolid3d[0])));

        assertEquals(0L, target.revision());
        assertTrue(target.isEmpty());
    }

    @Test
    void replacingWithAnEmptySnapshotClearsANonemptyHolderOnce() {
        EntityHitboxHolder target = holder(id("first"), pointPlacement(1.0D), id("second"), pointPlacement(2.0D));

        assertTrue(target.replaceContents(snapshot(57L, new ResourceLocation[0], new PlacedSolid3d[0])));

        assertEquals(3L, target.revision());
        assertTrue(target.isEmpty());
    }

    @Test
    void equalContentsIgnoreTheSourceSnapshotRevision() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, second);

        assertFalse(target.replaceContents(snapshot(
                4_294_967_296L,
                new ResourceLocation[] {firstId, secondId},
                new PlacedSolid3d[] {first, second})));

        assertEquals(2L, target.revision());
        assertEquals(firstId, target.snapshot().id(0));
        assertEquals(secondId, target.snapshot().id(1));
    }

    @Test
    void sameMappingsInADifferentOrderReplaceTheOrderedContents() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, second);

        assertTrue(target.replaceContents(snapshot(
                12L,
                new ResourceLocation[] {secondId, firstId},
                new PlacedSolid3d[] {second, first})));

        assertEquals(3L, target.revision());
        assertEquals(secondId, target.snapshot().id(0));
        assertEquals(firstId, target.snapshot().id(1));
    }

    @Test
    void oneChangedPlacementReplacesTheCompleteContentsOnce() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d oldSecond = pointPlacement(2.0D);
        PlacedSolid3d newSecond = pointPlacement(3.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, oldSecond);

        assertTrue(target.replaceContents(snapshot(
                1L,
                new ResourceLocation[] {firstId, secondId},
                new PlacedSolid3d[] {first, newSecond})));

        assertEquals(3L, target.revision());
        assertSame(first, target.snapshot().placement(0));
        assertSame(newSecond, target.snapshot().placement(1));
    }

    @Test
    void oneChangedIdReplacesTheCompleteContentsOnce() {
        ResourceLocation firstId = id("first");
        ResourceLocation oldSecondId = id("old_second");
        ResourceLocation newSecondId = id("new_second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = holder(firstId, first, oldSecondId, second);

        assertTrue(target.replaceContents(snapshot(
                1L,
                new ResourceLocation[] {firstId, newSecondId},
                new PlacedSolid3d[] {first, second})));

        assertEquals(3L, target.revision());
        assertFalse(target.find(oldSecondId).isPresent());
        assertSame(second, target.find(newSecondId).orElseThrow());
    }

    @Test
    void anAddedEntryAdvancesTheLocalRevisionOnlyOnce() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        ResourceLocation thirdId = id("third");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        PlacedSolid3d third = pointPlacement(3.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, second);

        assertTrue(target.replaceContents(snapshot(
                300L,
                new ResourceLocation[] {firstId, secondId, thirdId},
                new PlacedSolid3d[] {first, second, third})));

        assertEquals(3L, target.revision());
        assertEquals(3, target.size());
        assertSame(third, target.find(thirdId).orElseThrow());
    }

    @Test
    void aRemovedEntryAdvancesTheLocalRevisionOnlyOnce() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        ResourceLocation thirdId = id("third");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        PlacedSolid3d third = pointPlacement(3.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, second, thirdId, third);

        assertTrue(target.replaceContents(snapshot(
                300L,
                new ResourceLocation[] {firstId, thirdId},
                new PlacedSolid3d[] {first, third})));

        assertEquals(4L, target.revision());
        assertEquals(2, target.size());
        assertFalse(target.find(secondId).isPresent());
    }

    @Test
    void replacingManyEntriesDoesNotAdoptOrIncrementForEachSourceEntry() {
        EntityHitboxHolder target = holder(id("old"), pointPlacement(-1.0D));
        EntityHitboxSnapshot source = snapshot(
                9_999L,
                new ResourceLocation[] {id("first"), id("second"), id("third")},
                new PlacedSolid3d[] {
                        pointPlacement(1.0D), pointPlacement(2.0D), pointPlacement(3.0D)});

        assertTrue(target.replaceContents(source));

        assertEquals(2L, target.revision());
        assertEquals(3, target.size());
    }

    @Test
    void replacementRetainsTheExactImmutablePlacementInstances() {
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = new EntityHitboxHolder();
        EntityHitboxSnapshot source = snapshot(
                1L,
                new ResourceLocation[] {id("first"), id("second")},
                new PlacedSolid3d[] {first, second});

        target.replaceContents(source);

        assertSame(first, target.snapshot().placement(0));
        assertSame(second, target.snapshot().placement(1));
    }

    @Test
    void equalButDistinctImmutablePlacementValuesAreANoOp() {
        ResourceLocation firstId = id("first");
        PlacedSolid3d stored = pointPlacement(1.0D);
        PlacedSolid3d equalSourcePlacement = pointPlacement(1.0D);
        EntityHitboxHolder target = holder(firstId, stored);

        assertFalse(target.replaceContents(snapshot(
                73L,
                new ResourceLocation[] {firstId},
                new PlacedSolid3d[] {equalSourcePlacement})));

        assertEquals(1L, target.revision());
        assertSame(stored, target.snapshot().placement(0));
    }

    @Test
    void duplicateIdsAreRejectedBeforeChangingTheHolder() {
        ResourceLocation originalId = id("original");
        PlacedSolid3d original = pointPlacement(1.0D);
        EntityHitboxHolder target = holder(originalId, original);
        EntityHitboxSnapshot before = target.snapshot();
        ResourceLocation duplicate = id("duplicate");
        EntityHitboxSnapshot malformed = snapshot(
                11L,
                new ResourceLocation[] {duplicate, duplicate},
                new PlacedSolid3d[] {pointPlacement(2.0D), pointPlacement(3.0D)});

        assertThrows(IllegalArgumentException.class, () -> target.replaceContents(malformed));

        assertEquals(before, target.snapshot());
        assertSame(original, target.find(originalId).orElseThrow());
    }

    @Test
    void nullSnapshotIdsAreRejectedBeforeChangingTheHolder() {
        EntityHitboxHolder target = holder(id("original"), pointPlacement(1.0D));
        EntityHitboxSnapshot before = target.snapshot();
        EntityHitboxSnapshot malformed = snapshot(
                11L,
                new ResourceLocation[] {null},
                new PlacedSolid3d[] {pointPlacement(2.0D)});

        assertThrows(NullPointerException.class, () -> target.replaceContents(malformed));

        assertEquals(before, target.snapshot());
    }

    @Test
    void nullSnapshotPlacementsAreRejectedBeforeChangingTheHolder() {
        EntityHitboxHolder target = holder(id("original"), pointPlacement(1.0D));
        EntityHitboxSnapshot before = target.snapshot();
        EntityHitboxSnapshot malformed = snapshot(
                11L,
                new ResourceLocation[] {id("replacement")},
                new PlacedSolid3d[] {null});

        assertThrows(NullPointerException.class, () -> target.replaceContents(malformed));

        assertEquals(before, target.snapshot());
    }

    @Test
    void overflowOnAContentChangeLeavesTheHolderUnchanged() throws ReflectiveOperationException {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = holder(firstId, first, secondId, second);
        EntityHitboxSnapshot before = target.snapshot();
        setRevision(target, Long.MAX_VALUE);

        assertThrows(IllegalStateException.class, () -> target.replaceContents(snapshot(
                1L,
                new ResourceLocation[] {secondId, firstId},
                new PlacedSolid3d[] {second, first})));

        assertEquals(Long.MAX_VALUE, target.revision());
        assertEquals(before.size(), target.size());
        assertEquals(firstId, target.snapshot().id(0));
        assertSame(first, target.snapshot().placement(0));
    }

    @Test
    void equalContentsRemainALegalNoOpAtRevisionOverflow() throws ReflectiveOperationException {
        ResourceLocation firstId = id("first");
        PlacedSolid3d first = pointPlacement(1.0D);
        EntityHitboxHolder target = holder(firstId, first);
        setRevision(target, Long.MAX_VALUE);

        assertFalse(target.replaceContents(snapshot(
                0L,
                new ResourceLocation[] {firstId},
                new PlacedSolid3d[] {first})));

        assertEquals(Long.MAX_VALUE, target.revision());
        assertSame(first, target.find(firstId).orElseThrow());
    }

    @Test
    void replacementDoesNotMutateAnEarlierHolderSnapshot() {
        ResourceLocation firstId = id("first");
        ResourceLocation secondId = id("second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        EntityHitboxHolder target = holder(firstId, first);
        EntityHitboxSnapshot before = target.snapshot();

        target.replaceContents(snapshot(
                8L,
                new ResourceLocation[] {secondId},
                new PlacedSolid3d[] {second}));

        assertEquals(1L, before.revision());
        assertEquals(firstId, before.id(0));
        assertSame(first, before.placement(0));
        assertEquals(secondId, target.snapshot().id(0));
    }

    @Test
    void emptyReplacementDoesNotMutateAnEarlierHolderSnapshot() {
        ResourceLocation firstId = id("first");
        PlacedSolid3d first = pointPlacement(1.0D);
        EntityHitboxHolder target = holder(firstId, first);
        EntityHitboxSnapshot before = target.snapshot();

        target.replaceContents(snapshot(8L, new ResourceLocation[0], new PlacedSolid3d[0]));

        assertEquals(1, before.size());
        assertEquals(firstId, before.id(0));
        assertSame(first, before.placement(0));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("crlhitbox", path);
    }

    private static EntityHitboxHolder holder(Object... entries) {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        for (int index = 0; index < entries.length; index += 2) {
            holder.put((ResourceLocation) entries[index], (PlacedSolid3d) entries[index + 1]);
        }
        return holder;
    }

    private static EntityHitboxSnapshot snapshot(
            long revision,
            ResourceLocation[] ids,
            PlacedSolid3d[] placements
    ) {
        return new EntityHitboxSnapshot(revision, ids, placements);
    }

    private static void setRevision(EntityHitboxHolder holder, long revision)
            throws ReflectiveOperationException {
        Field field = EntityHitboxHolder.class.getDeclaredField("revision");
        field.setAccessible(true);
        field.setLong(holder, revision);
    }

    private static PlacedSolid3d pointPlacement(double x) {
        Vec3d point = new Vec3d(x, 0.0D, 0.0D);
        return new PlacedSolid3d(new Aabb(point, point), RigidTransform3d.identity());
    }
}
