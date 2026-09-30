package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.CompoundColliderSnapshot;
import dev.crlhitbox.api.collider.Ray3d;
import dev.crlhitbox.api.collider.RayColliderSnapshot;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice E acceptance: the generic entity collider cache, its snapshot and the coordinate adapters. */
class EntityColliderCacheTest {
    private static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);
    private static final ResourceLocation BODY = new ResourceLocation("crlhitbox", "body");
    private static final ResourceLocation PROBE = new ResourceLocation("crlhitbox", "probe");

    @Test
    void freshCacheIsEmptyAtRevisionZero() {
        EntityColliderHolder holder = new EntityColliderHolder();

        assertEquals(0L, holder.revision());
        assertTrue(holder.isEmpty());
        assertEquals(0, holder.size());
        assertTrue(holder.find(BODY).isEmpty());
        assertTrue(holder.remove(BODY) == false, "removing an absent id is a no-op");
        assertFalse(holder.clear(), "clearing an empty cache is a no-op");
    }

    @Test
    void cacheAcceptsEveryColliderKindAndPreservesInsertionOrder() {
        EntityColliderHolder holder = new EntityColliderHolder();

        assertTrue(holder.put(BODY, solidAt(ZERO)));
        assertTrue(holder.put(PROBE, new RayColliderSnapshot(
                new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), 2.0D),
                RigidTransform3d.identity(), true)));
        assertEquals(2L, holder.revision());
        assertEquals(2, holder.size());
        assertInstanceOf(SolidColliderSnapshot.class, holder.find(BODY).orElseThrow());
        assertInstanceOf(RayColliderSnapshot.class, holder.find(PROBE).orElseThrow());

        EntityColliderSnapshot snapshot = holder.snapshot();
        assertEquals(BODY, snapshot.id(0), "insertion order is preserved");
        assertEquals(PROBE, snapshot.id(1));
        assertEquals(2L, snapshot.revision());
        assertEquals(holder.find(PROBE).orElseThrow(), snapshot.collider(1));
        assertEquals(holder.find(BODY).orElseThrow(), snapshot.find(BODY).orElseThrow());
    }

    @Test
    void equalReplacementIsANoOpAndUnequalReplacementKeepsPosition() {
        EntityColliderHolder holder = new EntityColliderHolder();
        holder.put(BODY, solidAt(ZERO));
        holder.put(PROBE, solidAt(new Vec3d(5.0D, 0.0D, 0.0D)));

        assertFalse(holder.put(BODY, solidAt(ZERO)), "an equal value does not mutate the cache");
        assertEquals(2L, holder.revision());

        assertTrue(holder.put(BODY, solidAt(new Vec3d(1.0D, 0.0D, 0.0D))));
        assertEquals(3L, holder.revision());
        assertEquals(BODY, holder.snapshot().id(0), "replacement keeps the ID position");
    }

    @Test
    void replaceContentsPublishesAtomicallyAndIgnoresTheSourceRevision() {
        EntityColliderHolder holder = new EntityColliderHolder();
        holder.put(BODY, solidAt(ZERO));

        Map<ResourceLocation, ColliderSnapshot> entries = new LinkedHashMap<>();
        entries.put(PROBE, solidAt(new Vec3d(2.0D, 0.0D, 0.0D)));
        entries.put(BODY, solidAt(new Vec3d(3.0D, 0.0D, 0.0D)));
        EntityColliderSnapshot incoming = EntityColliderSnapshot.of(99L, entries);

        assertTrue(holder.replaceContents(incoming));
        assertEquals(2L, holder.revision(), "the cache keeps its own revision sequence");
        assertEquals(PROBE, holder.snapshot().id(0), "the incoming order is adopted");
        assertEquals(BODY, holder.snapshot().id(1));

        assertFalse(holder.replaceContents(holder.snapshot()),
                "equal ordered contents are a no-op even with a different source revision");
        assertEquals(2L, holder.revision());
    }

    @Test
    void nullAndDuplicateInputsAreRejected() {
        EntityColliderHolder holder = new EntityColliderHolder();
        assertThrows(NullPointerException.class, () -> holder.put(null, solidAt(ZERO)));
        assertThrows(NullPointerException.class, () -> holder.put(BODY, null));
        assertThrows(NullPointerException.class, () -> holder.find(null));
        assertThrows(NullPointerException.class, () -> holder.replaceContents(null));
        assertThrows(NullPointerException.class, () -> EntityColliderSnapshot.of(0L, null));
        assertThrows(IllegalArgumentException.class,
                () -> EntityColliderSnapshot.of(-1L, Map.of()));

        Map<ResourceLocation, ColliderSnapshot> withNull = new LinkedHashMap<>();
        withNull.put(BODY, null);
        assertThrows(NullPointerException.class, () -> EntityColliderSnapshot.of(0L, withNull));
    }

    @Test
    void snapshotCopiesItsSourceMapDefensively() {
        Map<ResourceLocation, ColliderSnapshot> entries = new LinkedHashMap<>();
        entries.put(BODY, solidAt(ZERO));
        EntityColliderSnapshot snapshot = EntityColliderSnapshot.of(1L, entries);

        entries.clear();

        assertEquals(1, snapshot.size());
        assertEquals(solidAt(ZERO), snapshot.collider(0),
                "the capture still owns the entry it was given");
        assertTrue(snapshot.find(BODY).isPresent());
    }

    @Test
    void snapshotsCompareStructurallyAndExposeNoMutableStorage() {
        EntityColliderSnapshot first = EntityColliderSnapshot.of(1L, orderedEntries());
        EntityColliderSnapshot same = EntityColliderSnapshot.of(1L, orderedEntries());
        Map<ResourceLocation, ColliderSnapshot> reversed = new LinkedHashMap<>();
        reversed.put(PROBE, solidAt(ZERO));
        reversed.put(BODY, solidAt(ZERO));
        EntityColliderSnapshot reordered = EntityColliderSnapshot.of(1L, reversed);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotSame(first, reordered);
        assertFalse(first.equals(reordered), "entry order participates in equality");
        assertFalse(first.isEmpty());
        assertEquals("EntityColliderSnapshot", first.toString().split("\\[")[0]);
    }

    @Test
    void revisionOverflowIsRejectedWithoutChangingTheCache() throws Exception {
        EntityColliderHolder holder = new EntityColliderHolder();
        holder.put(BODY, solidAt(ZERO));
        Field revisionField = EntityColliderHolder.class.getDeclaredField("revision");
        revisionField.setAccessible(true);
        revisionField.setLong(holder, Long.MAX_VALUE);
        EntityColliderSnapshot before = holder.snapshot();

        assertThrows(IllegalStateException.class, () -> holder.put(PROBE, solidAt(ZERO)));
        assertThrows(IllegalStateException.class, () -> holder.clear());

        assertEquals(Long.MAX_VALUE, holder.revision());
        assertEquals(1, holder.size(), "the rejected mutation is not applied");
        assertEquals(before, holder.snapshot(), "the contents are unchanged");
    }

    @Test
    void earlierSnapshotsStayStableAfterLaterMutations() {
        EntityColliderHolder holder = new EntityColliderHolder();
        holder.put(BODY, solidAt(ZERO));
        EntityColliderSnapshot first = holder.snapshot();

        holder.put(BODY, solidAt(new Vec3d(9.0D, 0.0D, 0.0D)));

        assertEquals(solidAt(ZERO), first.collider(0), "the earlier capture is immutable");
        assertEquals(solidAt(new Vec3d(9.0D, 0.0D, 0.0D)), holder.snapshot().collider(0));
    }

    @Test
    void placeInWorldComposesEachPlacementExactlyOnce() {
        RigidTransform3d frame = new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D));
        RigidTransform3d entityLocalPlacement = new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D));

        ColliderSnapshot local = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), entityLocalPlacement, true);
        ColliderSnapshot world = EntityColliderFrames.placeInWorld(local, frame);

        Aabb bounds = world.bounds().orElseThrow();
        assertEquals(10.0D, bounds.min().x(), 0.0D,
                "the local placement puts the sphere at x=1 and the frame adds 10, each once");
        assertEquals(12.0D, bounds.max().x(), 0.0D);
        assertEquals(entityLocalPlacement, local.localToParent(), "the source snapshot is unchanged");
        assertNotSame(local, world);
    }

    @Test
    void placeInWorldHandlesRaysAndCompounds() {
        RigidTransform3d frame = new RigidTransform3d(
                Rotation3d.identity(), new Vec3d(10.0D, 0.0D, 0.0D));
        RayColliderSnapshot ray = new RayColliderSnapshot(
                new Ray3d(ZERO, new Vec3d(1.0D, 0.0D, 0.0D), 2.0D),
                RigidTransform3d.identity(), true);
        CompoundColliderSnapshot compound = new CompoundColliderSnapshot(
                List.of(solidAt(new Vec3d(1.0D, 0.0D, 0.0D))),
                RigidTransform3d.identity(), true);

        ColliderSnapshot placedRay = EntityColliderFrames.placeInWorld(ray, frame);
        ColliderSnapshot placedCompound = EntityColliderFrames.placeInWorld(compound, frame);

        assertInstanceOf(RayColliderSnapshot.class, placedRay);
        assertEquals(10.0D, placedRay.bounds().orElseThrow().min().x(), 0.0D);
        assertEquals(12.0D, placedRay.bounds().orElseThrow().max().x(), 0.0D);

        assertInstanceOf(CompoundColliderSnapshot.class, placedCompound);
        assertEquals(10.0D, placedCompound.bounds().orElseThrow().min().x(), 0.0D,
                "the child stays relative to the compound, so only the frame is added");
        assertEquals(12.0D, placedCompound.bounds().orElseThrow().max().x(), 0.0D);
    }

    @Test
    void placeInWorldPreservesDisabledStateAndRejectsNull() {
        RigidTransform3d frame = RigidTransform3d.identity();
        ColliderSnapshot disabled = new SolidColliderSnapshot(
                new Sphere(ZERO, 1.0D), RigidTransform3d.identity(), false);

        ColliderSnapshot placed = EntityColliderFrames.placeInWorld(disabled, frame);
        assertFalse(placed.enabled());
        assertTrue(placed.bounds().isEmpty());

        assertThrows(NullPointerException.class,
                () -> EntityColliderFrames.placeInWorld(null, frame));
        assertThrows(NullPointerException.class,
                () -> EntityColliderFrames.placeInWorld(disabled, null));
    }

    private static ColliderSnapshot solidAt(Vec3d center) {
        return new SolidColliderSnapshot(new Sphere(center, 1.0D),
                RigidTransform3d.identity(), true);
    }

    private static Map<ResourceLocation, ColliderSnapshot> orderedEntries() {
        Map<ResourceLocation, ColliderSnapshot> entries = new LinkedHashMap<>();
        entries.put(BODY, solidAt(ZERO));
        entries.put(PROBE, solidAt(new Vec3d(4.0D, 0.0D, 0.0D)));
        return entries;
    }
}
