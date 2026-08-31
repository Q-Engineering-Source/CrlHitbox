package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** Pure closed-set intersection queries for the explicitly supported pointwise pair set. */
public final class GeometryIntersections {
    private GeometryIntersections() {
    }

    /** Returns whether two closed solids overlap or touch. */
    public static boolean intersects(Solid3d first, Solid3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (!intersects(first.bounds(), second.bounds())) return false;
        if (first instanceof Composite firstComposite) {
            if (second instanceof Composite secondComposite) {
                return intersectsComposites(firstComposite, secondComposite);
            }
            return intersectsCompositePrimitive(firstComposite, second, true);
        }
        if (second instanceof Composite secondComposite) {
            return intersectsCompositePrimitive(secondComposite, first, false);
        }
        return intersectsPrimitives(first, second);
    }

    /** Returns whether a finite closed segment overlaps or touches a closed solid. */
    public static boolean intersects(Segment3d segment, Solid3d solid) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(solid, "solid");
        if (!intersects(segment, solid.bounds())) return false;
        if (solid instanceof Composite composite) {
            for (int index = 0; index < composite.childCount(); index++) {
                Solid3d leaf = composite.child(index);
                if (intersects(segment, leaf.bounds()) && intersectsSegmentPrimitive(segment, leaf)) {
                    return true;
                }
            }
            return false;
        }
        return intersectsSegmentPrimitive(segment, solid);
    }

    /** Returns whether a closed solid overlaps or touches a finite closed segment. */
    public static boolean intersects(Solid3d solid, Segment3d segment) {
        return intersects(segment, solid);
    }

    /** Returns whether two solids placed into the same parent frame overlap or touch. */
    public static boolean intersects(PlacedSolid3d first, PlacedSolid3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return PlacedQueries.intersects(first, second);
    }

    /** Returns whether a parent-frame segment overlaps or touches a solid placed in that frame. */
    public static boolean intersects(Segment3d segmentInParent, PlacedSolid3d solid) {
        Objects.requireNonNull(segmentInParent, "segmentInParent");
        Objects.requireNonNull(solid, "solid");
        return PlacedQueries.intersects(segmentInParent, solid);
    }

    /** Returns whether a placed solid overlaps or touches a parent-frame segment. */
    public static boolean intersects(PlacedSolid3d solid, Segment3d segmentInParent) {
        return intersects(segmentInParent, solid);
    }

    /** Returns whether two closed axis-aligned boxes overlap or touch. */
    public static boolean intersects(Aabb first, Aabb second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return first.min().x() <= second.max().x() && first.max().x() >= second.min().x()
                && first.min().y() <= second.max().y() && first.max().y() >= second.min().y()
                && first.min().z() <= second.max().z() && first.max().z() >= second.min().z();
    }

    /** Returns whether two closed spheres overlap or touch. */
    public static boolean intersects(Sphere first, Sphere second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return centersWithin(first.center(), second.center(), first.radius(), second.radius());
    }

    /** Returns whether a closed sphere and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Sphere sphere, Aabb box) {
        Objects.requireNonNull(sphere, "sphere");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.pointToAabbWithin(sphere.center(), box, sphere.radius());
    }

    /** Returns whether a closed axis-aligned box and closed sphere overlap or touch. */
    public static boolean intersects(Aabb box, Sphere sphere) { return intersects(sphere, box); }

    /** Returns whether a closed sphere and closed oriented box overlap or touch. */
    public static boolean intersects(Sphere sphere, Obb box) {
        Objects.requireNonNull(sphere, "sphere");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.pointToObbWithin(sphere.center(), box, sphere.radius());
    }

    /** Returns whether a closed oriented box and closed sphere overlap or touch. */
    public static boolean intersects(Obb box, Sphere sphere) { return intersects(sphere, box); }

    /** Returns whether two closed oriented boxes overlap or touch. */
    public static boolean intersects(Obb first, Obb second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return BoxSat.intersects(first, second);
    }

    /** Returns whether a closed axis-aligned box and closed oriented box overlap or touch. */
    public static boolean intersects(Aabb first, Obb second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return BoxSat.intersects(first, second);
    }

    /** Returns whether a closed oriented box and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Obb first, Aabb second) { return intersects(second, first); }

    /** Returns whether a closed capsule and closed sphere overlap or touch. */
    public static boolean intersects(Capsule capsule, Sphere sphere) {
        Objects.requireNonNull(capsule, "capsule");
        Objects.requireNonNull(sphere, "sphere");
        return GeometryDistances.pointToSegmentWithin(sphere.center(), capsule.centerline(), sphere.radius(), capsule.radius());
    }

    /** Returns whether a closed sphere and closed capsule overlap or touch. */
    public static boolean intersects(Sphere sphere, Capsule capsule) { return intersects(capsule, sphere); }

    /** Returns whether two closed capsules overlap or touch. */
    public static boolean intersects(Capsule first, Capsule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return GeometryDistances.segmentToSegmentWithin(first.centerline(), second.centerline(), first.radius(), second.radius());
    }

    /** Returns whether a closed capsule and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Capsule capsule, Aabb box) {
        Objects.requireNonNull(capsule, "capsule");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.segmentToAabbWithin(capsule.centerline(), box, capsule.radius());
    }

    /** Returns whether a closed axis-aligned box and closed capsule overlap or touch. */
    public static boolean intersects(Aabb box, Capsule capsule) { return intersects(capsule, box); }

    /** Returns whether a closed capsule and closed oriented box overlap or touch. */
    public static boolean intersects(Capsule capsule, Obb box) {
        Objects.requireNonNull(capsule, "capsule");
        Objects.requireNonNull(box, "box");
        return GeometryDistances.segmentToObbWithin(capsule.centerline(), box, capsule.radius());
    }

    /** Returns whether a closed oriented box and closed capsule overlap or touch. */
    public static boolean intersects(Obb box, Capsule capsule) { return intersects(capsule, box); }

    /** Returns whether a finite closed segment and closed sphere overlap or touch. */
    public static boolean intersects(Segment3d segment, Sphere sphere) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(sphere, "sphere");
        return GeometryDistances.pointToSegmentWithin(sphere.center(), segment, sphere.radius(), 0.0D);
    }

    /** Returns whether a closed sphere and finite closed segment overlap or touch. */
    public static boolean intersects(Sphere sphere, Segment3d segment) { return intersects(segment, sphere); }

    /** Returns whether a finite closed segment and closed axis-aligned box overlap or touch. */
    public static boolean intersects(Segment3d segment, Aabb box) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(box, "box");
        return NormalizedSegmentBox.fromAabb(segment, box).intersects();
    }

    /** Returns whether a closed axis-aligned box and finite closed segment overlap or touch. */
    public static boolean intersects(Aabb box, Segment3d segment) { return intersects(segment, box); }

    /** Returns whether a finite closed segment and closed oriented box overlap or touch. */
    public static boolean intersects(Segment3d segment, Obb box) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(box, "box");
        if (box.contains(segment.start()) || box.contains(segment.end())) return true;
        return NormalizedSegmentBox.fromObb(segment, box).intersects();
    }

    /** Returns whether a closed oriented box and finite closed segment overlap or touch. */
    public static boolean intersects(Obb box, Segment3d segment) { return intersects(segment, box); }

    /** Returns whether a finite closed segment and closed capsule overlap or touch. */
    public static boolean intersects(Segment3d segment, Capsule capsule) {
        Objects.requireNonNull(segment, "segment");
        Objects.requireNonNull(capsule, "capsule");
        return GeometryDistances.segmentToSegmentWithin(segment, capsule.centerline(), 0.0D, capsule.radius());
    }

    /** Returns whether a closed capsule and finite closed segment overlap or touch. */
    public static boolean intersects(Capsule capsule, Segment3d segment) { return intersects(segment, capsule); }

    private static boolean intersectsComposites(Composite first, Composite second) {
        for (int firstIndex = 0; firstIndex < first.childCount(); firstIndex++) {
            Solid3d firstLeaf = first.child(firstIndex);
            for (int secondIndex = 0; secondIndex < second.childCount(); secondIndex++) {
                Solid3d secondLeaf = second.child(secondIndex);
                if (intersects(firstLeaf.bounds(), secondLeaf.bounds())
                        && intersectsPrimitives(firstLeaf, secondLeaf)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean intersectsCompositePrimitive(
            Composite composite,
            Solid3d primitive,
            boolean compositeFirst
    ) {
        for (int index = 0; index < composite.childCount(); index++) {
            Solid3d leaf = composite.child(index);
            if (!intersects(leaf.bounds(), primitive.bounds())) continue;
            if (compositeFirst
                    ? intersectsPrimitives(leaf, primitive)
                    : intersectsPrimitives(primitive, leaf)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersectsPrimitives(Solid3d first, Solid3d second) {
        return switch (first) {
            case Aabb left -> switch (second) {
                case Aabb right -> intersects(left, right);
                case Sphere right -> intersects(left, right);
                case Obb right -> intersects(left, right);
                case Capsule right -> intersects(left, right);
                case Composite ignored -> throw nonPrimitiveDispatch();
            };
            case Sphere left -> switch (second) {
                case Aabb right -> intersects(left, right);
                case Sphere right -> intersects(left, right);
                case Obb right -> intersects(left, right);
                case Capsule right -> intersects(left, right);
                case Composite ignored -> throw nonPrimitiveDispatch();
            };
            case Obb left -> switch (second) {
                case Aabb right -> intersects(left, right);
                case Sphere right -> intersects(left, right);
                case Obb right -> intersects(left, right);
                case Capsule right -> intersects(left, right);
                case Composite ignored -> throw nonPrimitiveDispatch();
            };
            case Capsule left -> switch (second) {
                case Aabb right -> intersects(left, right);
                case Sphere right -> intersects(left, right);
                case Obb right -> intersects(left, right);
                case Capsule right -> intersects(left, right);
                case Composite ignored -> throw nonPrimitiveDispatch();
            };
            case Composite ignored -> throw nonPrimitiveDispatch();
        };
    }

    private static boolean intersectsSegmentPrimitive(Segment3d segment, Solid3d primitive) {
        return switch (primitive) {
            case Aabb box -> intersects(segment, box);
            case Sphere sphere -> intersects(segment, sphere);
            case Obb box -> intersects(segment, box);
            case Capsule capsule -> intersects(segment, capsule);
            case Composite ignored -> throw nonPrimitiveDispatch();
        };
    }

    private static AssertionError nonPrimitiveDispatch() {
        return new AssertionError("Composite must be flattened before primitive dispatch");
    }

    private static boolean centersWithin(Vec3d first, Vec3d second, double firstRadius, double secondRadius) {
        double rawX = first.x() - second.x();
        double rawY = first.y() - second.y();
        double rawZ = first.z() - second.z();
        boolean directRelativeCoordinates = Double.isFinite(rawX) && Double.isFinite(rawY) && Double.isFinite(rawZ);
        double scale = directRelativeCoordinates
                ? maximumMagnitude(rawX, rawY, rawZ, firstRadius, secondRadius)
                : maximumMagnitude(first.x(), first.y(), first.z(), second.x(), second.y(), second.z(), firstRadius, secondRadius);
        if (scale == 0.0D) return true;
        double dx = directRelativeCoordinates ? rawX / scale : GeometryDistances.normalizedDifference(first.x(), second.x(), scale);
        double dy = directRelativeCoordinates ? rawY / scale : GeometryDistances.normalizedDifference(first.y(), second.y(), scale);
        double dz = directRelativeCoordinates ? rawZ / scale : GeometryDistances.normalizedDifference(first.z(), second.z(), scale);
        double radius = firstRadius / scale + secondRadius / scale;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private static boolean intersectsSlab(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double halfX, double halfY, double halfZ) {
        return intersectsSlab(startX, startY, startZ, directionX, directionY, directionZ, -halfX, -halfY, -halfZ, halfX, halfY, halfZ);
    }

    private static boolean intersectsSlab(double startX, double startY, double startZ, double directionX, double directionY, double directionZ, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        double low = 0.0D;
        double high = 1.0D;
        double[] starts = {startX, startY, startZ};
        double[] directions = {directionX, directionY, directionZ};
        double[] minimums = {minX, minY, minZ};
        double[] maximums = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double direction = directions[axis];
            if (direction == 0.0D) {
                if (starts[axis] < minimums[axis] || starts[axis] > maximums[axis]) return false;
                continue;
            }
            double first = (minimums[axis] - starts[axis]) / direction;
            double second = (maximums[axis] - starts[axis]) / direction;
            low = Math.max(low, Math.min(first, second));
            high = Math.min(high, Math.max(first, second));
            if (low > high) return false;
        }
        return true;
    }

    private static double maximumMagnitude(double... values) {
        double maximum = 0.0D;
        for (double value : values) maximum = Math.max(maximum, Math.abs(value));
        return maximum;
    }

}
