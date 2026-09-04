package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Tests the frozen wire representation prerequisite before introducing a production codec. */
class GeometryWireRepresentationPhase2BTest {
    @Test
    void quarterTurnControlPreservesExactStoredRotationAndBytes() {
        Rotation3d original = new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D);
        byte[] encoded = encodeRotation(original);
        Rotation3d decoded = decodeRotation(encoded);

        assertEquals(original, decoded);
        assertArrayEquals(encoded, encodeRotation(decoded));

        Rotation3d legacyDecoded = decodeRotationWithLegacyConstructor(encoded);
        assertEquals(original, legacyDecoded);
        assertArrayEquals(encoded, encodeRotation(legacyDecoded));
    }

    @Test
    void canonicalServerQuaternionReconstructionPreservesExactStoredRotationAndBytes() {
        Rotation3d original = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        byte[] encoded = encodeRotation(original);
        Rotation3d decoded = decodeRotation(encoded);
        byte[] reencoded = encodeRotation(decoded);
        String evidence = evidence(original, decoded, encoded, reencoded);

        assertAll(
                () -> assertEquals(original, decoded, evidence),
                () -> assertArrayEquals(encoded, reencoded, evidence));
    }

    @Test
    void canonicalServerPlacedObbReconstructionPreservesExactValueAndBytes() {
        Rotation3d rotation = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        PlacedSolid3d original = new PlacedSolid3d(
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 3.0D), rotation),
                new RigidTransform3d(rotation, new Vec3d(4.0D, 5.0D, 6.0D)));
        byte[] encoded = encodePlacedObb(original);
        PlacedSolid3d decoded = decodePlacedObb(encoded);
        byte[] reencoded = encodePlacedObb(decoded);
        String evidence = evidence(rotation, decoded.localToParent().rotation(), encoded, reencoded);

        assertAll(
                () -> assertEquals(original.localSolid(), decoded.localSolid(), evidence),
                () -> assertEquals(original.localToParent(), decoded.localToParent(), evidence),
                () -> assertEquals(original, decoded, evidence),
                () -> assertArrayEquals(encoded, reencoded, evidence));
    }

    @Test
    void historicalDirectConstructorReentryRemainsExplicitlyNonIdempotentAtByteOffsetTwentyThree() {
        Rotation3d original = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        byte[] encoded = encodeRotation(original);
        Rotation3d reentered = decodeRotationWithLegacyConstructor(encoded);
        byte[] reencoded = encodeRotation(reentered);

        assertAll(
                () -> assertEquals("3fe8c97ef43f7248", Long.toHexString(Double.doubleToRawLongBits(original.z()))),
                () -> assertEquals("3fe8c97ef43f7249", Long.toHexString(Double.doubleToRawLongBits(reentered.z()))),
                () -> assertNotEquals(original, reentered),
                () -> assertEquals(23, Arrays.mismatch(encoded, reencoded)));
    }

    @Test
    void historicalPlacedObbDirectConstructorReentryRemainsExplicitlyNonIdempotentAtByteOffsetSeventyTwo() {
        Rotation3d rotation = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        PlacedSolid3d original = new PlacedSolid3d(
                new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 3.0D), rotation),
                new RigidTransform3d(rotation, new Vec3d(4.0D, 5.0D, 6.0D)));
        byte[] encoded = encodePlacedObb(original);
        PlacedSolid3d reentered = decodePlacedObbWithLegacyConstructor(encoded);
        byte[] reencoded = encodePlacedObb(reentered);

        assertAll(
                () -> assertNotEquals(original, reentered),
                () -> assertEquals(72, Arrays.mismatch(encoded, reencoded)));
    }

    private static byte[] encodeRotation(Rotation3d rotation) {
        ByteBuf buffer = Unpooled.buffer(32, 32);
        try {
            writeRotation(buffer, rotation);
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static Rotation3d decodeRotation(byte[] encoded) {
        ByteBuf buffer = Unpooled.wrappedBuffer(encoded);
        try {
            Rotation3d result = readRotation(buffer);
            assertEquals(0, buffer.readableBytes());
            return result;
        } finally {
            buffer.release();
        }
    }

    private static Rotation3d decodeRotationWithLegacyConstructor(byte[] encoded) {
        ByteBuf buffer = Unpooled.wrappedBuffer(encoded);
        try {
            Rotation3d result = readRotationWithLegacyConstructor(buffer);
            assertEquals(0, buffer.readableBytes());
            return result;
        } finally {
            buffer.release();
        }
    }

    private static byte[] encodePlacedObb(PlacedSolid3d placement) {
        ByteBuf buffer = Unpooled.buffer(137, 137);
        try {
            Obb box = assertInstanceOf(Obb.class, placement.localSolid());
            buffer.writeByte(2);
            writeVector(buffer, box.center());
            writeVector(buffer, box.halfExtents());
            writeRotation(buffer, box.orientation());
            writeRotation(buffer, placement.localToParent().rotation());
            writeVector(buffer, placement.localToParent().translation());
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    private static PlacedSolid3d decodePlacedObb(byte[] encoded) {
        ByteBuf buffer = Unpooled.wrappedBuffer(encoded);
        try {
            assertEquals(2, buffer.readUnsignedByte());
            Obb box = new Obb(readVector(buffer), readVector(buffer), readRotation(buffer));
            RigidTransform3d transform = new RigidTransform3d(readRotation(buffer), readVector(buffer));
            PlacedSolid3d result = new PlacedSolid3d(box, transform);
            assertEquals(0, buffer.readableBytes());
            return result;
        } finally {
            buffer.release();
        }
    }

    private static PlacedSolid3d decodePlacedObbWithLegacyConstructor(byte[] encoded) {
        ByteBuf buffer = Unpooled.wrappedBuffer(encoded);
        try {
            assertEquals(2, buffer.readUnsignedByte());
            Obb box = new Obb(readVector(buffer), readVector(buffer), readRotationWithLegacyConstructor(buffer));
            RigidTransform3d transform = new RigidTransform3d(readRotationWithLegacyConstructor(buffer), readVector(buffer));
            PlacedSolid3d result = new PlacedSolid3d(box, transform);
            assertEquals(0, buffer.readableBytes());
            return result;
        } finally {
            buffer.release();
        }
    }

    private static void writeRotation(ByteBuf buffer, Rotation3d rotation) {
        buffer.writeDouble(rotation.x());
        buffer.writeDouble(rotation.y());
        buffer.writeDouble(rotation.z());
        buffer.writeDouble(rotation.w());
    }

    private static Rotation3d readRotation(ByteBuf buffer) {
        return Rotation3d.reconstructExact(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static Rotation3d readRotationWithLegacyConstructor(ByteBuf buffer) {
        return new Rotation3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVector(ByteBuf buffer, Vec3d vector) {
        buffer.writeDouble(vector.x());
        buffer.writeDouble(vector.y());
        buffer.writeDouble(vector.z());
    }

    private static Vec3d readVector(ByteBuf buffer) {
        return new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static byte[] bytes(ByteBuf buffer) {
        byte[] result = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), result);
        return result;
    }

    private static String evidence(Rotation3d original, Rotation3d decoded, byte[] encoded, byte[] reencoded) {
        return "raw server constructor input=(1,1,3,2)"
                + ", original=" + original + ", originalBits=" + bits(original)
                + ", decoded=" + decoded + ", decodedBits=" + bits(decoded)
                + ", encodedLength=" + encoded.length
                + ", firstDifferentByte=" + Arrays.mismatch(encoded, reencoded)
                + ", encoded=" + HexFormat.of().formatHex(encoded)
                + ", reencoded=" + HexFormat.of().formatHex(reencoded);
    }

    private static String bits(Rotation3d rotation) {
        return Long.toHexString(Double.doubleToRawLongBits(rotation.x())) + ","
                + Long.toHexString(Double.doubleToRawLongBits(rotation.y())) + ","
                + Long.toHexString(Double.doubleToRawLongBits(rotation.z())) + ","
                + Long.toHexString(Double.doubleToRawLongBits(rotation.w()));
    }
}
