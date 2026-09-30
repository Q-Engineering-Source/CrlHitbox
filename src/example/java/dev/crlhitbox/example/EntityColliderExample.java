package dev.crlhitbox.example;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.ColliderQueries;
import dev.crlhitbox.api.collider.MutableCapsuleCollider;
import dev.crlhitbox.api.collider.MutableRayCollider;
import dev.crlhitbox.api.entity.EntityColliderFrames;
import dev.crlhitbox.api.entity.EntityColliderHolder;
import dev.crlhitbox.api.entity.EntityColliders;
import dev.crlhitbox.api.event.EntityColliderUpdateEvent;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Minimal consumer that uses only the public CRL Hitbox API.
 *
 * <p>This fixture exists to prove the API is usable from another mod: it constructs mutable colliders,
 * publishes their immutable snapshots into the entity cache, queries them in the world frame, and
 * reacts to the explicit update entry. It imports nothing from {@code dev.crlhitbox.internal}, uses no
 * reflection, and never copies a kernel implementation.</p>
 *
 * <p>The shape values are illustrative. A real consumer keeps its own state and republishes on its own
 * events; it does not rebuild an entire model per query.</p>
 */
public final class EntityColliderExample {
    private static final ResourceLocation BODY = new ResourceLocation("example", "body");
    private static final ResourceLocation PROBE = new ResourceLocation("example", "probe");

    private static final Vec3d STANDING_CENTER = new Vec3d(0.0D, 0.9D, 0.0D);
    private static final double STANDING_LENGTH = 1.2D;
    private static final Vec3d CROUCHING_CENTER = new Vec3d(0.0D, 0.6D, 0.0D);
    private static final double CROUCHING_LENGTH = 0.6D;
    private static final double BODY_RADIUS = 0.3D;

    private final MutableCapsuleCollider body =
            new MutableCapsuleCollider(STANDING_CENTER, STANDING_LENGTH, BODY_RADIUS,
                    Rotation3d.identity());
    private final MutableRayCollider probe = new MutableRayCollider(
            new Vec3d(0.0D, 1.4D, 0.0D), new Vec3d(0.0D, 0.0D, 1.0D), 4.0D);

    /** Publishes the current shapes as immutable snapshots. */
    public void publish(Entity entity) {
        EntityColliderHolder holder = EntityColliders.require(entity);
        holder.put(BODY, body.snapshot());
        holder.put(PROBE, probe.snapshot());
    }

    /** Resizes the body and republishes it when the shape actually changed. */
    public void setCrouching(Entity entity, boolean crouching) {
        boolean changed = crouching
                ? body.setShape(CROUCHING_CENTER, CROUCHING_LENGTH, BODY_RADIUS, Rotation3d.identity())
                : body.setShape(STANDING_CENTER, STANDING_LENGTH, BODY_RADIUS, Rotation3d.identity());
        if (changed) {
            EntityColliders.require(entity).put(BODY, body.snapshot());
        }
    }

    /** Queries the cached probe against one obstacle expressed in the same world frame. */
    public boolean probeHits(Entity entity, ColliderSnapshot obstacleInWorld) {
        ColliderSnapshot local = EntityColliders.require(entity).find(PROBE).orElseThrow();
        ColliderSnapshot inWorld = EntityColliderFrames.placeInWorld(
                local, EntityColliderFrames.translationOnly(entity));
        return ColliderQueries.intersects(inWorld, obstacleInWorld);
    }

    /**
     * Explicit update entry: the consumer recomputes its own shape and publishes it.
     *
     * <p>The consumer calls {@code EntityColliders.requestUpdate(entity, reason)} itself from a real
     * logical event; this listener only reacts to that request.</p>
     */
    @SubscribeEvent
    public void onUpdate(EntityColliderUpdateEvent event) {
        setCrouching(event.getEntity(), event.getEntity().isSneaking());
    }
}
