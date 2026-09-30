package dev.crlhitbox.internal.entity;

import dev.crlhitbox.CrlHitbox;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.Objects;

/**
 * Logical-thread requirement for collider update entries.
 *
 * <p>An update mutates a cache, so it must run on the logical game thread that owns that cache: the
 * server thread for a server-side entity and the client thread for a client-side entity. The server
 * case is checked directly; the client case goes through the sided dispatch seam, so no client class
 * becomes reachable from common code.</p>
 */
public final class EntityColliderThreads {
    private EntityColliderThreads() {
    }

    /**
     * Fails unless the caller is on the owning logical game thread.
     *
     * @throws IllegalStateException when the entity has no world, or the caller is on another thread
     */
    public static void requireLogicalThread(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        World world = entity.world;
        if (world == null) {
            throw new IllegalStateException("entity has no world, so no logical thread can own it");
        }
        if (!world.isRemote) {
            MinecraftServer server = entity.getServer();
            if (server == null || !server.isCallingFromMinecraftThread()) {
                throw new IllegalStateException(
                        "a collider update must run on the server thread");
            }
            return;
        }
        if (!CrlHitbox.snapshotDispatcher.isOnClientThread()) {
            throw new IllegalStateException("a collider update must run on the client thread");
        }
    }
}
