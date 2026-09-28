package dev.crlhitbox.internal.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.PROPERTY_ITERATIONS;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.SEED_CODEC_ROUND_TRIP;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.SEED_MALFORMED;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.describe;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.encode;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.hex;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.randomPayload;
import static dev.crlhitbox.internal.network.FullSnapshotPhase2BTestSupport.reader;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic fixed-seed codec property suites.
 *
 * <p>Seed {@code 0x5EED_2B01L} covers exact round trips and truncation rejection; seed
 * {@code 0x5EED_2B02L} covers arbitrary byte mutation with the "accepted implies value-stable"
 * invariant. Every randomized failure report carries the seed, iteration, entity identity,
 * generation, revision, ordered identifiers, encoded length, and the offending byte levels.</p>
 */
class EntityHitboxWirePropertyPhase2BTest {
    @Test
    void codecRoundTripsEveryDeterministicRandomPayload() {
        Random random = new Random(SEED_CODEC_ROUND_TRIP);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            FullSnapshotPayload payload = randomPayload(random, SEED_CODEC_ROUND_TRIP, iteration);
            byte[] encoded = encode(payload);
            FullSnapshotPayload decoded = decodeOrFail(encoded,
                    context(SEED_CODEC_ROUND_TRIP, iteration, payload, encoded.length, -1));
            String context = context(SEED_CODEC_ROUND_TRIP, iteration, payload, encoded.length, -1);

            assertEquals(payload.size(), decoded.size(), context);
            for (int index = 0; index < payload.size(); index++) {
                assertEquals(payload.id(index), decoded.id(index),
                        context + ", expectedId=" + payload.id(index) + ", actualId=" + decoded.id(index));
                assertEquals(payload.placement(index), decoded.placement(index),
                        context + ", entryIndex=" + index);
            }
            assertEquals(payload.serverRevision(), decoded.serverRevision(), context);
            assertEquals(payload.holderGeneration(), decoded.holderGeneration(), context);
            assertArrayEquals(encoded, encode(decoded), context);
        }
    }

    @Test
    void everyProperTruncationIsRejected() {
        Random random = new Random(SEED_CODEC_ROUND_TRIP);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            FullSnapshotPayload payload = randomPayload(random, SEED_CODEC_ROUND_TRIP, iteration);
            byte[] encoded = encode(payload);
            int length = random.nextInt(encoded.length);
            byte[] truncated = Arrays.copyOf(encoded, length);
            String context = context(SEED_CODEC_ROUND_TRIP, iteration, payload, encoded.length, length);

            assertThrows(ProtocolDecodeException.class, () -> decodeUnchecked(truncated),
                    "expected truncation at " + length + " byte(s) to fail closed: " + context);
        }
    }

    @Test
    void everyAcceptedByteSequenceDecodesToAStableValue() {
        Random random = new Random(SEED_MALFORMED);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            FullSnapshotPayload payload = randomPayload(random, SEED_MALFORMED, iteration);
            byte[] mutated = mutate(random, iteration, encode(payload));
            String context = context(SEED_MALFORMED, iteration, payload, mutated.length, -1);

            try {
                FullSnapshotPayload accepted = decodeUnchecked(mutated);
                byte[] reencoded = encode(accepted);
                FullSnapshotPayload redecoded = decodeUnchecked(reencoded);

                assertPayloadsMatch(accepted, redecoded, "accepted payload must be value-stable; "
                        + "encoded=" + hex(mutated) + ", reencoded=" + hex(reencoded) + ", "
                        + context);
                assertArrayEquals(reencoded, encode(redecoded),
                        "re-encoding a decoded payload must be byte-stable: " + context);
            } catch (ProtocolDecodeException expectedFailure) {
                assertEquals(ProtocolDecodeException.class, expectedFailure.getClass(),
                        "decode failures must be protocol failures: " + context);
            }
        }
    }

    @Test
    void encodedSizeMatchesEmittedBytesForEveryPayload() {
        Random random = new Random(SEED_CODEC_ROUND_TRIP);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            FullSnapshotPayload payload = randomPayload(random, SEED_CODEC_ROUND_TRIP, iteration);
            byte[] encoded = encode(payload);
            String context = context(SEED_CODEC_ROUND_TRIP, iteration, payload, encoded.length, -1);

            assertEquals(encoded.length, EntityHitboxWireCodec.encodedSize(payload),
                    "pre-computed size must equal the emitted bytes; " + context);
            assertEquals(oracleLeafCount(payload), countedLeaves(payload),
                    "production leaf accounting must match the independent count; " + context);
        }
    }

    @Test
    void decodedIdentifiersAreCanonicalResourceLocationText() {
        Random random = new Random(SEED_MALFORMED);
        for (int iteration = 0; iteration < PROPERTY_ITERATIONS; iteration++) {
            FullSnapshotPayload payload = randomPayload(random, SEED_MALFORMED, iteration);
            byte[] encoded = encode(payload);
            FullSnapshotPayload decoded = decodeUnchecked(encoded);
            String context = context(SEED_MALFORMED, iteration, payload, encoded.length, -1);

            for (int index = 0; index < decoded.size(); index++) {
                ResourceLocation key = decoded.id(index);
                String text = key.toString();
                assertEquals(key, new ResourceLocation(text),
                        "wire text must be canonical resource-location text; " + context);
                assertTrue(text.getBytes(StandardCharsets.UTF_8).length
                                <= FullSnapshotProtocol.MAX_RESOURCE_LOCATION_BYTES,
                        "identifier stays inside the frozen limit; " + context);
            }
        }
    }

    private static int countedLeaves(FullSnapshotPayload payload) {
        int leaves = 0;
        for (int index = 0; index < payload.size(); index++) {
            leaves += SolidWireCodec.primitiveLeafCount(payload.placement(index).localSolid());
        }
        return leaves;
    }

    private static int oracleLeafCount(FullSnapshotPayload payload) {
        int leaves = 0;
        for (int index = 0; index < payload.size(); index++) {
            leaves += payload.placement(index).localSolid() instanceof dev.crlhitbox.api.geometry.Composite composite
                    ? composite.childCount() : 1;
        }
        return leaves;
    }

    private static void assertPayloadsMatch(FullSnapshotPayload expected, FullSnapshotPayload actual,
            String context) {        assertEquals(expected.dimensionId(), actual.dimensionId(), context);
        assertEquals(expected.entityId(), actual.entityId(), context);
        assertEquals(expected.uuidMost(), actual.uuidMost(), context);
        assertEquals(expected.uuidLeast(), actual.uuidLeast(), context);
        assertEquals(expected.holderGeneration(), actual.holderGeneration(), context);
        assertEquals(expected.serverRevision(), actual.serverRevision(), context);
        assertEquals(expected.size(), actual.size(), context);
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.id(index), actual.id(index),
                    context + ", entryIndex=" + index + ", identifier");
            assertEquals(expected.placement(index), actual.placement(index),
                    context + ", entryIndex=" + index + ", placement");
        }
    }

    private static byte[] mutate(Random random, int iteration, byte[] encoded) {
        byte[] mutated = encoded.clone();
        switch (Math.floorMod(iteration, 4)) {
            case 0 -> {
                int length = random.nextInt(mutated.length + 1);
                mutated = Arrays.copyOf(mutated, length);
            }
            case 1 -> mutated[random.nextInt(mutated.length)] ^= (byte) (1 << random.nextInt(8));
            case 2 -> {
                byte[] extended = Arrays.copyOf(mutated, mutated.length + 1);
                extended[mutated.length] = (byte) random.nextInt(256);
                mutated = extended;
            }
            default -> {
                int index = random.nextInt(mutated.length);
                mutated[index] = (byte) random.nextInt(256);
            }
        }
        return mutated;
    }

    private static FullSnapshotPayload decodeOrFail(byte[] encoded, String context) {
        try {
            return decodeUnchecked(encoded);
        } catch (RuntimeException failure) {
            throw new AssertionError("round trip failed: " + failure + ", " + context, failure);
        }
    }

    private static FullSnapshotPayload decodeUnchecked(byte[] encoded) {
        ByteBuf buffer = reader(encoded);
        try {
            return EntityHitboxWireCodec.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static String context(long seed, int iteration, FullSnapshotPayload payload,
            int encodedLength, int truncatedLength) {
        StringBuilder orderedIds = new StringBuilder();
        for (int index = 0; index < payload.size(); index++) {
            orderedIds.append(index == 0 ? "" : ",").append(payload.id(index));
        }
        return "seed=0x" + Long.toUnsignedString(seed, 16).toUpperCase() + "L"
                + ", iteration=" + iteration
                + ", " + describe(payload)
                + ", orderedEntryIds=[" + orderedIds + "]"
                + ", encodedLength=" + encodedLength
                + ", truncatedLength=" + truncatedLength;
    }
}
