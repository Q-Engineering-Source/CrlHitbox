package dev.crlhitbox.internal.client;

import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.entity.EntityColliderSnapshot;
import dev.crlhitbox.api.entity.EntityColliderFrames;
import dev.crlhitbox.api.entity.EntityColliders;
import dev.crlhitbox.api.entity.EntityHitboxSnapshot;
import dev.crlhitbox.api.entity.EntityHitboxes;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only F3+B debug drawing for CRL Hitbox collider caches.
 *
 * <p>The handler reads the vanilla debug bounding-box toggle and exits immediately when it is off, so
 * no complex geometry work happens while the overlay is disabled. It merges the legacy Phase 2A solid
 * cache with the generic collider cache — the generic entry wins on a shared {@code ResourceLocation}
 * so nothing is drawn twice — places each snapshot with the display-only interpolated frame, and hands
 * the result to the renderer.</p>
 *
 * <p>Nothing here triggers an update event, recomputes a shape, persists data, or contacts the
 * network; drawing is strictly a read of the current caches.</p>
 */
public final class ColliderDebugRenderHandler {
    /** Draws the visible collider outlines when the vanilla green debug overlay is enabled. */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderManager renderManager = minecraft.getRenderManager();
        if (!renderManager.isDebugBoundingBox()) {
            return;
        }
        WorldClient world = minecraft.world;
        if (world == null) {
            return;
        }
        float partialTicks = event.getPartialTicks();
        Vec3d camera = new Vec3d(
                renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        List<ColliderSnapshot> visible = new ArrayList<>();
        for (Entity entity : world.loadedEntityList) {
            List<ColliderSnapshot> entityLocal = collectVisibleSnapshots(entity);
            if (entityLocal.isEmpty()) {
                continue;
            }
            RigidTransform3d frame =
                    EntityColliderRenderFrames.interpolatedTranslation(entity, partialTicks);
            for (ColliderSnapshot snapshot : entityLocal) {
                visible.add(EntityColliderFrames.placeInWorld(snapshot, frame));
            }
        }
        ColliderDebugRenderer.render(visible, camera);
    }

    /**
     * Merges the legacy solid cache and the generic collider cache for one entity.
     *
     * <p>Legacy {@code PlacedSolid3d} entries become temporary solid snapshots; a generic entry with
     * the same ID replaces it. The two caches are never copied into one another.</p>
     */
    static List<ColliderSnapshot> collectVisibleSnapshots(Entity entity) {
        Map<ResourceLocation, ColliderSnapshot> merged = new LinkedHashMap<>();
        EntityHitboxes.find(entity).ifPresent(legacy -> {
            EntityHitboxSnapshot snapshot = legacy.snapshot();
            for (int index = 0; index < snapshot.size(); index++) {
                ResourceLocation id = snapshot.id(index);
                PlacedSolid3d placement = snapshot.placement(index);
                merged.put(id, new SolidColliderSnapshot(
                        placement.localSolid(), placement.localToParent(), true));
            }
        });
        EntityColliders.find(entity).ifPresent(colliders -> {
            EntityColliderSnapshot snapshot = colliders.snapshot();
            for (int index = 0; index < snapshot.size(); index++) {
                merged.put(snapshot.id(index), snapshot.collider(index));
            }
        });
        return new ArrayList<>(merged.values());
    }
}
