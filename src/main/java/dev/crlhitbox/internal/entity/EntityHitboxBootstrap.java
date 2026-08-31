package dev.crlhitbox.internal.entity;

import net.minecraftforge.common.MinecraftForge;

/** Internal Phase 2A lifecycle bootstrap. */
public final class EntityHitboxBootstrap {
    private EntityHitboxBootstrap() {
    }

    /** Registers the capability before registering its Entity attachment listener. */
    public static void initialize() {
        EntityHitboxCapability.register();
        MinecraftForge.EVENT_BUS.register(new EntityHitboxAttachmentHandler());
    }
}
