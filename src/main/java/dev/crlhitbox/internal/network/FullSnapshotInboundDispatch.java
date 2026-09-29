package dev.crlhitbox.internal.network;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;

/**
 * Side verification and scheduling for one inbound full snapshot.
 *
 * <p>This is the complete body of the network-thread handler: it verifies the receive side and
 * submits the message to the client dispatch seam. It performs no world lookup, Entity lookup,
 * capability access, holder mutation, or pending-store mutation, and it always returns
 * {@code null}.</p>
 */
final class FullSnapshotInboundDispatch {
    private FullSnapshotInboundDispatch() {
    }

    static IMessage dispatch(Side side, FullSnapshotMessage message, SnapshotDispatchProxy dispatcher) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(dispatcher, "dispatcher");
        if (side != Side.CLIENT) {
            throw new IllegalStateException("full snapshot received on side " + side
                    + "; only Side.CLIENT is registered");
        }
        dispatcher.scheduleClientInstall(message);
        return null;
    }
}
