package dev.crlhitbox.api.geometry;

/** Fixed-size, per-axis-scaled local representation shared by segment/box queries. */
final class NormalizedSegmentBox {
    private final Axis x;
    private final Axis y;
    private final Axis z;

    private NormalizedSegmentBox(Axis x, Axis y, Axis z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static NormalizedSegmentBox fromAabb(Segment3d segment, Aabb box) { return fromAabb(segment, box, 0.0D); }

    static NormalizedSegmentBox fromAabb(Segment3d segment, Aabb box, double additionalMagnitude) {
        Vec3d start = segment.start();
        Vec3d end = segment.end();
        Vec3d min = box.min();
        Vec3d max = box.max();
        return new NormalizedSegmentBox(
                Axis.fromAabb(start.x(), end.x(), min.x(), max.x()),
                Axis.fromAabb(start.y(), end.y(), min.y(), max.y()),
                Axis.fromAabb(start.z(), end.z(), min.z(), max.z())
        );
    }

    static NormalizedSegmentBox fromObb(Segment3d segment, Obb box) { return fromObb(segment, box, 0.0D); }

    static NormalizedSegmentBox fromObb(Segment3d segment, Obb box, double additionalMagnitude) {
        Vec3d start = segment.start();
        Vec3d delta = segment.delta();
        Vec3d center = box.center();
        Vec3d half = box.halfExtents();
        return new NormalizedSegmentBox(
                Axis.fromObb(start, delta, center, half.x(), box.orientation().basisX()),
                Axis.fromObb(start, delta, center, half.y(), box.orientation().basisY()),
                Axis.fromObb(start, delta, center, half.z(), box.orientation().basisZ())
        );
    }

    static NormalizedSegmentBox fromParentEndpoints(
            Vec3d parentStart,
            Vec3d parentEnd,
            RigidIntervalBox box
    ) {
        try {
            Vec3d localStart = box.localToParent().inverseTransformPoint(parentStart);
            Vec3d localEnd = box.localToParent().inverseTransformPoint(parentEnd);
            Vec3d minimum = box.localMin();
            Vec3d maximum = box.localMax();
            return new NormalizedSegmentBox(
                    Axis.fromLocalEndpoints(localStart.x(), localEnd.x(), minimum.x(), maximum.x()),
                    Axis.fromLocalEndpoints(localStart.y(), localEnd.y(), minimum.y(), maximum.y()),
                    Axis.fromLocalEndpoints(localStart.z(), localEnd.z(), minimum.z(), maximum.z())
            );
        } catch (IllegalArgumentException unrepresentableDirectMapping) {
            // A relative scaled projection can remain answerable when point-translation or the
            // inverse-rotated result cannot itself be represented as a finite public Vec3d.
            return fromScaledParentEndpoints(parentStart, parentEnd, box);
        }
    }

    private static NormalizedSegmentBox fromScaledParentEndpoints(
            Vec3d parentStart,
            Vec3d parentEnd,
            RigidIntervalBox box
    ) {
        Vec3d origin = box.parentOrigin();
        Vec3d minimum = box.localMin();
        Vec3d maximum = box.localMax();
        return new NormalizedSegmentBox(
                Axis.fromRigidIntervalBox(parentStart, parentEnd, origin, minimum.x(), maximum.x(), box.parentBasisX()),
                Axis.fromRigidIntervalBox(parentStart, parentEnd, origin, minimum.y(), maximum.y(), box.parentBasisY()),
                Axis.fromRigidIntervalBox(parentStart, parentEnd, origin, minimum.z(), maximum.z(), box.parentBasisZ())
        );
    }

    boolean intersects() {
        double low = 0.0D;
        double high = 1.0D;
        Axis[] axes = {x, y, z};
        for (Axis axis : axes) {
            if (axis.delta == 0.0D) {
                if (axis.start < axis.min || axis.start > axis.max) return false;
                continue;
            }
            double first = (axis.min - axis.start) / axis.delta;
            double second = (axis.max - axis.start) / axis.delta;
            low = Math.max(low, Math.min(first, second));
            high = Math.min(high, Math.max(first, second));
            if (low > high) return false;
        }
        return true;
    }

    double squaredDistance() {
        if (intersects()) return 0.0D;
        return minimumNorm().squared();
    }

    boolean withinRadius(double radius) { return minimumNorm().lessOrEqual(radius); }

    private ScaledNorm minimumNorm() {
        double[] breakpoints = new double[8];
        int count = 2;
        breakpoints[0] = 0.0D;
        breakpoints[1] = 1.0D;
        count = addCrossings(breakpoints, count, x);
        count = addCrossings(breakpoints, count, y);
        count = addCrossings(breakpoints, count, z);
        sort(breakpoints, count);
        int unique = deduplicate(breakpoints, count);
        ScaledNorm minimum = ScaledNorm.infinity();
        for (int index = 0; index < unique; index++) minimum = minimum.minimum(normAt(breakpoints[index]));
        for (int index = 0; index + 1 < unique; index++) {
            double low = breakpoints[index];
            double high = breakpoints[index + 1];
            if (!(low < high)) continue;
            double middle = low + (high - low) * 0.5D;
            Axis[] axes = {x, y, z};
            double[] offsets = new double[3];
            boolean[] active = new boolean[3];
            int commonExponent = Integer.MIN_VALUE;
            for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
                Axis axis = axes[axisIndex];
                double point = Math.fma(axis.delta, middle, axis.start);
                if (point < axis.min) { offsets[axisIndex] = axis.start - axis.min; active[axisIndex] = true; }
                else if (point > axis.max) { offsets[axisIndex] = axis.start - axis.max; active[axisIndex] = true; }
                if (active[axisIndex]) {
                    commonExponent = Math.max(commonExponent, axis.termExponent(offsets[axisIndex]));
                    commonExponent = Math.max(commonExponent, axis.termExponent(axis.delta));
                }
            }
            if (commonExponent == Integer.MIN_VALUE) return ScaledNorm.zero();
            double coefficient = 0.0D;
            double linear = 0.0D;
            for (int axisIndex = 0; axisIndex < 3; axisIndex++) if (active[axisIndex]) {
                Axis axis = axes[axisIndex];
                double offset = axis.scaledTo(offsets[axisIndex], commonExponent);
                double delta = axis.scaledTo(axis.delta, commonExponent);
                coefficient = Math.fma(delta, delta, coefficient);
                linear = Math.fma(offset, delta, linear);
            }
            if (coefficient != 0.0D) {
                double stationary = -linear / coefficient;
                if (Double.isFinite(stationary) && stationary > low && stationary < high) minimum = minimum.minimum(normAt(stationary));
            }
        }
        return minimum;
    }

