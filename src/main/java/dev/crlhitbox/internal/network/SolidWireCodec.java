package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Composite;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct binary codec for the sealed {@code Solid3d} set.
 *
 * <p>Shape tags are frozen protocol literals. A {@code Composite} is written as its canonical flat
 * primitive-leaf sequence: grouping history, bounds, cached values, identifiers, transforms, and
 * metadata are never encoded, and a nested Composite tag is rejected on decode. Every decoded leaf
 * is built through an existing public immutable geometry constructor, so finite checks, radius and
 * half-extent validation, and AABB ordering remain the geometry package's own rules.</p>
 */
final class SolidWireCodec {
    private SolidWireCodec() {
    }

    /**
     * Mutable per-payload primitive-leaf accounting.
     *
     * <p>Counts are validated against the frozen total budget before any leaf storage is allocated,
     * so a declared count can never drive an unbounded allocation.</p>
     */
    static final class LeafBudget {
        private final int maximum;
        private int total;

        LeafBudget(int maximum) {
            this.maximum = maximum;
        }

        void add(int leaves, String field) {
            if (leaves < 1) {
                throw new ProtocolDecodeException("non-positive primitive leaf count in " + field);
            }
            if (total > maximum - leaves) {
                throw new ProtocolDecodeException("primitive leaf budget exceeded by " + field
                        + ": " + (total + leaves) + " leaves against maximum " + maximum);
            }
            total += leaves;
        }

        int total() {
            return total;
        }
    }

    /** Returns the number of canonical primitive leaves contributed by one local solid. */
    static int primitiveLeafCount(Solid3d solid) {
        return solid instanceof Composite composite ? composite.childCount() : 1;
    }

    /** Returns the exact encoded size of one local solid. */
    static int encodedSize(Solid3d solid) {
        return switch (solid) {
            case Aabb aabb -> FullSnapshotProtocol.SOLID_TAG_BYTES + 2 * FullSnapshotProtocol.VECTOR_BYTES;
            case Sphere sphere -> FullSnapshotProtocol.SOLID_TAG_BYTES + FullSnapshotProtocol.VECTOR_BYTES
                    + FullSnapshotProtocol.DOUBLE_BYTES;
            case Obb obb -> FullSnapshotProtocol.SOLID_TAG_BYTES + 2 * FullSnapshotProtocol.VECTOR_BYTES
                    + FullSnapshotProtocol.ROTATION_BYTES;
            case Capsule capsule -> FullSnapshotProtocol.SOLID_TAG_BYTES
                    + 2 * FullSnapshotProtocol.VECTOR_BYTES + FullSnapshotProtocol.DOUBLE_BYTES;
            case Composite composite -> compositeSize(composite);
        };
    }

    /** Writes one local solid, including its frozen shape tag. */
    static void write(ByteBuf out, Solid3d solid) {
        switch (solid) {
            case Composite composite -> {
                int count = validateComposite(composite);
                out.writeByte(FullSnapshotProtocol.TAG_COMPOSITE);
                WireVarInts.writeInt(out, count);
                for (int index = 0; index < count; index++) {
                    writePrimitive(out, composite.child(index));
                }
            }
            default -> writePrimitive(out, solid);
        }
    }

    /** Reads one local solid, charging its primitive leaves to {@code budget}. */
    static Solid3d read(ByteBuf in, LeafBudget budget) {
        int tag = WireIo.readUnsignedByte(in, "solid tag");
        if (tag == FullSnapshotProtocol.TAG_COMPOSITE) {
            return readComposite(in, budget);
        }
        budget.add(1, "local solid");
        return readPrimitive(in, tag);
    }

    private static int compositeSize(Composite composite) {
        int count = validateComposite(composite);
        int size = FullSnapshotProtocol.SOLID_TAG_BYTES + WireVarInts.size(count);
        for (int index = 0; index < count; index++) {
            size += encodedSize(composite.child(index));
        }
        return size;
    }

    /**
     * Validates one flat Composite against the frozen leaf rules and returns its leaf count.
     *
     * <p>This runs inside {@link #encodedSize} as well as {@link #write}, so a size calculation
     * already fails before any byte reaches the caller's buffer.</p>
     */
    private static int validateComposite(Composite composite) {
        int count = composite.childCount();
        if (count < 1) {
            throw new ProtocolEncodeException("Composite with zero primitive leaves");
        }
        if (count > FullSnapshotProtocol.MAX_COMPOSITE_LEAVES) {
            throw new ProtocolEncodeException("Composite leaf count " + count
                    + " exceeds MAX_COMPOSITE_LEAVES "
                    + FullSnapshotProtocol.MAX_COMPOSITE_LEAVES);
        }
        for (int index = 0; index < count; index++) {
            if (composite.child(index) instanceof Composite nested) {
                throw new ProtocolEncodeException(
                        "nested Composite leaf at index " + index + " cannot be encoded: " + nested);
            }
        }
        return count;
    }

