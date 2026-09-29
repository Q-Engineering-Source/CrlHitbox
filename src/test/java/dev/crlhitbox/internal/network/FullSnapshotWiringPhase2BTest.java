package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.ENTITY_ID;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.GENERATION;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.UUID_LEAST;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.UUID_MOST;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.bytes;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.encode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.id;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.payloadOf;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.placed;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.reader;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transport wiring behavior: the technical message type, the network-thread dispatch seam, the
 * server send guards, and the client installation entry.
 */
class FullSnapshotWiringPhase2BTest {
    @Test
    void messageRoundTripsThroughTransportMethods() {
        FullSnapshotPayload payload = samplePayload();
        FullSnapshotMessage message = FullSnapshotMessage.of(payload);
        byte[] encoded = transportBytes(message);

        FullSnapshotMessage decoded = new FullSnapshotMessage();
        ByteBuf buffer = reader(encoded);
        try {
            decoded.fromBytes(buffer);
            assertEquals(0, buffer.readableBytes(), "the decoder must consume the whole payload");
        } finally {
            buffer.release();
        }

        assertNotNull(decoded.payload(), "decoded payload");
        assertEquals(payload.size(), decoded.payload().size());
        for (int index = 0; index < payload.size(); index++) {
            assertEquals(payload.id(index), decoded.payload().id(index));
            assertEquals(payload.placement(index), decoded.payload().placement(index));
        }
        assertEquals(payload.holderGeneration(), decoded.payload().holderGeneration());
        assertEquals(payload.serverRevision(), decoded.payload().serverRevision());
        assertEquals(ENTITY_ID, decoded.payload().entityId());
        assertEquals(UUID_MOST, decoded.payload().uuidMost());
        assertEquals(UUID_LEAST, decoded.payload().uuidLeast());
        assertEquals(GENERATION, decoded.payload().holderGeneration());
        assertArrayEquals(encoded, encode(decoded.payload()),
                "re-encoding the decoded payload must reproduce the transport bytes");
    }

    @Test
    void transportEncodingMatchesTheProductionCodecBytes() {
        FullSnapshotPayload payload = samplePayload();
        byte[] expected = encode(payload);

        assertArrayEquals(expected, transportBytes(FullSnapshotMessage.of(payload)));
    }

    private static byte[] transportBytes(FullSnapshotMessage message) {
        FullSnapshotPayload payload = message.payload();
        assertNotNull(payload, "transport encoding requires a payload");
        int size = EntityHitboxWireCodec.encodedSize(payload);
        ByteBuf buffer = Unpooled.buffer(size, size);
        try {
            message.toBytes(buffer);
            assertEquals(size, buffer.readableBytes(),
                    "the transport encoding must match the validated size");
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    @Test
    void messageWithoutPayloadCannotBeEncoded() {
        FullSnapshotMessage message = new FullSnapshotMessage();
        assertNull(message.payload(), "a freshly constructed Forge instance has no payload");

        ByteBuf buffer = Unpooled.buffer();
        try {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> message.toBytes(buffer));
            assertTrue(failure.getMessage().contains("no payload"), failure.getMessage());
            assertEquals(0, buffer.writerIndex(), "nothing may be written for an empty message");
        } finally {
            buffer.release();
        }
    }

    @Test
    void failedDecodeLeavesTheMessageWithoutPayload() {
        FullSnapshotMessage message = new FullSnapshotMessage();
        byte[] malformed = {1, 0, 0, 0, 0, 0x7F};
        ByteBuf buffer = reader(malformed);
        try {
            assertThrows(ProtocolDecodeException.class, () -> message.fromBytes(buffer));
        } finally {
            buffer.release();
        }

        assertNull(message.payload(),
                "an incompletely decoded message must never expose a payload");
    }

    @Test
    void dispatchVerifiesSideSchedulesOnceAndReturnsNull() {
        FullSnapshotMessage message = FullSnapshotMessage.of(samplePayload());
        RecordingDispatcher dispatcher = new RecordingDispatcher();

        Object reply = FullSnapshotInboundDispatch.dispatch(Side.CLIENT, message, dispatcher);

        assertNull(reply, "the handler must return null");
        assertEquals(1, dispatcher.messages.size(), "exactly one scheduled task");
        assertEquals(message, dispatcher.messages.get(0), "the decoded message is forwarded as is");
    }

    @Test
    void dispatchRejectsATransportThatRunsOnTheWrongSide() {
        FullSnapshotMessage message = FullSnapshotMessage.of(samplePayload());
        RecordingDispatcher dispatcher = new RecordingDispatcher();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> FullSnapshotInboundDispatch.dispatch(Side.SERVER, message, dispatcher));

        assertTrue(failure.getMessage().contains("only Side.CLIENT is registered"),
                failure.getMessage());
        assertTrue(dispatcher.messages.isEmpty(), "a wrongly sided message must not be scheduled");
    }

