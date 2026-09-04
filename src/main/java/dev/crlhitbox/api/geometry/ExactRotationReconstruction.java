package dev.crlhitbox.api.geometry;

import java.math.BigInteger;

/** Complete bounded inverse-rounding-cell witness enumeration for {@link Rotation3d}. */
final class ExactRotationReconstruction {
    private static final long NEGATIVE_ZERO_BITS = Long.MIN_VALUE;
    private static final long ONE_BITS = Double.doubleToRawLongBits(1.0D);
    private static final BigInteger SCALE = BigInteger.ONE.shiftLeft(1074);

    private ExactRotationReconstruction() { }

    static Rotation3d reconstruct(double x, double y, double z, double w) {
        validateInput(x, y, z, w);
        double[] target = {x, y, z, w};
        double c = maximumMagnitude(target);
        if (c < 0.5D || c > 1.0D) {
            throw new IllegalArgumentException("rotation is outside the legacy constructor image magnitude range");
        }

        CandidateSet[] candidates = new CandidateSet[4];
        for (int index = 0; index < target.length; index++) {
            candidates[index] = target[index] == 0.0D
                    ? zeroCandidates(c)
                    : candidatesForNonzero(Math.abs(target[index]), c, target[index] < 0.0D);
        }

        int constructorVerifications = 0;
        for (int pivot = 0; pivot < target.length; pivot++) {
            if (Math.abs(target[pivot]) != c) continue;
            double pivotValue = target[pivot] < 0.0D ? -1.0D : 1.0D;
            for (int first = 0; first < candidates[(pivot + 1) & 3].size; first++) {
                for (int second = 0; second < candidates[(pivot + 2) & 3].size; second++) {
                    for (int third = 0; third < candidates[(pivot + 3) & 3].size; third++) {
                        double[] witness = new double[4];
                        witness[pivot] = pivotValue;
                        witness[(pivot + 1) & 3] = candidates[(pivot + 1) & 3].value(first);
                        witness[(pivot + 2) & 3] = candidates[(pivot + 2) & 3].value(second);
                        witness[(pivot + 3) & 3] = candidates[(pivot + 3) & 3].value(third);
                        if (++constructorVerifications > 108) {
                            throw new IllegalStateException("inverse rounding witness enumeration exceeded its constructor verification bound");
                        }
                        Rotation3d actual = new Rotation3d(witness[0], witness[1], witness[2], witness[3]);
                        if (matchesRawBits(actual, target)) return actual;
                    }
                }
            }
        }
        throw new IllegalArgumentException("rotation is not an exact legacy constructor output");
    }

    private static void validateInput(double x, double y, double z, double w) {
        double[] values = {x, y, z, w};
        boolean anyNonzero = false;
        for (double value : values) {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("rotation components must be finite");
            if (Double.doubleToRawLongBits(value) == NEGATIVE_ZERO_BITS) {
                throw new IllegalArgumentException("rotation components must not use negative zero");
            }
            anyNonzero |= value != 0.0D;
        }
        if (!anyNonzero) throw new IllegalArgumentException("rotation must not be zero");
        if (shouldNegate(x, y, z, w)) throw new IllegalArgumentException("rotation sign is not canonical");
    }

    private static boolean shouldNegate(double x, double y, double z, double w) {
        return w < 0.0D || w == 0.0D && (x < 0.0D || x == 0.0D && (y < 0.0D || y == 0.0D && z < 0.0D));
    }

    private static double maximumMagnitude(double[] values) {
        return Math.max(Math.max(Math.abs(values[0]), Math.abs(values[1])), Math.max(Math.abs(values[2]), Math.abs(values[3])));
    }

    private static CandidateSet candidatesForNonzero(double magnitude, double c, boolean negative) {
        long magnitudeBits = Double.doubleToRawLongBits(magnitude);
        BigInteger target = dyadicMagnitude(magnitudeBits);
        BigInteger previous = dyadicMagnitude(Double.doubleToRawLongBits(Math.nextDown(magnitude)));
        BigInteger next = dyadicMagnitude(Double.doubleToRawLongBits(Math.nextUp(magnitude)));
        BigInteger twiceC = dyadicMagnitude(Double.doubleToRawLongBits(c)).shiftLeft(1);
        BigInteger lower = target.add(previous).multiply(SCALE);
        BigInteger upper = target.add(next).multiply(SCALE);
        long bits = lowerBound(twiceC, lower);
        CandidateSet result = new CandidateSet();
        while (bits <= ONE_BITS && withinUpperBound(twiceC, dyadicMagnitude(bits), upper)) {
            if (result.size == 3) throw new IllegalStateException("inverse rounding cell exceeded its proved candidate bound");
            double value = Double.longBitsToDouble(bits);
            result.add(negative ? -value : value);
            if (bits == ONE_BITS) break;
            bits++;
        }
        return result;
    }

    private static CandidateSet zeroCandidates(double c) {
        return c == 0.5D ? CandidateSet.ZERO_CELL : CandidateSet.EXACT_ZERO;
    }