    private ScaledNorm normAt(double parameter) {
        double residualX = x.residual(parameter);
        double residualY = y.residual(parameter);
        double residualZ = z.residual(parameter);
        int commonExponent = Math.max(x.termExponent(residualX), Math.max(y.termExponent(residualY), z.termExponent(residualZ)));
        if (commonExponent == Integer.MIN_VALUE) return ScaledNorm.zero();
        double scaledX = x.scaledTo(residualX, commonExponent);
        double scaledY = y.scaledTo(residualY, commonExponent);
        double scaledZ = z.scaledTo(residualZ, commonExponent);
        return ScaledNorm.of(Math.sqrt(Math.fma(scaledX, scaledX, Math.fma(scaledY, scaledY, scaledZ * scaledZ))), commonExponent);
    }

    private static int addCrossings(double[] breakpoints, int count, Axis axis) {
        if (axis.delta == 0.0D) return count;
        count = addIfDomainBreakpoint(breakpoints, count, (axis.min - axis.start) / axis.delta);
        return addIfDomainBreakpoint(breakpoints, count, (axis.max - axis.start) / axis.delta);
    }

    private static int addIfDomainBreakpoint(double[] breakpoints, int count, double value) {
        if (Double.isFinite(value) && value >= 0.0D && value <= 1.0D) breakpoints[count++] = value == 0.0D ? 0.0D : value;
        return count;
    }

    private static void sort(double[] values, int count) {
        for (int index = 1; index < count; index++) {
            double value = values[index];
            int insertion = index;
            while (insertion > 0 && value < values[insertion - 1]) { values[insertion] = values[insertion - 1]; insertion--; }
            values[insertion] = value;
        }
    }

    private static int deduplicate(double[] values, int count) {
        int unique = 1;
        for (int index = 1; index < count; index++) if (values[index] != values[unique - 1]) values[unique++] = values[index];
        return unique;
    }

    private static final class Axis {
        private final double start;
        private final double delta;
        private final double min;
        private final double max;
        private final int exponent;

        private Axis(double start, double delta, double min, double max, int exponent) {
            this.start = start;
            this.delta = delta;
            this.min = min;
            this.max = max;
            this.exponent = exponent;
        }

