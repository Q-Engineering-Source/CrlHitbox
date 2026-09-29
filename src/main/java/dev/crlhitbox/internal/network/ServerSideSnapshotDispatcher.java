package dev.crlhitbox.internal.network;

/**
 * Dedicated-server implementation of the sided dispatch seam.
 *
 * <p>The message is registered for {@code Side.CLIENT} only, so this path is unreachable in normal
 * operation; if it is ever entered, it fails clearly instead of silently ignoring a snapshot.</p>
 */
public final class ServerSideSnapshotDispatcher implements SnapshotDispatchProxy {
    @Override
    public void scheduleClientInstall(FullSnapshotMessage message) {
        throw new IllegalStateException(
                "client full-snapshot installation is unreachable on the dedicated server");
    }
}
