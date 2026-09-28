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
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.DIMENSION_ID;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.ENTITY_ID;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.GENERATION;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.UUID_LEAST;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.UUID_MOST;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.decode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.describe;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.encode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.id;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.payload;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.payloadOf;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.placed;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.rotated;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.snapshotOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Value and byte round trips of the frozen Phase 2B full-snapshot wire contract. */
class EntityHitboxWireCodecPhase2BTest {
    @Test
    void emptySnapshotAtRevisionZeroRoundTrips() {
        EntityHitboxSnapshot snapshot = new EntityHitboxHolder().snapshot();
        FullSnapshotPayload payload = payload(snapshot);
        byte[] encoded = encode(payload);
        FullSnapshotPayload decoded = decode(encoded);

        assertAll(
                () -> assertEquals(0, decoded.size(), describe(decoded)),
                () -> assertEquals(0L, decoded.serverRevision(), describe(decoded)),
                () -> assertEquals(DIMENSION_ID, decoded.dimensionId(), describe(decoded)),
                () -> assertEquals(ENTITY_ID, decoded.entityId(), describe(decoded)),
                () -> assertEquals(GENERATION, decoded.holderGeneration(), describe(decoded)),
                () -> assertArrayEquals(encoded, encode(decoded), describe(decoded)));
    }

    @Test
    void emptySnapshotAtNonzeroWireRevisionRoundTrips() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        holder.put(id("transient"), placed(sphere(0.0D, 1.0D)));
        holder.clear();
        EntityHitboxSnapshot snapshot = holder.snapshot();
        FullSnapshotPayload payload = payload(snapshot);
        byte[] encoded = encode(payload);
        FullSnapshotPayload decoded = decode(encoded);