        static Axis fromAabb(double start, double end, double min, double max) {
            return fromLocalEndpoints(start, end, min, max);
        }

        static Axis fromLocalEndpoints(double start, double end, double min, double max) {
            int exponent = Math.max(differenceExponent(end, start), Math.max(differenceExponent(min, start), differenceExponent(max, start)));
            if (exponent == Integer.MIN_VALUE) exponent = 0;
            return new Axis(0.0D, scaledDifference(end, start, exponent), scaledDifference(min, start, exponent), scaledDifference(max, start, exponent), exponent);
        }

        static Axis fromObb(Vec3d start, Vec3d delta, Vec3d center, double halfExtent, Vec3d basis) {
            int exponent = magnitudeExponent(halfExtent);
            if (basis.x() != 0.0D) exponent = Math.max(exponent, Math.max(differenceProductExponent(start.x(), center.x(), basis.x()), productExponent(delta.x(), basis.x())));
            if (basis.y() != 0.0D) exponent = Math.max(exponent, Math.max(differenceProductExponent(start.y(), center.y(), basis.y()), productExponent(delta.y(), basis.y())));
            if (basis.z() != 0.0D) exponent = Math.max(exponent, Math.max(differenceProductExponent(start.z(), center.z(), basis.z()), productExponent(delta.z(), basis.z())));
            if (exponent == Integer.MIN_VALUE) exponent = 0;
            return new Axis(scaledDotDifference(start, center, basis, exponent), scaledDot(delta, basis, exponent), -Math.scalb(halfExtent, -exponent), Math.scalb(halfExtent, -exponent), exponent);
        }

        static Axis fromRigidIntervalBox(
                Vec3d parentStart,
                Vec3d parentEnd,
                Vec3d parentOrigin,
                double minimum,
                double maximum,
                Vec3d basis
        ) {
            int exponent = Math.max(
                    Math.max(magnitudeExponent(minimum), magnitudeExponent(maximum)),
                    Math.max(
                            dotDifferenceExponent(parentStart, parentOrigin, basis),
                            dotDifferenceExponent(parentEnd, parentStart, basis)));
            if (exponent == Integer.MIN_VALUE) exponent = 0;
            return new Axis(
                    scaledDotDifference(parentStart, parentOrigin, basis, exponent),
                    scaledDotDifference(parentEnd, parentStart, basis, exponent),
                    Math.scalb(minimum, -exponent),
                    Math.scalb(maximum, -exponent),
                    exponent);
        }

        double residual(double parameter) {
            double value = Math.fma(delta, parameter, start);
            return value < min ? value - min : value > max ? value - max : 0.0D;
        }

        int termExponent(double value) {
            int magnitude = magnitudeExponent(value);
            return magnitude == Integer.MIN_VALUE ? Integer.MIN_VALUE : exponent + magnitude;
        }

        double scaledTo(double value, int commonExponent) { return Math.scalb(value, exponent - commonExponent); }
    }

    private static final class ScaledNorm {
        private final double magnitude;
        private final int exponent;

        private ScaledNorm(double magnitude, int exponent) {
            this.magnitude = magnitude;
            this.exponent = exponent;
        }

        static ScaledNorm zero() { return new ScaledNorm(0.0D, 0); }
        static ScaledNorm infinity() { return new ScaledNorm(Double.POSITIVE_INFINITY, Integer.MAX_VALUE); }

        static ScaledNorm of(double magnitude, int exponent) {
            if (magnitude == 0.0D) return zero();
            int magnitudeExponent = magnitudeExponent(magnitude);
            return new ScaledNorm(Math.scalb(magnitude, -magnitudeExponent), exponent + magnitudeExponent);
        }

        ScaledNorm minimum(ScaledNorm other) {
            if (magnitude == 0.0D || other.magnitude == 0.0D) return magnitude == 0.0D ? this : other;
            if (exponent != other.exponent) return exponent < other.exponent ? this : other;
            return magnitude <= other.magnitude ? this : other;
        }

        boolean lessOrEqual(double radius) {
            if (magnitude == 0.0D) return true;
            int radiusExponent = magnitudeExponent(radius);
            if (radiusExponent == Integer.MIN_VALUE) return false;
            if (exponent != radiusExponent) return exponent < radiusExponent;
            return magnitude <= Math.scalb(radius, -radiusExponent);
        }

