package dev.crlhitbox.api.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/** Test-only floating-point assertions with a local scale-aware tolerance. */
final class GeometryTestSupport {
    private GeometryTestSupport() {
    }

    static void assertClose(double expected, double actual) {
        assertClose(expected, actual, "");
    }

    static void assertClose(double expected, double actual, String message) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            fail(message + ", expected and actual must both be non-NaN: expected=" + expected + ", actual=" + actual);
        }
        if (Double.isInfinite(expected) || Double.isInfinite(actual)) {
            assertEquals(expected, actual, message);
            return;
        }
        double scale = Math.max(Math.abs(expected), Math.abs(actual));
        double tolerance = Math.max(Math.ulp(scale) * 64.0D, Math.ulp(1.0D) * 64.0D);
        assertEquals(expected, actual, tolerance, message);
    }

    static void assertVectorClose(Vec3d expected, Vec3d actual) {
        assertVectorClose(expected, actual, "");
    }

    static void assertVectorClose(Vec3d expected, Vec3d actual, String message) {
        assertClose(expected.x(), actual.x(), message + ", component=x");
        assertClose(expected.y(), actual.y(), message + ", component=y");
        assertClose(expected.z(), actual.z(), message + ", component=z");
    }
}
