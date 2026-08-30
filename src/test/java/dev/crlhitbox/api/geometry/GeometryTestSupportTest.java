package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeometryTestSupportTest {
    @Test
    void finiteExpectationsDoNotAcceptNanOrInfinityButMatchingInfinityDoes() {
        assertThrows(AssertionError.class, () -> GeometryTestSupport.assertClose(1.0D, Double.NaN, "nan actual"));
        assertThrows(AssertionError.class, () -> GeometryTestSupport.assertClose(1.0D, Double.POSITIVE_INFINITY, "infinite actual"));
        assertThrows(AssertionError.class, () -> GeometryTestSupport.assertClose(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, "opposite infinities"));
        assertDoesNotThrow(() -> GeometryTestSupport.assertClose(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, "matching infinity"));
        assertDoesNotThrow(() -> GeometryTestSupport.assertClose(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, "matching negative infinity"));
    }
}
