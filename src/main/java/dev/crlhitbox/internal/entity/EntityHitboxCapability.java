package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal registration state for the non-persistent entity hitbox capabilities.
 *
 * <p>Two capabilities are registered before the Entity attachment listener: the public holder and
 * the internal Phase 2B synchronization/replica state. Both storages are inert, so neither holder
 * entries nor accepted remote state are ever written to NBT.</p>
 */
public final class EntityHitboxCapability {
    @CapabilityInject(EntityHitboxHolder.class)
    private static final Capability<EntityHitboxHolder> CAPABILITY = null;

    @CapabilityInject(EntityHitboxReplicaState.class)
    private static final Capability<EntityHitboxReplicaState> REPLICA_CAPABILITY = null;

    static final Capability.IStorage<EntityHitboxHolder> STORAGE = new Capability.IStorage<>() {
        @Override
        public NBTBase writeNBT(
                Capability<EntityHitboxHolder> capability,
                EntityHitboxHolder instance,
                EnumFacing side
        ) {
            return null;
        }

        @Override
        public void readNBT(
                Capability<EntityHitboxHolder> capability,
                EntityHitboxHolder instance,
                EnumFacing side,
                NBTBase nbt
        ) {
            // Phase 2A holder state is intentionally non-persistent.
        }
    };

    static final Capability.IStorage<EntityHitboxReplicaState> REPLICA_STORAGE =
            new Capability.IStorage<>() {
                @Override
                public NBTBase writeNBT(
                        Capability<EntityHitboxReplicaState> capability,
                        EntityHitboxReplicaState instance,
                        EnumFacing side
                ) {
                    return null;
                }

                @Override
                public void readNBT(
                        Capability<EntityHitboxReplicaState> capability,
                        EntityHitboxReplicaState instance,
                        EnumFacing side,
                        NBTBase nbt
                ) {
                    // Phase 2B synchronization state is intentionally non-persistent.
                }
            };

    private EntityHitboxCapability() {
    }

    static void register() {
        CapabilityManager.INSTANCE.register(
                EntityHitboxHolder.class,
                STORAGE,
                EntityHitboxHolder::new);
        CapabilityManager.INSTANCE.register(
                EntityHitboxReplicaState.class,
                REPLICA_STORAGE,
                EntityHitboxReplicaState::create);
    }

    static Capability<EntityHitboxHolder> requireCapability() {
        if (CAPABILITY == null) {
            throw new IllegalStateException(
                    "EntityHitboxHolder capability has not been registered and injected");
        }
        return CAPABILITY;
    }

    static Capability<EntityHitboxReplicaState> requireReplicaCapability() {
        if (REPLICA_CAPABILITY == null) {
            throw new IllegalStateException(
                    "EntityHitboxReplicaState capability has not been registered and injected");
        }
        return REPLICA_CAPABILITY;
    }

    /** Internal bridge used by the public facade without exposing the Capability object. */
    public static Optional<EntityHitboxHolder> find(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(entity.getCapability(requireCapability(), null));
    }

    /** Internal bridge to the Phase 2B synchronization state of one Entity. */
    public static Optional<EntityHitboxReplicaState> findReplica(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(entity.getCapability(requireReplicaCapability(), null));
    }
}
