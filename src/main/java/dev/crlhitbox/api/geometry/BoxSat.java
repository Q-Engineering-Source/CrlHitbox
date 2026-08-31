package dev.crlhitbox.api.geometry;

/** Fixed-work separating-axis queries for the Phase 1B closed box pairs. */
final class BoxSat {
    private BoxSat() {
    }

    static boolean intersects(Obb first, Obb second) {
        if (compare(first, second) > 0) return intersectsOrdered(second, first);
        return intersectsOrdered(first, second);
    }

    static boolean intersects(Aabb first, Obb second) {
        Vec3d bx = second.orientation().basisX();
        Vec3d by = second.orientation().basisY();
        Vec3d bz = second.orientation().basisZ();
        boolean separated = separatedAabbObb(first, second, 1.0D, 0.0D, 0.0D);
        separated |= separatedAabbObb(first, second, 0.0D, 1.0D, 0.0D);
        separated |= separatedAabbObb(first, second, 0.0D, 0.0D, 1.0D);
        separated |= separatedAabbObb(first, second, bx.x(), bx.y(), bx.z());
        separated |= separatedAabbObb(first, second, by.x(), by.y(), by.z());
        separated |= separatedAabbObb(first, second, bz.x(), bz.y(), bz.z());
        separated |= separatedAabbObb(first, second, 0.0D, -bz.z(), bz.y());
        separated |= separatedAabbObb(first, second, bz.z(), 0.0D, -bz.x());
        separated |= separatedAabbObb(first, second, -bz.y(), bz.x(), 0.0D);
        separated |= separatedAabbObb(first, second, 0.0D, -by.z(), by.y());
        separated |= separatedAabbObb(first, second, by.z(), 0.0D, -by.x());
        separated |= separatedAabbObb(first, second, -by.y(), by.x(), 0.0D);
        separated |= separatedAabbObb(first, second, 0.0D, -bx.z(), bx.y());
        separated |= separatedAabbObb(first, second, bx.z(), 0.0D, -bx.x());
        separated |= separatedAabbObb(first, second, -bx.y(), bx.x(), 0.0D);
        return !separated;
    }

    private static boolean intersectsOrdered(Obb first, Obb second) {
        Vec3d ax = first.orientation().basisX();
        Vec3d ay = first.orientation().basisY();
        Vec3d az = first.orientation().basisZ();
        Vec3d bx = second.orientation().basisX();
        Vec3d by = second.orientation().basisY();
        Vec3d bz = second.orientation().basisZ();
        boolean separated = separatedObbObb(first, second, ax.x(), ax.y(), ax.z());
        separated |= separatedObbObb(first, second, ay.x(), ay.y(), ay.z());
        separated |= separatedObbObb(first, second, az.x(), az.y(), az.z());
        separated |= separatedObbObb(first, second, bx.x(), bx.y(), bx.z());
        separated |= separatedObbObb(first, second, by.x(), by.y(), by.z());
        separated |= separatedObbObb(first, second, bz.x(), bz.y(), bz.z());
        separated |= crossSeparated(first, second, ax, bx);
        separated |= crossSeparated(first, second, ax, by);
        separated |= crossSeparated(first, second, ax, bz);
        separated |= crossSeparated(first, second, ay, bx);
        separated |= crossSeparated(first, second, ay, by);
        separated |= crossSeparated(first, second, ay, bz);
        separated |= crossSeparated(first, second, az, bx);
        separated |= crossSeparated(first, second, az, by);
        separated |= crossSeparated(first, second, az, bz);
        return !separated;
    }

    private static boolean crossSeparated(Obb first, Obb second, Vec3d left, Vec3d right) {
        double x = Math.fma(left.y(), right.z(), -left.z() * right.y());
        double y = Math.fma(left.z(), right.x(), -left.x() * right.z());
        double z = Math.fma(left.x(), right.y(), -left.y() * right.x());
        return separatedObbObb(first, second, x, y, z);
    }

