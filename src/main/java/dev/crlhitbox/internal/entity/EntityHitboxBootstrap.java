package dev.crlhitbox.internal.entity;

import dev.crlhitbox.internal.network.EntityHitboxNetwork;
import net.minecraftforge.common.MinecraftForge;

/** Internal lifecycle bootstrap for the capability, attachment listener, and network channel. */
public final class EntityHitboxBootstrap {
    private EntityHitboxBootstrap() {
    }

    /**
     * Registers both capabilities, then the Entity attachment listener, then the event handlers and
     * the network channel.
     *
     * <p>Every supported capability is registered before the attachment listener so that no provider
     * can be asked for a capability that is not yet injectable.</p>
     *
     * <p>All Forge and FML events used here are posted on {@code MinecraftForge.EVENT_BUS}: in the
     * exact 0.6.8 build, {@code FMLCommonHandler} initializes its {@code eventBus} field from
     * {@code MinecraftForge.EVENT_BUS}, so {@code FMLCommonHandler.instance().bus()} and
     * {@code MinecraftForge.EVENT_BUS} are the same instance and each listener is registered exactly
     * once.</p>
     */
    public static void initialize() {
        EntityHitboxCapability.register();
        MinecraftForge.EVENT_BUS.register(new EntityHitboxAttachmentHandler());
        MinecraftForge.EVENT_BUS.register(new EntityHitboxTrackingHandler());
        MinecraftForge.EVENT_BUS.register(new EntityHitboxPlayerLifecycleHandler());
        EntityHitboxNetwork.initialize();
    }
}
