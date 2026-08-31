package dev.crlhitbox.api.entity;

import dev.crlhitbox.internal.entity.EntityHitboxCapability;
import net.minecraft.entity.Entity;

import java.util.Objects;
import java.util.Optional;

/**
 * Public non-sided access to an Entity's already-attached CRL Hitbox holder capability.
 * This utility never creates, attaches, persists, or synchronizes capability state.
 */
public final class EntityHitboxes {
    private EntityHitboxes() {
    }

    /** Finds the already-attached holder without creating or mutating capability state. */
    public static Optional<EntityHitboxHolder> find(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return EntityHitboxCapability.find(entity);
    }

    /** Returns the attached holder or fails when capability attachment is absent. */
    public static EntityHitboxHolder require(Entity entity) {
        return find(entity).orElseThrow(() -> new IllegalStateException(
                "Entity does not have the CRL Hitbox entity_hitboxes capability"));
    }
}
