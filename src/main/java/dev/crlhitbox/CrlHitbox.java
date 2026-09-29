package dev.crlhitbox;

import dev.crlhitbox.internal.entity.EntityHitboxBootstrap;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import dev.crlhitbox.internal.network.SnapshotDispatchProxy;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)
public final class CrlHitbox {
    /**
     * Sided dispatch seam for inbound hitbox snapshots.
     *
     * <p>Forge injects the client implementation by class name only, so this common class never
     * holds a JVM class constant for a client-only type and the dedicated server never loads one.</p>
     */
    @SidedProxy(
            clientSide = "dev.crlhitbox.internal.client.ClientSideSnapshotDispatcher",
            serverSide = "dev.crlhitbox.internal.network.ServerSideSnapshotDispatcher")
    public static SnapshotDispatchProxy snapshotDispatcher;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityHitboxBootstrap.initialize();
    }
}