    // The exact lower-cell predicate D * B >= A starts at B = ceil(A / D).
    private static long lowerBound(BigInteger twiceC, BigInteger lower) {
        int commonPowerOfTwo = Math.min(twiceC.getLowestSetBit(), lower.getLowestSetBit());
        BigInteger divisor = commonPowerOfTwo == 0 ? twiceC : twiceC.shiftRight(commonPowerOfTwo);
        BigInteger dividend = commonPowerOfTwo == 0 ? lower : lower.shiftRight(commonPowerOfTwo);
        BigInteger[] quotientAndRemainder = dividend.divideAndRemainder(divisor);
        BigInteger firstInteger = quotientAndRemainder[1].signum() == 0
                ? quotientAndRemainder[0]
                : quotientAndRemainder[0].add(BigInteger.ONE);
        if (firstInteger.signum() <= 0 || firstInteger.compareTo(SCALE) > 0) {
            throw new IllegalStateException("inverse rounding cell lower bound is outside the binary64 unit interval");
        }

        BigInteger representable = ceilToBinary64Lattice(firstInteger);
        if (representable.compareTo(SCALE) > 0 || twiceC.multiply(representable).compareTo(lower) < 0) {
            throw new IllegalStateException("inverse rounding cell lower bound cannot be represented in the binary64 unit interval");
        }
        return encodePositiveUnitIntervalDyadic(representable);
    }

    // Above 53 bits, round upward to the current binary64 significand lattice; a carry may enter the next binade.
    private static BigInteger ceilToBinary64Lattice(BigInteger value) {
        int bitLength = value.bitLength();
        if (bitLength <= 53) return value;
        int shift = bitLength - 53;
        BigInteger lattice = BigInteger.ONE.shiftLeft(shift);
        BigInteger[] quotientAndRemainder = value.divideAndRemainder(lattice);
        BigInteger rounded = quotientAndRemainder[1].signum() == 0
                ? quotientAndRemainder[0]
                : quotientAndRemainder[0].add(BigInteger.ONE);
        return rounded.shiftLeft(shift);
    }

    private static long encodePositiveUnitIntervalDyadic(BigInteger value) {
        if (value.signum() <= 0 || value.compareTo(SCALE) > 0) {
            throw new IllegalStateException("binary64 unit-interval dyadic is outside its valid range");
        }
        if (value.bitLength() <= 52) return value.longValueExact();

        int exponent = value.bitLength() - 52;
        if (exponent < 1 || exponent > 1023) {
            throw new IllegalStateException("binary64 unit-interval dyadic has an invalid normal exponent");
        }
        BigInteger significand = value.shiftRight(exponent - 1);
        BigInteger hiddenBit = BigInteger.ONE.shiftLeft(52);
        if (significand.compareTo(hiddenBit) < 0 || significand.bitLength() > 53
                || !value.equals(significand.shiftLeft(exponent - 1))) {
            throw new IllegalStateException("binary64 unit-interval dyadic is not on its normal significand lattice");
        }
        long fraction = significand.subtract(hiddenBit).longValueExact();
        long rawBits = ((long) exponent << 52) | fraction;
        if (rawBits > ONE_BITS || !dyadicMagnitude(rawBits).equals(value)) {
            throw new IllegalStateException("binary64 unit-interval dyadic encoding was not exact");
        }
        return rawBits;
    }

    private static boolean withinUpperBound(BigInteger twiceC, BigInteger candidate, BigInteger upper) {
        return twiceC.multiply(candidate).compareTo(upper) <= 0;
    }

    private static BigInteger dyadicMagnitude(long positiveBits) {
        long fraction = positiveBits & 0x000f_ffff_ffff_ffffL;
        int exponent = (int) ((positiveBits >>> 52) & 0x7ffL);
        if (exponent == 0) return BigInteger.valueOf(fraction);
        return BigInteger.valueOf(fraction | (1L << 52)).shiftLeft(exponent - 1);
    }

    private static boolean matchesRawBits(Rotation3d actual, double[] target) {
        return Double.doubleToRawLongBits(actual.x()) == Double.doubleToRawLongBits(target[0])
                && Double.doubleToRawLongBits(actual.y()) == Double.doubleToRawLongBits(target[1])
                && Double.doubleToRawLongBits(actual.z()) == Double.doubleToRawLongBits(target[2])
                && Double.doubleToRawLongBits(actual.w()) == Double.doubleToRawLongBits(target[3]);
    }

    private static final class CandidateSet {
        private static final CandidateSet ZERO_CELL = of(-Double.MIN_VALUE, 0.0D, Double.MIN_VALUE);
        private static final CandidateSet EXACT_ZERO = of(0.0D);

        private final double[] values = new double[3];
        private int size;

        private static CandidateSet of(double first, double second, double third) {
            CandidateSet result = new CandidateSet();
            result.add(first);
            result.add(second);
            result.add(third);
            return result;
        }

        private static CandidateSet of(double only) {
            CandidateSet result = new CandidateSet();
            result.add(only);
            return result;
        }

        private void add(double value) { values[size++] = value; }
        private double value(int index) { return values[index]; }
    }
}
