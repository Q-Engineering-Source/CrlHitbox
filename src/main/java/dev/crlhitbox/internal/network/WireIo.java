package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;

/**
 * Fixed-width binary64 plumbing shared by the full-snapshot codec.
 *
 * <p>Every double is written and read as one IEEE-754 binary64 value through the buffer's ordinary
 * network byte order. Rotations are restored through the geometry-owned exact reconstruction entry
 * rather than the raw normalizing constructor, so the four transmitted component bits survive the
 * round trip. Platform geometry types never enter this package; only their public immutable
 * constructors and accessors are used.</p>
 */
final class WireIo {
    private WireIo() {
    }

    /** Fails when the buffer cannot supply the requested number of bytes. */
    static void requireReadable(ByteBuf in, int bytes, String field) {
        int available = in.readableBytes();
        if (available < bytes) {
            throw new ProtocolDecodeException("truncated " + field + ": need " + bytes
                    + " byte(s) at reader index " + in.readerIndex() + ", have " + available);
        }
    }

    /** Reads one unsigned byte. */
    static int readUnsignedByte(ByteBuf in, String field) {
        requireReadable(in, 1, field);
        return in.readUnsignedByte();
    }

    /** Reads one signed 32-bit integer. */
    static int readInt(ByteBuf in, String field) {
        requireReadable(in, Integer.BYTES, field);
        return in.readInt();
    }

    /** Reads one signed 64-bit long. */
    static long readLong(ByteBuf in, String field) {
        requireReadable(in, Long.BYTES, field);
        return in.readLong();
    }

    /** Writes one vector as three binary64 components. */
    static void writeVector(ByteBuf out, Vec3d vector) {
        out.writeDouble(vector.x());
        out.writeDouble(vector.y());
        out.writeDouble(vector.z());
    }

    /** Reads one vector as three binary64 components. */
    static Vec3d readVector(ByteBuf in, String field) {
        requireReadable(in, FullSnapshotProtocol.VECTOR_BYTES, field);
        try {
            return new Vec3d(in.readDouble(), in.readDouble(), in.readDouble());
        } catch (IllegalArgumentException failure) {
            throw new ProtocolDecodeException("invalid " + field + ": " + failure.getMessage(),
                    failure);
        }
    }

    /** Writes one rotation as four binary64 stored components. */
    static void writeRotation(ByteBuf out, Rotation3d rotation) {
        out.writeDouble(rotation.x());
        out.writeDouble(rotation.y());
        out.writeDouble(rotation.z());
        out.writeDouble(rotation.w());
    }

    /** Reads one rotation through the geometry-owned exact reconstruction entry. */
    static Rotation3d readRotation(ByteBuf in, String field) {
        requireReadable(in, FullSnapshotProtocol.ROTATION_BYTES, field);
        double x = in.readDouble();
        double y = in.readDouble();
        double z = in.readDouble();
        double w = in.readDouble();
        try {
            return Rotation3d.reconstructExact(x, y, z, w);
        } catch (IllegalArgumentException failure) {
            throw new ProtocolDecodeException("invalid " + field + ": " + failure.getMessage(),
                    failure);
        }
    }

    /** Writes one rigid transform as rotation then translation. */
    static void writeTransform(ByteBuf out, RigidTransform3d transform) {
        writeRotation(out, transform.rotation());
        writeVector(out, transform.translation());
    }

    /** Reads one rigid transform as rotation then translation. */
    static RigidTransform3d readTransform(ByteBuf in, String field) {
        Rotation3d rotation = readRotation(in, field + " rotation");
        Vec3d translation = readVector(in, field + " translation");
        try {
            return new RigidTransform3d(rotation, translation);
        } catch (IllegalArgumentException failure) {
            throw new ProtocolDecodeException("invalid " + field + ": " + failure.getMessage(),
                    failure);
        }
    }
}
