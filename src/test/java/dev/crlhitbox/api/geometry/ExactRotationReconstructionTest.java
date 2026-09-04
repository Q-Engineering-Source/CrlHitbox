package dev.crlhitbox.api.geometry;

import java.math.BigDecimal;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactRotationReconstructionTest {
    private static final long ONE_BITS = Double.doubleToRawLongBits(1.0D);
    private static final long RAW_CONSTRUCTOR_SEED = 0x6a09e667f3bcc909L;
    private static final long COMPOSITION_SEED = 0xbb67ae8584caa73bL;

    @Test
    void reconstructsTheHistoricalConstructorOutputWithoutRenormalizingItAgain() {
        Rotation3d original = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);

        Rotation3d reconstructed = Rotation3d.reconstructExact(
                original.x(), original.y(), original.z(), original.w());

        assertRawBitsEqual(original, reconstructed, "historical constructor output");
    }

    @Test
    void reconstructsPublicConstructorOutputsAtExponentBinadeSubnormalPivotAndSignBoundaries() {
        Rotation3d[] outputs = {
                Rotation3d.identity(),
                new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D),
                new Rotation3d(1.0D, 1.0D, 1.0D, 1.0D),
                new Rotation3d(4.0D, -2.0D, 1.0D, 3.0D),
                new Rotation3d(-4.0D, 2.0D, -1.0D, -3.0D),
                new Rotation3d(0.0D, Double.MIN_VALUE, -Math.nextUp(Double.MIN_VALUE), Double.MIN_NORMAL),
                new Rotation3d(Math.scalb(1.0D, -1022), Math.scalb(-1.0D, -1021), Math.scalb(1.0D, 0), Math.scalb(-1.0D, 1023)),
                new Rotation3d(1.0D, Math.nextUp(0.5D), Math.nextDown(0.5D), Math.nextUp(1.0D)),
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Rotation3d(1.0D, 0.0D, 0.0D, 0.0D)
        };

        for (int index = 0; index < outputs.length; index++) assertReconstructs(outputs[index], "directed fixture index=" + index);
    }

    @Test
    void reconstructsTwoThousandFortyEightDeterministicRawConstructorOutputs() {
        SplittableRandom random = new SplittableRandom(RAW_CONSTRUCTOR_SEED);
        for (int iteration = 0; iteration < 2048; iteration++) {
            double x = randomFiniteComponent(random);
            double y = randomFiniteComponent(random);
            double z = randomFiniteComponent(random);
            double w = randomFiniteComponent(random);
            Rotation3d output = new Rotation3d(x, y, z, w);
            assertReconstructs(output, propertyContext("raw constructor", RAW_CONSTRUCTOR_SEED, iteration, x, y, z, w));
        }
    }

    @Test
    void reconstructsTwoThousandFortyEightDeterministicInverseAndCompositionOutputs() {
        SplittableRandom random = new SplittableRandom(COMPOSITION_SEED);
        for (int iteration = 0; iteration < 2048; iteration++) {
            double firstX = randomFiniteComponent(random);
            double firstY = randomFiniteComponent(random);
            double firstZ = randomFiniteComponent(random);
            double firstW = randomFiniteComponent(random);
            double secondX = randomFiniteComponent(random);
            double secondY = randomFiniteComponent(random);
            double secondZ = randomFiniteComponent(random);
            double secondW = randomFiniteComponent(random);
            Rotation3d first = new Rotation3d(firstX, firstY, firstZ, firstW);
            Rotation3d second = new Rotation3d(secondX, secondY, secondZ, secondW);
            String context = propertyContext("inverse/composition", COMPOSITION_SEED, iteration,
                    firstX, firstY, firstZ, firstW, secondX, secondY, secondZ, secondW);
            assertReconstructs(first.inverse(), context + ", route=inverse");
            Vec3d zero = new Vec3d(0.0D, 0.0D, 0.0D);
            RigidTransform3d composition = new RigidTransform3d(first, zero)
                    .andThen(new RigidTransform3d(second, zero));
            assertReconstructs(composition.rotation(), context + ", route=andThen");
        }
    }

    @Test
    void rejectsEveryStrictInvalidEncodingBeforeReconstruction() {
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.0D, 0.0D, 0.0D, 0.0D));
        for (int position = 0; position < 4; position++) {
            int componentIndex = position;
            for (double invalid : new double[] {-0.0D, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                double[] values = {0.0D, 0.0D, 0.0D, 1.0D};
                values[componentIndex] = invalid;
                assertThrows(IllegalArgumentException.class,
                        () -> Rotation3d.reconstructExact(values[0], values[1], values[2], values[3]),
                        () -> "invalid raw component position=" + componentIndex + ", bits=" + Long.toHexString(Double.doubleToRawLongBits(invalid)));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.0D, 0.0D, 0.0D, -1.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(-1.0D, 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.0D, -1.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.0D, 0.0D, -1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(Math.nextUp(1.0D), 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.49D, 0.0D, 0.0D, 0.0D));
        double a = Math.scalb(1.0D, -26);
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(1.0D, a, a, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(0.5D, 0.5D, 0.0D, 0.0D));
    }

    @Test
    void independentBigDecimalReferenceMatchesPublicAcceptanceAtCellsAndExhaustionBoundaries() {
        Rotation3d subnormalOutput = new Rotation3d(Double.MIN_VALUE, 1.0D, 0.0D, 1.0D);
        assertTrue(subnormalOutput.x() != 0.0D && Math.abs(subnormalOutput.x()) < Double.MIN_NORMAL,
                () -> "expected a nonzero subnormal output but was " + rawBits(subnormalOutput));
        assertTrue(Math.max(Math.max(Math.abs(subnormalOutput.x()), Math.abs(subnormalOutput.y())), Math.max(Math.abs(subnormalOutput.z()), Math.abs(subnormalOutput.w()))) < 1.0D);

        Rotation3d[] images = {
                Rotation3d.identity(),
                new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D),
                new Rotation3d(1.0D, 1.0D, 1.0D, 1.0D),
                new Rotation3d(Double.MIN_VALUE, 1.0D, 0.0D, 1.0D),
                new Rotation3d(Double.MIN_VALUE, Math.nextUp(Double.MIN_VALUE), Double.MIN_NORMAL, -1.0D),
                new Rotation3d(Math.nextDown(1.0D), Math.nextUp(0.5D), Math.nextDown(0.5D), 1.0D)
        };
        for (Rotation3d image : images) assertMatchesIndependentReference(image.x(), image.y(), image.z(), image.w());

        assertMatchesIndependentReference(1.0D, Math.scalb(1.0D, -26), Math.scalb(1.0D, -26), 0.0D);
        assertMatchesIndependentReference(0.5D, 0.5D, 0.0D, 0.0D);
    }

    @Test
    void publicSeamCoversLatticeBinadesTiedPivotsAndNegativeMaximumFixtures() {
        double beforeMinimumNormal = Math.nextDown(Double.MIN_NORMAL);
        double afterMinimumNormal = Math.nextUp(Double.MIN_NORMAL);
        double twiceMinimumNormal = Math.scalb(Double.MIN_NORMAL, 1);
        Rotation3d[] outputs = {
                new Rotation3d(Double.MIN_VALUE, 1.0D, 0.0D, 1.0D),
                new Rotation3d(Double.MIN_VALUE, 1.0D, 0.0D, -1.0D),
                new Rotation3d(beforeMinimumNormal, 1.0D, 0.0D, 1.0D),
                new Rotation3d(Double.MIN_NORMAL, 1.0D, 0.0D, 1.0D),
                new Rotation3d(afterMinimumNormal, 1.0D, 0.0D, 1.0D),
                new Rotation3d(twiceMinimumNormal, 1.0D, 0.0D, 1.0D),
                new Rotation3d(Math.nextDown(0.5D), Math.nextUp(0.5D), Math.nextDown(1.0D), Math.nextUp(1.0D)),
                new Rotation3d(Math.nextDown(0.25D), Math.nextUp(0.25D), Math.nextDown(0.125D), Math.nextUp(0.125D)),
                new Rotation3d(1.0D, 0.0D, 0.0D, 0.0D),
                new Rotation3d(1.0D, 1.0D, 0.0D, 0.0D),
                new Rotation3d(1.0D, 1.0D, 1.0D, 0.0D),
                new Rotation3d(1.0D, 1.0D, 1.0D, 1.0D),
                new Rotation3d(-4.0D, 1.0D, -2.0D, 0.5D),
                new Rotation3d(Math.nextUp(1.0D), Math.nextDown(1.0D), Math.nextUp(0.5D), Math.nextDown(0.5D))
        };
        for (Rotation3d output : outputs) {
            assertMatchesIndependentReference(output.x(), output.y(), output.z(), output.w());
        }
        assertMatchesIndependentReference(Double.MIN_VALUE, 0.5D, 0.5D, 0.5D);
    }

    @Test
    void independentReferenceVerifiesSubnormalCellEndpointsParityAndUniqueYPivot() {
        double eta = Double.MIN_VALUE;
        double twiceEta = Math.nextUp(eta);
        double threeEta = Math.nextUp(twiceEta);
        double fourEta = Math.nextUp(threeEta);
        double fiveEta = Math.nextUp(fourEta);

        assertRawCellCandidates(eta, 1.0D, eta);
        assertRawCellCandidates(Double.MIN_NORMAL, 0.5D, Math.nextDown(Math.scalb(Double.MIN_NORMAL, 1)), Math.scalb(Double.MIN_NORMAL, 1));
        assertRawCellCandidates(eta, 0.5D, eta, twiceEta, threeEta);
        assertRawCellCandidates(twiceEta, 0.5D, threeEta, fourEta, fiveEta);

        assertExactEndpoint(eta, 0.5D, eta, false);
        assertExactEndpoint(threeEta, 0.5D, eta, true);
        assertEquals(0L, Double.doubleToRawLongBits(0.5D * eta));
        assertEquals(Double.doubleToRawLongBits(twiceEta), Double.doubleToRawLongBits(0.5D * threeEta));
        assertExactEndpoint(threeEta, 0.5D, twiceEta, false);
        assertExactEndpoint(fiveEta, 0.5D, twiceEta, true);
        assertEquals(Double.doubleToRawLongBits(twiceEta), Double.doubleToRawLongBits(0.5D * threeEta));
        assertEquals(Double.doubleToRawLongBits(twiceEta), Double.doubleToRawLongBits(0.5D * fiveEta));

        assertMatchesIndependentReference(0.5D, 0.5D, 0.5D, eta);
        assertMatchesIndependentReference(0.5D, 0.5D, 0.5D, twiceEta);

        Rotation3d uniqueYPivot = new Rotation3d(1.0D, 4.0D, -2.0D, 0.5D);
        assertTrue(Math.abs(uniqueYPivot.y()) > Math.abs(uniqueYPivot.x())
                && Math.abs(uniqueYPivot.y()) > Math.abs(uniqueYPivot.z())
                && Math.abs(uniqueYPivot.y()) > Math.abs(uniqueYPivot.w()),
                () -> "expected a unique y pivot but was " + rawBits(uniqueYPivot));
        assertMatchesIndependentReference(uniqueYPivot.x(), uniqueYPivot.y(), uniqueYPivot.z(), uniqueYPivot.w());
    }

    @Test
    void reconstructedRotationsPreserveCanonicalFlatCompositePlacementAndIndependentPairQuery() {
        Rotation3d intrinsic = new Rotation3d(1.0D, 1.0D, 3.0D, 2.0D);
        Rotation3d outer = new Rotation3d(2.0D, -3.0D, 4.0D, 5.0D);
        Obb originalBox = new Obb(new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(1.0D, 2.0D, 3.0D), intrinsic);
        Composite originalLocal = new Composite(List.of(
                new Composite(List.of(originalBox, new Sphere(new Vec3d(4.0D, 0.0D, 0.0D), 0.5D))),
                new Aabb(new Vec3d(-3.0D, -1.0D, -1.0D), new Vec3d(-2.0D, 1.0D, 1.0D))));
        RigidTransform3d originalTransform = new RigidTransform3d(outer, new Vec3d(5.0D, -7.0D, 11.0D));
        PlacedSolid3d original = new PlacedSolid3d(originalLocal, originalTransform);

        Rotation3d reconstructedIntrinsic = Rotation3d.reconstructExact(intrinsic.x(), intrinsic.y(), intrinsic.z(), intrinsic.w());
        Rotation3d reconstructedOuter = Rotation3d.reconstructExact(outer.x(), outer.y(), outer.z(), outer.w());
        Obb reconstructedBox = new Obb(originalBox.center(), originalBox.halfExtents(), reconstructedIntrinsic);
        Composite reconstructedLocal = new Composite(List.of(
                new Composite(List.of(reconstructedBox, new Sphere(new Vec3d(4.0D, 0.0D, 0.0D), 0.5D))),
                new Aabb(new Vec3d(-3.0D, -1.0D, -1.0D), new Vec3d(-2.0D, 1.0D, 1.0D))));
        PlacedSolid3d reconstructed = new PlacedSolid3d(reconstructedLocal,
                new RigidTransform3d(reconstructedOuter, originalTransform.translation()));

        assertEquals(3, originalLocal.childCount());
        assertEquals(originalLocal, reconstructedLocal);
        assertEquals(originalLocal.hashCode(), reconstructedLocal.hashCode());
        assertEquals(originalLocal.bounds(), reconstructedLocal.bounds());
        assertEquals(original, reconstructed);
        assertEquals(original.hashCode(), reconstructed.hashCode());
        assertEquals(original.bounds(), reconstructed.bounds());

        Vec3d parentCenter = originalTransform.transformPoint(originalBox.center());
        PlacedSolid3d independentProbe = new PlacedSolid3d(new Sphere(parentCenter, 0.25D), RigidTransform3d.identity());
        assertTrue(GeometryIntersections.intersects(original, independentProbe));
        assertEquals(GeometryIntersections.intersects(original, independentProbe),
                GeometryIntersections.intersects(reconstructed, independentProbe));
    }

    @Test
    void reconstructedValuesRemainExactAcrossReimportAndRepeatedInverseRoutes() {
        Rotation3d original = new Rotation3d(1.0D, -2.0D, 3.0D, 4.0D);
        Rotation3d first = Rotation3d.reconstructExact(original.x(), original.y(), original.z(), original.w());
        Rotation3d second = Rotation3d.reconstructExact(first.x(), first.y(), first.z(), first.w());
        assertRawBitsEqual(original, first, "first reimport");
        assertRawBitsEqual(first, second, "repeated reimport");
        Rotation3d expectedInverse = original.inverse();
        Rotation3d importedInverse = first.inverse();
        assertRawBitsEqual(expectedInverse, importedInverse, "imported inverse");
        Rotation3d reconstructedInverse = Rotation3d.reconstructExact(importedInverse.x(), importedInverse.y(), importedInverse.z(), importedInverse.w());
        assertRawBitsEqual(importedInverse, reconstructedInverse, "inverse reimport");
        Rotation3d repeatedInverse = importedInverse.inverse();
        Rotation3d reconstructedRepeatedInverse = Rotation3d.reconstructExact(
                repeatedInverse.x(), repeatedInverse.y(), repeatedInverse.z(), repeatedInverse.w());
        assertRawBitsEqual(repeatedInverse, reconstructedRepeatedInverse, "repeated inverse reimport");

        Rotation3d otherOriginal = new Rotation3d(-3.0D, 5.0D, 2.0D, 7.0D);
        Rotation3d importedOther = Rotation3d.reconstructExact(otherOriginal.x(), otherOriginal.y(), otherOriginal.z(), otherOriginal.w());
        Vec3d zero = new Vec3d(0.0D, 0.0D, 0.0D);
        Rotation3d expectedComposition = new RigidTransform3d(original, zero)
                .andThen(new RigidTransform3d(otherOriginal, zero)).rotation();
        Rotation3d importedComposition = new RigidTransform3d(first, zero)
                .andThen(new RigidTransform3d(importedOther, zero)).rotation();
        assertRawBitsEqual(expectedComposition, importedComposition, "imported successful composition");
        Rotation3d reconstructedComposition = Rotation3d.reconstructExact(
                importedComposition.x(), importedComposition.y(), importedComposition.z(), importedComposition.w());
        assertRawBitsEqual(importedComposition, reconstructedComposition, "composition reimport");
    }

    private static void assertMatchesIndependentReference(double x, double y, double z, double w) {
        Rotation3d reference = independentReference(x, y, z, w);
        if (reference == null) {
            assertThrows(IllegalArgumentException.class, () -> Rotation3d.reconstructExact(x, y, z, w),
                    () -> "reference rejected tuple=" + rawBits(x, y, z, w));
            return;
        }
        Rotation3d actual = Rotation3d.reconstructExact(x, y, z, w);
        assertRawBitsEqual(reference, actual, "BigDecimal reference tuple=" + rawBits(x, y, z, w));
    }

    private static double randomFiniteComponent(SplittableRandom random) {
        double magnitude = Math.scalb(0.5D + random.nextDouble() * 0.5D, -1022 + random.nextInt(2046));
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    private static void assertReconstructs(Rotation3d output, String context) {
        assertRawBitsEqual(output, Rotation3d.reconstructExact(output.x(), output.y(), output.z(), output.w()),
                context + ", outputBits=" + rawBits(output));
    }

    private static void assertRawBitsEqual(Rotation3d expected, Rotation3d actual, String context) {
        assertEquals(Double.doubleToRawLongBits(expected.x()), Double.doubleToRawLongBits(actual.x()), context + ", component=x");
        assertEquals(Double.doubleToRawLongBits(expected.y()), Double.doubleToRawLongBits(actual.y()), context + ", component=y");
        assertEquals(Double.doubleToRawLongBits(expected.z()), Double.doubleToRawLongBits(actual.z()), context + ", component=z");
        assertEquals(Double.doubleToRawLongBits(expected.w()), Double.doubleToRawLongBits(actual.w()), context + ", component=w");
    }

    private static Rotation3d independentReference(double x, double y, double z, double w) {
        double[] target = {x, y, z, w};
        double c = Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.max(Math.abs(z), Math.abs(w)));
        double[][] candidates = new double[4][];
        for (int index = 0; index < target.length; index++) {
            candidates[index] = target[index] == 0.0D
                    ? c == 0.5D ? new double[] {-Double.MIN_VALUE, 0.0D, Double.MIN_VALUE} : new double[] {0.0D}
                    : independentCellCandidates(Math.abs(target[index]), c, target[index] < 0.0D);
        }
        for (int pivot = 0; pivot < target.length; pivot++) {
            if (Math.abs(target[pivot]) != c) continue;
            for (double first : candidates[(pivot + 1) & 3]) {
                for (double second : candidates[(pivot + 2) & 3]) {
                    for (double third : candidates[(pivot + 3) & 3]) {
                        double[] witness = new double[4];
                        witness[pivot] = target[pivot] < 0.0D ? -1.0D : 1.0D;
                        witness[(pivot + 1) & 3] = first;
                        witness[(pivot + 2) & 3] = second;
                        witness[(pivot + 3) & 3] = third;
                        Rotation3d actual = new Rotation3d(witness[0], witness[1], witness[2], witness[3]);
                        if (rawBits(actual).equals(rawBits(x, y, z, w))) return actual;
                    }
                }
            }
        }
        return null;
    }

    private static double[] independentCellCandidates(double magnitude, double c, boolean negative) {
        BigDecimal lowerEndpoint = exact(magnitude).add(exact(Math.nextDown(magnitude)));
        BigDecimal upperEndpoint = exact(magnitude).add(exact(Math.nextUp(magnitude)));
        BigDecimal twiceC = exact(c).multiply(BigDecimal.TWO);
        long low = 0L;
        long high = ONE_BITS;
        while (low < high) {
            long middle = (low + high) >>> 1;
            if (exact(Double.longBitsToDouble(middle)).multiply(twiceC).compareTo(lowerEndpoint) >= 0) high = middle;
            else low = middle + 1L;
        }
        double[] values = new double[3];
        int count = 0;
        while (low <= ONE_BITS && exact(Double.longBitsToDouble(low)).multiply(twiceC).compareTo(upperEndpoint) <= 0) {
            assertTrue(count < 3, () -> "reference inverse rounding cell exceeded three values for magnitude=" + magnitude + ", c=" + c);
            double value = Double.longBitsToDouble(low);
            values[count++] = negative ? -value : value;
            if (low == ONE_BITS) break;
            low++;
        }
        assertTrue(count > 0, () -> "reference inverse rounding cell was empty for magnitude=" + magnitude + ", c=" + c);
        double[] result = new double[count];
        System.arraycopy(values, 0, result, 0, count);
        return result;
    }

    private static void assertRawCellCandidates(double magnitude, double c, double... expected) {
        double[] actual = independentCellCandidates(magnitude, c, false);
        assertEquals(expected.length, actual.length, () -> "candidate count for magnitude=" + magnitude + ", c=" + c);
        for (int index = 0; index < expected.length; index++) {
            int candidateIndex = index;
            assertEquals(Double.doubleToRawLongBits(expected[index]), Double.doubleToRawLongBits(actual[index]),
                    () -> "candidate index=" + candidateIndex + ", magnitude=" + magnitude + ", c=" + c);
        }
    }

    private static void assertExactEndpoint(double candidate, double c, double magnitude, boolean upper) {
        double endpoint = magnitude + (upper ? Math.nextUp(magnitude) : Math.nextDown(magnitude));
        BigDecimal product = exact(candidate).multiply(exact(c).multiply(BigDecimal.TWO));
        assertEquals(0, product.compareTo(exact(endpoint)),
                () -> "candidate=" + candidate + ", c=" + c + ", magnitude=" + magnitude + ", upper=" + upper + ", endpoint=" + endpoint);
    }

    private static BigDecimal exact(double value) { return new BigDecimal(value); }

    private static String propertyContext(String route, long seed, int iteration, double... source) {
        return "route=" + route + ", seed=0x" + Long.toHexString(seed) + ", iteration=" + iteration + ", sourceBits=" + rawBits(source);
    }

    private static String rawBits(Rotation3d rotation) {
        return rawBits(rotation.x(), rotation.y(), rotation.z(), rotation.w());
    }

    private static String rawBits(double... values) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index != 0) result.append(',');
            result.append(Long.toHexString(Double.doubleToRawLongBits(values[index])));
        }
        return result.toString();
    }
}
