package dev.crlhitbox.api.geometry;

/** Exact local min/max intervals carried by a rigid local-to-parent frame. */
final class RigidIntervalBox {
    private final Vec3d localMin;
    private final Vec3d localMax;
    private final RigidTransform3d localToParent;
    private final Vec3d parentBasisX;
    private final Vec3d parentBasisY;
    private final Vec3d parentBasisZ;

    private RigidIntervalBox(Vec3d localMin, Vec3d localMax, RigidTransform3d localToParent) {
        this.localMin = localMin;
        this.localMax = localMax;
        this.localToParent = localToParent;
        this.parentBasisX = localToParent.rotation().basisX();
        this.parentBasisY = localToParent.rotation().basisY();
        this.parentBasisZ = localToParent.rotation().basisZ();
    }

    static RigidIntervalBox fromAabb(Aabb local, RigidTransform3d placement) {
        return new RigidIntervalBox(local.min(), local.max(), placement);
    }

    static RigidIntervalBox fromObb(Obb local, RigidTransform3d placement) {
        Vec3d half = local.halfExtents();
        RigidTransform3d intrinsic = new RigidTransform3d(local.orientation(), local.center());
        return new RigidIntervalBox(half.negate(), half, intrinsic.andThen(placement));
    }

    Vec3d localMin() { return localMin; }
    Vec3d localMax() { return localMax; }
    RigidTransform3d localToParent() { return localToParent; }
    Vec3d parentOrigin() { return localToParent.translation(); }
    Vec3d parentBasisX() { return parentBasisX; }
    Vec3d parentBasisY() { return parentBasisY; }
    Vec3d parentBasisZ() { return parentBasisZ; }

    Aabb conservativeBounds() {
        double firstX = support(1.0D, 0.0D, 0.0D, false);
        double secondX = support(1.0D, 0.0D, 0.0D, true);
        double firstY = support(0.0D, 1.0D, 0.0D, false);
        double secondY = support(0.0D, 1.0D, 0.0D, true);
        double firstZ = support(0.0D, 0.0D, 1.0D, false);
        double secondZ = support(0.0D, 0.0D, 1.0D, true);
        Vec3d minimum = new Vec3d(
                Math.min(firstX, secondX),
                Math.min(firstY, secondY),
                Math.min(firstZ, secondZ));
        Vec3d maximum = new Vec3d(
                Math.max(firstX, secondX),
                Math.max(firstY, secondY),
                Math.max(firstZ, secondZ));
        for (int signs = 0; signs < 8; signs++) {
            Vec3d localCorner = new Vec3d(
                    (signs & 1) == 0 ? localMin.x() : localMax.x(),
                    (signs & 2) == 0 ? localMin.y() : localMax.y(),
                    (signs & 4) == 0 ? localMin.z() : localMax.z());
            Vec3d parentCorner = localToParent.transformPoint(localCorner);
            minimum = minimum.min(parentCorner);
            maximum = maximum.max(parentCorner);
        }
        return new Aabb(minimum, maximum);
    }

    private double support(double axisX, double axisY, double axisZ, boolean high) {
        double coefficientX = dot(parentBasisX, axisX, axisY, axisZ);
        double coefficientY = dot(parentBasisY, axisX, axisY, axisZ);
        double coefficientZ = dot(parentBasisZ, axisX, axisY, axisZ);
        double endpointX = endpoint(localMin.x(), localMax.x(), coefficientX, high);
        double endpointY = endpoint(localMin.y(), localMax.y(), coefficientY, high);
        double endpointZ = endpoint(localMin.z(), localMax.z(), coefficientZ, high);
        Vec3d origin = parentOrigin();
        int exponent = ScaledArithmetic.maximumExponent(
                ScaledArithmetic.productExponent(origin.x(), axisX),
                ScaledArithmetic.productExponent(origin.y(), axisY),
                ScaledArithmetic.productExponent(origin.z(), axisZ),
                ScaledArithmetic.productExponent(endpointX, coefficientX),
                ScaledArithmetic.productExponent(endpointY, coefficientY),
                ScaledArithmetic.productExponent(endpointZ, coefficientZ));
        if (exponent == ScaledArithmetic.ZERO_EXPONENT) return 0.0D;
        double scaled = ScaledArithmetic.compensatedSum(
                ScaledArithmetic.scaledProduct(origin.x(), axisX, exponent),
                ScaledArithmetic.scaledProduct(origin.y(), axisY, exponent),
                ScaledArithmetic.scaledProduct(origin.z(), axisZ, exponent),
                ScaledArithmetic.scaledProduct(endpointX, coefficientX, exponent),
                ScaledArithmetic.scaledProduct(endpointY, coefficientY, exponent),
                ScaledArithmetic.scaledProduct(endpointZ, coefficientZ, exponent));
        return Vec3d.canonicalFinite(Math.scalb(scaled, exponent), "placed box support");
    }

    private static double endpoint(double minimum, double maximum, double coefficient, boolean high) {
        if (coefficient >= 0.0D) return high ? maximum : minimum;
        return high ? minimum : maximum;
    }

    private static double dot(Vec3d value, double axisX, double axisY, double axisZ) {
        return Math.fma(value.x(), axisX, Math.fma(value.y(), axisY, value.z() * axisZ));
    }
}
