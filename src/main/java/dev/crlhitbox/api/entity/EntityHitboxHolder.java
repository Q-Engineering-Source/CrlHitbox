package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.PlacedSolid3d;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable named hitbox state in one caller-defined entity-local frame.
 *
 * <p>The holder retains neither an Entity nor a World. It is deliberately unsynchronized and must
 * be read, mutated, and snapshotted on its owning logical game thread: the server thread for a
 * server-side holder and the client thread for a client-side holder. Phase 2A neither enforces side
 * authority nor synchronizes the independent side-local holders. An immutable snapshot may be
 * handed to another thread after capture.</p>
 */
public final class EntityHitboxHolder {
    private Map<ResourceLocation, PlacedSolid3d> entries = Collections.emptyMap();
    private long revision;

    /** Creates an empty holder at revision zero. */
    public EntityHitboxHolder() {
    }

    /** Returns the holder-local effective-mutation revision. */
    public long revision() {
        return revision;
    }

    /** Returns the number of named placements. */
    public int size() {
        return entries.size();
    }

    /** Returns whether this holder has no named placements. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Returns the exact placement currently stored under {@code id}, if present. */
    public Optional<PlacedSolid3d> find(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(entries.get(id));
    }

    /** Adds or replaces one named placement, preserving an existing ID's position. */
    public boolean put(ResourceLocation id, PlacedSolid3d localPlacement) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(localPlacement, "localPlacement");
        if (localPlacement.equals(entries.get(id))) return false;
        long nextRevision = checkedNextRevision();
        if (entries.isEmpty()) entries = new LinkedHashMap<>();
        entries.put(id, localPlacement);
        revision = nextRevision;
        return true;
    }

    /** Removes one named placement when present. */
    public boolean remove(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!entries.containsKey(id)) return false;
        long nextRevision = checkedNextRevision();
        entries.remove(id);
        if (entries.isEmpty()) entries = Collections.emptyMap();
        revision = nextRevision;
        return true;
    }

    /** Removes every placement as one effective mutation. */
    public boolean clear() {
        if (entries.isEmpty()) return false;
        long nextRevision = checkedNextRevision();
        entries = Collections.emptyMap();
        revision = nextRevision;
        return true;
    }

    /** Captures this holder's revision and ordered entries for immutable handoff. */
    public EntityHitboxSnapshot snapshot() {
        ResourceLocation[] ids = new ResourceLocation[entries.size()];
        PlacedSolid3d[] placements = new PlacedSolid3d[entries.size()];
        int index = 0;
        for (Map.Entry<ResourceLocation, PlacedSolid3d> entry : entries.entrySet()) {
            ids[index] = entry.getKey();
            placements[index] = entry.getValue();
            index++;
        }
        return new EntityHitboxSnapshot(revision, ids, placements);
    }

    private long checkedNextRevision() {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("EntityHitboxHolder revision overflow");
        }
        return revision + 1L;
    }
}
