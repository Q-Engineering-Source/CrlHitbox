package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * One non-persistent capability provider for one Entity.
 *
 * <p>The provider owns a fresh holder and one fresh Phase 2B synchronization/replica state. It
 * retains no Entity or World reference, and both capabilities are served by this same provider so
 * that lookups always observe one consistent owner.</p>
 */
final class EntityHitboxProvider implements ICapabilityProvider {
    private final EntityHitboxHolder holder = new EntityHitboxHolder();
    private final EntityHitboxReplicaState replicaState = EntityHitboxReplicaState.create();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == EntityHitboxCapability.requireCapability()
                || capability == EntityHitboxCapability.requireReplicaCapability();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        Capability<EntityHitboxHolder> registered = EntityHitboxCapability.requireCapability();
        if (capability == registered) {
            return registered.cast(holder);
        }
        Capability<EntityHitboxReplicaState> replica =
                EntityHitboxCapability.requireReplicaCapability();
        return capability == replica ? replica.cast(replicaState) : null;
    }
}
