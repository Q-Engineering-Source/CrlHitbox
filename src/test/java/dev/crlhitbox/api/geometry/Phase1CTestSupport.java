package dev.crlhitbox.api.geometry;

/** Test-only Phase 1C reference dispatch that never calls a generic production overload. */
final class Phase1CTestSupport {
    private Phase1CTestSupport() {
    }

    static boolean intersectsPrimitiveTyped(Solid3d first, Solid3d second) {
        return switch (first) {
            case Aabb left -> switch (second) {
                case Aabb right -> GeometryIntersections.intersects(left, right);
                case Sphere right -> GeometryIntersections.intersects(left, right);
                case Obb right -> GeometryIntersections.intersects(left, right);
                case Capsule right -> GeometryIntersections.intersects(left, right);
                case Composite ignored -> throw new IllegalArgumentException("second must be primitive");
            };
            case Sphere left -> switch (second) {
                case Aabb right -> GeometryIntersections.intersects(left, right);
                case Sphere right -> GeometryIntersections.intersects(left, right);
                case Obb right -> GeometryIntersections.intersects(left, right);
                case Capsule right -> GeometryIntersections.intersects(left, right);
                case Composite ignored -> throw new IllegalArgumentException("second must be primitive");
            };
            case Obb left -> switch (second) {
                case Aabb right -> GeometryIntersections.intersects(left, right);
                case Sphere right -> GeometryIntersections.intersects(left, right);
                case Obb right -> GeometryIntersections.intersects(left, right);
                case Capsule right -> GeometryIntersections.intersects(left, right);
                case Composite ignored -> throw new IllegalArgumentException("second must be primitive");
            };
            case Capsule left -> switch (second) {
                case Aabb right -> GeometryIntersections.intersects(left, right);
                case Sphere right -> GeometryIntersections.intersects(left, right);
                case Obb right -> GeometryIntersections.intersects(left, right);
                case Capsule right -> GeometryIntersections.intersects(left, right);
                case Composite ignored -> throw new IllegalArgumentException("second must be primitive");
            };
            case Composite ignored -> throw new IllegalArgumentException("first must be primitive");
        };
    }

    static boolean intersectsSolidOracle(Solid3d first, Solid3d second) {
        if (first instanceof Composite left) {
            if (second instanceof Composite right) return intersectsCompositeCompositeOracle(left, right);
            return intersectsCompositePrimitiveOracle(left, second);
        }
        if (second instanceof Composite right) {
            for (int index = 0; index < right.childCount(); index++) {
                if (intersectsPrimitiveTyped(first, right.child(index))) return true;
            }
            return false;
        }
        return intersectsPrimitiveTyped(first, second);
    }

    static boolean intersectsCompositePrimitiveOracle(Composite composite, Solid3d primitive) {
        for (int index = 0; index < composite.childCount(); index++) {
            if (intersectsPrimitiveTyped(composite.child(index), primitive)) return true;
        }
        return false;
    }

    static boolean intersectsCompositeCompositeOracle(Composite first, Composite second) {
        for (int firstIndex = 0; firstIndex < first.childCount(); firstIndex++) {
            for (int secondIndex = 0; secondIndex < second.childCount(); secondIndex++) {
                if (intersectsPrimitiveTyped(first.child(firstIndex), second.child(secondIndex))) return true;
            }
        }
        return false;
    }

    static boolean intersectsSegmentPrimitiveTyped(Segment3d segment, Solid3d primitive) {
        return switch (primitive) {
            case Aabb box -> GeometryIntersections.intersects(segment, box);
            case Sphere sphere -> GeometryIntersections.intersects(segment, sphere);
            case Obb box -> GeometryIntersections.intersects(segment, box);
            case Capsule capsule -> GeometryIntersections.intersects(segment, capsule);
            case Composite ignored -> throw new IllegalArgumentException("solid must be primitive");
        };
    }

    static boolean intersectsSegmentCompositeOracle(Segment3d segment, Composite composite) {
        for (int index = 0; index < composite.childCount(); index++) {
            if (intersectsSegmentPrimitiveTyped(segment, composite.child(index))) return true;
        }
        return false;
    }

    static String canonicalLeaves(Composite composite) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < composite.childCount(); index++) {
            if (index != 0) result.append(", ");
            result.append(composite.child(index));
        }
        return result.append(']').toString();
    }
}
