package dev.crlhitbox.internal.network;

/**
 * Frozen Phase 2B full-snapshot protocol constants.
 *
 * <p>These values are internal protocol state, not stable public API. The wire format is direct
 * binary and versioned; the stable solid tags and the field order below may only change together
 * with {@link #PROTOCOL_VERSION}. Shape tags are frozen literals and are deliberately not derived
 * from class names, enum ordinals, hash codes, sealed-permit order, or reflection order.</p>
 */
final class FullSnapshotProtocol {
    /** Required first payload byte after the SimpleNetworkWrapper discriminator. */
    static final int PROTOCOL_VERSION = 1;

    /** SimpleNetworkWrapper discriminator of the single Phase 2B message. */
    static final int FULL_SNAPSHOT_DISCRIMINATOR = 0;

    /** Maximum encoded full-snapshot payload size, excluding the transport discriminator. */
    static final int MAX_MESSAGE_BYTES = 1_048_576;

    /** Maximum number of holder entries in one full snapshot. */
    static final int MAX_ENTRIES = 4_096;

    /** Maximum encoded UTF-8 byte length of one ResourceLocation identifier. */
    static final int MAX_RESOURCE_LOCATION_BYTES = 1_024;

    /** Maximum number of canonical primitive leaves in one Composite payload. */
    static final int MAX_COMPOSITE_LEAVES = 4_096;

    /** Maximum number of primitive leaves counted across one whole payload. */
    static final int MAX_TOTAL_PRIMITIVE_LEAVES = 16_384;

    /** Stable wire tag of {@code Aabb}. */
    static final int TAG_AABB = 0;

    /** Stable wire tag of {@code Sphere}. */
    static final int TAG_SPHERE = 1;

    /** Stable wire tag of {@code Obb}. */
    static final int TAG_OBB = 2;

    /** Stable wire tag of {@code Capsule}. */
    static final int TAG_CAPSULE = 3;

    /** Stable wire tag of {@code Composite}. Never valid inside a Composite payload. */
    static final int TAG_COMPOSITE = 4;

    static final int DOUBLE_BYTES = 8;
    static final int VECTOR_BYTES = 3 * DOUBLE_BYTES;
    static final int ROTATION_BYTES = 4 * DOUBLE_BYTES;
    static final int TRANSFORM_BYTES = ROTATION_BYTES + VECTOR_BYTES;
    static final int SOLID_TAG_BYTES = 1;

    private FullSnapshotProtocol() {
    }
}
