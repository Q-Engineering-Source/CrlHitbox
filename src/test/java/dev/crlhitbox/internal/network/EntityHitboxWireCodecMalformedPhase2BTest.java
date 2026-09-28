package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.bytes;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.encode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.id;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.payloadOf;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.placed;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.reader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fail-closed behavior of the Phase 2B full-snapshot decoder and the encoder's pre-transport
 * limits. Malformed payloads are assembled by hand so that the decoder is exercised independently
 * of the production encoder.
 */
class EntityHitboxWireCodecMalformedPhase2BTest {
    @Test
    void emptyPayloadRejected() {
        assertFailureContains(new byte[0], "protocol version");
    }

    @Test
    void unsupportedProtocolVersionRejected() {
        byte[] encoded = validBytes();
        encoded[0] = 2;
        assertFailureContains(encoded, "unsupported protocol version 2");
    }

    @Test
    void zeroProtocolVersionRejected() {
        byte[] encoded = validBytes();
        encoded[0] = 0;
        assertFailureContains(encoded, "unsupported protocol version 0");
    }

    @Test
    void truncatedAtEveryHeaderBoundaryRejected() {
        byte[] encoded = validBytes();
        for (int length = 0; length < entryStartOffset(); length++) {
            byte[] truncated = Arrays.copyOf(encoded, length);
            assertThrows(ProtocolDecodeException.class,
                    () -> decodeUnchecked(truncated),
                    "truncation at " + length + " byte(s) must fail closed");
        }
    }

    @Test
    void truncatedInsideEntryIdentifierRejected() {
        byte[] encoded = validBytes();
        assertFailureContains(Arrays.copyOf(encoded, entryStartOffset() + 1),
                "resource location");
    }

    @Test
    void truncatedInsideSolidPayloadRejected() {
        byte[] encoded = validBytes();
        assertThrows(ProtocolDecodeException.class,
                () -> decodeUnchecked(Arrays.copyOf(encoded, encoded.length - 57)),
                "a truncated solid payload must fail closed");
    }

    @Test
    void truncatedTransformTranslationRejected() {
        byte[] encoded = validBytes();
        assertThrows(ProtocolDecodeException.class,
                () -> decodeUnchecked(Arrays.copyOf(encoded, encoded.length - 1)),
                "a truncated translation component must fail closed");
    }

    @Test
    void negativeEntityIdVarintRejected() {
        byte[] encoded = manualHeader(1L, 0L, raw(0xFF, 0xFF, 0xFF, 0xFF, 0x0F), 0);
        assertFailureContains(encoded, "entity id");
    }

    @Test
    void varintExceedingThirtyTwoBitsRejected() {
        byte[] encoded = manualHeader(1L, 0L, raw(0x80, 0x80, 0x80, 0x80, 0x10), 0);
        assertFailureContains(encoded, "exceeds 32 bits");
    }

    @Test
    void overlongEntityIdVarintRejected() {
        byte[] encoded = manualHeader(1L, 0L,
                raw(0x80, 0x80, 0x80, 0x80, 0x80, 0x01), 0);
        assertFailureContains(encoded, "exceeds 5 encoded bytes");
    }

    @Test
    void nonShortestVarIntEncodingRejected() {
        byte[] encoded = manualHeader(1L, 0L, raw(0x80, 0x00), 0);
        assertFailureContains(encoded, "not the shortest encoding");
    }

    @Test
    void nonShortestEntryCountEncodingRejected() {
        byte[] encoded = manualHeader(1L, 0L, varInt(0), raw(0x80, 0x00));
        assertFailureContains(encoded, "not the shortest encoding");
    }

    @Test
    void generationZeroRejected() {
        assertFailureContains(manualHeader(0L, 0L, varInt(0), 0), "holder generation");
    }

    @Test
    void generationNegativeRejected() {
        assertFailureContains(manualHeader(-1L, 0L, varInt(0), 0), "holder generation");
    }

