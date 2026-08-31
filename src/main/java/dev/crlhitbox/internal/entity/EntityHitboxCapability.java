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

/** Internal registration state for the non-persistent entity hitbox capability. */
public final class EntityHitboxCapability {
    @CapabilityInject(EntityHitboxHolder.class)
    private static final Capability<EntityHitboxHolder> CAPABILITY = null;

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

    private EntityHitboxCapability() {
    }

    static void register() {
        CapabilityManager.INSTANCE.register(
                EntityHitboxHolder.class,
                STORAGE,
                EntityHitboxHolder::new);
    }

    static Capability<EntityHitboxHolder> requireCapability() {
        if (CAPABILITY == null) {
            throw new IllegalStateException(
                    "EntityHitboxHolder capability has not been registered and injected");
        }
        return CAPABILITY;
    }

    /** Internal bridge used by the public facade without exposing the Capability object. */
    public static Optional<EntityHitboxHolder> find(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return Optional.ofNullable(entity.getCapability(requireCapability(), null));
    }
}
