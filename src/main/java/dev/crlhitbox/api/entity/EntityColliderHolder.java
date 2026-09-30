package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable named collider cache in one caller-defined entity-local frame.
 *
 * <p>The cache stores immutable {@link ColliderSnapshot} values, so it accepts every supported kind,
 * including finite rays and compounds. It retains neither an Entity nor a World, is deliberately
 * unsynchronized, and must be read, mutated and snapshotted on the logical game thread that owns it;
 * a completed snapshot may be handed to another thread.</p>
 *
 * <p>Publishing is explicit: mutating a standalone collider changes the cache only when its snapshot
 * is stored here. Nothing in this class fires an event, performs networking, or persists anything.</p>
 */
public final class EntityColliderHolder {
    private Map<ResourceLocation, ColliderSnapshot> entries = Collections.emptyMap();
    private long revision;

    /** Creates an empty cache at revision zero. */
    public EntityColliderHolder() {
    }

    /** Returns the holder-local effective-mutation revision. */
    public long revision() {
        return revision;
    }

    /** Returns the number of named colliders. */
    public int size() {
        return entries.size();
    }

    /** Returns whether this cache has no named colliders. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Returns the exact collider currently stored under {@code id}, if present. */
    public Optional<ColliderSnapshot> find(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(entries.get(id));
    }

    /** Adds or replaces one named collider, preserving an existing ID's position. */
    public boolean put(ResourceLocation id, ColliderSnapshot localCollider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(localCollider, "localCollider");
        if (localCollider.equals(entries.get(id))) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        if (entries.isEmpty()) {
            entries = new LinkedHashMap<>();
        }
        entries.put(id, localCollider);
        revision = nextRevision;
        return true;
    }

    /** Removes one named collider when present. */
    public boolean remove(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!entries.containsKey(id)) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        entries.remove(id);
        if (entries.isEmpty()) {
            entries = Collections.emptyMap();
        }
        revision = nextRevision;
        return true;
    }

    /** Removes every collider as one effective mutation. */
    public boolean clear() {
        if (entries.isEmpty()) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        entries = Collections.emptyMap();
        revision = nextRevision;
        return true;
    }

    /**
     * Atomically replaces the ordered contents from {@code snapshot} while retaining this cache's own
     * local revision sequence.
     *
     * <p>The source snapshot revision is descriptive only and is never adopted.</p>
     */
    public boolean replaceContents(EntityColliderSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Map<ResourceLocation, ColliderSnapshot> replacement = replacementEntries(snapshot);
        if (hasOrderedContents(replacement)) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        entries = replacement;
        revision = nextRevision;
        return true;
    }

    /** Captures this cache's revision and ordered entries for immutable handoff. */
    public EntityColliderSnapshot snapshot() {
        ResourceLocation[] ids = new ResourceLocation[entries.size()];
        ColliderSnapshot[] colliders = new ColliderSnapshot[entries.size()];
        int index = 0;
        for (Map.Entry<ResourceLocation, ColliderSnapshot> entry : entries.entrySet()) {
            ids[index] = entry.getKey();
            colliders[index] = entry.getValue();
            index++;
        }
        return new EntityColliderSnapshot(revision, ids, colliders);
    }

    private static Map<ResourceLocation, ColliderSnapshot> replacementEntries(
            EntityColliderSnapshot snapshot) {
        if (snapshot.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<ResourceLocation, ColliderSnapshot> replacement = new LinkedHashMap<>(snapshot.size());
        for (int index = 0; index < snapshot.size(); index++) {
            ResourceLocation id = Objects.requireNonNull(snapshot.id(index), "snapshot id");
            ColliderSnapshot collider = Objects.requireNonNull(
                    snapshot.collider(index), "snapshot collider");
            if (replacement.containsKey(id)) {
                throw new IllegalArgumentException("snapshot contains duplicate id: " + id);
            }
            replacement.put(id, collider);
        }
        return replacement;
    }

    private boolean hasOrderedContents(Map<ResourceLocation, ColliderSnapshot> replacement) {
        if (entries.size() != replacement.size()) {
            return false;
        }
        var current = entries.entrySet().iterator();
        var candidate = replacement.entrySet().iterator();
        while (current.hasNext()) {
            Map.Entry<ResourceLocation, ColliderSnapshot> currentEntry = current.next();
            Map.Entry<ResourceLocation, ColliderSnapshot> candidateEntry = candidate.next();
            if (!currentEntry.getKey().equals(candidateEntry.getKey())
                    || !currentEntry.getValue().equals(candidateEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private long checkedNextRevision() {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("EntityColliderHolder revision overflow");
        }
        return revision + 1L;
    }
}
