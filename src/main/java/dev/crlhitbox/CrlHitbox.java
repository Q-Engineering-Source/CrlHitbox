package dev.crlhitbox;

import dev.crlhitbox.internal.entity.EntityHitboxBootstrap;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public final class CrlHitbox {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityHitboxBootstrap.initialize();
    }
}
