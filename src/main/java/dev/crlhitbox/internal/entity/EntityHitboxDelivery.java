package dev.crlhitbox.internal.entity;

import dev.crlhitbox.internal.network.EntityHitboxNetwork;
import dev.crlhitbox.internal.network.UnsendableFullSnapshotException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

/**
 * Shared, fail-safe delivery of one automatic full snapshot.
 *
 * <p>Automatic tracking and lifecycle handlers must never crash the event bus or a server tick, so a
 * local encoding failure is caught, reported once with the entity identity, generation, revision,
 * entry count and reason, and nothing is transmitted. Ordinary successful sends are never logged.</p>
 */
final class EntityHitboxDelivery {
    private static final String FAILURE_PREFIX = "CRL Hitbox full snapshot delivery failed";

    private EntityHitboxDelivery() {
    }

    /**
     * Sends one full snapshot to {@code recipient}, swallowing and reporting local failures.
     *
     * @return whether the snapshot was handed to the network layer
     */
    static boolean sendTo(Entity entity, EntityPlayerMP recipient, String trigger) {
        try {
            EntityHitboxNetwork.sendFullSnapshot(entity, recipient);
            return true;
        } catch (UnsendableFullSnapshotException failure) {
            logger().error("{} during {}: entityId={}, uuid={}, generation={}, revision={}, "
                            + "entryCount={}, reason={}",
                    FAILURE_PREFIX, trigger, failure.entityId(), failure.entityUuid(),
                    failure.holderGeneration(), failure.serverRevision(), failure.entryCount(),
                    failure.getMessage());
            return false;
        } catch (RuntimeException failure) {
            logger().error("{} during {}: entityId={}, reason={}",
                    FAILURE_PREFIX, trigger, entity.getEntityId(), failure.toString());
            return false;
        }
    }

    private static Logger logger() {
        return FMLCommonHandler.instance().getFMLLogger();
    }
}
