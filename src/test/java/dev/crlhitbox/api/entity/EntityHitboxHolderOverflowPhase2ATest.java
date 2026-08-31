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

class EntityHitboxHolderOverflowPhase2ATest {
    @Test
    void overflowingPutFailsBeforeAddingOrReplacing() throws ReflectiveOperationException {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        ResourceLocation addedId = new ResourceLocation("crlhitbox", "added");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d replacement = pointPlacement(3.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, first);
        holder.put(secondId, second);
        setRevision(holder, Long.MAX_VALUE);

        assertThrows(NullPointerException.class, () -> holder.put(null, replacement));
        assertThrows(NullPointerException.class, () -> holder.put(firstId, null));
        assertFalse(holder.put(firstId, first));
        IllegalStateException addFailure = assertThrows(
                IllegalStateException.class,
                () -> holder.put(addedId, replacement));
        assertTrue(addFailure.getMessage().contains("revision overflow"));
        assertEquals(Long.MAX_VALUE, holder.revision());
        assertEquals(2, holder.size());
        assertFalse(holder.find(addedId).isPresent());
        assertSame(first, holder.find(firstId).orElseThrow());

        assertThrows(IllegalStateException.class, () -> holder.put(firstId, replacement));
        assertEquals(Long.MAX_VALUE, holder.revision());
        assertSame(first, holder.find(firstId).orElseThrow());
        assertEquals(firstId, holder.snapshot().id(0));
        assertEquals(secondId, holder.snapshot().id(1));
    }

    @Test
    void overflowingRemoveFailsBeforeRemoving() throws ReflectiveOperationException {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, first);
        holder.put(secondId, second);
        setRevision(holder, Long.MAX_VALUE);

        assertThrows(IllegalStateException.class, () -> holder.remove(firstId));
        assertFalse(holder.remove(new ResourceLocation("crlhitbox", "absent")));
        assertEquals(Long.MAX_VALUE, holder.revision());
        assertEquals(2, holder.size());
        assertSame(first, holder.find(firstId).orElseThrow());
        assertSame(second, holder.find(secondId).orElseThrow());
        assertEquals(firstId, holder.snapshot().id(0));
        assertEquals(secondId, holder.snapshot().id(1));

        EntityHitboxHolder empty = new EntityHitboxHolder();
        setRevision(empty, Long.MAX_VALUE);
        assertFalse(empty.clear());
        assertEquals(Long.MAX_VALUE, empty.revision());
    }

    @Test
    void overflowingClearFailsBeforeRemovingAnyEntry() throws ReflectiveOperationException {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        ResourceLocation firstId = new ResourceLocation("crlhitbox", "first");
        ResourceLocation secondId = new ResourceLocation("crlhitbox", "second");
        PlacedSolid3d first = pointPlacement(1.0D);
        PlacedSolid3d second = pointPlacement(2.0D);
        holder.put(firstId, first);
        holder.put(secondId, second);
        setRevision(holder, Long.MAX_VALUE);

        assertThrows(IllegalStateException.class, holder::clear);
        assertEquals(Long.MAX_VALUE, holder.revision());
        assertEquals(2, holder.size());
        assertSame(first, holder.find(firstId).orElseThrow());
        assertSame(second, holder.find(secondId).orElseThrow());
        assertEquals(firstId, holder.snapshot().id(0));
        assertEquals(secondId, holder.snapshot().id(1));
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
