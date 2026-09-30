package dev.crlhitbox.internal.client;

import dev.crlhitbox.internal.network.ClientSnapshotInstallation;
import dev.crlhitbox.internal.network.FullSnapshotMessage;
import dev.crlhitbox.internal.network.PendingFullSnapshots;
import dev.crlhitbox.internal.network.SnapshotDispatchProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Client-only implementation of the common sided dispatch seam.
 *
 * <p>This is the only production entry that touches client classes for hitbox synchronization. Forge
 * injects it by class name, so no common or server class holds a JVM class constant for it and the
 * dedicated server never loads it. It registers itself on the Forge event bus during instantiation,
 * which keeps every client lifecycle hook inside this client-only package.</p>
 *
 * <p>The network handler only schedules; all world lookup, capability access, ordering checks, and
 * holder replacement happen in the scheduled client-thread task. Messages whose target is not yet
 * available enter the bounded pending store, which is retried on client tick end and cleared on
 * connection start, disconnection, and the unload of a targeted world.</p>
 */
public final class ClientSideSnapshotDispatcher implements SnapshotDispatchProxy {
    private final PendingFullSnapshots pending = new PendingFullSnapshots();
    private int clientTicks;
    private volatile boolean sessionResetRequested;

    /** Registers this client-only listener set on the Forge event bus. */
    public ClientSideSnapshotDispatcher() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ColliderDebugRenderHandler());
    }

    @Override
    public void scheduleClientInstall(FullSnapshotMessage message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> installOnClientThread(message, minecraft));
    }

    @Override
    public boolean isOnClientThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    /** Retries pending messages for the current world, then expires stale entries. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        clientTicks++;
        if (sessionResetRequested) {
            // Connection events are posted from the network thread, so the store is only mutated
            // here, on the client logical game thread.
            sessionResetRequested = false;
            pending.clear();
            clientTicks = 0;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        World world = minecraft.world;
        if (world != null) {
            int dimension = world.provider.getDimension();
            for (PendingFullSnapshots.Key key : pending.keysForDimension(dimension)) {
                FullSnapshotMessage message = pending.peek(key);
                if (message == null) {
                    continue;
                }
                ClientSnapshotInstallation.Result result =
                        ClientSnapshotInstallation.install(message, world);
                if (result.isInstalled() || result.isRejected()) {
                    pending.remove(key);
                }
            }
        }
        pending.expire(clientTicks);
    }

    /** Requests session-state cleanup before a new server session is accepted. */
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        sessionResetRequested = true;
    }

    /** Requests session-state cleanup after the connection ends. */
    @SubscribeEvent
    public void onClientDisconnectedFromServer(
            FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        sessionResetRequested = true;
    }

    /** Drops pending messages that target an unloading client world. */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null && world.isRemote) {
            pending.removeDimension(world.provider.getDimension());
        }
    }

    private void installOnClientThread(FullSnapshotMessage message, Minecraft minecraft) {
        ClientSnapshotInstallation.Result result =
                ClientSnapshotInstallation.install(message, minecraft.world);
        if (result.isPending()) {
            pending.offer(message, clientTicks);
        }
    }
}