    @Test
    void dispatchRequiresBothMessageAndDispatcher() {
        RecordingDispatcher dispatcher = new RecordingDispatcher();
        assertThrows(NullPointerException.class,
                () -> FullSnapshotInboundDispatch.dispatch(Side.CLIENT, null, dispatcher));
        assertThrows(NullPointerException.class,
                () -> FullSnapshotInboundDispatch.dispatch(Side.CLIENT,
                        FullSnapshotMessage.of(samplePayload()), null));
    }

    @Test
    void serverSendGuardsRejectClientSideEntitiesAndNonServerThreadCallers() {
        assertThrows(IllegalStateException.class, () -> ServerSendGuards.requireServerSideEntity(true));
        assertThrows(IllegalStateException.class, () -> ServerSendGuards.requireServerThread(false, true));
        assertThrows(IllegalStateException.class, () -> ServerSendGuards.requireServerThread(true, false));

        ServerSendGuards.requireServerSideEntity(false);
        ServerSendGuards.requireServerThread(true, true);
    }

    @Test
    void installationWithoutAClientWorldIsPending() {
        FullSnapshotMessage message = FullSnapshotMessage.of(samplePayload());

        assertEquals(ClientSnapshotInstallation.Result.PENDING_NO_WORLD,
                ClientSnapshotInstallation.install(message, null));
        assertTrue(ClientSnapshotInstallation.Result.PENDING_NO_WORLD.isPending());
        assertFalse(ClientSnapshotInstallation.Result.PENDING_NO_WORLD.isInstalled());
        assertFalse(ClientSnapshotInstallation.Result.PENDING_NO_WORLD.isRejected());
    }

    @Test
    void installationRequiresADecodedPayload() {
        assertThrows(IllegalStateException.class,
                () -> ClientSnapshotInstallation.install(new FullSnapshotMessage(), null));
    }

    @Test
    void installationRequiresTheMessage() {
        assertThrows(NullPointerException.class,
                () -> ClientSnapshotInstallation.install(null, null));
    }

    @Test
    void installationResultsClassifyInstallationPendingAndRejection() {
        assertTrue(ClientSnapshotInstallation.Result.INSTALLED_CHANGED.isInstalled());
        assertTrue(ClientSnapshotInstallation.Result.INSTALLED_UNCHANGED.isInstalled());
        assertTrue(ClientSnapshotInstallation.Result.PENDING_OTHER_DIMENSION.isPending());
        assertTrue(ClientSnapshotInstallation.Result.PENDING_NO_ENTITY.isPending());
        assertTrue(ClientSnapshotInstallation.Result.REJECTED_IDENTITY_MISMATCH.isRejected());
        assertTrue(ClientSnapshotInstallation.Result.REJECTED_MISSING_CAPABILITY.isRejected());
        assertTrue(ClientSnapshotInstallation.Result.REJECTED_STALE_GENERATION.isRejected());
        assertTrue(ClientSnapshotInstallation.Result.REJECTED_STALE_REVISION.isRejected());

        for (ClientSnapshotInstallation.Result result : ClientSnapshotInstallation.Result.values()) {
            assertEquals(1, (result.isInstalled() ? 1 : 0) + (result.isPending() ? 1 : 0)
                    + (result.isRejected() ? 1 : 0), "exactly one classification for " + result);
        }
    }

    @Test
    void dedicatedServerDispatcherFailsClearlyIfEverInvoked() {
        ServerSideSnapshotDispatcher dispatcher = new ServerSideSnapshotDispatcher();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> dispatcher.scheduleClientInstall(FullSnapshotMessage.of(samplePayload())));
        assertTrue(failure.getMessage().contains("unreachable on the dedicated server"),
                failure.getMessage());
    }

    private static FullSnapshotPayload samplePayload() {
        ResourceLocation[] ids = {id("head"), id("body")};
        PlacedSolid3d[] placements = {
                placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.5D, 0.5D, 0.5D))),
                placed(new Sphere(new Vec3d(1.0D, 1.0D, 1.0D), 0.25D))};
        return payloadOf(2L, ids, placements);
    }

    /** Test transport seam that records scheduled messages instead of touching a client. */
    private static final class RecordingDispatcher implements SnapshotDispatchProxy {
        private final List<FullSnapshotMessage> messages = new ArrayList<>();

        @Override
        public void scheduleClientInstall(FullSnapshotMessage message) {
            messages.add(message);
        }
    }
}
