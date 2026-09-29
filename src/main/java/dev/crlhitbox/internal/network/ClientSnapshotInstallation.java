package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.entity.EntityHitboxes;
import dev.crlhitbox.internal.entity.EntityHitboxCapability;
import dev.crlhitbox.internal.entity.EntityHitboxReplicaState;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Common client-side installation of one decoded full snapshot.
 *
 * <p>This runs only on the client logical game thread and only for a client world. It performs the
 * complete target resolution contract: dimension equality, runtime-ID lookup, exact UUID match, both
 * CRL Hitbox capabilities, the frozen generation/revision acceptance rule, atomic holder
 * replacement, and replica-state update strictly after a successful replacement.</p>
 *
 * <p>A missing world, a different dimension, or a not-yet-available Entity is reported as a pending
 * outcome so the caller can retain the message in the bounded pending store. Identity mismatches and
 * missing capabilities are definitive rejections and are never queued for the mismatched Entity.
 * Nothing here creates Entities, attaches capabilities, or scans loaded Entities by UUID.</p>
 */
public final class ClientSnapshotInstallation {
    /** Outcome of one installation attempt. */
    public enum Result {
        INSTALLED_CHANGED(true),
        INSTALLED_UNCHANGED(true),
        PENDING_NO_WORLD(false),
        PENDING_OTHER_DIMENSION(false),
        PENDING_NO_ENTITY(false),
        REJECTED_IDENTITY_MISMATCH(false),
        REJECTED_MISSING_CAPABILITY(false),
        REJECTED_STALE_GENERATION(false),
        REJECTED_STALE_REVISION(false);

        private final boolean installed;

        Result(boolean installed) {
            this.installed = installed;
        }

        /** Returns whether the holder was replaced or authoritatively reasserted. */
        public boolean isInstalled() {
            return installed;
        }

        /** Returns whether the message should be retained for a later attempt. */
        public boolean isPending() {
            return this == PENDING_NO_WORLD || this == PENDING_OTHER_DIMENSION
                    || this == PENDING_NO_ENTITY;
        }

        /** Returns whether the message is a definitive rejection. */
        public boolean isRejected() {
            return !installed && !isPending();
        }
    }

    private ClientSnapshotInstallation() {
    }

    /**
     * Attempts to install one decoded snapshot into the matching client Entity's holder.
     *
     * @param message the fully decoded inbound message
     * @param world the current client world, or {@code null} when no world is loaded
     */
    public static Result install(FullSnapshotMessage message, World world) {
        Objects.requireNonNull(message, "message");
        FullSnapshotPayload payload = message.payload();
        if (payload == null) {
            throw new IllegalStateException("inbound full snapshot message has no decoded payload");
        }
        if (world == null) {
            return Result.PENDING_NO_WORLD;
        }
        if (!world.isRemote) {
            throw new IllegalStateException(
                    "client snapshot installation must not run against a server-side world");
        }
        if (world.provider.getDimension() != payload.dimensionId()) {
            return Result.PENDING_OTHER_DIMENSION;
        }
        Entity entity = world.getEntityByID(payload.entityId());
        if (entity == null) {
            return Result.PENDING_NO_ENTITY;
        }
        UUID uuid = entity.getUniqueID();
        if (uuid.getMostSignificantBits() != payload.uuidMost()
                || uuid.getLeastSignificantBits() != payload.uuidLeast()) {
            return Result.REJECTED_IDENTITY_MISMATCH;
        }
        Optional<EntityHitboxHolder> holder = EntityHitboxes.find(entity);
        Optional<EntityHitboxReplicaState> replicaState = EntityHitboxCapability.findReplica(entity);
        if (holder.isEmpty() || replicaState.isEmpty()) {
            logger().error("CRL Hitbox: entity {} (uuid {}) is missing an expected CRL Hitbox"
                            + " capability (dimension {}, generation {}, revision {}); full snapshot"
                            + " rejected without attaching or constructing capability state",
                    payload.entityId(), new UUID(payload.uuidMost(), payload.uuidLeast()),
                    payload.dimensionId(), payload.holderGeneration(), payload.serverRevision());
            return Result.REJECTED_MISSING_CAPABILITY;
        }
        EntityHitboxReplicaState state = replicaState.get();
        FullSnapshotOrdering ordering = state.classify(
                payload.holderGeneration(), payload.serverRevision());
        if (!ordering.isAccepted()) {
            return ordering == FullSnapshotOrdering.STALE_GENERATION
                    ? Result.REJECTED_STALE_GENERATION
                    : Result.REJECTED_STALE_REVISION;
        }
        EntityHitboxHolder target = holder.get();
        boolean changed = target.replaceContents(payload.toSnapshot());
        state.recordAcceptedInstall(
                payload.holderGeneration(), payload.serverRevision(), target.revision());
        return changed ? Result.INSTALLED_CHANGED : Result.INSTALLED_UNCHANGED;
    }

    private static Logger logger() {
        return FMLCommonHandler.instance().getFMLLogger();
    }
}
