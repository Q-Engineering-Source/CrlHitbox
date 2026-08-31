package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.geometry.PlacedSolid3d;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable ordered capture of one entity hitbox holder revision.
 *
 * <p>The snapshot defensively owns its ordered storage. Its ResourceLocation and PlacedSolid3d
 * elements are immutable values, so a completed capture may safely be handed to another thread.</p>
 */
public final class EntityHitboxSnapshot {
    private final long revision;
    private final ResourceLocation[] ids;
    private final PlacedSolid3d[] placements;

    EntityHitboxSnapshot(long revision, ResourceLocation[] ids, PlacedSolid3d[] placements) {
        this.revision = revision;
        this.ids = ids.clone();
        this.placements = placements.clone();
    }

    /** Returns the captured holder revision. */
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

    /** Returns the exact immutable placement at {@code index}. */
    public PlacedSolid3d placement(int index) {
        return placements[index];
    }

    /** Finds a captured placement by ID using the snapshot's ordered storage. */
    public Optional<PlacedSolid3d> find(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        for (int index = 0; index < ids.length; index++) {
            if (ids[index].equals(id)) return Optional.of(placements[index]);
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof EntityHitboxSnapshot other
                && revision == other.revision
                && Arrays.equals(ids, other.ids)
                && Arrays.equals(placements, other.placements);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(revision);
        result = 31 * result + Arrays.hashCode(ids);
        return 31 * result + Arrays.hashCode(placements);
    }

    @Override
    public String toString() {
        return "EntityHitboxSnapshot[revision=" + revision
                + ", ids=" + Arrays.toString(ids)
                + ", placements=" + Arrays.toString(placements) + "]";
    }
}