    @Test
    void serverRevisionNegativeRejected() {
        assertFailureContains(manualHeader(1L, -1L, varInt(0), 0), "server revision");
    }

    @Test
    void negativeEntryCountRejected() {
        byte[] encoded = manualHeader(1L, 0L, varInt(0), raw(0xFF, 0xFF, 0xFF, 0xFF, 0x0F));
        assertFailureContains(encoded, "entry count");
    }

    @Test
    void entryCountAboveLimitRejected() {
        byte[] encoded = manualHeader(1L, 0L, varInt(0),
                varInt(FullSnapshotProtocol.MAX_ENTRIES + 1));
        assertFailureContains(encoded, "exceeds MAX_ENTRIES");
    }

    @Test
    void resourceLocationLengthZeroRejected() {
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 1), varInt(0));
        assertFailureContains(encoded, "at least 1");
    }

    @Test
    void resourceLocationLengthAboveLimitRejected() {
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 1),
                varInt(FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES + 1));
        assertFailureContains(encoded, "exceeds MAX_RESOURCE_LOCATION_BYTES");
    }

    @Test
    void malformedUtf8IdentifierRejected() {
        byte[] text = {(byte) 0xC3, (byte) 0x28};
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 1), varInt(text.length), text);
        assertFailureContains(encoded, "malformed UTF-8");
    }

    @Test
    void truncatedIdentifierBytesRejected() {
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 1), varInt(64));
        assertFailureContains(encoded, "truncated resource location bytes");
    }

    @Test
    void duplicateEntryIdsRejected() {
        byte[] first = entry("crlhitbox:dup", EntityHitboxWireCodecMalformedPhase2BTest::writeAabbEntry);
        byte[] second = entry("crlhitbox:dup", EntityHitboxWireCodecMalformedPhase2BTest::writeAabbEntry);
        byte[] encoded = concat(manualHeader(1L, 2L, varInt(0), 2), first, second);
        assertFailureContains(encoded, "duplicate entry id");
    }

    @Test
    void unknownSolidTagRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:tag", buffer -> buffer.writeByte(7)));
        assertFailureContains(encoded, "unknown solid tag: 7");
    }

    @Test
    void unknownCompositeLeafTagRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:composite_tag", buffer -> {
                    buffer.writeByte(4);
                    writeVarIntLiteral(buffer, 1);
                    buffer.writeByte(9);
                }));
        assertFailureContains(encoded, "unknown solid tag: 9");
    }

    @Test
    void nestedCompositeTagRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:nested", buffer -> {
                    buffer.writeByte(4);
                    writeVarIntLiteral(buffer, 1);
                    buffer.writeByte(4);
                }));
        assertFailureContains(encoded, "nested Composite tag");
    }

    @Test
    void zeroLeafCompositeRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:empty_union", buffer -> {
                    buffer.writeByte(4);
                    writeVarIntLiteral(buffer, 0);
                }));
        assertFailureContains(encoded, "zero-leaf Composite");
    }

    @Test
    void compositeLeafCountAboveLimitRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:many", buffer -> {
                    buffer.writeByte(4);
                    writeVarIntLiteral(buffer, FullSnapshotProtocol.MAX_COMPOSITE_LEAVES + 1);
                }));
        assertFailureContains(encoded, "exceeds MAX_COMPOSITE_LEAVES");
    }

    @Test
    void totalPrimitiveLeafBudgetExceededRejected() {
        assertFailureContains(overBudgetBytes(), "primitive leaf budget exceeded");
    }

    @Test
    void nonFiniteDoublesRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:nan", buffer -> {
                    buffer.writeByte(0);
                    buffer.writeDouble(Double.NaN);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(1.0D);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid Aabb min");
    }

    @Test
    void infiniteDoublesRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:infinite", buffer -> {
                    buffer.writeByte(0);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(Double.POSITIVE_INFINITY);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(1.0D);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid Aabb max");
    }

    @Test
    void negativeSphereRadiusRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:negative_radius", buffer -> {
                    buffer.writeByte(1);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(-1.0D);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid solid payload for tag 1");
    }

    @Test
    void negativeObbHalfExtentRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:negative_extent", buffer -> {
                    buffer.writeByte(2);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(-0.5D);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(1.0D);
                    writeIdentityRotation(buffer);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid solid payload for tag 2");
    }

    @Test
    void invalidAabbOrderingRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:inverted", buffer -> {
                    buffer.writeByte(0);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(1.0D);
                    buffer.writeDouble(1.0D);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid solid payload for tag 0");
    }

    @Test
    void unrepresentableMandatoryBoundsRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:overflow", buffer -> {
                    buffer.writeByte(1);
                    buffer.writeDouble(1.0E308D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(1.0E308D);
                    writeIdentityTransform(buffer);
                }));
        assertFailureContains(encoded, "invalid solid payload for tag 1");
    }

    @Test
    void nonCanonicalRotationRejected() {
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1),
                entry("crlhitbox:bad_rotation", buffer -> {
                    buffer.writeByte(FullSnapshotProtocol.TAG_AABB);
                    for (int component = 0; component < 6; component++) {
                        buffer.writeDouble(component < 3 ? 0.0D : 1.0D);
                    }
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                    buffer.writeDouble(0.0D);
                }));
        assertFailureContains(encoded, "rotation");
    }

    @Test
    void trailingBytesRejected() {
        byte[] encoded = concat(validBytes(), new byte[] {0});
        assertFailureContains(encoded, "trailing bytes");
    }

    @Test
    void emptySnapshotWithTrailingBytesRejected() {
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 0), new byte[] {0x7F});
        assertFailureContains(encoded, "trailing bytes");
    }

    @Test
    void messageByteLimitExceededRejected() {
        byte[] oversized = new byte[FullSnapshotProtocol.MAX_MESSAGE_BYTES + 1];
        assertFailureContains(oversized, "exceeds MAX_MESSAGE_BYTES");
    }

    @Test
    void failureMessagesNameTheOffendingField() {
        byte[] encoded = concat(manualHeader(1L, 0L, varInt(0), 3), varInt(1));
        ProtocolDecodeException failure = assertThrows(ProtocolDecodeException.class,
                () -> decodeUnchecked(encoded));
        assertTrue(failure.getMessage().contains("resource location"),
                "actionable field name expected, got: " + failure.getMessage());
    }

    @Test
    void failedDecodeDoesNotProduceAnyValueOrMutateExistingState() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        holder.put(id("kept"), placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D)));
        byte[] intact = encode(payloadOf(1L, new ResourceLocation[] {id("kept")},
                new PlacedSolid3d[] {placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D))}));
        byte[] malformed = Arrays.copyOf(intact, intact.length - 3);

        assertThrows(ProtocolDecodeException.class, () -> decodeUnchecked(malformed));
        assertEquals(1, holder.size(), "failed decode must not touch unrelated holder state");
        assertEquals(1L, holder.revision(), "holder revision unchanged");
        assertEquals(intact.length - 3, malformed.length, "fixture remained truncated");
    }

    @Test
    void encoderRejectsOversizedIdentifier() {
        StringBuilder path = new StringBuilder();
        path.append("k".repeat(FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES));
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, 1L,
                new ResourceLocation[] {new ResourceLocation("crlhitbox", path.toString())},
                new PlacedSolid3d[] {placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D))});

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("MAX_RESOURCE_LOCATION_BYTES"));
    }

    @Test
    void encoderRejectsNonPositiveGenerationAndNegativeRevision() {
        PlacedSolid3d placement = placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        ResourceLocation[] ids = {id("x")};
        PlacedSolid3d[] placements = {placement};
        FullSnapshotPayload zeroGeneration = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 0L, 0L, ids, placements);
        FullSnapshotPayload negativeGeneration = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, -5L, 0L, ids, placements);
        FullSnapshotPayload negativeRevision = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, -1L, ids, placements);

        assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(zeroGeneration));
        assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(negativeGeneration));
        assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(negativeRevision));
    }

    @Test
    void encoderRejectsNegativeEntityId() {
        PlacedSolid3d placement = placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, -1, 0L, 0L, 1L, 1L,
                new ResourceLocation[] {id("x")}, new PlacedSolid3d[] {placement});

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("negative entity id"));
    }

    @Test
    void encoderRejectsEntryCountAboveLimit() {
        int entryCount = FullSnapshotProtocol.MAX_ENTRIES + 1;
        ResourceLocation[] ids = new ResourceLocation[entryCount];
        PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
        PlacedSolid3d shared = placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(1.0D, 1.0D, 1.0D)));
        for (int index = 0; index < entryCount; index++) {
            ids[index] = id("overflow_" + index);
            placements[index] = shared;
        }
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, entryCount, ids, placements);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("MAX_ENTRIES"));
    }

    @Test
    void encoderRejectsDuplicateEntryIds() {
        PlacedSolid3d placement = placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        ResourceLocation duplicated = id("same");
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, 2L,
                new ResourceLocation[] {duplicated, duplicated},
                new PlacedSolid3d[] {placement, placement});

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("duplicate entry id"));
    }

    @Test
    void encoderRejectsPayloadAboveMessageByteLimit() {
        PlacedSolid3d shared = placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(1.0D, 1.0D, 1.0D)), RigidTransform3d.identity());
        String longPathBase = "l".repeat(1_014);
        int entryCount = 1_200;
        ResourceLocation[] ids = new ResourceLocation[entryCount];
        PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
        for (int index = 0; index < entryCount; index++) {
            String suffix = Integer.toString(index);
            ids[index] = new ResourceLocation("crlhitbox",
                    longPathBase.substring(suffix.length()) + suffix);
            placements[index] = shared;
        }
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, entryCount, ids, placements);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("exceeds MAX_MESSAGE_BYTES"));
    }

    @Test
    void encoderRejectsPrimitiveLeafBudgetExceeded() {
        int leavesPerComposite = FullSnapshotProtocol.MAX_COMPOSITE_LEAVES;
        int composites = FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES / leavesPerComposite + 1;
        Solid3d leaf = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        ResourceLocation[] ids = new ResourceLocation[composites];
        PlacedSolid3d[] placements = new PlacedSolid3d[composites];
        for (int index = 0; index < composites; index++) {
            ids[index] = id("budget_" + index);
            placements[index] = placed(new dev.crlhitbox.api.geometry.Composite(
                    java.util.Collections.nCopies(leavesPerComposite, leaf)),
                    RigidTransform3d.identity());
        }
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, composites, ids, placements);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("primitive leaf budget exceeded"));
    }

    @Test
    void encoderRejectsCompositeAboveLeafLimitBeforeWritingAnyByte() {
        Solid3d leaf = new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D));
        PlacedSolid3d placement = placed(new dev.crlhitbox.api.geometry.Composite(
                java.util.Collections.nCopies(FullSnapshotProtocol.MAX_COMPOSITE_LEAVES + 1, leaf)),
                RigidTransform3d.identity());
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, 1L,
                new ResourceLocation[] {id("too_many_leaves")},
                new PlacedSolid3d[] {placement});

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("MAX_COMPOSITE_LEAVES"));

        ByteBuf buffer = Unpooled.buffer();
        try {
            assertThrows(ProtocolEncodeException.class,
                    () -> EntityHitboxWireCodec.encode(buffer, payload));
            assertEquals(0, buffer.writerIndex(),
                    "a rejected payload must fail before writing any byte");
        } finally {
            buffer.release();
        }
    }

    @Test
    void encoderRejectsIdentifiersThatCollapseToTheSameWireText() {
        ResourceLocation first = new ResourceLocation("crl:hitbox", "part");
        ResourceLocation second = new ResourceLocation("crl", "hitbox:part");
        FullSnapshotPayload payload = payloadOfTwoIds(first, second);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("duplicate entry id"),
                "identifiers with identical wire text must be rejected before transmission");
    }

    @Test
    void encoderRejectsIdentifiersThatTheReceiverReconstructsAsOneValue() {
        ResourceLocation oneCharacterNamespace = new ResourceLocation("a", "b");
        ResourceLocation defaultNamespace = new ResourceLocation("minecraft", "b");
        assertEquals("a:b", oneCharacterNamespace.toString(), "sender-side wire text");
        assertEquals(defaultNamespace, new ResourceLocation(oneCharacterNamespace.toString()),
                "the receiver reconstructs both entries as the same identifier");
        FullSnapshotPayload payload = payloadOfTwoIds(oneCharacterNamespace, defaultNamespace);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("duplicate entry id"),
                "receiver-side reconstruction must drive encode-side duplicate detection");
    }

    @Test
    void encoderRejectsIdentifierWhoseCanonicalTextExceedsLimit() {
        ResourceLocation collapsing = new ResourceLocation("a", "b".repeat(
                FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES - 3));
        assertEquals(FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES - 1,
                collapsing.toString().getBytes(StandardCharsets.UTF_8).length,
                "sender-side wire text stays inside the frozen limit");
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, 1L,
                new ResourceLocation[] {collapsing},
                new PlacedSolid3d[] {placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D))});

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("canonicalizes to"),
                "an identifier the receiver would reject must fail before transmission");
    }

    @Test
    void encoderRejectsIdentifiersWhoseUtf8BytesAreEqual() {
        ResourceLocation unpairedSurrogate = new ResourceLocation("minecraft",
                String.valueOf((char) 0xD800));
        ResourceLocation replacement = new ResourceLocation("minecraft", "?");
        assertNotEquals(unpairedSurrogate, replacement, "distinct in-process identifiers");
        FullSnapshotPayload payload = payloadOfTwoIds(unpairedSurrogate, replacement);

        assertTrue(assertThrows(ProtocolEncodeException.class,
                () -> EntityHitboxWireCodec.encodedSize(payload)).getMessage()
                .contains("duplicate entry id"),
                "duplicate detection must key on the bytes actually written to the wire");
    }

    private static FullSnapshotPayload payloadOfTwoIds(ResourceLocation first,
            ResourceLocation second) {
        PlacedSolid3d placement = placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D));
        return FullSnapshotPayload.decoded(
                0, 1, 0L, 0L, 1L, 2L,
                new ResourceLocation[] {first, second},
                new PlacedSolid3d[] {placement, placement});
    }

    @Test
    void decoderRejectsIdentifierWhoseCanonicalTextExceedsLimit() {
        byte[] text = new byte[FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES];
        Arrays.fill(text, (byte) 'a');
        byte[] encoded = concat(manualHeader(1L, 1L, varInt(0), 1), varInt(text.length), text);
        assertFailureContains(encoded, "canonicalizes to");
    }

    private static byte[] validBytes() {
        return encode(payloadOf(1L, new ResourceLocation[] {id("probe")},
                new PlacedSolid3d[] {placed(new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 0.5D))}));
    }

    /**
     * Offset of the first entry record in {@link #validBytes()}:
     * version(1) + dimensionId(4) + entityId varint(2) + uuidMost(8) + uuidLeast(8)
     * + holderGeneration(8) + serverRevision(8) + entryCount varint(1).
     */
    private static int entryStartOffset() {
        return 1 + 4 + 2 + 8 + 8 + 8 + 8 + 1;
    }

    private static void decodeUnchecked(byte[] encoded) {
        ByteBuf buffer = reader(encoded);
        try {
            EntityHitboxWireCodec.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void assertFailureContains(byte[] encoded, String fragment) {
        ProtocolDecodeException failure = assertThrows(ProtocolDecodeException.class,
                () -> decodeUnchecked(encoded),
                "malformed payload must be rejected: "
                        + FullSnapshotPhase2BTestSupport.hex(encoded));
        assertTrue(failure.getMessage().contains(fragment),
                "expected failure message to contain '" + fragment + "', got: "
                        + failure.getMessage());
    }

    /**
     * Hand-built payload whose declared leaf total exceeds the frozen budget. It is written without
     * the production encoder so that the decoder's own budget accounting is exercised.
     */
    private static byte[] overBudgetBytes() {
        int leavesPerComposite = FullSnapshotProtocol.MAX_COMPOSITE_LEAVES;
        int composites = FullSnapshotProtocol.MAX_TOTAL_PRIMITIVE_LEAVES / leavesPerComposite;
        int entryCount = composites + 1;
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(FullSnapshotProtocol.PROTOCOL_VERSION);
            buffer.writeInt(0);
            writeVarIntLiteral(buffer, 0);
            buffer.writeLong(0L);
            buffer.writeLong(0L);
            buffer.writeLong(1L);
            buffer.writeLong(entryCount);
            writeVarIntLiteral(buffer, entryCount);
            for (int index = 0; index < composites; index++) {
                writeIdentifier(buffer, "crlhitbox:budget_" + index);
                buffer.writeByte(FullSnapshotProtocol.TAG_COMPOSITE);
                writeVarIntLiteral(buffer, leavesPerComposite);
                for (int leaf = 0; leaf < leavesPerComposite; leaf++) {
                    buffer.writeByte(FullSnapshotProtocol.TAG_AABB);
                    for (int component = 0; component < 6; component++) {
                        buffer.writeDouble(0.0D);
                    }
                }
                writeIdentityTransform(buffer);
            }
            writeIdentifier(buffer, "crlhitbox:budget_overflow");
            buffer.writeByte(FullSnapshotProtocol.TAG_AABB);
            for (int component = 0; component < 6; component++) {
                buffer.writeDouble(0.0D);
            }
            writeIdentityTransform(buffer);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void writeIdentifier(ByteBuf buffer, String text) {
        byte[] encoded = text.getBytes(StandardCharsets.UTF_8);
        writeVarIntLiteral(buffer, encoded.length);
        buffer.writeBytes(encoded);
    }

    private static byte[] manualHeader(long generation, long revision, byte[] entityIdBytes,
            int entryCount) {
        return manualHeader(generation, revision, entityIdBytes, varInt(entryCount));
    }

    private static byte[] manualHeader(long generation, long revision, byte[] entityIdBytes,
            byte[] entryCountBytes) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(FullSnapshotProtocol.PROTOCOL_VERSION);
            buffer.writeInt(0);
            for (int value : entityIdBytes) {
                buffer.writeByte(value);
            }
            buffer.writeLong(0L);
            buffer.writeLong(0L);
            buffer.writeLong(generation);
            buffer.writeLong(revision);
            buffer.writeBytes(entryCountBytes);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static byte[] entry(String idText, Consumer<ByteBuf> writer) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeIdentifier(buffer, idText);
            writer.accept(buffer);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void writeAabbEntry(ByteBuf buffer) {
        buffer.writeByte(FullSnapshotProtocol.TAG_AABB);
        for (int component = 0; component < 6; component++) {
            buffer.writeDouble(component < 3 ? 0.0D : 1.0D);
        }
        writeIdentityTransform(buffer);
    }

    private static void writeIdentityTransform(ByteBuf buffer) {
        writeIdentityRotation(buffer);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(0.0D);
    }

    private static void writeIdentityRotation(ByteBuf buffer) {
        buffer.writeDouble(0.0D);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(0.0D);
        buffer.writeDouble(1.0D);
    }

    private static byte[] varInt(int value) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeVarIntLiteral(buffer, value);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    /** Raw byte sequence used to inject values that no valid varint encoding can produce. */
    private static byte[] raw(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    private static void writeVarIntLiteral(ByteBuf buffer, int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            buffer.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        buffer.writeByte(remaining);
    }
}