    private static boolean separatedObbObb(Obb first, Obb second, double axisX, double axisY, double axisZ) {
        double maximumAxisComponent = maximumMagnitude(axisX, axisY, axisZ);
        if (maximumAxisComponent == 0.0D) return false;
        axisX /= maximumAxisComponent;
        axisY /= maximumAxisComponent;
        axisZ /= maximumAxisComponent;
        Vec3d firstCenter = first.center();
        Vec3d secondCenter = second.center();
        double rawX = firstCenter.x() - secondCenter.x();
        double rawY = firstCenter.y() - secondCenter.y();
        double rawZ = firstCenter.z() - secondCenter.z();
        Vec3d firstHalf = first.halfExtents();
        Vec3d secondHalf = second.halfExtents();
        Vec3d firstBasisX = first.orientation().basisX();
        Vec3d firstBasisY = first.orientation().basisY();
        Vec3d firstBasisZ = first.orientation().basisZ();
        Vec3d secondBasisX = second.orientation().basisX();
        Vec3d secondBasisY = second.orientation().basisY();
        Vec3d secondBasisZ = second.orientation().basisZ();
        double firstProjectionX = Math.abs(dot(firstBasisX.x(), firstBasisX.y(), firstBasisX.z(), axisX, axisY, axisZ));
        double firstProjectionY = Math.abs(dot(firstBasisY.x(), firstBasisY.y(), firstBasisY.z(), axisX, axisY, axisZ));
        double firstProjectionZ = Math.abs(dot(firstBasisZ.x(), firstBasisZ.y(), firstBasisZ.z(), axisX, axisY, axisZ));
        double secondProjectionX = Math.abs(dot(secondBasisX.x(), secondBasisX.y(), secondBasisX.z(), axisX, axisY, axisZ));
        double secondProjectionY = Math.abs(dot(secondBasisY.x(), secondBasisY.y(), secondBasisY.z(), axisX, axisY, axisZ));
        double secondProjectionZ = Math.abs(dot(secondBasisZ.x(), secondBasisZ.y(), secondBasisZ.z(), axisX, axisY, axisZ));
        boolean directDifference = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        int exponent = directDifference
                ? projectionExponent(rawX, rawY, rawZ, axisX, axisY, axisZ)
                : maximumExponent(projectionExponent(firstCenter.x(), firstCenter.y(), firstCenter.z(), axisX, axisY, axisZ), projectionExponent(secondCenter.x(), secondCenter.y(), secondCenter.z(), axisX, axisY, axisZ));
        exponent = maximumExponent(exponent, radiusExponent(firstHalf, firstProjectionX, firstProjectionY, firstProjectionZ));
        exponent = maximumExponent(exponent, radiusExponent(secondHalf, secondProjectionX, secondProjectionY, secondProjectionZ));
        if (exponent == Integer.MIN_VALUE) return false;
        double centerProjection = directDifference
                ? scaledDot(rawX, rawY, rawZ, axisX, axisY, axisZ, exponent)
                : scaledDot(firstCenter.x(), firstCenter.y(), firstCenter.z(), axisX, axisY, axisZ, exponent)
                        - scaledDot(secondCenter.x(), secondCenter.y(), secondCenter.z(), axisX, axisY, axisZ, exponent);
        double radius = scaledRadius(firstHalf, firstProjectionX, firstProjectionY, firstProjectionZ, exponent)
                + scaledRadius(secondHalf, secondProjectionX, secondProjectionY, secondProjectionZ, exponent);
        return Math.abs(centerProjection) > radius;
    }

    private static boolean separatedAabbObb(Aabb first, Obb second, double axisX, double axisY, double axisZ) {
        double maximumAxisComponent = maximumMagnitude(axisX, axisY, axisZ);
        if (maximumAxisComponent == 0.0D) return false;
        axisX /= maximumAxisComponent;
        axisY /= maximumAxisComponent;
        axisZ /= maximumAxisComponent;
        Vec3d minimum = first.min();
        Vec3d maximum = first.max();
        Vec3d center = second.center();
        double minX = minimum.x() - center.x();
        double maxX = maximum.x() - center.x();
        double minY = minimum.y() - center.y();
        double maxY = maximum.y() - center.y();
        double minZ = minimum.z() - center.z();
        double maxZ = maximum.z() - center.z();
        Vec3d half = second.halfExtents();
        Vec3d basisX = second.orientation().basisX();
        Vec3d basisY = second.orientation().basisY();
        Vec3d basisZ = second.orientation().basisZ();
        double projectionX = Math.abs(dot(basisX.x(), basisX.y(), basisX.z(), axisX, axisY, axisZ));
        double projectionY = Math.abs(dot(basisY.x(), basisY.y(), basisY.z(), axisX, axisY, axisZ));
        double projectionZ = Math.abs(dot(basisZ.x(), basisZ.y(), basisZ.z(), axisX, axisY, axisZ));
        boolean directDifference = Double.isFinite(minX) && Double.isFinite(maxX) && Double.isFinite(minY) && Double.isFinite(maxY) && Double.isFinite(minZ) && Double.isFinite(maxZ);
        double lowX = axisX >= 0.0D ? minX : maxX;
        double lowY = axisY >= 0.0D ? minY : maxY;
        double lowZ = axisZ >= 0.0D ? minZ : maxZ;
        double highX = axisX >= 0.0D ? maxX : minX;
        double highY = axisY >= 0.0D ? maxY : minY;
        double highZ = axisZ >= 0.0D ? maxZ : minZ;
        int exponent = directDifference
                ? maximumExponent(projectionExponent(lowX, lowY, lowZ, axisX, axisY, axisZ), projectionExponent(highX, highY, highZ, axisX, axisY, axisZ))
                : maximumExponent(maximumExponent(projectionExponent(axisX >= 0.0D ? minimum.x() : maximum.x(), axisY >= 0.0D ? minimum.y() : maximum.y(), axisZ >= 0.0D ? minimum.z() : maximum.z(), axisX, axisY, axisZ), projectionExponent(axisX >= 0.0D ? maximum.x() : minimum.x(), axisY >= 0.0D ? maximum.y() : minimum.y(), axisZ >= 0.0D ? maximum.z() : minimum.z(), axisX, axisY, axisZ)), projectionExponent(center.x(), center.y(), center.z(), axisX, axisY, axisZ));
        exponent = maximumExponent(exponent, radiusExponent(half, projectionX, projectionY, projectionZ));
        if (exponent == Integer.MIN_VALUE) return false;
        double low = directDifference
                ? scaledDot(lowX, lowY, lowZ, axisX, axisY, axisZ, exponent)
                : scaledDot(axisX >= 0.0D ? minimum.x() : maximum.x(), axisY >= 0.0D ? minimum.y() : maximum.y(), axisZ >= 0.0D ? minimum.z() : maximum.z(), axisX, axisY, axisZ, exponent) - scaledDot(center.x(), center.y(), center.z(), axisX, axisY, axisZ, exponent);
        double high = directDifference
                ? scaledDot(highX, highY, highZ, axisX, axisY, axisZ, exponent)
                : scaledDot(axisX >= 0.0D ? maximum.x() : minimum.x(), axisY >= 0.0D ? maximum.y() : minimum.y(), axisZ >= 0.0D ? maximum.z() : minimum.z(), axisX, axisY, axisZ, exponent) - scaledDot(center.x(), center.y(), center.z(), axisX, axisY, axisZ, exponent);
        double radius = scaledRadius(half, projectionX, projectionY, projectionZ, exponent);
        return low > radius || high < -radius;
    }

