package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Composite;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.bytes;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.encode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.id;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.payloadOf;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.placed;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.reader;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structurally independent reference encoder/decoder for the frozen wire contract.
 *
 * <p>This oracle restates the field order, the stable shape tags, the varint form, and the frozen
 * numeric limits as literals. It never calls the production encode/decode helpers or reads the
 * production protocol constants, and it counts entries and primitive leaves by itself.</p>
 */
class EntityHitboxWireCodecOraclePhase2BTest {
    @Test
    void frozenProtocolConstantsMatchDocumentedContract() {
        assertAll(
                () -> assertEquals(1, FullSnapshotProtocol.PROTOCOL_VERSION, "protocol version"),
                () -> assertEquals(0, FullSnapshotProtocol.FULL_SNAPSHOT_DISCRIMINATOR, "discriminator"),
                () -> assertEquals(1_048_576, FullSnapshotProtocol.MAX_MESSAGE_BYTES, "message bytes"),
                () -> assertEquals(4_096, FullSnapshotProtocol.MAX_ENTRIES, "entries"),
                () -> assertEquals(1_024, FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES,
                        "identifier bytes"),
                () -> assertEquals(4_096, FullSnapshotProtocol.MAX_COMPOSITE_LEAVES,
                        "composite leaves"),
                () -> assertEquals(16_384, FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES,
                        "total primitive leaves"),
                () -> assertEquals(0, FullSnapshotProtocol.TAG_AABB, "Aabb tag"),
                () -> assertEquals(1, FullSnapshotProtocol.TAG_SPHERE, "Sphere tag"),
                () -> assertEquals(2, FullSnapshotProtocol.TAG_OBB, "Obb tag"),
                () -> assertEquals(3, FullSnapshotProtocol.TAG_CAPSULE, "Capsule tag"),
                () -> assertEquals(4, FullSnapshotProtocol.TAG_COMPOSITE, "Composite tag"));
    }

    @Test
    void emptySnapshotBytesMatchIndependentOracle() {
        FullSnapshotPayload payload = payloadOf(0L, new ResourceLocation[0], new PlacedSolid3d[0]);
        assertArrayEquals(oracleBytes(payload), encode(payload), "empty snapshot bytes");
    }

