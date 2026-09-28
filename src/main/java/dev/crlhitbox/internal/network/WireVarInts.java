package dev.crlhitbox.internal.network;

import io.netty.buffer.ByteBuf;

/**
 * Strict unsigned LEB128 varint coding for nonnegative protocol counts and identifiers.
 *
 * <p>The encoding is the ordinary seven-bit-group little-endian form used by the 1.12.2 network
 * layer. Reading accepts only the shortest canonical encoding of a nonnegative 32-bit value: it is
 * bounded to {@link #MAX_BYTES_INT} bytes and rejects overlong, non-shortest, and out-of-domain
 * inputs, so a hostile payload can neither extend decoding work nor wrap a count. Because only the
 * canonical form is accepted, any accepted payload re-encodes to identical bytes.</p>
 */
final class WireVarInts {
    /** Maximum encoded width of one 32-bit varint. */
    static final int MAX_BYTES_INT = 5;

    private static final int PAYLOAD_MASK = 0x7F;
    private static final int CONTINUATION_MASK = 0x80;
    private static final int FINAL_BYTE_PAYLOAD_MASK = 0x0F;

    private WireVarInts() {
    }

    /** Writes one nonnegative varint value. */
    static void writeInt(ByteBuf out, int value) {
        if (value < 0) {
            throw new ProtocolEncodeException("negative protocol varint value: " + value);
        }
        int remaining = value;
        while ((remaining & ~PAYLOAD_MASK) != 0) {
            out.writeByte((remaining & PAYLOAD_MASK) | CONTINUATION_MASK);
            remaining >>>= 7;
        }
        out.writeByte(remaining);
    }

    /** Returns the encoded width of one nonnegative varint value. */
    static int size(int value) {
        if (value < 0) {
            throw new ProtocolEncodeException("negative protocol varint value: " + value);
        }
        int size = 1;
        int remaining = value >>> 7;
        while (remaining != 0) {
            size++;
            remaining >>>= 7;
        }
        return size;
    }

    /** Reads one nonnegative varint value of at most {@link #MAX_BYTES_INT} bytes. */
    static int readInt(ByteBuf in, String field) {
        int result = 0;
        for (int index = 0; index < MAX_BYTES_INT; index++) {
            WireIo.requireReadable(in, 1, field);
            int value = in.readUnsignedByte();
            if (index == MAX_BYTES_INT - 1) {
                if ((value & CONTINUATION_MASK) != 0) {
                    throw new ProtocolDecodeException(
                            field + " varint exceeds " + MAX_BYTES_INT + " encoded bytes");
                }
                if (value > FINAL_BYTE_PAYLOAD_MASK) {
                    throw new ProtocolDecodeException(
                            field + " varint exceeds 32 bits at byte " + (index + 1));
                }
            }
            result |= (value & PAYLOAD_MASK) << (index * 7);
            if ((value & CONTINUATION_MASK) == 0) {
                if (index > 0 && value == 0) {
                    throw new ProtocolDecodeException(
                            field + " varint is not the shortest encoding");
                }
                if (result < 0) {
                    throw new ProtocolDecodeException(field + " varint is negative: " + result);
                }
                return result;
            }
        }
        throw new ProtocolDecodeException(
                field + " varint exceeds " + MAX_BYTES_INT + " encoded bytes");
    }
}
