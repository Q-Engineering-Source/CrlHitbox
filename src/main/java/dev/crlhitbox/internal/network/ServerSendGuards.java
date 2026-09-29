package dev.crlhitbox.internal.network;

/**
 * Server-context guards for the explicit full-snapshot send paths.
 *
 * <p>The guards take already extracted primitives rather than platform objects so that the exact
 * caller contract is directly testable: only a server-side Entity may be replicated, and the call
 * must originate on the server logical game thread. When the exact 0.6.8 API offers no safe
 * main-thread assertion, the strongest available local check is used and the caller contract is
 * documented.</p>
 */
final class ServerSendGuards {
    private ServerSendGuards() {
    }

    /** Rejects a client-side Entity. */
    static void requireServerSideEntity(boolean remoteWorld) {
        if (remoteWorld) {
            throw new IllegalStateException(
                    "full snapshot send requires a server-side Entity, not a client-side Entity");
        }
    }

    /** Rejects a missing or non-server-thread caller. */
    static void requireServerThread(boolean serverPresent, boolean callingFromServerThread) {
        if (!serverPresent) {
            throw new IllegalStateException(
                    "full snapshot send requires a running server for this Entity");
        }
        if (!callingFromServerThread) {
            throw new IllegalStateException(
                    "full snapshot send must be called from the server logical game thread");
        }
    }
}
