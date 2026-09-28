package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic direct-binary codec for one Phase 2B full Entity hitbox snapshot.
 *
 * <p>This class is transport-free: it performs codec work only and never touches a channel,
 * handler, world, Entity, capability, holder, or pending store. It is internal protocol state, not
 * stable public API.</p>
 *
 * <p>The payload excludes the SimpleNetworkWrapper discriminator. Field order is exactly the frozen
 * contract: protocol version, dimension ID, runtime entity ID, UUID most/least bits, holder
 * generation, server revision, entry count, then the ordered entry records of identifier, local
 * solid, and local-to-parent transform.</p>
 *
 * <p>Encoding validates counts, identifiers, the primitive-leaf budget and the total size before
 * writing, so an unsendable snapshot fails before any transport call. Decoding is all-or-nothing:
 * counts are validated before allocation, a failed decode publishes no value and mutates nothing,
 * and trailing or truncated bytes are rejected.</p>
 */
final class EntityHitboxWireCodec {
    private static final int HEADER_FIXED_BYTES = 1 + Integer.BYTES
            + 2 * Long.BYTES + 2 * Long.BYTES;

    private EntityHitboxWireCodec() {
    }

    /** Returns the exact encoded size of {@code payload}, validating every frozen limit. */
    static int encodedSize(FullSnapshotPayload payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.entityId() < 0) {
            throw new ProtocolEncodeException("negative entity id: " + payload.entityId());
        }
        if (payload.holderGeneration() <= 0) {
            throw new ProtocolEncodeException(
                    "holder generation must be positive: " + payload.holderGeneration());
        }
        if (payload.serverRevision() < 0) {
            throw new ProtocolEncodeException(
                    "server revision must be nonnegative: " + payload.serverRevision());
        }
        int entryCount = payload.size();
        if (entryCount > FullSnapshotProtocol.MAX_ENTRIES) {
            throw new ProtocolEncodeException("entry count " + entryCount
                    + " exceeds MAX_ENTRIES " + FullSnapshotProtocol.MAX_ENTRIES);
        }
        int size = HEADER_FIXED_BYTES + WireVarInts.size(payload.entityId())
                + WireVarInts.size(entryCount);
        int leaves = 0;
        Set<String> seen = entryCount == 0 ? Set.of() : new HashSet<>(entryCount * 2);
        for (int index = 0; index < entryCount; index++) {
            ResourceLocation id = payload.id(index);
            if (id == null) {
                throw new ProtocolEncodeException("null entry id at index " + index);
            }
            EncodedIdentifier identifier = encodeIdentifier(id);
            if (!seen.add(identifier.canonicalKey())) {
                throw new ProtocolEncodeException("duplicate entry id: " + id);
            }
            size += WireVarInts.size(identifier.bytes().length) + identifier.bytes().length;
            PlacedSolid3d placement = payload.placement(index);
            if (placement == null) {
                throw new ProtocolEncodeException("null entry placement at index " + index);
            }
            Solid3d solid = placement.localSolid();
            int solidLeaves = SolidWireCodec.primitiveLeafCount(solid);
            if (solidLeaves < 1) {
                throw new ProtocolEncodeException("local solid at index " + index
                        + " contributes no primitive leaf");
            }
            leaves += solidLeaves;
            if (leaves > FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES) {
                throw new ProtocolEncodeException("primitive leaf budget exceeded: " + leaves
                        + " leaves against maximum "
                        + FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES);
            }
            size += SolidWireCodec.encodedSize(solid) + FullSnapshotProtocol.TRANSFORM_BYTES;
        }
        if (size > FullSnapshotProtocol.MAX_MESSAGE_BYTES) {
            throw new ProtocolEncodeException("encoded full snapshot payload of " + size
                    + " bytes exceeds MAX_MESSAGE_BYTES "
                    + FullSnapshotProtocol.MAX_MESSAGE_BYTES);
        }
        return size;
    }

    /** Writes one complete payload, failing before the first byte when limits are exceeded. */
    static void encode(ByteBuf out, FullSnapshotPayload payload) {
        Objects.requireNonNull(out, "out");
        int expectedSize = encodedSize(payload);
        int startIndex = out.writerIndex();
        out.writeByte(FullSnapshotProtocol.PROTOCOL_VERSION);
        out.writeInt(payload.dimensionId());
        WireVarInts.writeInt(out, payload.entityId());
        out.writeLong(payload.uuidMost());
        out.writeLong(payload.uuidLeast());
        out.writeLong(payload.holderGeneration());
        out.writeLong(payload.serverRevision());
        int entryCount = payload.size();
        WireVarInts.writeInt(out, entryCount);
        for (int index = 0; index < entryCount; index++) {
            EncodedIdentifier identifier = encodeIdentifier(payload.id(index));
            WireVarInts.writeInt(out, identifier.bytes().length);
            out.writeBytes(identifier.bytes());
            PlacedSolid3d placement = payload.placement(index);
            SolidWireCodec.write(out, placement.localSolid());
            WireIo.writeTransform(out, placement.localToParent());
        }
        int written = out.writerIndex() - startIndex;
        if (written != expectedSize) {
            throw new ProtocolEncodeException("internal codec size mismatch: wrote " + written
                    + " byte(s), expected " + expectedSize);
        }
    }

    /** Decodes exactly one complete payload and requires exact buffer exhaustion. */
    static FullSnapshotPayload decode(ByteBuf in) {
        Objects.requireNonNull(in, "in");
        int payloadSize = in.readableBytes();
        if (payloadSize > FullSnapshotProtocol.MAX_MESSAGE_BYTES) {
            throw new ProtocolDecodeException("full snapshot payload of " + payloadSize
                    + " bytes exceeds MAX_MESSAGE_BYTES "
                    + FullSnapshotProtocol.MAX_MESSAGE_BYTES);
        }
        try {
            int version = WireIo.readUnsignedByte(in, "protocol version");
            if (version != FullSnapshotProtocol.PROTOCOL_VERSION) {
                throw new ProtocolDecodeException("unsupported protocol version " + version
                        + ", expected " + FullSnapshotProtocol.PROTOCOL_VERSION);
            }
            int dimensionId = WireIo.readInt(in, "dimension id");
            int entityId = WireVarInts.readInt(in, "entity id");
            long uuidMost = WireIo.readLong(in, "entity uuid most");
            long uuidLeast = WireIo.readLong(in, "entity uuid least");
            long holderGeneration = WireIo.readLong(in, "holder generation");
            if (holderGeneration <= 0) {
                throw new ProtocolDecodeException(
                        "holder generation must be positive: " + holderGeneration);
            }
            long serverRevision = WireIo.readLong(in, "server revision");
            if (serverRevision < 0) {
                throw new ProtocolDecodeException(
                        "server revision must be nonnegative: " + serverRevision);
            }
            int entryCount = WireVarInts.readInt(in, "entry count");
            if (entryCount > FullSnapshotProtocol.MAX_ENTRIES) {
                throw new ProtocolDecodeException("entry count " + entryCount
                        + " exceeds MAX_ENTRIES " + FullSnapshotProtocol.MAX_ENTRIES);
            }
            ResourceLocation[] ids = new ResourceLocation[entryCount];
            PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
            Set<ResourceLocation> seen = entryCount == 0 ? Set.of() : new HashSet<>(entryCount * 2);
            SolidWireCodec.LeafBudget budget = new SolidWireCodec.LeafBudget(
                    FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES);
            for (int index = 0; index < entryCount; index++) {
                ResourceLocation id = readIdentifier(in);
                if (!seen.add(id)) {
                    throw new ProtocolDecodeException("duplicate entry id: " + id);
                }
                Solid3d solid = SolidWireCodec.read(in, budget);
                RigidTransform3d transform = WireIo.readTransform(in, "entry " + index + " transform");
                ids[index] = id;
                placements[index] = new PlacedSolid3d(solid, transform);
            }
            if (in.isReadable()) {
                throw new ProtocolDecodeException("trailing bytes after full snapshot: "
                        + in.readableBytes() + " byte(s) at reader index " + in.readerIndex());
            }
            return FullSnapshotPayload.decoded(
                    dimensionId, entityId, uuidMost, uuidLeast, holderGeneration, serverRevision,
                    ids, placements);
        } catch (ProtocolDecodeException failure) {
            throw failure;
        } catch (IllegalArgumentException | IndexOutOfBoundsException failure) {
            throw new ProtocolDecodeException(
                    "malformed full snapshot payload: " + failure, failure);
        }
    }

    private static ResourceLocation readIdentifier(ByteBuf in) {
        int length = WireVarInts.readInt(in, "resource location length");
        if (length < 1) {
            throw new ProtocolDecodeException(
                    "resource location byte length must be at least 1: " + length);
        }
        if (length > FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES) {
            throw new ProtocolDecodeException("resource location byte length " + length
                    + " exceeds MAX_RESOURCE_LOCATION_BYTES "
                    + FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES);
        }
        WireIo.requireReadable(in, length, "resource location bytes");
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String text = decodeStrictUtf8(bytes);
        ResourceLocation identifier;
        try {
            identifier = new ResourceLocation(text);
        } catch (RuntimeException failure) {
            throw new ProtocolDecodeException(
                    "invalid resource location text: " + failure.getMessage(), failure);
        }
        int canonicalLength = identifier.toString().getBytes(StandardCharsets.UTF_8).length;
        if (canonicalLength > FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES) {
            throw new ProtocolDecodeException("resource location " + identifier
                    + " canonicalizes to " + canonicalLength
                    + " bytes, exceeding MAX_RESOURCE_LOCATION_BYTES "
                    + FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES);
        }
        return identifier;
    }

    private static String decodeStrictUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException failure) {
            throw new ProtocolDecodeException("malformed UTF-8 resource location bytes", failure);
        }
    }

    /**
     * Encodes one identifier and derives the key the receiving decoder will observe.
     *
     * <p>The key is the identifier text that the receiver reconstructs by passing the transmitted
     * bytes back through the 1.12.2 {@code ResourceLocation} constructor. Using that value — rather
     * than the sender's own {@code toString()} — makes encode-side duplicate detection and the
     * frozen byte limit exactly as strict as the receiving decoder, so a server can never transmit a
     * snapshot that every receiver is guaranteed to reject.</p>
     */
    private static EncodedIdentifier encodeIdentifier(ResourceLocation id) {
        String text = id.toString();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 1) {
            throw new ProtocolEncodeException("empty resource location encoding for: " + text);
        }
        if (bytes.length > FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES) {
            throw new ProtocolEncodeException("resource location " + text + " encodes to "
                    + bytes.length + " bytes, exceeding MAX_RESOURCE_LOCATION_BYTES "
                    + FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES);
        }
        // The key is derived from the bytes that are actually written, never from the in-memory
        // text: UTF-8 encoding can rewrite text (for example an unpaired surrogate becomes '?'),
        // and only the written bytes are what the receiver reconstructs.
        String wireText = new String(bytes, StandardCharsets.UTF_8);
        String canonicalKey;
        try {
            canonicalKey = new ResourceLocation(wireText).toString();
        } catch (RuntimeException failure) {
            throw new ProtocolEncodeException(
                    "invalid resource location text " + wireText + ": " + failure.getMessage());
        }
        int canonicalLength = canonicalKey.getBytes(StandardCharsets.UTF_8).length;
        if (canonicalLength > FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES) {
            throw new ProtocolEncodeException("resource location " + text + " canonicalizes to "
                    + canonicalLength + " bytes, exceeding MAX_RESOURCE_LOCATION_BYTES "
                    + FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES);
        }
        return new EncodedIdentifier(bytes, canonicalKey);
    }

    /** Wire bytes plus the identifier the receiver will reconstruct from them. */
    private record EncodedIdentifier(byte[] bytes, String canonicalKey) {
    }
}
