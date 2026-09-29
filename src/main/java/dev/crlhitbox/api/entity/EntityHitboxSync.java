package dev.crlhitbox.api.entity;

import dev.crlhitbox.internal.network.EntityHitboxNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Explicit server-side full-snapshot resend for entity hitbox holders.
 *
 * <p>Server-authoritative replication of this module is explicit: mutating a holder never sends a
 * packet, so an already-tracked Entity requires one of these calls. Both methods are server-only and
 * server-thread-confined, capture one immutable snapshot per call, and never mutate the holder or
 * its revision. They surface an actionable exception before transport when the captured snapshot
 * cannot be encoded inside the frozen protocol limits.</p>
 *
 * <p>Tracking entry is automatic: when a player starts tracking an Entity, the server sends that
 * player a full snapshot. There is no client-to-server path, no delta packet, and no resync request
 * in this phase.</p>
 */
public final class EntityHitboxSync {
    private EntityHitboxSync() {
    }

    /** Sends the current full snapshot to one recipient, tracked or not. */
    public static void sendFullTo(Entity entity, EntityPlayerMP recipient) {
        EntityHitboxNetwork.sendFullSnapshot(entity, recipient);
    }

    /** Sends one captured full snapshot to all current tracking players and the player Entity itself. */
    public static void sendFullToTrackingAndSelf(Entity entity) {
        EntityHitboxNetwork.sendFullSnapshotToTrackingAndSelf(entity);
    }
}
