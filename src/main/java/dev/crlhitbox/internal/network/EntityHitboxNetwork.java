package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.entity.EntityHitboxSnapshot;
import dev.crlhitbox.api.entity.EntityHitboxes;
import dev.crlhitbox.internal.entity.EntityHitboxCapability;
import dev.crlhitbox.internal.entity.EntityHitboxReplicaState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;
import java.util.UUID;

/**
 * The single Phase 2B network channel and its server-authoritative send paths.
 *
 * <p>The channel is named {@code crlhitbox} and registers exactly one message: discriminator
 * {@code 0}, direction {@code Side.CLIENT}. No client-to-server message, handshake, acknowledgement,
 * or resync request is registered, and no send path targets the server.</p>
 *
 * <p>Every send captures one immutable snapshot plus the provider generation once, validates the
 * frozen protocol limits before transport, and never mutates the holder or its revision.</p>
 */
public final class EntityHitboxNetwork {
    /** Frozen SimpleNetworkWrapper channel name. */
    static final String CHANNEL_NAME = "crlhitbox";

    private static SimpleNetworkWrapper channel;

    private EntityHitboxNetwork() {
    }

    /** Registers the channel and its only message. Must run after capability registration. */
    public static synchronized void initialize() {
        if (channel != null) {
            throw new IllegalStateException("the crlhitbox network channel is already registered");
        }
        SimpleNetworkWrapper created = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        created.registerMessage(
                new FullSnapshotHandler(),
                FullSnapshotMessage.class,
                FullSnapshotProtocol.FULL_SNAPSHOT_DISCRIMINATOR,
                Side.CLIENT);
        channel = created;
    }

    /** Sends one full snapshot to exactly one recipient. */
    public static void sendFullSnapshot(Entity entity, EntityPlayerMP recipient) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(recipient, "recipient");
        SimpleNetworkWrapper registered = requireChannel();
        FullSnapshotMessage message = capture(entity);
        registered.sendTo(message, recipient);
    }

    /** Sends one full snapshot to the current tracking players and, for a player Entity, itself. */
    public static void sendFullSnapshotToTrackingAndSelf(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        SimpleNetworkWrapper registered = requireChannel();
        FullSnapshotMessage message = capture(entity);
        registered.sendToAllTracking(message, entity);
        if (entity instanceof EntityPlayerMP player) {
            registered.sendTo(message, player);
        }
    }

    /**
     * Captures one immutable, validated message for {@code entity}.
     *
     * <p>The capture is intentionally shared by both send paths so a tracking broadcast and a
     * player self-send reuse one generation, one revision, and one contents snapshot.</p>
     */
    static FullSnapshotMessage capture(Entity entity) {
        MinecraftServer server = entity.getServer();
        ServerSendGuards.requireServerSideEntity(entity.world == null || entity.world.isRemote);
        ServerSendGuards.requireServerThread(server != null,
                server != null && server.isCallingFromMinecraftThread());
        EntityHitboxHolder holder = EntityHitboxes.find(entity).orElseThrow(
                () -> new IllegalStateException(
                        "Entity lacks the CRL Hitbox entity_hitboxes capability"));
        EntityHitboxReplicaState replicaState = EntityHitboxCapability.findReplica(entity).orElseThrow(
                () -> new IllegalStateException(
                        "Entity lacks the CRL Hitbox synchronization state capability"));
        EntityHitboxSnapshot snapshot = holder.snapshot();
        UUID uuid = entity.getUniqueID();
        FullSnapshotPayload payload = FullSnapshotPayload.fromSnapshot(
                entity.dimension,
                entity.getEntityId(),
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits(),
                replicaState.localGeneration(),
                snapshot);
        // Validate every frozen limit (and the exact encoded size) before any transport call.
        try {
            EntityHitboxWireCodec.encodedSize(payload);
        } catch (RuntimeException failure) {
            throw new UnsendableFullSnapshotException(payload, failure);
        }
        return FullSnapshotMessage.of(payload);
    }

    private static SimpleNetworkWrapper requireChannel() {
        SimpleNetworkWrapper registered = channel;
        if (registered == null) {
            throw new IllegalStateException("the crlhitbox network channel is not registered");
        }
        return registered;
    }
}
