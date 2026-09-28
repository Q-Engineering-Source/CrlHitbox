package dev.crlhitbox.internal.network;

/**
 * Failure to encode a full snapshot that cannot be represented inside the frozen protocol limits.
 *
 * <p>The encoder validates counts, identifiers, and the total encoded size before writing, so an
 * oversize or otherwise unsendable snapshot fails before any transport call. It is an internal
 * protocol failure type, not stable public API.</p>
 */
final class ProtocolEncodeException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    ProtocolEncodeException(String message) {
        super(message);
    }
}
