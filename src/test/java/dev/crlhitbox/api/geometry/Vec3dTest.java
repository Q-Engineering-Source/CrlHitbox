package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Vec3dTest {
    @Test
    void exposesCanonicalFiniteComponentsAndExactValueSemantics() {
        Vec3d vector = new Vec3d(-0.0D, 2.0D, -3.0D);

        assertEquals(0.0D, vector.x());
        assertEquals(2.0D, vector.y());
        assertEquals(-3.0D, vector.z());
        assertEquals(new Vec3d(0.0D, 2.0D, -3.0D), vector);
        assertEquals(new Vec3d(0.0D, 2.0D, -3.0D).hashCode(), vector.hashCode());
        assertNotEquals(new Vec3d(0.0D, 2.0D, 3.0D), vector);
        assertEquals("Vec3d[x=0.0, y=2.0, z=-3.0]", vector.toString());
    }

    @Test
    void rejectsNonFiniteComponents() {
        assertThrows(IllegalArgumentException.class, () -> new Vec3d(Double.NaN, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new Vec3d(0.0D, Double.POSITIVE_INFINITY, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new Vec3d(0.0D, 0.0D, Double.NEGATIVE_INFINITY));
    }

    @Test
    void performsVectorArithmeticWithoutMutatingInputs() {
        Vec3d first = new Vec3d(1.0D, 2.0D, 3.0D);
        Vec3d second = new Vec3d(-4.0D, 5.0D, -6.0D);

        assertEquals(new Vec3d(-3.0D, 7.0D, -3.0D), first.add(second));
        assertEquals(new Vec3d(5.0D, -3.0D, 9.0D), first.subtract(second));
        assertEquals(new Vec3d(-1.0D, -2.0D, -3.0D), first.negate());
        assertEquals(new Vec3d(2.0D, 4.0D, 6.0D), first.multiply(2.0D));
        assertEquals(-12.0D, first.dot(second));
        assertEquals(new Vec3d(-27.0D, -6.0D, 13.0D), first.cross(second));
        assertEquals(14.0D, first.lengthSquared());
        assertEquals(115.0D, first.distanceSquared(second));
        assertEquals(new Vec3d(-4.0D, 2.0D, -6.0D), first.min(second));
        assertEquals(new Vec3d(1.0D, 5.0D, 3.0D), first.max(second));
        assertEquals(new Vec3d(1.0D, 2.0D, 3.0D), first);
        assertEquals(new Vec3d(-4.0D, 5.0D, -6.0D), second);
    }

    @Test
    void rejectsDerivedNonFiniteVectorsButPermitsSquaredOverflow() {
        Vec3d maximum = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);

        assertThrows(IllegalArgumentException.class, () -> maximum.add(maximum));
        assertThrows(IllegalArgumentException.class, () -> maximum.multiply(2.0D));
        assertEquals(Double.POSITIVE_INFINITY, maximum.lengthSquared());
    }
}
