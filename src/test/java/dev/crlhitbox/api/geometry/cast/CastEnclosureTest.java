package dev.crlhitbox.api.geometry.cast;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;
import java.util.function.Supplier;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CastEnclosureTest {
    @Test
    void scalarSingletonStorageKeepsBitsFailuresAndIndependentDyadicBounds() {
        double[][] ranges = {{-3, -0.125}, {-2, 4}, {0.125, 7}, {0, 0}, {-0.0, -0.0},
                {1, 1}, {-1, -1}, {-Double.MIN_NORMAL, Double.MIN_NORMAL},
                {Double.MIN_VALUE, Math.nextUp(Double.MIN_VALUE)}, {Double.MAX_VALUE, Double.MAX_VALUE}};
        double[] scalars = {-3, -1, -0.125, -0.0, 0.0, 0.125, 1, 2, Double.MIN_VALUE,
                -Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY};
        for (double[] range : ranges) for (double scalar : scalars) {
            var value = new CastEnclosure(range[0], range[1]);
            sameBoundsOrFailure(() -> value.add(CastEnclosure.point(scalar)), () -> value.add(scalar));
            sameBoundsOrFailure(() -> value.subtract(CastEnclosure.point(scalar)), () -> value.subtract(scalar));
            sameBoundsOrFailure(() -> value.multiply(CastEnclosure.point(scalar)), () -> value.multiply(scalar));
            sameBoundsOrFailure(() -> value.divide(CastEnclosure.point(scalar)), () -> value.divide(scalar));
            if (Double.isFinite(scalar) && Math.abs(scalar) <= 3 && Math.abs(range[0]) <= 3 && Math.abs(range[1]) <= 7) {
                for (double endpoint : range) {
                    encloses(value.add(scalar), exact(endpoint).add(exact(scalar)));
                    encloses(value.subtract(scalar), exact(endpoint).subtract(exact(scalar)));
                    encloses(value.multiply(scalar), exact(endpoint).multiply(exact(scalar)));
                }
            }
        }
    }

    private static void sameBoundsOrFailure(Supplier<CastEnclosure> boxed, Supplier<CastEnclosure> scalar) {
        CastEnclosure expected;
        try { expected = boxed.get(); }
        catch (ArithmeticException failure) {
            assertThrows(ArithmeticException.class, scalar::get);
            return;
        }
        var actual = scalar.get();
        assertEquals(Double.doubleToRawLongBits(expected.lower()), Double.doubleToRawLongBits(actual.lower()));
        assertEquals(Double.doubleToRawLongBits(expected.upper()), Double.doubleToRawLongBits(actual.upper()));
    }

    @Test
    void mixedSignEndpointOperationsEncloseExactDyadicReferences() {
        double[][] ranges = {{-3, -0.125}, {-2, 4}, {0.125, 7}, {-Double.MIN_NORMAL, Double.MIN_NORMAL},
                {Double.MIN_VALUE, Math.nextUp(Double.MIN_VALUE)}, {0, 0}};
        for (double[] x : ranges) for (double[] y : ranges) {
            var a = new CastEnclosure(x[0], x[1]);
            var b = new CastEnclosure(y[0], y[1]);
            for (double p : x) for (double q : y) {
                encloses(a.add(b), exact(p).add(exact(q)));
                encloses(a.subtract(b), exact(p).subtract(exact(q)));
                encloses(a.multiply(b), exact(p).multiply(exact(q)));
                if (y[0] > 0 || y[1] < 0) {
                    // Cross-multiply instead of trusting a rounded high-precision quotient.
                    // Avoid real quotient overflow, which has its own explicit failure test.
                    if (Double.isFinite(p / q) && Math.abs(p / q) < Double.MAX_VALUE / 2) {
                        var quotient = CastEnclosure.point(p).divide(CastEnclosure.point(q));
                        BigDecimal lowerProduct = exact(quotient.lower()).multiply(exact(q));
                        BigDecimal upperProduct = exact(quotient.upper()).multiply(exact(q));
                        assertTrue(q > 0 ? lowerProduct.compareTo(exact(p)) <= 0 : lowerProduct.compareTo(exact(p)) >= 0);
                        assertTrue(q > 0 ? upperProduct.compareTo(exact(p)) >= 0 : upperProduct.compareTo(exact(p)) <= 0);
                    }
                }
            }
        }
    }

    @Test
    void uncertainCancellationStaysUncertainAndInvalidArithmeticFailsExplicitly() {
        var uncertain = new CastEnclosure(Math.nextDown(1.0), Math.nextUp(1.0));
        var difference = uncertain.subtract(uncertain);
        assertTrue(difference.lower() < 0 && difference.upper() > 0);
        assertEquals(0L, Double.doubleToRawLongBits(CastEnclosure.point(-0.0).lower()));
        assertThrows(ArithmeticException.class, () -> CastEnclosure.point(1).divide(new CastEnclosure(-1, 1)));
        assertThrows(ArithmeticException.class, () -> CastEnclosure.point(Double.MAX_VALUE).multiply(CastEnclosure.point(2)));
        assertThrows(IllegalArgumentException.class, () -> new CastEnclosure(2, 1));
    }

    @Test
    void normAndSqrtBoundsAreCheckedByExactSquaresNotApproximateSquareRoots() {
        var random = new Random(0x4e4f524dL);
        for (int i = 0; i < 512; i++) {
            double x = Math.scalb(random.nextDouble() - 0.5, random.nextInt(1100) - 1074);
            double y = Math.scalb(random.nextDouble() - 0.5, random.nextInt(1100) - 1074);
            double z = Math.scalb(random.nextDouble() - 0.5, random.nextInt(1100) - 1074);
            var norm = CastEnclosure.norm(CastEnclosure.point(x), CastEnclosure.point(y), CastEnclosure.point(z));
            BigDecimal squared = exact(x).pow(2).add(exact(y).pow(2)).add(exact(z).pow(2));
            assertTrue(exact(norm.lower()).pow(2).compareTo(squared) <= 0);
            assertTrue(exact(norm.upper()).pow(2).compareTo(squared) >= 0);
        }
    }

    @Test
    void largeOriginComposedSupportUsesTheActualPerturbedAxisAndExactPath() {
        var a = new Vec3d(30_000_002, 30_000_003, 30_000_004);
        var b = new Vec3d(30_000_003, 30_000_001, 30_000_002);
        var movement = new Vec3d(-0.75, 0.125, -0.5);
        var box = new Aabb(new Vec3d(30_000_000, 30_000_000, 30_000_000), new Vec3d(30_000_001, 30_000_001, 30_000_001));
        for (double radius : new double[]{0, 0.25}) {
            var context = CapsuleAabbCastKernel.Context.create(new Capsule(new Segment3d(a, b), radius), movement, box);
            double[] axis = {1, Math.nextUp(-0.5), 0.25};
            var norm = CastEnclosure.norm(CastEnclosure.point(axis[0]), CastEnclosure.point(axis[1]), CastEnclosure.point(axis[2]));
            BigDecimal normSquared = exact(axis[0]).pow(2).add(exact(axis[1]).pow(2)).add(exact(axis[2]).pow(2));
            assertTrue(exact(norm.lower()).pow(2).compareTo(normSquared) <= 0);
            assertTrue(exact(norm.upper()).pow(2).compareTo(normSquared) >= 0);
            for (double t : new double[]{0, 0.125, 0.7, 1}) {
                var gap = context.gap(context.at(context.start, t), context.at(context.end, t), axis, norm);
                BigDecimal dotA = exactLocalDot(a, movement, box.min(), axis, t);
                BigDecimal dotB = exactLocalDot(b, movement, box.min(), axis, t);
                BigDecimal support = exact(1).add(exact(0.25));
                BigDecimal base = dotA.min(dotB).subtract(support);
                // Independent sqrt bracket [1,2] would be too coarse to compare enclosure endpoints.
                // For radius>0, prove each bound via squaring the required norm inequality instead.
                if (radius == 0) encloses(gap, base);
                else {
                    BigDecimal lowRequired = base.subtract(exact(gap.lower())).divide(exact(radius));
                    BigDecimal highRequired = base.subtract(exact(gap.upper())).divide(exact(radius));
                    assertTrue(lowRequired.signum() >= 0 && lowRequired.pow(2).compareTo(normSquared) >= 0);
                    assertTrue(highRequired.signum() <= 0 || highRequired.pow(2).compareTo(normSquared) <= 0);
                }
            }
        }
    }

    private static BigDecimal exactLocalDot(Vec3d point, Vec3d delta, Vec3d origin, double[] axis, double t) {
        double[] p = {point.x(), point.y(), point.z()};
        double[] d = {delta.x(), delta.y(), delta.z()};
        double[] o = {origin.x(), origin.y(), origin.z()};
        BigDecimal result = BigDecimal.ZERO;
        for (int i = 0; i < 3; i++) result = result.add(exact(p[i]).subtract(exact(o[i]))
                .add(exact(t).multiply(exact(d[i]))).multiply(exact(axis[i])));
        return result;
    }

    private static BigDecimal exact(double value) { return new BigDecimal(value); }

    private static void encloses(CastEnclosure value, BigDecimal exact) {
        assertTrue(new BigDecimal(value.lower()).compareTo(exact) <= 0);
        assertTrue(new BigDecimal(value.upper()).compareTo(exact) >= 0);
    }

    @Test
    void normOfTwoSubnormalComponentsEnclosesItsExactSquaredLength() {
        var value = CastEnclosure.point(Double.MIN_VALUE);
        var norm = CastEnclosure.norm(value, value, CastEnclosure.ZERO);
        BigDecimal exactSquared = new BigDecimal(Double.MIN_VALUE).pow(2).multiply(BigDecimal.valueOf(2));
        assertTrue(norm.lower() >= 0.0);
        assertTrue(new BigDecimal(norm.lower()).pow(2).compareTo(exactSquared) <= 0);
        assertTrue(new BigDecimal(norm.upper()).pow(2).compareTo(exactSquared) >= 0);
    }

    @Test
    void composedSupportAndNegativeSlopContainAnIndependentExactDyadicValue() {
        double penetration = Math.scalb(1.0, -31);
        CastEnclosure center = CastEnclosure.point(-0.25 + penetration);
        CastEnclosure axisNorm = CastEnclosure.norm(CastEnclosure.point(-1.0), CastEnclosure.ZERO, CastEnclosure.ZERO);
        CastEnclosure gap = center.multiply(CastEnclosure.point(-1.0))
                .subtract(CastEnclosure.point(0.25).multiply(axisNorm));
        BigDecimal exactGap = new BigDecimal(penetration).negate();
        assertTrue(new BigDecimal(gap.lower()).compareTo(exactGap) <= 0);
        assertTrue(new BigDecimal(gap.upper()).compareTo(exactGap) >= 0);
        assertTrue(gap.add(CastEnclosure.point(8e-10).multiply(CastEnclosure.point(axisNorm.lower()))).lower() >= 0.0);
        assertEquals(CastEnclosure.ZERO, CastEnclosure.point(0.125).subtract(CastEnclosure.point(0.125)));
        assertEquals(CastEnclosure.ZERO, CastEnclosure.ZERO.multiply(CastEnclosure.point(-1.0)).negate());
        assertEquals(CastEnclosure.point(1.0), CastEnclosure.point(1.0).sqrt());
        CastEnclosure ratio = CastEnclosure.point(0.75).divide(CastEnclosure.point(2.0));
        assertTrue(ratio.lower() <= 0.375 && ratio.upper() >= 0.375);
    }

    @Test
    void underflowingNonzeroProductRemainsEnclosedInsteadOfBecomingExactZero() {
        CastEnclosure product = CastEnclosure.point(Double.MIN_VALUE).multiply(CastEnclosure.point(0.5));
        BigDecimal exact = new BigDecimal(Double.MIN_VALUE).divide(BigDecimal.valueOf(2));
        assertTrue(new BigDecimal(product.lower()).compareTo(exact) <= 0);
        assertTrue(new BigDecimal(product.upper()).compareTo(exact) >= 0);
        assertTrue(product.upper() > 0.0);
        assertEquals(0.0, product.lower());
    }
}