    private static void writePrimitive(ByteBuf out, Solid3d solid) {
        switch (solid) {
            case Aabb aabb -> {
                out.writeByte(FullSnapshotProtocol.TAG_AABB);
                WireIo.writeVector(out, aabb.min());
                WireIo.writeVector(out, aabb.max());
            }
            case Sphere sphere -> {
                out.writeByte(FullSnapshotProtocol.TAG_SPHERE);
                WireIo.writeVector(out, sphere.center());
                out.writeDouble(sphere.radius());
            }
            case Obb obb -> {
                out.writeByte(FullSnapshotProtocol.TAG_OBB);
                WireIo.writeVector(out, obb.center());
                WireIo.writeVector(out, obb.halfExtents());
                WireIo.writeRotation(out, obb.orientation());
            }
            case Capsule capsule -> {
                out.writeByte(FullSnapshotProtocol.TAG_CAPSULE);
                Segment3d centerline = capsule.centerline();
                WireIo.writeVector(out, centerline.start());
                WireIo.writeVector(out, centerline.end());
                out.writeDouble(capsule.radius());
            }
            case Composite composite -> throw new ProtocolEncodeException(
                    "nested Composite leaf cannot be encoded: " + composite);
        }
    }

    private static Composite readComposite(ByteBuf in, LeafBudget budget) {
        int count = WireVarInts.readInt(in, "composite leaf count");
        if (count < 1) {
            throw new ProtocolDecodeException("zero-leaf Composite payload");
        }
        if (count > FullSnapshotProtocol.MAX_COMPOSITE_LEAVES) {
            throw new ProtocolDecodeException("composite leaf count " + count
                    + " exceeds MAX_COMPOSITE_LEAVES "
                    + FullSnapshotProtocol.MAX_COMPOSITE_LEAVES);
        }
        budget.add(count, "Composite payload");
        List<Solid3d> leaves = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int tag = WireIo.readUnsignedByte(in, "composite leaf tag");
            if (tag == FullSnapshotProtocol.TAG_COMPOSITE) {
                throw new ProtocolDecodeException(
                        "nested Composite tag at composite leaf index " + index);
            }
            leaves.add(readPrimitive(in, tag));
        }
        try {
            return new Composite(leaves);
        } catch (IllegalArgumentException failure) {
            throw new ProtocolDecodeException("invalid Composite payload: " + failure.getMessage(),
                    failure);
        }
    }

    private static Solid3d readPrimitive(ByteBuf in, int tag) {
        try {
            return switch (tag) {
                case FullSnapshotProtocol.TAG_AABB -> new Aabb(
                        WireIo.readVector(in, "Aabb min"),
                        WireIo.readVector(in, "Aabb max"));
                case FullSnapshotProtocol.TAG_SPHERE -> new Sphere(
                        WireIo.readVector(in, "Sphere center"),
                        readDouble(in, "Sphere radius"));
                case FullSnapshotProtocol.TAG_OBB -> new Obb(
                        WireIo.readVector(in, "Obb center"),
                        WireIo.readVector(in, "Obb half extents"),
                        WireIo.readRotation(in, "Obb orientation"));
                case FullSnapshotProtocol.TAG_CAPSULE -> new Capsule(
                        new Segment3d(
                                WireIo.readVector(in, "Capsule centerline start"),
                                WireIo.readVector(in, "Capsule centerline end")),
                        readDouble(in, "Capsule radius"));
                default -> throw new ProtocolDecodeException("unknown solid tag: " + tag);
            };
        } catch (ProtocolDecodeException failure) {
            throw failure;
        } catch (IllegalArgumentException failure) {
            throw new ProtocolDecodeException(
                    "invalid solid payload for tag " + tag + ": " + failure.getMessage(), failure);
        }
    }

    private static double readDouble(ByteBuf in, String field) {
        WireIo.requireReadable(in, FullSnapshotProtocol.DOUBLE_BYTES, field);
        return in.readDouble();
    }
}