        double squared() {
            if (magnitude == 0.0D) return 0.0D;
            return Math.scalb(magnitude * magnitude, 2 * exponent);
        }
    }

    private static int differenceExponent(double first, double second) {
        double difference = first - second;
        return Double.isFinite(difference) ? magnitudeExponent(difference) : Math.max(magnitudeExponent(first), magnitudeExponent(second));
    }

    private static int productExponent(double first, double second) {
        int firstExponent = magnitudeExponent(first);
        int secondExponent = magnitudeExponent(second);
        return firstExponent == Integer.MIN_VALUE || secondExponent == Integer.MIN_VALUE ? Integer.MIN_VALUE : firstExponent + secondExponent;
    }

    private static int differenceProductExponent(double first, double second, double coefficient) {
        int differenceExponent = differenceExponent(first, second);
        int coefficientExponent = magnitudeExponent(coefficient);
        return differenceExponent == Integer.MIN_VALUE || coefficientExponent == Integer.MIN_VALUE ? Integer.MIN_VALUE : differenceExponent + coefficientExponent;
    }

    private static int dotDifferenceExponent(Vec3d first, Vec3d second, Vec3d basis) {
        int exponent = Integer.MIN_VALUE;
        if (basis.x() != 0.0D) exponent = Math.max(exponent, differenceProductExponent(first.x(), second.x(), basis.x()));
        if (basis.y() != 0.0D) exponent = Math.max(exponent, differenceProductExponent(first.y(), second.y(), basis.y()));
        if (basis.z() != 0.0D) exponent = Math.max(exponent, differenceProductExponent(first.z(), second.z(), basis.z()));
        return exponent;
    }

    private static int magnitudeExponent(double value) {
        double magnitude = Math.abs(value);
        if (magnitude == 0.0D) return Integer.MIN_VALUE;
        if (magnitude >= Double.MIN_NORMAL) return Math.getExponent(magnitude);
        return Math.getExponent(Math.scalb(magnitude, 52)) - 52;
    }

    private static double scaledDifference(double first, double second, int exponent) {
        double difference = first - second;
        return Double.isFinite(difference) ? Math.scalb(difference, -exponent) : Math.scalb(first, -exponent) - Math.scalb(second, -exponent);
    }

    private static double scaledDotDifference(Vec3d first, Vec3d second, Vec3d basis, int exponent) {
        return compensatedSum(
                basis.x() == 0.0D ? 0.0D : scaledDifferenceProduct(first.x(), second.x(), basis.x(), exponent),
                basis.y() == 0.0D ? 0.0D : scaledDifferenceProduct(first.y(), second.y(), basis.y(), exponent),
                basis.z() == 0.0D ? 0.0D : scaledDifferenceProduct(first.z(), second.z(), basis.z(), exponent)
        );
    }

    private static double scaledDot(Vec3d value, Vec3d basis, int exponent) {
        return compensatedSum(
                basis.x() == 0.0D ? 0.0D : scaledProduct(value.x(), basis.x(), exponent),
                basis.y() == 0.0D ? 0.0D : scaledProduct(value.y(), basis.y(), exponent),
                basis.z() == 0.0D ? 0.0D : scaledProduct(value.z(), basis.z(), exponent)
        );
    }

    private static double scaledProduct(double first, double second, int exponent) {
        int firstExponent = magnitudeExponent(first);
        int secondExponent = magnitudeExponent(second);
        if (firstExponent == Integer.MIN_VALUE || secondExponent == Integer.MIN_VALUE) return 0.0D;
        return Math.scalb(Math.scalb(first, -firstExponent) * Math.scalb(second, -secondExponent), firstExponent + secondExponent - exponent);
    }

    private static double scaledDifferenceProduct(double first, double second, double coefficient, int exponent) {
        int differenceExponent = differenceExponent(first, second);
        int coefficientExponent = magnitudeExponent(coefficient);
        if (differenceExponent == Integer.MIN_VALUE || coefficientExponent == Integer.MIN_VALUE) return 0.0D;
        return Math.scalb(scaledDifference(first, second, differenceExponent) * Math.scalb(coefficient, -coefficientExponent), differenceExponent + coefficientExponent - exponent);
    }

    private static double compensatedSum(double first, double second, double third) {
        double sum = first;
        double correctedSecond = second;
        double afterSecond = sum + correctedSecond;
        double compensation = (afterSecond - sum) - correctedSecond;
        double correctedThird = third - compensation;
        return afterSecond + correctedThird;
    }
}
