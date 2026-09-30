package dev.crlhitbox.api.entity;

import dev.crlhitbox.internal.entity.EntityColliderCapability;
import net.minecraft.entity.Entity;

import java.util.Objects;
import java.util.Optional;

/**
 * Public access to an Entity's already-attached CRL Hitbox collider cache.
 *
 * <p>The cache accepts every supported collider kind, including finite rays and compounds. This
 * utility never creates, attaches, persists, or synchronizes capability state; it only reads the
 * cache that the capability provider already owns.</p>
 */
public final class EntityColliders {
    private EntityColliders() {
    }

    /** Finds the already-attached cache without creating or mutating capability state. */
    public static Optional<EntityColliderHolder> find(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return EntityColliderCapability.find(entity);
    }

    /** Returns the attached cache or fails when capability attachment is absent. */
    public static EntityColliderHolder require(Entity entity) {
        return find(entity).orElseThrow(() -> new IllegalStateException(
                "Entity does not have the CRL Hitbox entity_colliders capability"));
    }
}
