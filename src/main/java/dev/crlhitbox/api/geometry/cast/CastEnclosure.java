package dev.crlhitbox.api.geometry.cast;

/** Private finite enclosures; a chosen-pair bound is never a minimum-distance lower bound. */
record CastEnclosure(double lower, double upper) {
    static final CastEnclosure ZERO = new CastEnclosure(0.0, 0.0);

    CastEnclosure {
        if (!Double.isFinite(lower) || !Double.isFinite(upper)) {
            throw new ArithmeticException("finite enclosure unavailable");
        }
        if (lower > upper) throw new IllegalArgumentException("reversed enclosure");
        lower = lower == 0.0 ? 0.0 : lower;
        upper = upper == 0.0 ? 0.0 : upper;
    }

    static CastEnclosure point(double value) {
        return new CastEnclosure(value, value);
    }

    boolean isZero() {
        return lower == 0.0 && upper == 0.0;
    }

    CastEnclosure negate() {
        return new CastEnclosure(-upper, -lower);
    }

    CastEnclosure add(CastEnclosure other) {
        if (isZero()) return other;
        if (other.isZero()) return this;
        return new CastEnclosure(Math.nextDown(lower + other.lower), Math.nextUp(upper + other.upper));
    }

    /** Singleton operands need no temporary record; retain the boxed endpoint expression graph. */
    CastEnclosure add(double other) {
        other = finitePoint(other);
        if (isZero()) return point(other);
        if (other == 0.0) return this;
        return new CastEnclosure(Math.nextDown(lower + other), Math.nextUp(upper + other));
    }

    CastEnclosure subtract(CastEnclosure other) {
        if (lower == upper && other.lower == other.upper && lower == other.lower) return ZERO;
        return add(other.negate());
    }

    CastEnclosure subtract(double other) {
        other = finitePoint(other);
        if (lower == upper && lower == other) return ZERO;
        return add(-other);
    }

    CastEnclosure multiply(CastEnclosure other) {
        return multiply(other.lower, other.upper);
    }

    CastEnclosure multiply(double other) {
        other = finitePoint(other);
        return multiply(other, other);
    }

    private CastEnclosure multiply(double otherLower, double otherUpper) {
        if (isZero() || otherLower == 0.0 && otherUpper == 0.0) return ZERO;
        double p0 = lower * otherLower;
        double p1 = lower * otherUpper;
        double p2 = upper * otherLower;
        double p3 = upper * otherUpper;
        double low = Math.nextDown(Math.min(Math.min(p0, p1), Math.min(p2, p3)));
        double high = Math.nextUp(Math.max(Math.max(p0, p1), Math.max(p2, p3)));
        if (lower >= 0.0 && otherLower >= 0.0 || upper <= 0.0 && otherUpper <= 0.0) {
            low = Math.max(0.0, low);
        }
        if (lower >= 0.0 && otherUpper <= 0.0 || upper <= 0.0 && otherLower >= 0.0) {
            high = Math.min(0.0, high);
        }
        return new CastEnclosure(low, high);
    }

    CastEnclosure divide(CastEnclosure other) {
        return divide(other.lower, other.upper);
    }

    CastEnclosure divide(double other) {
        other = finitePoint(other);
        return divide(other, other);
    }

    private CastEnclosure divide(double otherLower, double otherUpper) {
        if (otherLower <= 0.0 && otherUpper >= 0.0) {
            throw new ArithmeticException("division enclosure contains zero");
        }
        if (isZero()) return ZERO;
        double q0 = lower / otherLower;
        double q1 = lower / otherUpper;
        double q2 = upper / otherLower;
        double q3 = upper / otherUpper;
        double low = Math.nextDown(Math.min(Math.min(q0, q1), Math.min(q2, q3)));
        double high = Math.nextUp(Math.max(Math.max(q0, q1), Math.max(q2, q3)));
        if (lower >= 0.0 && otherLower > 0.0 || upper <= 0.0 && otherUpper < 0.0) low = Math.max(0.0, low);
        if (lower >= 0.0 && otherUpper < 0.0 || upper <= 0.0 && otherLower > 0.0) high = Math.min(0.0, high);
        return new CastEnclosure(low, high);
    }

    CastEnclosure square() {
        if (isZero()) return ZERO;
        if (lower == upper && Math.abs(lower) == 1.0) return point(1.0);
        double smallest = lower <= 0.0 && upper >= 0.0 ? 0.0 : Math.min(Math.abs(lower), Math.abs(upper));
        double largest = Math.max(Math.abs(lower), Math.abs(upper));
        return new CastEnclosure(Math.max(0.0, Math.nextDown(smallest * smallest)), Math.nextUp(largest * largest));
    }

    CastEnclosure sqrt() {
        if (lower < 0.0) throw new IllegalArgumentException("negative square-root enclosure");
        if (isZero() || lower == 1.0 && upper == 1.0) return this;
        return new CastEnclosure(Math.max(0.0, Math.nextDown(Math.sqrt(lower))), Math.nextUp(Math.sqrt(upper)));
    }

    static CastEnclosure norm(CastEnclosure x, CastEnclosure y, CastEnclosure z) {
        CastEnclosure squared = x.square().add(y.square()).add(z.square());
        // A sum of squares is nonnegative even when outward addition crosses zero.
        return new CastEnclosure(Math.max(0.0, squared.lower), squared.upper).sqrt();
    }

    static CastEnclosure norm(CastEnclosure[] vector) {
        return norm(vector[0], vector[1], vector[2]);
    }

    static CastEnclosure dot(CastEnclosure[] vector, double[] axis) {
        return dot(vector[0], vector[1], vector[2], axis);
    }

    static CastEnclosure dot(CastEnclosure x, CastEnclosure y, CastEnclosure z, double[] axis) {
        return x.multiply(axis[0]).add(y.multiply(axis[1])).add(z.multiply(axis[2]));
    }

    static CastEnclosure minimum(CastEnclosure first, CastEnclosure second) {
        return new CastEnclosure(Math.min(first.lower, second.lower), Math.min(first.upper, second.upper));
    }

    double absoluteUpper() {
        return Math.max(Math.abs(lower), Math.abs(upper));
    }

    private static double finitePoint(double value) {
        if (!Double.isFinite(value)) throw new ArithmeticException("finite enclosure unavailable");
        return value == 0.0 ? 0.0 : value;
    }
}
