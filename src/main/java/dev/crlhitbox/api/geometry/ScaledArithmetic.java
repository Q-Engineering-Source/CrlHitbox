package dev.crlhitbox.api.geometry;

/** Package-private power-of-two scaling primitives for placed rigid-frame geometry. */
final class ScaledArithmetic {
    static final int ZERO_EXPONENT = Integer.MIN_VALUE;

    private ScaledArithmetic() {
    }

    static int magnitudeExponent(double value) {
        double magnitude = Math.abs(value);
        if (magnitude == 0.0D) return ZERO_EXPONENT;
        if (magnitude >= Double.MIN_NORMAL) return Math.getExponent(magnitude);
        return Math.getExponent(Math.scalb(magnitude, 52)) - 52;
    }

    static int productExponent(double first, double second) {
        int firstExponent = magnitudeExponent(first);
        int secondExponent = magnitudeExponent(second);
        return firstExponent == ZERO_EXPONENT || secondExponent == ZERO_EXPONENT
                ? ZERO_EXPONENT
                : firstExponent + secondExponent + 2;
    }

    static int differenceExponent(double first, double second) {
        double difference = first - second;
        return Double.isFinite(difference)
                ? magnitudeExponent(difference)
                : Math.max(magnitudeExponent(first), magnitudeExponent(second));
    }

    static int differenceProductExponent(double first, double second, double coefficient) {
        int differenceExponent = differenceExponent(first, second);
        int coefficientExponent = magnitudeExponent(coefficient);
        return differenceExponent == ZERO_EXPONENT || coefficientExponent == ZERO_EXPONENT
                ? ZERO_EXPONENT
                : differenceExponent + coefficientExponent + 2;
    }

    static int maximumExponent(int... exponents) {
        int maximum = ZERO_EXPONENT;
        for (int exponent : exponents) maximum = Math.max(maximum, exponent);
        return maximum;
    }

    static double scaledProduct(double first, double second, int commonExponent) {
        int firstExponent = magnitudeExponent(first);
        int secondExponent = magnitudeExponent(second);
        if (firstExponent == ZERO_EXPONENT || secondExponent == ZERO_EXPONENT) return 0.0D;
        return Math.scalb(
                Math.scalb(first, -firstExponent) * Math.scalb(second, -secondExponent),
                firstExponent + secondExponent - commonExponent);
    }

    static double scaledDifferenceProduct(
            double first,
            double second,
            double coefficient,
            int commonExponent
    ) {
        int differenceExponent = differenceExponent(first, second);
        int coefficientExponent = magnitudeExponent(coefficient);
        if (differenceExponent == ZERO_EXPONENT || coefficientExponent == ZERO_EXPONENT) return 0.0D;
        return Math.scalb(
                scaledDifference(first, second, differenceExponent)
                        * Math.scalb(coefficient, -coefficientExponent),
                differenceExponent + coefficientExponent - commonExponent);
    }

    static double scaledDifference(double first, double second, int exponent) {
        double difference = first - second;
        return Double.isFinite(difference)
                ? Math.scalb(difference, -exponent)
                : Math.scalb(first, -exponent) - Math.scalb(second, -exponent);
    }

    static double compensatedSum(double... values) {
        double sum = 0.0D;
        double correction = 0.0D;
        for (double value : values) {
            double next = sum + value;
            correction += Math.abs(sum) >= Math.abs(value)
                    ? sum - next + value
                    : value - next + sum;
            sum = next;
        }
        return sum + correction;
    }
}
