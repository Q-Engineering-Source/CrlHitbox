package dev.crlhitbox.internal.client;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.entity.Entity;

/**
 * Display-only interpolated translation for one entity.
 *
 * <p>This exists purely for smooth drawing: it interpolates the previous and current position by the
 * render partial tick and uses world-parallel axes. It never writes to the collider cache, never
 * affects the logical pose, and reads no yaw, pitch or model state.</p>
 */
final class EntityColliderRenderFrames {
    private EntityColliderRenderFrames() {
    }

    /** Returns the interpolated position point of {@code entity} as a translation-only frame. */
    static RigidTransform3d interpolatedTranslation(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        return new RigidTransform3d(Rotation3d.identity(), new Vec3d(x, y, z));
    }
}
