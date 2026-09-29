package dev.crlhitbox.internal.network;

import dev.crlhitbox.CrlHitbox;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Network-thread handler of the single Phase 2B full-snapshot message.
 *
 * <p>The handler instance is registered instead of the handler class, which keeps this type
 * package-private. All work happens inside the scheduled client task reached through the dispatch
 * seam; this class itself only verifies the side and schedules.</p>
 */
final class FullSnapshotHandler implements IMessageHandler<FullSnapshotMessage, IMessage> {
    @Override
    public IMessage onMessage(FullSnapshotMessage message, MessageContext context) {
        return FullSnapshotInboundDispatch.dispatch(
                context.side, message, CrlHitbox.snapshotDispatcher);
    }
}
