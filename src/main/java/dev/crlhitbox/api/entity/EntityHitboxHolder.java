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

    /**
     * Atomically replaces the ordered contents from {@code snapshot} while retaining this holder's
     * own local revision sequence.
     *
     * <p>The source snapshot revision is descriptive only. Equal ordered contents are a no-op;
     * otherwise this holder publishes all replacement entries and advances its revision once.</p>
     */
    public boolean replaceContents(EntityHitboxSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<ResourceLocation, PlacedSolid3d> replacement = replacementEntries(snapshot);
        if (hasOrderedContents(replacement)) return false;
        long nextRevision = checkedNextRevision();
        entries = replacement;
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

    private static Map<ResourceLocation, PlacedSolid3d> replacementEntries(EntityHitboxSnapshot snapshot) {
        if (snapshot.isEmpty()) return Collections.emptyMap();
        Map<ResourceLocation, PlacedSolid3d> replacement = new LinkedHashMap<>(snapshot.size());
        for (int index = 0; index < snapshot.size(); index++) {
            ResourceLocation id = Objects.requireNonNull(snapshot.id(index), "snapshot id");
            PlacedSolid3d placement = Objects.requireNonNull(
                    snapshot.placement(index), "snapshot placement");
            if (replacement.containsKey(id)) {
                throw new IllegalArgumentException("snapshot contains duplicate id: " + id);
            }
            replacement.put(id, placement);
        }
        return replacement;
    }

    private boolean hasOrderedContents(Map<ResourceLocation, PlacedSolid3d> replacement) {
        if (entries.size() != replacement.size()) return false;
        var current = entries.entrySet().iterator();
        var candidate = replacement.entrySet().iterator();
        while (current.hasNext()) {
            Map.Entry<ResourceLocation, PlacedSolid3d> currentEntry = current.next();
            Map.Entry<ResourceLocation, PlacedSolid3d> candidateEntry = candidate.next();
            if (!currentEntry.getKey().equals(candidateEntry.getKey())
                    || !currentEntry.getValue().equals(candidateEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private long checkedNextRevision() {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("EntityHitboxHolder revision overflow");
        }
        return revision + 1L;
    }
}
