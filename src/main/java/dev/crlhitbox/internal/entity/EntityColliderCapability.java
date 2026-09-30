package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityColliderHolder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Internal registration state for the non-persistent generic entity collider cache.
 *
 * <p>This capability is separate from the Phase 2A {@code EntityHitboxHolder} capability so that the
 * existing holder, its capability key and its semantics stay untouched. Both are served by the same
 * provider, and the storage is inert: no collider is ever written to NBT.</p>
 */
public final class EntityColliderCapability {
    @CapabilityInject(EntityColliderHolder.class)
    private static final Capability<EntityColliderHolder> CAPABILITY = null;

    static final Capability.IStorage<EntityColliderHolder> STORAGE = new Capability.IStorage<>() {
        @Override
        public NBTBase writeNBT(
                Capability<EntityColliderHolder> capability,
                EntityColliderHolder instance,
                EnumFacing side
        ) {
            return null;
        }

        @Override
        public void readNBT(
                Capability<EntityColliderHolder> capability,
                EntityColliderHolder instance,
                EnumFacing side,
                NBTBase nbt
        ) {
            // The generic collider cache is intentionally non-persistent.
        }
    };

    private EntityColliderCapability() {
    }

    static void register() {
        CapabilityManager.INSTANCE.register(
                EntityColliderHolder.class,
                STORAGE,
                EntityColliderHolder::new);
    }

    static Capability<EntityColliderHolder> requireCapability() {
        if (CAPABILITY == null) {
            throw new IllegalStateException(
                    "EntityColliderHolder capability has not been registered and injected");
        }
        return CAPABILITY;
    }

    /** Internal bridge used by the public facade without exposing the Capability object. */
    public static Optional<EntityColliderHolder> find(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(entity.getCapability(requireCapability(), null));
    }
}
