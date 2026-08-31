package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/** One non-persistent capability provider and fresh holder for one Entity. */
final class EntityHitboxProvider implements ICapabilityProvider {
    private final EntityHitboxHolder holder = new EntityHitboxHolder();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == EntityHitboxCapability.requireCapability();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        Capability<EntityHitboxHolder> registered = EntityHitboxCapability.requireCapability();
        return capability == registered ? registered.cast(holder) : null;
    }
}