        assertAll(
                () -> assertEquals(2L, snapshot.revision(), "two effective mutations"),
                () -> assertEquals(0, decoded.size(), describe(decoded)),
                () -> assertEquals(2L, decoded.serverRevision(), describe(decoded)),
                () -> assertArrayEquals(encoded, encode(decoded), describe(decoded)));
    }

    @Test
    void singleEntryRoundTripsExactly() {
        ResourceLocation key = id("head");
        PlacedSolid3d placement = placed(
                new Aabb(new Vec3d(-0.5D, 0.0D, -0.5D), new Vec3d(0.5D, 1.5D, 0.5D)),
                new RigidTransform3d(rotated(0.0D, 0.0D, 0.25D, 1.0D), new Vec3d(1.0D, 2.0D, 3.0D)));
        FullSnapshotPayload payload = payloadOf(1L, new ResourceLocation[] {key},
                new PlacedSolid3d[] {placement});
        FullSnapshotPayload decoded = decode(encode(payload));

        assertAll(
                () -> assertEquals(1, decoded.size(), describe(decoded)),
                () -> assertEquals(key, decoded.id(0), describe(decoded)),
                () -> assertEquals(placement, decoded.placement(0), describe(decoded)),
                () -> assertEquals(1L, decoded.serverRevision(), describe(decoded)));
    }

    @Test
    void manyEntriesPreserveInsertionOrder() {
        List<ResourceLocation> ids = new ArrayList<>();
        List<PlacedSolid3d> placements = new ArrayList<>();
        for (int index = 0; index < 64; index++) {
            ids.add(id("part_" + index));
            placements.add(placed(sphere(index * 0.5D, index * 0.125D)));
        }
        FullSnapshotPayload payload = payload(snapshotOf(ids, placements));
        FullSnapshotPayload decoded = decode(encode(payload));

        assertEquals(64, decoded.size(), describe(decoded));
        for (int index = 0; index < 64; index++) {
            assertEquals(ids.get(index), decoded.id(index), "id order at index " + index);
            assertEquals(placements.get(index), decoded.placement(index),
                    "placement order at index " + index);
        }
    }

    @Test
    void namespacesAndPathsRoundTrip() {
        ResourceLocation[] ids = {
                new ResourceLocation("minecraft", "stone"),
                new ResourceLocation("crl", "a"),
                new ResourceLocation("crl", ""),
                new ResourceLocation("crlhitbox", "a/b.c-d_e"),
                new ResourceLocation("a/b", "path:ish")
        };
        PlacedSolid3d placement = placed(sphere(1.0D, 0.5D));
        PlacedSolid3d[] placements = new PlacedSolid3d[ids.length];
        for (int index = 0; index < ids.length; index++) {
            placements[index] = placement;
        }
        FullSnapshotPayload decoded = decode(encode(payloadOf(ids.length, ids, placements)));

        for (int index = 0; index < ids.length; index++) {
            assertEquals(ids[index], decoded.id(index), "identifier at index " + index);
        }
    }

    @Test
    void everyPrimitiveLocalSolidRoundTrips() {
        List<Solid3d> solids = List.of(
                new Aabb(new Vec3d(-1.0D, -2.0D, -3.0D), new Vec3d(4.0D, 5.0D, 6.0D)),
                new Sphere(new Vec3d(0.25D, -0.5D, 0.75D), 2.5D),
                new Obb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(0.5D, 1.0D, 1.5D),
                        rotated(0.0D, 0.0D, 1.0D, 1.0D)),
                new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D),
                        new Vec3d(1.0D, 2.0D, 3.0D)), 0.75D));
        for (Solid3d solid : solids) {
            PlacedSolid3d placement = placed(solid,
                    new RigidTransform3d(rotated(0.0D, 0.0D, 0.25D, 1.0D),
                            new Vec3d(-1.0D, 0.5D, 2.0D)));
            FullSnapshotPayload decoded = decode(
                    encode(payloadOf(1L, new ResourceLocation[] {id("solid")},
                            new PlacedSolid3d[] {placement})));
            assertEquals(placement, decoded.placement(0), "solid round trip for " + solid);
        }
    }

    @Test
    void compositeWithAllFourPrimitiveKindsRoundTrips() {
        Composite composite = new Composite(List.of(
                new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 0.5D),
                new Obb(new Vec3d(3.0D, 0.0D, 0.0D), new Vec3d(0.5D, 0.5D, 0.5D),
                        rotated(0.0D, 0.0D, 1.0D, 1.0D)),
                new Capsule(new Segment3d(new Vec3d(4.0D, 0.0D, 0.0D),
                        new Vec3d(4.0D, 1.0D, 0.0D)), 0.25D),
                new Sphere(new Vec3d(2.0D, 0.0D, 0.0D), 0.5D)));
        PlacedSolid3d placement = placed(composite, identityRotated());
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("union")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "Composite round trip");
        assertEquals(5, ((Composite) decoded.placement(0).localSolid()).childCount(),
                "flat canonical leaf sequence");
    }

    @Test
    void duplicateGeometriesUnderDifferentIdsRoundTrip() {
        PlacedSolid3d shared = placed(sphere(1.0D, 1.0D));
        ResourceLocation[] ids = {id("first"), id("second"), id("third")};
        PlacedSolid3d[] placements = {shared, shared, shared};
        FullSnapshotPayload decoded = decode(encode(payloadOf(3L, ids, placements)));

        assertAll(
                () -> assertEquals(3, decoded.size(), describe(decoded)),
                () -> assertEquals(ids[0], decoded.id(0), describe(decoded)),
                () -> assertEquals(ids[1], decoded.id(1), describe(decoded)),
                () -> assertEquals(ids[2], decoded.id(2), describe(decoded)),
                () -> assertEquals(shared, decoded.placement(1), describe(decoded)));
    }

    @Test
    void identityTransformRoundTrips() {
        FullSnapshotPayload decoded = decode(encode(payloadOf(1L,
                new ResourceLocation[] {id("identity")},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D), RigidTransform3d.identity())})));

        assertEquals(RigidTransform3d.identity(), decoded.placement(0).localToParent(),
                "identity transform");
    }

    @Test
    void rotatedTranslatedTransformRoundTrips() {
        Rotation3d rotation = rotated(1.0D, 1.0D, 3.0D, 2.0D);
        RigidTransform3d transform = new RigidTransform3d(rotation, new Vec3d(4.0D, 5.0D, 6.0D));
        FullSnapshotPayload decoded = decode(encode(payloadOf(1L,
                new ResourceLocation[] {id("pose")},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D), transform)})));

        assertAll(
                () -> assertEquals(rotation, decoded.placement(0).localToParent().rotation(),
                        "rotation bits preserved through exact reconstruction"),
                () -> assertEquals(transform, decoded.placement(0).localToParent(),
                        "transform round trip"));
    }

    @Test
    void signedZeroCanonicalValuesRoundTrip() {
        PlacedSolid3d placement = placed(new Aabb(
                new Vec3d(-0.0D, -0.0D, -0.0D), new Vec3d(0.0D, 0.0D, 0.0D)));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("zero")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "signed zero canonicalizes once");
    }

    @Test
    void zeroRadiusSphereRoundTrips() {
        PlacedSolid3d placement = placed(new Sphere(new Vec3d(1.0D, 2.0D, 3.0D), 0.0D));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("point_sphere")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "zero-radius sphere");
    }

    @Test
    void zeroRadiusCapsuleRoundTrips() {
        PlacedSolid3d placement = placed(new Capsule(
                new Segment3d(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(4.0D, 5.0D, 6.0D)), 0.0D));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("segment")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "zero-radius capsule");
    }

    @Test
    void zeroLengthCapsuleRoundTrips() {
        PlacedSolid3d placement = placed(new Capsule(
                new Segment3d(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(1.0D, 1.0D, 1.0D)), 0.5D));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("degenerate_capsule")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "zero-length capsule");
    }

    @Test
    void degenerateAabbAndObbRoundTrip() {
        List<Solid3d> solids = List.of(
                new Aabb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                new Aabb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(1.0D, 1.0D, 3.0D)),
                new Aabb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(3.0D, 1.0D, 3.0D)),
                new Obb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(0.0D, 0.0D, 0.0D),
                        RigidTransform3d.identity().rotation()),
                new Obb(new Vec3d(1.0D, 1.0D, 1.0D), new Vec3d(2.0D, 0.0D, 0.0D),
                        RigidTransform3d.identity().rotation()));
        for (Solid3d solid : solids) {
            PlacedSolid3d placement = placed(solid);
            FullSnapshotPayload decoded = decode(
                    encode(payloadOf(1L, new ResourceLocation[] {id("degenerate")},
                            new PlacedSolid3d[] {placement})));
            assertEquals(placement, decoded.placement(0), "degenerate solid " + solid);
        }
    }

    @Test
    void adjacentRepresentableAabbEndpointsRoundTrip() {
        double lower = 1.0D;
        double upper = Math.nextUp(1.0D);
        PlacedSolid3d placement = placed(new Aabb(
                new Vec3d(lower, lower, lower), new Vec3d(upper, upper, upper)));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("adjacent")},
                        new PlacedSolid3d[] {placement})));
        Aabb decodedBox = (Aabb) decoded.placement(0).localSolid();

        assertAll(
                () -> assertEquals(placement, decoded.placement(0), "adjacent endpoints"),
                () -> assertEquals(lower, decodedBox.min().x(), "exact lower endpoint"),
                () -> assertEquals(upper, decodedBox.max().x(), "exact upper endpoint"));
    }

    @Test
    void subnormalValuesRoundTrip() {
        PlacedSolid3d placement = placed(new Aabb(
                new Vec3d(Double.MIN_VALUE, 0.0D, 0.0D),
                new Vec3d(Double.MIN_NORMAL, 1.0D, 1.0D)));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("subnormal")},
                        new PlacedSolid3d[] {placement})));
        Aabb decodedBox = (Aabb) decoded.placement(0).localSolid();

        assertAll(
                () -> assertEquals(placement, decoded.placement(0), "subnormal endpoints"),
                () -> assertEquals(Double.MIN_VALUE, decodedBox.min().x(), "subnormal minimum"),
                () -> assertEquals(Double.MIN_NORMAL, decodedBox.max().x(), "smallest normal"));
    }

    @Test
    void largeFiniteValuesRoundTrip() {
        double large = 1.0E300D;
        PlacedSolid3d placement = placed(new Aabb(
                new Vec3d(-large, -large, -large), new Vec3d(large, large, large)));
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("large")},
                        new PlacedSolid3d[] {placement})));

        assertEquals(placement, decoded.placement(0), "large finite endpoints");
    }

    @Test
    void canonicalQuaternionIsStableAcrossSignEquivalentInputs() {
        Rotation3d positive = rotated(0.0D, 0.0D, 1.0D, 1.0D);
        Rotation3d negated = rotated(-0.0D, -0.0D, -1.0D, -1.0D);
        FullSnapshotPayload first = payloadOf(1L, new ResourceLocation[] {id("spin")},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D),
                        new RigidTransform3d(positive, new Vec3d(0.0D, 0.0D, 0.0D)))});
        FullSnapshotPayload second = payloadOf(1L, new ResourceLocation[] {id("spin")},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D),
                        new RigidTransform3d(negated, new Vec3d(0.0D, 0.0D, 0.0D)))});

        assertAll(
                () -> assertEquals(positive, negated, "q/-q canonicalize to one stored value"),
                () -> assertArrayEquals(encode(first), encode(second),
                        "sign-equivalent rotations encode identically"));
    }

    @Test
    void repeatedEncodingIsDeterministic() {
        List<ResourceLocation> ids = FullSnapshotPhase2BTestSupport.ids("alpha", "beta", "gamma");
        List<PlacedSolid3d> placements = List.of(
                placed(sphere(0.0D, 1.0D)),
                placed(new Capsule(new Segment3d(new Vec3d(0.0D, 0.0D, 0.0D),
                        new Vec3d(1.0D, 1.0D, 1.0D)), 0.25D)),
                placed(new Composite(List.of(
                        new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                        new Sphere(new Vec3d(2.0D, 2.0D, 2.0D), 0.5D)))));
        FullSnapshotPayload payload = payload(snapshotOf(ids, placements));
        byte[] first = encode(payload);
        byte[] second = encode(payload);
        byte[] third = encode(payload(payload.toSnapshot()));

        assertAll(
                () -> assertArrayEquals(first, second, "repeated encoding must not drift"),
                () -> assertArrayEquals(first, third,
                        "re-encoding the decoded snapshot must reproduce the bytes"));
    }

    @Test
    void decodedPayloadReencodesToIdenticalBytes() {
        List<ResourceLocation> ids = FullSnapshotPhase2BTestSupport.ids("one", "two");
        List<PlacedSolid3d> placements = List.of(
                placed(new Obb(new Vec3d(1.0D, 2.0D, 3.0D), new Vec3d(0.5D, 1.0D, 1.5D),
                        rotated(1.0D, 1.0D, 3.0D, 2.0D)),
                        new RigidTransform3d(rotated(1.0D, 1.0D, 3.0D, 2.0D),
                                new Vec3d(4.0D, 5.0D, 6.0D))),
                placed(sphere(-1.0D, 2.0D)));
        byte[] encoded = encode(payload(snapshotOf(ids, placements)));
        FullSnapshotPayload decoded = decode(encoded);

        assertArrayEquals(encoded, encode(decoded), describe(decoded));
    }

    @Test
    void wireRevisionIsPreservedSeparatelyFromTemporaryHolderRevision() {
        PlacedSolid3d placement = placed(sphere(0.0D, 1.0D));
        FullSnapshotPayload payload = payloadOf(3L, new ResourceLocation[] {id("revision")},
                new PlacedSolid3d[] {placement});
        FullSnapshotPayload decoded = decode(encode(payload));
        EntityHitboxSnapshot rebuilt = decoded.toSnapshot();

        assertAll(
                () -> assertEquals(3L, decoded.serverRevision(), "wire revision preserved"),
                () -> assertEquals(1L, rebuilt.revision(),
                        "temporary holder revision is a local mutation counter, not the wire value"),
                () -> assertNotEquals(decoded.serverRevision(), rebuilt.revision()),
                () -> assertEquals(decoded.placement(0), rebuilt.placement(0)));
    }

    @Test
    void maximumLegalResourceLocationByteLengthRoundTrips() {
        StringBuilder path = new StringBuilder();
        path.append("k".repeat(1_014));
        ResourceLocation key = new ResourceLocation("crlhitbox", path.toString());
        assertEquals(1_024, key.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                "fixture must be exactly at MAX_RESOURCE_LOCATION_BYTES");
        FullSnapshotPayload decoded = decode(encode(payloadOf(1L, new ResourceLocation[] {key},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D))})));

        assertEquals(key, decoded.id(0), "maximum legal identifier length");
    }

    @Test
    void maximumEntryCountPayloadEncodesWithinProtocolLimits() {
        int entryCount = FullSnapshotProtocol.MAX_ENTRIES;
        ResourceLocation[] ids = new ResourceLocation[entryCount];
        PlacedSolid3d[] placements = new PlacedSolid3d[entryCount];
        PlacedSolid3d shared = placed(new Aabb(new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(1.0D, 1.0D, 1.0D)));
        for (int index = 0; index < entryCount; index++) {
            ids[index] = id("e" + index);
            placements[index] = shared;
        }
        FullSnapshotPayload payload = payloadOf(entryCount, ids, placements);
        byte[] encoded = encode(payload);
        FullSnapshotPayload decoded = decode(encoded);

        assertAll(
                () -> assertTrue(encoded.length <= FullSnapshotProtocol.MAX_MESSAGE_BYTES,
                        "encoded length " + encoded.length),
                () -> assertEquals(entryCount, decoded.size(), describe(decoded)),
                () -> assertEquals(ids[0], decoded.id(0), "first identifier"),
                () -> assertEquals(ids[entryCount - 1], decoded.id(entryCount - 1),
                        "last identifier"),
                () -> assertArrayEquals(encoded, encode(decoded), "maximum payload round trip"));
    }

    @Test
    void compositeLeafOrderAndMultiplicityArePreserved() {
        Composite composite = new Composite(List.of(
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D)));
        PlacedSolid3d placement = placed(composite);
        FullSnapshotPayload decoded = decode(
                encode(payloadOf(1L, new ResourceLocation[] {id("ordered_union")},
                        new PlacedSolid3d[] {placement})));
        Composite decodedComposite = (Composite) decoded.placement(0).localSolid();

        assertAll(
                () -> assertEquals(composite, decodedComposite, "exact structural equality"),
                () -> assertTrue(decodedComposite.child(0) instanceof Sphere, "first leaf"),
                () -> assertTrue(decodedComposite.child(1) instanceof Aabb, "second leaf"),
                () -> assertTrue(decodedComposite.child(2) instanceof Sphere, "third leaf"),
                () -> assertEquals(decodedComposite.child(0), decodedComposite.child(2),
                        "duplicate leaf multiplicity preserved"),
                () -> assertFalse(decodedComposite.equals(new Composite(List.of(
                        new Aabb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 1.0D, 1.0D)),
                        new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D),
                        new Sphere(new Vec3d(0.0D, 0.0D, 0.0D), 1.0D)))),
                        "leaf order affects structural equality"));
    }

    @Test
    void singleCharacterNamespaceIdentifierDoesNotRoundTrip() {
        ResourceLocation key = new ResourceLocation("a", "b");
        FullSnapshotPayload decoded = decode(encode(payloadOf(1L, new ResourceLocation[] {key},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D))})));

        assertAll(
                () -> assertEquals("a:b", key.toString(), "encoded text"),
                () -> assertEquals(new ResourceLocation("minecraft", "b"), decoded.id(0),
                        "one-character namespace is not adopted by the 1.12.2 string constructor"),
                () -> assertNotEquals(key, decoded.id(0),
                        "known ResourceLocation wire-identity conflict, preserved as evidence"));
    }

    @Test
    void colonBearingNamespaceIdentifierDoesNotRoundTrip() {
        ResourceLocation key = new ResourceLocation("crl:hitbox", "part");
        FullSnapshotPayload decoded = decode(encode(payloadOf(1L, new ResourceLocation[] {key},
                new PlacedSolid3d[] {placed(sphere(0.0D, 1.0D))})));

        assertAll(
                () -> assertEquals("crl:hitbox:part", key.toString(), "encoded text"),
                () -> assertEquals(new ResourceLocation("crl", "hitbox:part"), decoded.id(0),
                        "the first colon wins when the identifier text is re-split"),
                () -> assertNotEquals(key, decoded.id(0),
                        "known ResourceLocation wire-identity conflict, preserved as evidence"));
    }

    private static Sphere sphere(double offset, double radius) {
        return new Sphere(new Vec3d(offset, 0.0D, 0.0D), radius);
    }

    private static RigidTransform3d identityRotated() {
        return new RigidTransform3d(RigidTransform3d.identity().rotation(),
                new Vec3d(0.0D, 0.0D, 0.0D));
    }
}
