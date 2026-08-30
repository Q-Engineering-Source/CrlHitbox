package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Rotation3dTest {
    @Test
    void identityAndPrincipalAxisRotationHaveExpectedBases() {
        Rotation3d identity = Rotation3d.identity();
        assertEquals(new Vec3d(1.0D, 2.0D, 3.0D), identity.rotate(new Vec3d(1.0D, 2.0D, 3.0D)));

        double halfSqrt = Math.sqrt(0.5D);
        Rotation3d quarterTurnAroundZ = new Rotation3d(0.0D, 0.0D, halfSqrt, halfSqrt);
        GeometryTestSupport.assertVectorClose(new Vec3d(0.0D, 1.0D, 0.0D), quarterTurnAroundZ.rotate(new Vec3d(1.0D, 0.0D, 0.0D)));
        GeometryTestSupport.assertVectorClose(new Vec3d(0.0D, 1.0D, 0.0D), quarterTurnAroundZ.basisX());
        GeometryTestSupport.assertVectorClose(new Vec3d(-1.0D, 0.0D, 0.0D), quarterTurnAroundZ.basisY());
        GeometryTestSupport.assertVectorClose(new Vec3d(0.0D, 0.0D, 1.0D), quarterTurnAroundZ.basisZ());

        Rotation3d quarterTurnAroundX = new Rotation3d(halfSqrt, 0.0D, 0.0D, halfSqrt);
        GeometryTestSupport.assertVectorClose(new Vec3d(0.0D, 0.0D, 1.0D), quarterTurnAroundX.rotate(new Vec3d(0.0D, 1.0D, 0.0D)));
        Rotation3d quarterTurnAroundY = new Rotation3d(0.0D, halfSqrt, 0.0D, halfSqrt);
        GeometryTestSupport.assertVectorClose(new Vec3d(0.0D, 0.0D, -1.0D), quarterTurnAroundY.rotate(new Vec3d(1.0D, 0.0D, 0.0D)));
    }

    @Test
    void normalizesScaleAndCanonicalizesQuaternionSignAndSignedZero() {
        Rotation3d rotation = new Rotation3d(0.0D, 0.0D, 2.0D, 2.0D);
        Rotation3d negated = new Rotation3d(-0.0D, -0.0D, -2.0D, -2.0D);
        Rotation3d huge = new Rotation3d(0.0D, 0.0D, Double.MAX_VALUE, Double.MAX_VALUE);
        Rotation3d tiny = new Rotation3d(0.0D, 0.0D, Double.MIN_VALUE, Double.MIN_VALUE);

        assertEquals(rotation, negated);
        assertEquals(rotation, huge);
        assertEquals(rotation, tiny);
        assertEquals(0.0D, rotation.x());
        assertEquals(0.0D, rotation.y());
        GeometryTestSupport.assertClose(Math.sqrt(0.5D), rotation.z());
        GeometryTestSupport.assertClose(Math.sqrt(0.5D), rotation.w());
    }

    @Test
    void inverseRoundTripsAndPreservesVectorLength() {
        Rotation3d rotation = new Rotation3d(1.0D, -2.0D, 3.0D, 4.0D);
        Vec3d source = new Vec3d(7.0D, -11.0D, 13.0D);
        Vec3d rotated = rotation.rotate(source);

        GeometryTestSupport.assertVectorClose(source, rotation.inverseRotate(rotated));
        GeometryTestSupport.assertVectorClose(source, rotation.inverse().rotate(rotated));
        GeometryTestSupport.assertClose(source.lengthSquared(), rotated.lengthSquared());
        GeometryTestSupport.assertClose(1.0D, rotation.basisX().lengthSquared());
        GeometryTestSupport.assertClose(0.0D, rotation.basisX().dot(rotation.basisY()));
    }

    @Test
    void rejectsInvalidRawQuaternions() {
        assertThrows(IllegalArgumentException.class, () -> new Rotation3d(0.0D, 0.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class, () -> new Rotation3d(Double.NaN, 0.0D, 0.0D, 1.0D));
        assertThrows(IllegalArgumentException.class, () -> new Rotation3d(0.0D, 0.0D, 0.0D, Double.POSITIVE_INFINITY));
    }

    @Test
    void projectivelyEquivalentExtremeBinary64QuaternionsHaveExactCanonicalValueSemantics() {
        double x = Math.scalb(1.0D, 900);
        double y = Math.scalb(-2.0D, 899);
        double z = Math.scalb(3.0D, 898);
        double w = Math.scalb(5.0D, 897);
        Rotation3d positive = new Rotation3d(x, y, z, w);
        Rotation3d negative = new Rotation3d(-x, -y, -z, -w);
        Rotation3d tinyPositive = new Rotation3d(Math.scalb(x, -1800), Math.scalb(y, -1800), Math.scalb(z, -1800), Math.scalb(w, -1800));
        Rotation3d tinyNegative = new Rotation3d(Math.scalb(-x, -1800), Math.scalb(-y, -1800), Math.scalb(-z, -1800), Math.scalb(-w, -1800));

        assertEquals(positive, negative);
        assertEquals(positive.hashCode(), negative.hashCode());
        assertEquals(positive, tinyPositive);
        assertEquals(positive.hashCode(), tinyPositive.hashCode());
        assertEquals(tinyPositive, tinyNegative);
        assertEquals(tinyPositive.hashCode(), tinyNegative.hashCode());
    }
}
