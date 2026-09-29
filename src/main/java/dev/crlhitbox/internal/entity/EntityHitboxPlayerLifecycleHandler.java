package dev.crlhitbox.internal.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Server-side self snapshots for player login, respawn, and dimension change.
 *
 * <p>Only the player's own Entity is targeted, and no holder state is copied: a respawn creates a
 * fresh provider with an empty revision-zero holder, which is exactly what the self snapshot then
 * asserts. Duplicate same-generation/same-revision snapshots are safe, so repeated events need no
 * suppression. No logout packet and no clone-copy handler exists.</p>
 */
public final class EntityHitboxPlayerLifecycleHandler {
    /** Sends the joining player's own full snapshot. */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendSelfSnapshot(event.player, "PlayerLoggedInEvent");
    }

    /** Sends the respawning player's own full snapshot. */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendSelfSnapshot(event.player, "PlayerRespawnEvent");
    }

    /** Sends the player's own full snapshot after a dimension change. */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendSelfSnapshot(event.player, "PlayerChangedDimensionEvent");
    }

    private static void sendSelfSnapshot(EntityPlayer player, String trigger) {
        if (!(player instanceof EntityPlayerMP serverPlayer)) {
            return;
        }
        EntityHitboxDelivery.sendTo(serverPlayer, serverPlayer, trigger);
    }
}
