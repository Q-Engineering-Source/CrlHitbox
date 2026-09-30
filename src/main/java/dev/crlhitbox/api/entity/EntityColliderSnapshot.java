package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable ordered capture of one entity collider cache revision.
 *
 * <p>The capture defensively owns its ordered storage, and its elements are immutable values, so a
 * completed capture may safely be handed to another thread.</p>
 */
public final class EntityColliderSnapshot {
    private final long revision;
    private final ResourceLocation[] ids;
    private final ColliderSnapshot[] colliders;

    EntityColliderSnapshot(long revision, ResourceLocation[] ids, ColliderSnapshot[] colliders) {
        this.revision = revision;
        this.ids = ids.clone();
        this.colliders = colliders.clone();
    }

    /**
     * Captures one ordered mapping.
     *
     * <p>The map's iteration order is preserved; consumers that need a defined order must pass an
     * ordered map. Null keys and null colliders are rejected.</p>
     *
     * @param revision the nonnegative source revision
     * @param entries the ordered entries to capture
     */
    public static EntityColliderSnapshot of(
            long revision,
            Map<ResourceLocation, ? extends ColliderSnapshot> entries
    ) {
        Objects.requireNonNull(entries, "entries");
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must be nonnegative: " + revision);
        }
        ResourceLocation[] ids = new ResourceLocation[entries.size()];
        ColliderSnapshot[] colliders = new ColliderSnapshot[entries.size()];
        int index = 0;
        for (Map.Entry<ResourceLocation, ? extends ColliderSnapshot> entry : entries.entrySet()) {
            ids[index] = Objects.requireNonNull(entry.getKey(), "entry id");
            colliders[index] = Objects.requireNonNull(entry.getValue(), "entry collider");
            index++;
        }
        return new EntityColliderSnapshot(revision, ids, colliders);
    }

    /** Returns the captured revision. */
    public long revision() {
        return revision;
    }

    /** Returns the number of captured entries. */
    public int size() {
        return ids.length;
    }

    /** Returns whether this capture has no entries. */
    public boolean isEmpty() {
        return ids.length == 0;
    }

    /** Returns the entry ID at {@code index}. */
    public ResourceLocation id(int index) {
        return ids[index];
    }

    /** Returns the exact immutable collider at {@code index}. */
    public ColliderSnapshot collider(int index) {
        return colliders[index];
    }

    /** Finds a captured collider by ID using the snapshot's ordered storage. */
    public Optional<ColliderSnapshot> find(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        for (int index = 0; index < ids.length; index++) {
            if (ids[index].equals(id)) {
                return Optional.of(colliders[index]);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof EntityColliderSnapshot other
                && revision == other.revision
                && Arrays.equals(ids, other.ids)
                && Arrays.equals(colliders, other.colliders);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(revision);
        result = 31 * result + Arrays.hashCode(ids);
        return 31 * result + Arrays.hashCode(colliders);
    }

    @Override
    public String toString() {
        return "EntityColliderSnapshot[revision=" + revision
                + ", ids=" + Arrays.toString(ids)
                + ", colliders=" + Arrays.toString(colliders) + "]";
    }
}
