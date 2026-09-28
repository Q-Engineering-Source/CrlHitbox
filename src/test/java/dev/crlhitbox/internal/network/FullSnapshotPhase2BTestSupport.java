package dev.crlhitbox.internal.network;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.entity.EntityHitboxSnapshot;
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

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Deterministic Phase 2B full-snapshot fixtures shared by the wire-codec test suites. */
final class FullSnapshotPhase2BTestSupport {
    static final int DIMENSION_ID = 0;
    static final int ENTITY_ID = 4242;
    static final long UUID_MOST = 0x0123456789ABCDEFL;
    static final long UUID_LEAST = 0xFEDCBA9876543210L;
    static final long GENERATION = 7L;
    static final long SEED_CODEC_ROUND_TRIP = 0x5EED_2B01L;
    static final long SEED_MALFORMED = 0x5EED_2B02L;
    static final int PROPERTY_ITERATIONS = 2_048;

    private FullSnapshotPhase2BTestSupport() {
    }

    static ByteBuf reader(byte[] encoded) {
        return Unpooled.wrappedBuffer(encoded);
    }

    static byte[] bytes(ByteBuf buffer) {
        byte[] result = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), result);
        return result;
    }

    static String hex(byte[] encoded) {
        return HexFormat.of().formatHex(encoded);
    }

    /** Encodes through the production codec into an exactly sized buffer. */
    static byte[] encode(FullSnapshotPayload payload) {
        int size = EntityHitboxWireCodec.encodedSize(payload);
        ByteBuf buffer = Unpooled.buffer(size, size);
        try {
            EntityHitboxWireCodec.encode(buffer, payload);
            assertEquals(size, buffer.readableBytes(),
                    "encoded size must match the pre-computed size; " + describe(payload));
            return bytes(buffer);
        } finally {
            buffer.release();
        }
    }

    /** Decodes one complete payload and requires exact exhaustion. */
    static FullSnapshotPayload decode(byte[] encoded) {
        ByteBuf buffer = reader(encoded);
        try {
            FullSnapshotPayload payload = EntityHitboxWireCodec.decode(buffer);
            assertEquals(0, buffer.readableBytes(),
                    "decode must consume the whole payload; encoded=" + hex(encoded));
            return payload;
        } finally {
            buffer.release();
        }
    }

    static FullSnapshotPayload payload(
            EntityHitboxSnapshot snapshot,
            int dimensionId,
            int entityId,
            long uuidMost,
            long uuidLeast,
            long generation
    ) {
        return FullSnapshotPayload.fromSnapshot(
                dimensionId, entityId, uuidMost, uuidLeast, generation, snapshot);
    }

    static FullSnapshotPayload payload(EntityHitboxSnapshot snapshot) {
        return payload(snapshot, DIMENSION_ID, ENTITY_ID, UUID_MOST, UUID_LEAST, GENERATION);
    }

    static FullSnapshotPayload payloadOf(
            long serverRevision,
            ResourceLocation[] ids,
            PlacedSolid3d[] placements
    ) {
        return FullSnapshotPayload.decoded(
                DIMENSION_ID, ENTITY_ID, UUID_MOST, UUID_LEAST, GENERATION, serverRevision,
                ids, placements);
    }

    static EntityHitboxSnapshot snapshotOf(
            long wireRevision,
            ResourceLocation[] ids,
            PlacedSolid3d[] placements
    ) {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        for (int index = 0; index < ids.length; index++) {
            holder.put(ids[index], placements[index]);
        }
        EntityHitboxSnapshot snapshot = holder.snapshot();
        if (snapshot.revision() != wireRevision) {
            throw new AssertionError("fixture wire revision " + wireRevision
                    + " is unreachable for a fresh holder of " + ids.length + " entries");
        }
        return snapshot;
    }

    static EntityHitboxSnapshot snapshotOf(List<ResourceLocation> ids, List<PlacedSolid3d> placements) {
        return snapshotOf(ids.size(), ids.toArray(ResourceLocation[]::new),
                placements.toArray(PlacedSolid3d[]::new));
    }

    static ResourceLocation id(String path) {
        return new ResourceLocation("crlhitbox", path);
    }

    static RigidTransform3d identity() {
        return RigidTransform3d.identity();
    }

    static PlacedSolid3d placed(Solid3d solid) {
        return new PlacedSolid3d(solid, identity());
    }

    static PlacedSolid3d placed(Solid3d solid, RigidTransform3d transform) {
        return new PlacedSolid3d(solid, transform);
    }

    static Rotation3d rotated(double x, double y, double z, double w) {
        return new Rotation3d(x, y, z, w);
    }

    static String describe(FullSnapshotPayload payload) {
        return "dimensionId=" + payload.dimensionId()
                + ", entityId=" + payload.entityId()
                + ", uuidMost=" + payload.uuidMost()
                + ", uuidLeast=" + payload.uuidLeast()
                + ", holderGeneration=" + payload.holderGeneration()
                + ", wireRevision=" + payload.serverRevision()
                + ", entryCount=" + payload.size();
    }

    /** Random but always valid payload used by the fixed-seed property suites. */
    static FullSnapshotPayload randomPayload(Random random, long seed, int iteration) {
        int entryCount = random.nextInt(7);
        ResourceLocation[] ids = new ResourceLocation[entryCount];
        PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
        for (int index = 0; index < entryCount; index++) {
            ids[index] = id("entry_" + index + "_" + random.nextInt(64));
            placements[index] = randomPlacement(random, Math.floorMod(iteration + index, 5));
        }
        long generation = 1L + random.nextInt(Integer.MAX_VALUE);
        long revision = random.nextInt(4_096);
        FullSnapshotPayload payload = FullSnapshotPayload.decoded(
                DIMENSION_ID, ENTITY_ID, UUID_MOST, UUID_LEAST, generation, revision,
                ids, placements);
        String context = "seed=0x" + Long.toUnsignedString(seed, 16).toUpperCase()
                + "L, iteration=" + iteration + ", " + describe(payload);
        if (payload.size() != entryCount) {
            throw new AssertionError("fixture lost entries: " + context);
        }
        return payload;
    }

    static PlacedSolid3d randomPlacement(Random random, int kind) {
        Vec3d origin = randomPoint(random);
        Solid3d solid = switch (kind) {
            case 0 -> new Aabb(origin, origin.add(randomExtent(random)));
            case 1 -> new Sphere(origin, randomRadius(random));
            case 2 -> new Obb(origin, randomExtent(random), randomRotation(random));
            case 3 -> new Capsule(
                    new Segment3d(origin, origin.add(randomDelta(random))),
                    randomRadius(random));
            case 4 -> new Composite(List.of(
                    new Aabb(origin, origin.add(randomExtent(random))),
                    new Sphere(origin.add(randomDelta(random)), randomRadius(random)),
                    new Capsule(
                            new Segment3d(origin, origin.add(randomDelta(random))),
                            randomRadius(random))));
            default -> throw new AssertionError("unreachable solid kind " + kind);
        };
        return new PlacedSolid3d(
                solid,
                new RigidTransform3d(randomRotation(random), randomPoint(random)));
    }

    static Vec3d randomPoint(Random random) {
        return new Vec3d(component(random), component(random), component(random));
    }

    static Vec3d randomExtent(Random random) {
        return new Vec3d(Math.abs(component(random)) + 0.125D,
                Math.abs(component(random)) + 0.125D,
                Math.abs(component(random)) + 0.125D);
    }

    static Vec3d randomDelta(Random random) {
        return new Vec3d(component(random), component(random), component(random));
    }

    static double randomRadius(Random random) {
        return random.nextInt(1_024) * 0.125D;
    }

    static Rotation3d randomRotation(Random random) {
        double x = component(random);
        double y = component(random);
        double z = component(random);
        double w = component(random) + 1.0D;
        return new Rotation3d(x, y, z, w);
    }

    static double component(Random random) {
        return (random.nextInt(129) - 64) * 0.125D;
    }

    static List<ResourceLocation> ids(String... paths) {
        List<ResourceLocation> ids = new ArrayList<>(paths.length);
        for (String path : paths) {
            ids.add(id(path));
        }
        return ids;
    }
}