    private static int projectionExponent(double x, double y, double z, double axisX, double axisY, double axisZ) { return maximumExponent(productExponent(x, axisX), productExponent(y, axisY), productExponent(z, axisZ)); }

    private static int radiusExponent(Vec3d half, double projectionX, double projectionY, double projectionZ) { return maximumExponent(productExponent(half.x(), projectionX), productExponent(half.y(), projectionY), productExponent(half.z(), projectionZ)); }

    private static int productExponent(double first, double second) {
        if (first == 0.0D || second == 0.0D) return Integer.MIN_VALUE;
        return Math.getExponent(Math.abs(first)) + Math.getExponent(Math.abs(second)) + 2;
    }

    private static int maximumExponent(int... exponents) {
        int maximum = Integer.MIN_VALUE;
        for (int exponent : exponents) maximum = Math.max(maximum, exponent);
        return maximum;
    }

    private static double scaledDot(double x, double y, double z, double axisX, double axisY, double axisZ, int exponent) { return compensatedSum(scaledProduct(x, axisX, exponent), scaledProduct(y, axisY, exponent), scaledProduct(z, axisZ, exponent)); }

    private static double scaledRadius(Vec3d half, double projectionX, double projectionY, double projectionZ, int exponent) { return compensatedSum(scaledProduct(half.x(), projectionX, exponent), scaledProduct(half.y(), projectionY, exponent), scaledProduct(half.z(), projectionZ, exponent)); }

    private static double scaledProduct(double first, double second, int exponent) {
        if (first == 0.0D || second == 0.0D) return 0.0D;
        int firstExponent = Math.getExponent(Math.abs(first));
        int secondExponent = Math.getExponent(Math.abs(second));
        double firstMantissa = Math.scalb(first, -firstExponent);
        double secondMantissa = Math.scalb(second, -secondExponent);
        return Math.scalb(firstMantissa * secondMantissa, firstExponent + secondExponent - exponent);
    }

    private static double compensatedSum(double first, double second, double third) {
        double sum = first + second;
        double correction = Math.abs(first) >= Math.abs(second) ? first - sum + second : second - sum + first;
        double total = sum + third;
        correction += Math.abs(sum) >= Math.abs(third) ? sum - total + third : third - total + sum;
        return total + correction;
    }

    private static double dot(double x, double y, double z, double axisX, double axisY, double axisZ) {
        return Math.fma(x, axisX, Math.fma(y, axisY, z * axisZ));
    }

    private static double maximumMagnitude(double... values) {
        double maximum = 0.0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }

    private static int compare(Obb first, Obb second) {
        int compared = compare(first.center(), second.center());
        if (compared != 0) return compared;
        compared = compare(first.halfExtents(), second.halfExtents());
        if (compared != 0) return compared;
        Rotation3d a = first.orientation();
        Rotation3d b = second.orientation();
        compared = Long.compare(Double.doubleToLongBits(a.x()), Double.doubleToLongBits(b.x()));
        if (compared != 0) return compared;
        compared = Long.compare(Double.doubleToLongBits(a.y()), Double.doubleToLongBits(b.y()));
        if (compared != 0) return compared;
        compared = Long.compare(Double.doubleToLongBits(a.z()), Double.doubleToLongBits(b.z()));
        if (compared != 0) return compared;
        return Long.compare(Double.doubleToLongBits(a.w()), Double.doubleToLongBits(b.w()));
    }

    private static int compare(Vec3d first, Vec3d second) {
        int compared = Long.compare(Double.doubleToLongBits(first.x()), Double.doubleToLongBits(second.x()));
        if (compared != 0) return compared;
        compared = Long.compare(Double.doubleToLongBits(first.y()), Double.doubleToLongBits(second.y()));
        if (compared != 0) return compared;
        return Long.compare(Double.doubleToLongBits(first.z()), Double.doubleToLongBits(second.z()));
    }
}
