package dev.crlhitbox.internal.network;

/**
 * Actionable failure of a full-snapshot payload decode.
 *
 * <p>Decoding is all-or-nothing: this exception always means that no decoded value escaped and
 * that no world, holder, capability, or pending state was touched. It is an internal protocol
 * failure type, not stable public API.</p>
 */
final class ProtocolDecodeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ProtocolDecodeException(String message) {
        super(message);
    }

    ProtocolDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
