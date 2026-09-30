package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityColliderHolder;
import dev.crlhitbox.api.entity.EntityHitboxHolder;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

/**
 * One non-persistent capability provider for one Entity.
 *
 * <p>The provider owns a fresh Phase 2A holder, one fresh Phase 2B synchronization/replica state, and
 * one fresh generic collider cache. It retains no Entity or World reference, and all capabilities are
 * served by this same provider so that lookups always observe one consistent owner.</p>
 */
final class EntityHitboxProvider implements ICapabilityProvider {
    private final EntityHitboxHolder holder = new EntityHitboxHolder();
    private final EntityHitboxReplicaState replicaState = EntityHitboxReplicaState.create();
    private final EntityColliderHolder colliderHolder = new EntityColliderHolder();

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == EntityHitboxCapability.requireCapability()
                || capability == EntityHitboxCapability.requireReplicaCapability()
                || capability == EntityColliderCapability.requireCapability();
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        Capability<EntityHitboxHolder> registered = EntityHitboxCapability.requireCapability();
        if (capability == registered) {
            return registered.cast(holder);
        }
        Capability<EntityHitboxReplicaState> replica =
                EntityHitboxCapability.requireReplicaCapability();
        if (capability == replica) {
            return replica.cast(replicaState);
        }
        Capability<EntityColliderHolder> colliders = EntityColliderCapability.requireCapability();
        return capability == colliders ? colliders.cast(colliderHolder) : null;
    }
}
