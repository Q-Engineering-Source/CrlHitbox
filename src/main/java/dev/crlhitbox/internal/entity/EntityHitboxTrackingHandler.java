package dev.crlhitbox.internal.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Server-side delivery when a player starts tracking an Entity.
 *
 * <p>One full snapshot is sent directly to the entering player, including when the holder is empty:
 * the empty snapshot is meaningful because it establishes generation and revision and clears stale
 * client contents. The target may be any Entity, is never filtered by class, and is never resolved
 * through a world scan or tick loop.</p>
 */
public final class EntityHitboxTrackingHandler {
    /** Sends the entity's current full snapshot to the player that just started tracking it. */
    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP recipient)) {
            return;
        }
        EntityHitboxDelivery.sendTo(
                event.getTarget(), recipient, "PlayerEvent.StartTracking");
    }
}
