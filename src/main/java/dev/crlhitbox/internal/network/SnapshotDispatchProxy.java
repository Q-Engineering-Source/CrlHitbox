package dev.crlhitbox.internal.network;

/**
 * Common-side seam that hands one decoded full snapshot to the client logical game thread.
 *
 * <p>The network handler runs on the network thread and must not touch world, Entity, capability,
 * holder, or pending state, so it only calls this seam. The concrete client implementation is
 * injected by Forge sided injection as a class <em>name</em>, which keeps the common handler free of
 * any {@code CONSTANT_Class} reference to a client-only class.</p>
 *
 * <p>This is internal platform plumbing, not stable public API.</p>
 */
public interface SnapshotDispatchProxy {
    /**
     * Submits one task that runs the client-side installation on the client logical game thread.
     *
     * @param message the fully decoded message; never {@code null}
     */
    void scheduleClientInstall(FullSnapshotMessage message);

    /**
     * Returns whether the caller is already on the client logical game thread.
     *
     * <p>The dedicated-server implementation returns {@code false}, because reaching it means the
     * client seam was never injected and a client-side cache update must not proceed.</p>
     */
    boolean isOnClientThread();
}
