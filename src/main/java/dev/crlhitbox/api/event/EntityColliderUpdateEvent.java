package dev.crlhitbox.api.event;

import dev.crlhitbox.api.entity.EntityColliderHolder;
import dev.crlhitbox.api.entity.EntityColliderSnapshot;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Objects;

/**
 * Explicit update request for one Entity's collider cache.
 *
 * <p>This is an <em>update entry</em>, not a collision callback: it is posted synchronously once by
 * {@code EntityColliders.requestUpdate} so that external listeners can recompute shapes from their
 * own state (pose, equipment, animation) and publish them through
 * {@link EntityColliderHolder#put} or {@link EntityColliderHolder#replaceContents}. Nothing is sent
 * over the network, no default shape is generated, and no Entity is scanned.</p>
 *
 * <p>The event is deliberately not cancellable: a listener that wants to change the outcome simply
 * publishes a different snapshot.</p>
 */
public final class EntityColliderUpdateEvent extends Event {
    private final Entity entity;
    private final ResourceLocation reason;
    private final EntityColliderHolder holder;
    private final EntityColliderSnapshot before;

    /**
     * Creates one update request.
     *
     * @param entity the entity whose cache is being updated
     * @param reason a caller-defined reason identifier
     * @param holder the attached collider cache
     * @param before the cache contents captured before the event was posted
     */
    public EntityColliderUpdateEvent(
            Entity entity,
            ResourceLocation reason,
            EntityColliderHolder holder,
            EntityColliderSnapshot before
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.holder = Objects.requireNonNull(holder, "holder");
        this.before = Objects.requireNonNull(before, "before");
    }

    /** Returns the entity whose collider cache is being updated. */
    public Entity getEntity() {
        return entity;
    }

    /** Returns the caller-defined reason identifier. */
    public ResourceLocation getReason() {
        return reason;
    }

    /** Returns the mutable cache that listeners publish into. */
    public EntityColliderHolder getHolder() {
        return holder;
    }

    /** Returns the immutable contents captured before the event was posted. */
    public EntityColliderSnapshot getBefore() {
        return before;
    }
}
