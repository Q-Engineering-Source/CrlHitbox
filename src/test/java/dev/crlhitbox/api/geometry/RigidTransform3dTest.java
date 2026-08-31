package dev.crlhitbox.api.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RigidTransform3dTest {
    @Test
    void constructorRetainsImmutableComponentsAndRejectsNulls() {
        Rotation3d rotation = new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D);
        Vec3d translation = new Vec3d(4.0D, -5.0D, 6.0D);
        RigidTransform3d transform = new RigidTransform3d(rotation, translation);

        assertSame(rotation, transform.rotation());
        assertSame(translation, transform.translation());
        assertThrows(NullPointerException.class, () -> new RigidTransform3d(null, translation));
        assertThrows(NullPointerException.class, () -> new RigidTransform3d(rotation, null));
    }

    @Test
    void identityMapsPointsAndVectorsExactlyAtOrdinaryAndExtremeFiniteScales() {
        RigidTransform3d identity = RigidTransform3d.identity();
        for (Vec3d value : new Vec3d[] {
                new Vec3d(1.0D, -2.0D, 3.0D),
                new Vec3d(Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_VALUE),
                new Vec3d(-0.0D, 0.0D, -0.0D)
        }) {
            assertEquals(value, identity.transformPoint(value));
            assertEquals(value, identity.transformVector(value));
            assertEquals(value, identity.inverseTransformPoint(value));
            assertEquals(value, identity.inverseTransformVector(value));
        }
        assertEquals(Rotation3d.identity(), identity.rotation());
        assertEquals(new Vec3d(0.0D, 0.0D, 0.0D), identity.translation());
        assertEquals(0L, Double.doubleToRawLongBits(identity.transformPoint(new Vec3d(-0.0D, 0.0D, -0.0D)).x()));
    }

    @Test
    void pureTranslationAffectsPointsButNeverVectors() {
        RigidTransform3d transform = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(4.0D, -5.0D, 6.0D));
        Vec3d input = new Vec3d(1.0D, 2.0D, 3.0D);

        assertEquals(new Vec3d(5.0D, -3.0D, 9.0D), transform.transformPoint(input));
        assertEquals(input, transform.transformVector(input));
    }

    @Test
    void quarterTurnAroundXUsesActivePointAndVectorMapping() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(1.0D, 0.0D, 0.0D, 1.0D),
                new Vec3d(2.0D, 3.0D, 4.0D));
        Vec3d vector = new Vec3d(0.0D, 1.0D, 0.0D);

        Phase1DTestSupport.assertVectorClose(new Vec3d(0.0D, 0.0D, 1.0D), transform.transformVector(vector), "x90 vector");
        Phase1DTestSupport.assertVectorClose(new Vec3d(2.0D, 3.0D, 5.0D), transform.transformPoint(vector), "x90 point");
    }

    @Test
    void quarterTurnAroundYUsesActivePointAndVectorMapping() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.0D, 1.0D, 0.0D, 1.0D),
                new Vec3d(-2.0D, 3.0D, 4.0D));
        Vec3d vector = new Vec3d(1.0D, 0.0D, 0.0D);

        Phase1DTestSupport.assertVectorClose(new Vec3d(0.0D, 0.0D, -1.0D), transform.transformVector(vector), "y90 vector");
        Phase1DTestSupport.assertVectorClose(new Vec3d(-2.0D, 3.0D, 3.0D), transform.transformPoint(vector), "y90 point");
    }

    @Test
    void quarterTurnAroundZUsesActivePointAndVectorMapping() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(2.0D, -3.0D, 4.0D));
        Vec3d vector = new Vec3d(1.0D, 0.0D, 0.0D);

        Phase1DTestSupport.assertVectorClose(new Vec3d(0.0D, 1.0D, 0.0D), transform.transformVector(vector), "z90 vector");
        Phase1DTestSupport.assertVectorClose(new Vec3d(2.0D, -2.0D, 4.0D), transform.transformPoint(vector), "z90 point");
    }

    @Test
    void pointDisplacementAgreesWithVectorMappingForModerateValues() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Vec3d point = new Vec3d(1.25D, -2.5D, 3.75D);
        Vec3d origin = new Vec3d(0.0D, 0.0D, 0.0D);

        Vec3d displacement = transform.transformPoint(point).subtract(transform.transformPoint(origin));
        Phase1DTestSupport.assertVectorClose(transform.transformVector(point), displacement, "point displacement/vector agreement");
    }

    @Test
    void mappingRejectsNullsIsDeterministicAndDoesNotMutateInputs() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.1D, 0.2D, -0.3D, 0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        Vec3d input = new Vec3d(7.0D, -8.0D, 9.0D);
        Vec3d inputSnapshot = new Vec3d(input.x(), input.y(), input.z());

        assertEquals(transform.transformPoint(input), transform.transformPoint(input));
        assertEquals(transform.transformVector(input), transform.transformVector(input));
        assertEquals(inputSnapshot, input);
        assertThrows(NullPointerException.class, () -> transform.transformPoint(null));
        assertThrows(NullPointerException.class, () -> transform.transformVector(null));
    }

    @Test
    void combinedTransformRoundTripsPointsAndVectorsInBothDirections() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Vec3d point = new Vec3d(1.25D, -2.5D, 3.75D);
        Vec3d vector = new Vec3d(-4.5D, 5.25D, -6.75D);

        Phase1DTestSupport.assertVectorClose(point, transform.inverseTransformPoint(transform.transformPoint(point)), "inverse(forward(point))");
        Phase1DTestSupport.assertVectorClose(point, transform.transformPoint(transform.inverseTransformPoint(point)), "forward(inverse(point))");
        Phase1DTestSupport.assertVectorClose(vector, transform.inverseTransformVector(transform.transformVector(vector)), "inverse(forward(vector))");
        Phase1DTestSupport.assertVectorClose(vector, transform.transformVector(transform.inverseTransformVector(vector)), "forward(inverse(vector))");
    }

    @Test
    void inversePointSubtractsTranslationBeforeApplyingInverseRotation() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(10.0D, 20.0D, 30.0D));
        Vec3d source = new Vec3d(1.0D, 2.0D, 3.0D);
        Vec3d target = transform.transformPoint(source);

        Phase1DTestSupport.assertVectorClose(source, transform.inverseTransformPoint(target), "R^-1(point - translation)");
    }

    @Test
    void pureTranslationHasExactInverseMappingAndInverseValue() {
        RigidTransform3d transform = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(4.0D, -8.0D, 16.0D));
        Vec3d point = new Vec3d(1.0D, 2.0D, 3.0D);
        Vec3d vector = new Vec3d(-5.0D, 6.0D, -7.0D);
        RigidTransform3d inverse = transform.inverse();

        assertEquals(new Vec3d(-3.0D, 10.0D, -13.0D), transform.inverseTransformPoint(point));
        assertEquals(vector, transform.inverseTransformVector(vector));
        assertEquals(Rotation3d.identity(), inverse.rotation());
        assertEquals(new Vec3d(-4.0D, 8.0D, -16.0D), inverse.translation());
    }

    @Test
    void pureRotationInverseUsesRotation3dInverseSemantics() {
        Rotation3d rotation = new Rotation3d(0.3D, -0.2D, 0.4D, 0.8D);
        RigidTransform3d transform = new RigidTransform3d(rotation, new Vec3d(0.0D, 0.0D, 0.0D));
        Vec3d input = new Vec3d(2.0D, -3.0D, 5.0D);

        Phase1DTestSupport.assertVectorClose(rotation.inverseRotate(input), transform.inverseTransformPoint(input), "pure rotation inverse point");
        Phase1DTestSupport.assertVectorClose(rotation.inverseRotate(input), transform.inverseTransformVector(input), "pure rotation inverse vector");
        assertEquals(rotation.inverse(), transform.inverse().rotation());
        assertEquals(new Vec3d(0.0D, 0.0D, 0.0D), transform.inverse().translation());
    }

    @Test
    void inverseValueObservationallyAgreesWithDirectInverseMethods() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.1D, 0.2D, -0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        Vec3d input = new Vec3d(7.0D, -8.0D, 9.0D);
        RigidTransform3d inverse = transform.inverse();

        Phase1DTestSupport.assertVectorClose(transform.inverseTransformPoint(input), inverse.transformPoint(input), "inverse value point");
        Phase1DTestSupport.assertVectorClose(transform.inverseTransformVector(input), inverse.transformVector(input), "inverse value vector");
        Phase1DTestSupport.assertVectorClose(transform.transformPoint(input), inverse.inverse().transformPoint(input), "double inverse point");
        Phase1DTestSupport.assertVectorClose(transform.transformVector(input), inverse.inverse().transformVector(input), "double inverse vector");
    }

    @Test
    void equivalentQuaternionSignsProduceEquivalentInverseMappings() {
        RigidTransform3d positive = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        RigidTransform3d negative = new RigidTransform3d(
                new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        Vec3d input = new Vec3d(7.0D, 8.0D, 9.0D);

        assertEquals(positive.inverseTransformPoint(input), negative.inverseTransformPoint(input));
        assertEquals(positive.inverseTransformVector(input), negative.inverseTransformVector(input));
        assertEquals(positive.inverse().rotation(), negative.inverse().rotation());
        assertEquals(positive.inverse().translation(), negative.inverse().translation());
    }

    @Test
    void inverseOperationsRejectNullsAreDeterministicAndDoNotMutateInputs() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.1D, 0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        Vec3d input = new Vec3d(7.0D, 8.0D, 9.0D);
        Vec3d snapshot = new Vec3d(input.x(), input.y(), input.z());

        assertEquals(transform.inverseTransformPoint(input), transform.inverseTransformPoint(input));
        assertEquals(transform.inverseTransformVector(input), transform.inverseTransformVector(input));
        assertEquals(transform.inverse().rotation(), transform.inverse().rotation());
        assertEquals(transform.inverse().translation(), transform.inverse().translation());
        assertEquals(snapshot, input);
        assertThrows(NullPointerException.class, () -> transform.inverseTransformPoint(null));
        assertThrows(NullPointerException.class, () -> transform.inverseTransformVector(null));
    }

    @Test
    void translationThenTranslationComposesExactlyInDeclaredOrder() {
        RigidTransform3d first = new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 2.0D, 3.0D));
        RigidTransform3d after = new RigidTransform3d(Rotation3d.identity(), new Vec3d(4.0D, 5.0D, 6.0D));
        RigidTransform3d composed = first.andThen(after);

        assertEquals(Rotation3d.identity(), composed.rotation());
        assertEquals(new Vec3d(5.0D, 7.0D, 9.0D), composed.translation());
        assertEquals(new Vec3d(6.0D, 8.0D, 10.0D), composed.transformPoint(new Vec3d(1.0D, 1.0D, 1.0D)));
    }

    @Test
    void translationThenRotationRotatesTheFirstTranslation() {
        RigidTransform3d first = new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D));
        RigidTransform3d after = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(0.0D, 0.0D, 0.0D));

        Phase1DTestSupport.assertVectorClose(new Vec3d(0.0D, 1.0D, 0.0D), first.andThen(after).translation(), "Rb(ta) + tb");
    }

    @Test
    void rotationThenTranslationDoesNotRotateTheLaterTranslation() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(0.0D, 0.0D, 0.0D));
        RigidTransform3d after = new RigidTransform3d(Rotation3d.identity(), new Vec3d(1.0D, 0.0D, 0.0D));

        assertEquals(new Vec3d(1.0D, 0.0D, 0.0D), first.andThen(after).translation());
    }

    @Test
    void noncommutingRotationsProveQuaternionMultiplicationOrder() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(1.0D, 0.0D, 0.0D, 1.0D),
                new Vec3d(1.0D, 2.0D, 3.0D));
        RigidTransform3d after = new RigidTransform3d(
                new Rotation3d(0.0D, 1.0D, 0.0D, 1.0D),
                new Vec3d(-4.0D, 5.0D, -6.0D));
        Vec3d asymmetric = new Vec3d(1.0D, 2.0D, 3.0D);

        Vec3d expectedPoint = after.transformPoint(first.transformPoint(asymmetric));
        Vec3d expectedVector = after.transformVector(first.transformVector(asymmetric));
        RigidTransform3d composed = first.andThen(after);

        Phase1DTestSupport.assertVectorClose(expectedPoint, composed.transformPoint(asymmetric), "noncommuting point order");
        Phase1DTestSupport.assertVectorClose(expectedVector, composed.transformVector(asymmetric), "noncommuting vector order");
    }

    @Test
    void combinedCompositionMatchesSequentialPointAndVectorApplication() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        RigidTransform3d after = new RigidTransform3d(
                new Rotation3d(-0.3D, 0.4D, 0.2D, 0.8D),
                new Vec3d(-7.0D, 8.0D, 9.0D));
        Vec3d input = new Vec3d(2.5D, -3.5D, 4.5D);
        RigidTransform3d composed = first.andThen(after);

        Phase1DTestSupport.assertVectorClose(after.transformPoint(first.transformPoint(input)), composed.transformPoint(input), "combined point composition");
        Phase1DTestSupport.assertVectorClose(after.transformVector(first.transformVector(input)), composed.transformVector(input), "combined vector composition");
    }

    @Test
    void identityIsAnObservationalLeftAndRightCompositionIdentity() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.1D, 0.2D, -0.3D, 0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        Vec3d input = new Vec3d(7.0D, -8.0D, 9.0D);

        Phase1DTestSupport.assertVectorClose(transform.transformPoint(input), RigidTransform3d.identity().andThen(transform).transformPoint(input), "identity then transform point");
        Phase1DTestSupport.assertVectorClose(transform.transformPoint(input), transform.andThen(RigidTransform3d.identity()).transformPoint(input), "transform then identity point");
        Phase1DTestSupport.assertVectorClose(transform.transformVector(input), RigidTransform3d.identity().andThen(transform).transformVector(input), "identity then transform vector");
        Phase1DTestSupport.assertVectorClose(transform.transformVector(input), transform.andThen(RigidTransform3d.identity()).transformVector(input), "transform then identity vector");
    }

    @Test
    void compositionWithInverseIsObservationallyIdentityInBothOrders() {
        RigidTransform3d transform = new RigidTransform3d(
                new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D),
                new Vec3d(5.0D, -7.0D, 11.0D));
        Vec3d input = new Vec3d(1.25D, -2.5D, 3.75D);

        Phase1DTestSupport.assertVectorClose(input, transform.andThen(transform.inverse()).transformPoint(input), "transform then inverse");
        Phase1DTestSupport.assertVectorClose(input, transform.inverse().andThen(transform).transformPoint(input), "inverse then transform");
    }

    @Test
    void inverseOfCompositionMatchesReverseCompositionOfInverses() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, -5.0D, 6.0D));
        RigidTransform3d after = new RigidTransform3d(
                new Rotation3d(-0.3D, 0.4D, 0.2D, 0.8D),
                new Vec3d(-7.0D, 8.0D, 9.0D));
        Vec3d input = new Vec3d(2.5D, -3.5D, 4.5D);

        RigidTransform3d expected = after.inverse().andThen(first.inverse());
        RigidTransform3d actual = first.andThen(after).inverse();
        Phase1DTestSupport.assertVectorClose(expected.transformPoint(input), actual.transformPoint(input), "inverse composition point");
        Phase1DTestSupport.assertVectorClose(expected.transformVector(input), actual.transformVector(input), "inverse composition vector");
    }

    @Test
    void threeTransformAssociativityIsObservationalNotExactValuePolicy() {
        RigidTransform3d first = new RigidTransform3d(new Rotation3d(0.1D, 0.2D, 0.3D, 0.9D), new Vec3d(1.0D, 2.0D, 3.0D));
        RigidTransform3d second = new RigidTransform3d(new Rotation3d(-0.2D, 0.3D, 0.1D, 0.8D), new Vec3d(-4.0D, 5.0D, 6.0D));
        RigidTransform3d third = new RigidTransform3d(new Rotation3d(0.3D, -0.1D, 0.2D, 0.7D), new Vec3d(7.0D, -8.0D, 9.0D));
        Vec3d input = new Vec3d(2.0D, -3.0D, 5.0D);

        RigidTransform3d leftGrouped = first.andThen(second).andThen(third);
        RigidTransform3d rightGrouped = first.andThen(second.andThen(third));
        Phase1DTestSupport.assertVectorClose(leftGrouped.transformPoint(input), rightGrouped.transformPoint(input), "three-transform point associativity");
        Phase1DTestSupport.assertVectorClose(leftGrouped.transformVector(input), rightGrouped.transformVector(input), "three-transform vector associativity");
    }

    @Test
    void compositionRejectsNullPreservesInputsAndCanonicalQuaternionSigns() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        RigidTransform3d equivalent = new RigidTransform3d(
                new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D),
                new Vec3d(4.0D, 5.0D, 6.0D));
        RigidTransform3d after = new RigidTransform3d(
                new Rotation3d(-0.3D, 0.4D, 0.2D, 0.8D),
                new Vec3d(-7.0D, 8.0D, 9.0D));
        Vec3d input = new Vec3d(2.5D, -3.5D, 4.5D);
        Rotation3d firstRotation = first.rotation();
        Vec3d firstTranslation = first.translation();
        Rotation3d afterRotation = after.rotation();
        Vec3d afterTranslation = after.translation();

        assertEquals(first.andThen(after).transformPoint(input), equivalent.andThen(after).transformPoint(input));
        assertSame(firstRotation, first.rotation());
        assertSame(firstTranslation, first.translation());
        assertSame(afterRotation, after.rotation());
        assertSame(afterTranslation, after.translation());
        assertThrows(NullPointerException.class, () -> first.andThen(null));
    }

    @Test
    void exactCanonicalValueSemanticsCoverQuaternionSignAndTranslationSignedZero() {
        RigidTransform3d first = new RigidTransform3d(
                new Rotation3d(0.1D, -0.2D, 0.3D, 0.9D),
                new Vec3d(-0.0D, 2.0D, -3.0D));
        RigidTransform3d equivalent = new RigidTransform3d(
                new Rotation3d(-0.1D, 0.2D, -0.3D, -0.9D),
                new Vec3d(0.0D, 2.0D, -3.0D));
        RigidTransform3d transitive = new RigidTransform3d(first.rotation(), first.translation());
        RigidTransform3d differentRotation = new RigidTransform3d(
                new Rotation3d(0.2D, -0.2D, 0.3D, 0.9D),
                first.translation());
        RigidTransform3d differentTranslation = new RigidTransform3d(
                first.rotation(),
                new Vec3d(0.0D, 2.0D, -4.0D));

        assertEquals(first, equivalent);
        assertEquals(equivalent, first, "symmetry");
        assertEquals(equivalent, transitive, "transitivity premise");
        assertEquals(first, transitive, "transitivity result");
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertEquals(0.0D, first.translation().x());
        assertNotEquals(first, differentRotation);
        assertNotEquals(first, differentTranslation);
        assertNotEquals(first, null);
        assertNotEquals(first, first.rotation());
        assertEquals(
                "RigidTransform3d[rotation=" + first.rotation() + ", translation=" + first.translation() + "]",
                first.toString());
    }

    @Test
    void identityInverseHasExactCanonicalValue() {
        assertEquals(RigidTransform3d.identity(), RigidTransform3d.identity().inverse());
        assertEquals(RigidTransform3d.identity().hashCode(), RigidTransform3d.identity().inverse().hashCode());
        assertEquals(RigidTransform3d.identity().toString(), RigidTransform3d.identity().inverse().toString());
    }

    @Test
    void largeRepresentableAndSubnormalMappingsRemainFinite() {
        double large = Double.MAX_VALUE / 16.0D;
        RigidTransform3d rotated = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(large, -large, large));
        Vec3d input = new Vec3d(large, large, -large);
        Vec3d mappedPoint = rotated.transformPoint(input);
        Vec3d mappedVector = rotated.transformVector(input);
        assertEquals(true, Double.isFinite(mappedPoint.x()) && Double.isFinite(mappedPoint.y()) && Double.isFinite(mappedPoint.z()));
        assertEquals(true, Double.isFinite(mappedVector.x()) && Double.isFinite(mappedVector.y()) && Double.isFinite(mappedVector.z()));

        RigidTransform3d largeTranslation = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(large, -large, large));
        assertEquals(new Vec3d(-large, large, -large), largeTranslation.inverse().translation());

        RigidTransform3d subnormal = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_VALUE));
        assertEquals(new Vec3d(Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_VALUE), subnormal.transformPoint(new Vec3d(0.0D, 0.0D, 0.0D)));
        assertEquals(new Vec3d(Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_VALUE), subnormal.transformVector(new Vec3d(Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_VALUE)));
        assertEquals(new Vec3d(-Double.MIN_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE), subnormal.inverse().translation());

        double tinyNormal = Double.MIN_NORMAL;
        RigidTransform3d tiny = new RigidTransform3d(Rotation3d.identity(), new Vec3d(tinyNormal, 0.0D, 0.0D));
        assertEquals(new Vec3d(-tinyNormal, 0.0D, 0.0D), tiny.inverse().translation());
    }

    @Test
    void exactlyRepresentableAxisAlignedLargeAndTinyCompositionsRemainFinite() {
        RigidTransform3d xHalfTurn = new RigidTransform3d(
                new Rotation3d(1.0D, 0.0D, 0.0D, 0.0D),
                new Vec3d(0.0D, 0.0D, 0.0D));
        RigidTransform3d yHalfTurn = new RigidTransform3d(
                new Rotation3d(0.0D, 1.0D, 0.0D, 0.0D),
                new Vec3d(0.0D, 0.0D, 0.0D));
        Vec3d vector = new Vec3d(1.0D, 2.0D, 3.0D);
        assertEquals(
                yHalfTurn.transformVector(xHalfTurn.transformVector(vector)),
                xHalfTurn.andThen(yHalfTurn).transformVector(vector));

        double large = Double.MAX_VALUE / 8.0D;
        RigidTransform3d largeFirst = new RigidTransform3d(Rotation3d.identity(), new Vec3d(large, 0.0D, 0.0D));
        RigidTransform3d largeAfter = new RigidTransform3d(Rotation3d.identity(), new Vec3d(large, 0.0D, 0.0D));
        assertEquals(new Vec3d(large + large, 0.0D, 0.0D), largeFirst.andThen(largeAfter).translation());

        RigidTransform3d tinyFirst = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MIN_NORMAL, Double.MIN_VALUE, 0.0D));
        RigidTransform3d tinyAfter = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(-Double.MIN_NORMAL, Double.MIN_VALUE, 0.0D));
        assertEquals(
                new Vec3d(0.0D, 2.0D * Double.MIN_VALUE, 0.0D),
                tinyFirst.andThen(tinyAfter).translation());
    }

    @Test
    void unrepresentablePointMappingFailsDeterministicallyWithoutMutation() {
        RigidTransform3d transform = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));
        Vec3d point = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);
        Vec3d snapshot = new Vec3d(point.x(), point.y(), point.z());

        assertThrows(IllegalArgumentException.class, () -> transform.transformPoint(point));
        assertThrows(IllegalArgumentException.class, () -> transform.transformPoint(point));
        assertEquals(snapshot, point);
        assertEquals(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), transform.translation());
    }

    @Test
    void unrepresentableComposedTranslationFailsWithoutSaturationOrMutation() {
        RigidTransform3d first = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));
        RigidTransform3d after = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D));

        assertThrows(IllegalArgumentException.class, () -> first.andThen(after));
        assertThrows(IllegalArgumentException.class, () -> first.andThen(after));
        assertEquals(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), first.translation());
        assertEquals(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), after.translation());
    }

    @Test
    void unrepresentableInverseAndInversePointFailBeforeNonfiniteValuesEscape() {
        RigidTransform3d inverseTranslationFailure = new RigidTransform3d(
                new Rotation3d(0.0D, 0.0D, 1.0D, 1.0D),
                new Vec3d(Double.MAX_VALUE, Double.MAX_VALUE, 0.0D));
        assertThrows(IllegalArgumentException.class, inverseTranslationFailure::inverse);
        assertThrows(IllegalArgumentException.class, inverseTranslationFailure::inverse);

        RigidTransform3d subtractionFailure = new RigidTransform3d(
                Rotation3d.identity(),
                new Vec3d(-Double.MAX_VALUE, 0.0D, 0.0D));
        Vec3d point = new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D);
        assertThrows(IllegalArgumentException.class, () -> subtractionFailure.inverseTransformPoint(point));
        assertThrows(IllegalArgumentException.class, () -> subtractionFailure.inverseTransformPoint(point));
        assertEquals(new Vec3d(Double.MAX_VALUE, 0.0D, 0.0D), point);
    }

    @Test
    void pointAndVectorMappingsAgreeWithIndependentBasisMatrixOracle() {
        Rotation3d rotation = new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D);
        Vec3d translation = new Vec3d(5.0D, -7.0D, 11.0D);
        RigidTransform3d transform = new RigidTransform3d(rotation, translation);
        Vec3d input = new Vec3d(1.25D, -2.5D, 3.75D);

        Phase1DTestSupport.assertVectorClose(
                Phase1DTestSupport.matrixTransformPoint(rotation, translation, input),
                transform.transformPoint(input),
                "independent basis-matrix point oracle");
        Phase1DTestSupport.assertVectorClose(
                Phase1DTestSupport.matrixTransformVector(rotation, input),
                transform.transformVector(input),
                "independent basis-matrix vector oracle");
    }

    @Test
    void inverseMappingsAgreeWithIndependentTransposedBasisOracle() {
        Rotation3d rotation = new Rotation3d(0.2D, -0.3D, 0.4D, 0.8D);
        Vec3d translation = new Vec3d(5.0D, -7.0D, 11.0D);
        RigidTransform3d transform = new RigidTransform3d(rotation, translation);
        Vec3d input = new Vec3d(1.25D, -2.5D, 3.75D);

        Phase1DTestSupport.assertVectorClose(
                Phase1DTestSupport.matrixInverseTransformPoint(rotation, translation, input),
                transform.inverseTransformPoint(input),
                "independent transposed-basis point oracle");
        Phase1DTestSupport.assertVectorClose(
                Phase1DTestSupport.matrixInverseTransformVector(rotation, input),
                transform.inverseTransformVector(input),
                "independent transposed-basis vector oracle");
    }
}
