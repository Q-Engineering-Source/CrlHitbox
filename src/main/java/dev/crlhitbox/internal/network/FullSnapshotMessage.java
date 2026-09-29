package dev.crlhitbox.internal.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Technical Forge-instantiation type for the single Phase 2B full-snapshot message.
 *
 * <p>The exact 0.6.8 indexed codec constructs message classes through a public no-argument
 * constructor, so this type has public JVM visibility. It is explicitly <em>not</em> stable public
 * API, is never exposed from {@code dev.crlhitbox.api.entity}, and carries no public mutable field.
 * Its transport methods delegate to the frozen wire codec and mutate nothing else.</p>
 *
 * <p>Decoding is all-or-nothing: {@link #fromBytes(ByteBuf)} assigns the captured payload only
 * after the complete payload has been validated and the buffer has been fully consumed, so an
 * incompletely decoded message can never reach a handler.</p>
 */
public final class FullSnapshotMessage implements IMessage {
    private FullSnapshotPayload payload;

    /** Required by the exact Forge 0.6.8 indexed codec; not stable public API. */
    public FullSnapshotMessage() {
    }

    private FullSnapshotMessage(FullSnapshotPayload payload) {
        this.payload = payload;
    }

    /** Captures one already validated payload for transmission. */
    static FullSnapshotMessage of(FullSnapshotPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload");
        }
        return new FullSnapshotMessage(payload);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        FullSnapshotPayload decoded = EntityHitboxWireCodec.decode(buffer);
        this.payload = decoded;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        if (payload == null) {
            throw new IllegalStateException(
                    "full snapshot message has no payload; decode or capture it first");
        }
        EntityHitboxWireCodec.encode(buffer, payload);
    }

    /** Returns the decoded or captured payload; only reachable inside this package. */
    FullSnapshotPayload payload() {
        return payload;
    }
}
