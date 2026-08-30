package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrimitiveValidationTest {
    @Test
    void acceptsAllSpecifiedDegenerateClosedGeometry() {
        Vec3d point = new Vec3d(1.0D, 2.0D, 3.0D);
        assertDoesNotThrow(() -> new Aabb(point, point));
        assertDoesNotThrow(() -> new Sphere(point, 0.0D));
        assertDoesNotThrow(() -> new Obb(point, new Vec3d(0.0D, 1.0D, 0.0D), Rotation3d.identity()));
        Segment3d segment = assertDoesNotThrow(() -> new Segment3d(point, point));
        assertDoesNotThrow(() -> new Capsule(segment, 0.0D));
        assertDoesNotThrow(() -> new Obb(point, new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity()));
        assertDoesNotThrow(() -> new Obb(point, new Vec3d(0.0D, 0.0D, 1.0D), Rotation3d.identity()));
        assertDoesNotThrow(() -> new Capsule(segment, 2.0D));
    }

    @Test
    void rejectsInvalidPrimitiveInputsAndUnrepresentableMandatoryBounds() {
        Vec3d origin = new Vec3d(0.0D, 0.0D, 0.0D);
        assertThrows(IllegalArgumentException.class, () -> new Aabb(new Vec3d(1.0D, 0.0D, 0.0D), origin));
        assertThrows(IllegalArgumentException.class, () -> new Sphere(origin, -1.0D));
        assertThrows(IllegalArgumentException.class, () -> new Obb(origin, new Vec3d(-1.0D, 0.0D, 0.0D), Rotation3d.identity()));
        assertThrows(IllegalArgumentException.class, () -> new Capsule(new Segment3d(origin, origin), -1.0D));
        assertThrows(NullPointerException.class, () -> new Aabb(null, origin));
        assertThrows(NullPointerException.class, () -> new Aabb(origin, null));
        assertThrows(NullPointerException.class, () -> new Sphere(null, 0.0D));
        assertThrows(NullPointerException.class, () -> new Segment3d(null, origin));
        assertThrows(NullPointerException.class, () -> new Segment3d(origin, null));
        assertThrows(NullPointerException.class, () -> new Capsule(null, 0.0D));
        assertThrows(NullPointerException.class, () -> new Obb(null, new Vec3d(0.0D, 0.0D, 0.0D), Rotation3d.identity()));
        assertThrows(NullPointerException.class, () -> new Obb(origin, null, Rotation3d.identity()));
        assertThrows(NullPointerException.class, () -> new Obb(origin, new Vec3d(0.0D, 0.0D, 0.0D), null));
        assertThrows(IllegalArgumentException.class, () -> new Sphere(origin, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Sphere(origin, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Sphere(origin, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Capsule(new Segment3d(origin, origin), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Capsule(new Segment3d(origin, origin), Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Capsule(new Segment3d(origin, origin), Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new Sphere(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), Double.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new Capsule(new Segment3d(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D)), Double.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new Obb(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), Rotation3d.identity()));
        assertThrows(IllegalArgumentException.class, () -> new Segment3d(new Vec3d(-Double.MAX_VALUE, 0.0D, 0.0D), new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D)));
    }

    @Test
    void segmentEndpointOrderIsCanonicalAndCapsuleLengthTerminologyIsExplicit() {
        Segment3d forward = new Segment3d(new Vec3d(3.0D, 0.0D, 0.0D), new Vec3d(1.0D, 0.0D, 0.0D));
        Segment3d reverse = new Segment3d(new Vec3d(1.0D, 0.0D, 0.0D), new Vec3d(3.0D, 0.0D, 0.0D));
        Capsule capsule = new Capsule(forward, 0.5D);

        assertEquals(forward, reverse);
        assertEquals(new Vec3d(1.0D, 0.0D, 0.0D), forward.start());
        assertEquals(new Vec3d(3.0D, 0.0D, 0.0D), forward.end());
        assertEquals(new Vec3d(2.0D, 0.0D, 0.0D), forward.delta());
        assertEquals(2.0D, capsule.centerlineLength());
        assertEquals(3.0D, capsule.exteriorLength());
    }
}