    @Test
    void everyShapeFixtureBytesMatchIndependentOracle() {
        for (Solid3d solid : List.of(
                new Aabb(new Vec3d(-1.0D, -2.0D, -3.0D), new Vec3d(4.0D, 5.0D, 6.0D)),
                new Sphere(new Vec3d(0.25D, 0.5D, -0.75D), 0.0D),
                new Obb(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(0.5D, 1.0D, 1.5D),
                        new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D)),
                new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D),
                        new Vec3d(1.0D, 2.0D, 3.0D)), 0.75D),
                new Composite(List.of(
                        new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                        new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D),
                                new Vec3d(1.0D, 1.0D, 1.0D)), 0.25D))))) {
            PlacedSolid3d placement = new PlacedSolid3d(solid,
                    new RigidTransform3d(new Rotation3d(0.0D, 0.0D, 0.25D, 1.0D),
                            new Vec3d(1.0D, -2.0D, 3.0D)));
            FullSnapshotPayload payload = payloadOf(1L, new ResourceLocation[] {id("shape")},
                    new PlacedSolid3d[] {placement});
            assertArrayEquals(oracleBytes(payload), encode(payload), "bytes for " + solid);
        }
    }

    @Test
    void multiEntryFixtureBytesMatchIndependentOracle() {
        List<ResourceLocation> ids = FullSnapshotPhase2BTestSupport.ids("alpha", "beta", "gamma");
        List<PlacedSolid3d> placements = List.of(
                placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D)),
                placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 3.0D))),
                placed(new Composite(List.of(
                        new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                        new Sphere(new Vec3d(1.0D, 1.0D, 1.0D), 2.0D)))));
        FullSnapshotPayload payload = payloadOf(3L, ids.toArray(ResourceLocation[]::new),
                placements.toArray(PlacedSolid3d[]::new));

        assertArrayEquals(oracleBytes(payload), encode(payload), "multi-entry bytes");
    }

    @Test
    void independentLeafAndSizeAccountingMatchesProduction() {
        List<PlacedSolid3d> placements = List.of(
                placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D))),
                placed(new Composite(List.of(
                        new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                        new Sphere(new Vec3d(1.0D, 0.0D, 0.0D), 1.0D),
                        new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 1.0D)))));
        ResourceLocation[] ids = {id("flat"), id("union")};
        FullSnapshotPayload payload = payloadOf(2L, ids,
                placements.toArray(PlacedSolid3d[]::new));

        assertAll(
                () -> assertEquals(4, oracleLeafCount(payload), "independent leaf count"),
                () -> assertEquals(oracleBytes(payload).length, EntityHitboxWireCodec.encodedSize(payload),
                        "independent size accounting"),
                () -> assertEquals(4, SolidWireCodec.primitiveLeafCount(
                        payload.placement(1).localSolid()) + 1, "production leaf count"));
    }

    @Test
    void independentDecodeOfProductionBytesMatchesDecodedValues() {
        Rotation3d rotation = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        RigidTransform3d transform = new RigidTransform3d(rotation, new Vec3d(4.0D, 5.0D, 6.0D));
        FullSnapshotPayload payload = payloadOf(2L,
                new ResourceLocation[] {id("first"), id("second")},
                new PlacedSolid3d[] {
                        placed(new Aabb(new Vec3d(-1.0D, 0.0D, 1.0D), new Vec3d(2.0D, 3.0D, 4.0D)),
                                transform),
                        placed(new Composite(List.of(
                                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                                new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D),
                                        new Vec3d(0.0D, 1.0D, 0.0D)), 0.5D))),
                                RigidTransform3d.identity())});
        byte[] encoded = encode(payload);
        OracleFrame oracle = oracleDecode(encoded);
        FullSnapshotPayload decoded = FullSnapshotPhase2BTestSupport.decode(encoded);

        assertAll(
                () -> assertEquals(1, oracle.protocolVersion(), "version"),
                () -> assertEquals(0, oracle.dimensionId(), "dimension"),
                () -> assertEquals(FullSnapshotPhase2BTestSupport.ENTITY_ID, oracle.entityId(),
                        "entity id"),
                () -> assertEquals(FullSnapshotPhase2BTestSupport.UUID_MOST, oracle.uuidMost(), "uuid most"),
                () -> assertEquals(FullSnapshotPhase2BTestSupport.UUID_LEAST, oracle.uuidLeast(),
                        "uuid least"),
                () -> assertEquals(FullSnapshotPhase2BTestSupport.GENERATION, oracle.generation(),
                        "generation"),
                () -> assertEquals(2L, oracle.revision(), "revision"),
                () -> assertEquals(2, oracle.ids().size(), "entry count"),
                () -> assertEquals("crlhitbox:first", oracle.ids().get(0), "first id text"),
                () -> assertEquals("crlhitbox:second", oracle.ids().get(1), "second id text"),
                () -> assertEquals(0, oracle.solidTags().get(0), "first solid tag"),
                () -> assertEquals(4, oracle.solidTags().get(1), "second solid tag"),
                () -> assertEquals(2, oracle.compositeLeafTags().get(1).size(),
                        "composite leaf count"),
                () -> assertEquals(decoded.size(), oracle.ids().size(), "decoded size"),
                () -> assertEquals(decoded.id(0), new ResourceLocation(oracle.ids().get(0)),
                        "decoded first id"),
                () -> assertEquals(decoded.id(1), new ResourceLocation(oracle.ids().get(1)),
                        "decoded second id"),
                () -> assertEquals(rotation, decoded.placement(0).localToParent().rotation(),
                        "decoded rotation bits"),
                () -> assertEquals(transform, decoded.placement(0).localToParent(), "decoded transform"),
                () -> assertEquals(Aabb.class, decoded.placement(0).localSolid().getClass(),
                        "decoded first solid type"),
                () -> assertInstanceOf(Composite.class, decoded.placement(1).localSolid()));
    }

    @Test
    void independentVarIntEncodingMatchesProductionBoundaries() {
        int[] values = {0, 1, 127, 128, 255, 256, 4_095, 4_096, 1_048_575, 1_048_576,
                Integer.MAX_VALUE};
        for (int value : values) {
            byte[] oracle = oracleVarInt(value);
            ByteBuf buffer = Unpooled.buffer();
            try {
                WireVarInts.writeInt(buffer, value);
                assertAll(
                        () -> assertArrayEquals(oracle, bytes(buffer),
                                "varint bytes for " + value),
                        () -> assertEquals(oracle.length, WireVarInts.size(value),
                                "varint size for " + value));
            } finally {
                buffer.release();
            }
            ByteBuf readBuffer = Unpooled.wrappedBuffer(oracle);
            try {
                assertEquals(value, WireVarInts.readInt(readBuffer, "oracle value"),
                        "varint round trip for " + value);
            } finally {
                readBuffer.release();
            }
        }
    }

    @Test
    void oracleRejectsNothingThatProductionAcceptsForRepresentativeFixtures() {
        List<PlacedSolid3d> placements = List.of(
                placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.0D)),
                placed(new Obb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(0.0D, 1.0D, 2.0D),
                        new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D))),
                placed(new Capsule(new Segment3d(new Vec3d(1.0D, 1.0D, 1.0D),
                        new Vec3d(1.0D, 1.0D, 1.0D)), 0.0D)));
        ResourceLocation[] ids = {id("zero_sphere"), id("flat_obb"), id("point_capsule")};
        FullSnapshotPayload payload = payloadOf(3L, ids, placements.toArray(PlacedSolid3d[]::new));

        assertArrayEquals(oracleBytes(payload), encode(payload), "degenerate fixtures");
        assertTrue(oracleLeafCount(payload) == 3, "independent leaf count for primitive entries");
    }

    private static byte[] oracleBytes(FullSnapshotPayload payload) {
        ByteBuf out = Unpooled.buffer();
        try {
            out.writeByte(1);
            out.writeInt(payload.dimensionId());
            oracleVarInt(out, payload.entityId());
            out.writeLong(payload.uuidMost());
            out.writeLong(payload.uuidLeast());
            out.writeLong(payload.holderGeneration());
            out.writeLong(payload.serverRevision());
            oracleVarInt(out, payload.size());
            for (int index = 0; index < payload.size(); index++) {
                byte[] text = payload.id(index).toString().getBytes(StandardCharsets.UTF_8);
                oracleVarInt(out, text.length);
                out.writeBytes(text);
                oracleSolid(out, payload.placement(index).localSolid());
                RigidTransform3d transform = payload.placement(index).localToParent();
                Rotation3d rotation = transform.rotation();
                out.writeDouble(rotation.x());
                out.writeDouble(rotation.y());
                out.writeDouble(rotation.z());
                out.writeDouble(rotation.w());
                Vec3d translation = transform.translation();
                out.writeDouble(translation.x());
                out.writeDouble(translation.y());
                out.writeDouble(translation.z());
            }
            return bytes(out);
        } finally {
            out.release();
        }
    }

    private static void oracleSolid(ByteBuf out, Solid3d solid) {
        if (solid instanceof Aabb aabb) {
            out.writeByte(0);
            oracleVector(out, aabb.min());
            oracleVector(out, aabb.max());
            return;
        }
        if (solid instanceof Sphere sphere) {
            out.writeByte(1);
            oracleVector(out, sphere.center());
            out.writeDouble(sphere.radius());
            return;
        }
        if (solid instanceof Obb obb) {
            out.writeByte(2);
            oracleVector(out, obb.center());
            oracleVector(out, obb.halfExtents());
            Rotation3d orientation = obb.orientation();
            out.writeDouble(orientation.x());
            out.writeDouble(orientation.y());
            out.writeDouble(orientation.z());
            out.writeDouble(orientation.w());
            return;
        }
        if (solid instanceof Capsule capsule) {
            out.writeByte(3);
            oracleVector(out, capsule.centerline().start());
            oracleVector(out, capsule.centerline().end());
            out.writeDouble(capsule.radius());
            return;
        }
        if (solid instanceof Composite composite) {
            out.writeByte(4);
            oracleVarInt(out, composite.childCount());
            for (int index = 0; index < composite.childCount(); index++) {
                oracleSolid(out, composite.child(index));
            }
            return;
        }
        throw new AssertionError("unsupported solid in oracle: " + solid);
    }

    private static void oracleVector(ByteBuf out, Vec3d vector) {
        out.writeDouble(vector.x());
        out.writeDouble(vector.y());
        out.writeDouble(vector.z());
    }

    private static int oracleLeafCount(FullSnapshotPayload payload) {
        int leaves = 0;
        for (int index = 0; index < payload.size(); index++) {
            leaves += payload.placement(index).localSolid() instanceof Composite composite
                    ? composite.childCount() : 1;
        }
        return leaves;
    }

    private static byte[] oracleVarInt(int value) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            oracleVarInt(buffer, value);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void oracleVarInt(ByteBuf out, int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    private static OracleFrame oracleDecode(byte[] encoded) {
        ByteBuf in = reader(encoded);
        try {
            int version = in.readUnsignedByte();
            int dimensionId = in.readInt();
            int entityId = readOracleVarInt(in);
            long uuidMost = in.readLong();
            long uuidLeast = in.readLong();
            long generation = in.readLong();
            long revision = in.readLong();
            int entryCount = readOracleVarInt(in);
            List<String> ids = new ArrayList<>(entryCount);
            List<Integer> solidTags = new ArrayList<>(entryCount);
            List<List<Integer>> compositeLeafTags = new ArrayList<>(entryCount);
            for (int index = 0; index < entryCount; index++) {
                int idLength = readOracleVarInt(in);
                byte[] text = new byte[idLength];
                in.readBytes(text);
                ids.add(new String(text, StandardCharsets.UTF_8));
                compositeLeafTags.add(new ArrayList<>());
                solidTags.add(oracleSkipSolid(in, compositeLeafTags.get(index)));
                for (int component = 0; component < 7; component++) {
                    in.readDouble();
                }
            }
            assertEquals(0, in.readableBytes(), "oracle must consume the whole payload");
            return new OracleFrame(version, dimensionId, entityId, uuidMost, uuidLeast, generation,
                    revision, ids, solidTags, compositeLeafTags);
        } finally {
            in.release();
        }
    }

    private static int oracleSkipSolid(ByteBuf in, List<Integer> compositeLeafTags) {
        int tag = in.readUnsignedByte();
        switch (tag) {
            case 0 -> skipDoubles(in, 6);
            case 1 -> skipDoubles(in, 4);
            case 2 -> skipDoubles(in, 10);
            case 3 -> skipDoubles(in, 7);
            case 4 -> {
                int leaves = readOracleVarInt(in);
                for (int index = 0; index < leaves; index++) {
                    compositeLeafTags.add(oracleSkipSolid(in, new ArrayList<>()));
                }
            }
            default -> throw new AssertionError("oracle saw unknown tag " + tag);
        }
        return tag;
    }

    private static void skipDoubles(ByteBuf in, int count) {
        for (int index = 0; index < count; index++) {
            in.readDouble();
        }
    }

    private static int readOracleVarInt(ByteBuf in) {
        int result = 0;
        for (int index = 0; index < 5; index++) {
            int value = in.readUnsignedByte();
            result |= (value & 0x7F) << (index * 7);
            if ((value & 0x80) == 0) {
                return result;
            }
        }
        throw new AssertionError("oracle saw an overlong varint");
    }

    private record OracleFrame(
            int protocolVersion,
            int dimensionId,
            int entityId,
            long uuidMost,
            long uuidLeast,
            long generation,
            long revision,
            List<String> ids,
            List<Integer> solidTags,
            List<List<Integer>> compositeLeafTags
    ) {
    }
}
