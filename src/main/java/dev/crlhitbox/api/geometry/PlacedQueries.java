package dev.crlhitbox.api.geometry;

/** Package-private exact narrow-phase dispatch for rigidly placed solids. */
final class PlacedQueries {
    private PlacedQueries() {
    }

    static boolean intersects(PlacedSolid3d first, PlacedSolid3d second) {
        if (!GeometryIntersections.intersects(first.bounds(), second.bounds())) return false;
        if (isIdentity(first.localToParent()) && isIdentity(second.localToParent())) {
            return GeometryIntersections.intersects(first.localSolid(), second.localSolid());
        }
        return intersects(
                first.localSolid(), first.localToParent(),
                second.localSolid(), second.localToParent());
    }

    static boolean intersects(Segment3d segment, PlacedSolid3d solid) {
        if (!GeometryIntersections.intersects(segment, solid.bounds())) return false;
        if (isIdentity(solid.localToParent())) {
            return GeometryIntersections.intersects(segment, solid.localSolid());
        }
        return intersects(segment, solid.localSolid(), solid.localToParent());
    }

    private static boolean intersects(
            Solid3d first,
            RigidTransform3d firstTransform,
            Solid3d second,
            RigidTransform3d secondTransform
    ) {
        if (first instanceof Composite firstComposite) {
            if (second instanceof Composite secondComposite) {
                return intersects(firstComposite, firstTransform, secondComposite, secondTransform);
            }
            for (int index = 0; index < firstComposite.childCount(); index++) {
                Solid3d leaf = firstComposite.child(index);
                if (boundsOverlap(leaf, firstTransform, second, secondTransform)
                        && intersectsPrimitive(leaf, firstTransform, second, secondTransform)) return true;
            }
            return false;
        }
        if (second instanceof Composite secondComposite) {
            for (int index = 0; index < secondComposite.childCount(); index++) {
                Solid3d leaf = secondComposite.child(index);
                if (boundsOverlap(first, firstTransform, leaf, secondTransform)
                        && intersectsPrimitive(first, firstTransform, leaf, secondTransform)) return true;
            }
            return false;
        }
        return intersectsPrimitive(first, firstTransform, second, secondTransform);
    }

    private static boolean intersects(
            Composite first,
            RigidTransform3d firstTransform,
            Composite second,
            RigidTransform3d secondTransform
    ) {
        for (int firstIndex = 0; firstIndex < first.childCount(); firstIndex++) {
            Solid3d firstLeaf = first.child(firstIndex);
            for (int secondIndex = 0; secondIndex < second.childCount(); secondIndex++) {
                Solid3d secondLeaf = second.child(secondIndex);
                if (boundsOverlap(firstLeaf, firstTransform, secondLeaf, secondTransform)
                        && intersectsPrimitive(firstLeaf, firstTransform, secondLeaf, secondTransform)) return true;
            }
        }
        return false;
    }

    private static boolean intersects(Segment3d segment, Solid3d solid, RigidTransform3d transform) {
        if (solid instanceof Composite composite) {
            for (int index = 0; index < composite.childCount(); index++) {
                Solid3d leaf = composite.child(index);
                if (GeometryIntersections.intersects(segment, PlacedSolid3d.placedBounds(leaf, transform))
                        && intersectsSegmentPrimitive(segment, leaf, transform)) return true;
            }
            return false;
        }
        return intersectsSegmentPrimitive(segment, solid, transform);
    }

    private static boolean intersectsPrimitive(
            Solid3d first,
            RigidTransform3d firstTransform,
            Solid3d second,
            RigidTransform3d secondTransform
    ) {
        boolean firstBox = isBox(first);
        boolean secondBox = isBox(second);
        if (firstBox && secondBox) {
            return BoxSat.intersects(rigidBox(first, firstTransform), rigidBox(second, secondTransform));
        }
        if (firstBox) return intersectsBoxAndPrimitive(rigidBox(first, firstTransform), second, secondTransform);
        if (secondBox) return intersectsBoxAndPrimitive(rigidBox(second, secondTransform), first, firstTransform);
        return intersectsNonBoxes(first, firstTransform, second, secondTransform);
    }

    private static boolean intersectsBoxAndPrimitive(
            RigidIntervalBox box,
            Solid3d primitive,
            RigidTransform3d transform
    ) {
        return switch (primitive) {
            case Sphere sphere -> {
                Vec3d center = transform.transformPoint(sphere.center());
                yield GeometryDistances.endpointsToRigidIntervalBoxWithin(center, center, box, sphere.radius());
            }
            case Capsule capsule -> GeometryDistances.endpointsToRigidIntervalBoxWithin(
                    transform.transformPoint(capsule.centerline().start()),
                    transform.transformPoint(capsule.centerline().end()),
                    box,
                    capsule.radius());
            case Aabb ignored -> throw new AssertionError("box was not classified");
            case Obb ignored -> throw new AssertionError("box was not classified");
            case Composite ignored -> throw new AssertionError("Composite must be flattened");
        };
    }

    private static boolean intersectsNonBoxes(
            Solid3d first,
            RigidTransform3d firstTransform,
            Solid3d second,
            RigidTransform3d secondTransform
    ) {
        if (first instanceof Sphere firstSphere) {
            Vec3d firstCenter = firstTransform.transformPoint(firstSphere.center());
            if (second instanceof Sphere secondSphere) {
                return GeometryIntersections.intersects(
                        new Sphere(firstCenter, firstSphere.radius()),
                        new Sphere(secondTransform.transformPoint(secondSphere.center()), secondSphere.radius()));
            }
            Capsule secondCapsule = (Capsule) second;
            return GeometryDistances.pointToSegmentEndpointsWithin(
                    firstCenter,
                    secondTransform.transformPoint(secondCapsule.centerline().start()),
                    secondTransform.transformPoint(secondCapsule.centerline().end()),
                    firstSphere.radius(),
                    secondCapsule.radius());
        }
        Capsule firstCapsule = (Capsule) first;
        Vec3d firstStart = firstTransform.transformPoint(firstCapsule.centerline().start());
        Vec3d firstEnd = firstTransform.transformPoint(firstCapsule.centerline().end());
        if (second instanceof Sphere secondSphere) {
            return GeometryDistances.pointToSegmentEndpointsWithin(
                    secondTransform.transformPoint(secondSphere.center()),
                    firstStart,
                    firstEnd,
                    secondSphere.radius(),
                    firstCapsule.radius());
        }
        Capsule secondCapsule = (Capsule) second;
        return GeometryDistances.segmentEndpointsToSegmentEndpointsWithin(
                firstStart, firstEnd,
                secondTransform.transformPoint(secondCapsule.centerline().start()),
                secondTransform.transformPoint(secondCapsule.centerline().end()),
                firstCapsule.radius(), secondCapsule.radius());
    }

    private static boolean intersectsSegmentPrimitive(
            Segment3d segment,
            Solid3d primitive,
            RigidTransform3d transform
    ) {
        if (isBox(primitive)) {
            return GeometryDistances.endpointsToRigidIntervalBoxWithin(
                    segment.start(), segment.end(), rigidBox(primitive, transform), 0.0D);
        }
        if (primitive instanceof Sphere sphere) {
            return GeometryDistances.pointToSegmentEndpointsWithin(
                    transform.transformPoint(sphere.center()),
                    segment.start(), segment.end(),
                    sphere.radius(), 0.0D);
        }
        Capsule capsule = (Capsule) primitive;
        return GeometryDistances.segmentEndpointsToSegmentEndpointsWithin(
                segment.start(), segment.end(),
                transform.transformPoint(capsule.centerline().start()),
                transform.transformPoint(capsule.centerline().end()),
                0.0D, capsule.radius());
    }

    private static boolean boundsOverlap(
            Solid3d first,
            RigidTransform3d firstTransform,
            Solid3d second,
            RigidTransform3d secondTransform
    ) {
        return GeometryIntersections.intersects(
                PlacedSolid3d.placedBounds(first, firstTransform),
                PlacedSolid3d.placedBounds(second, secondTransform));
    }

    private static boolean isBox(Solid3d solid) {
        return solid instanceof Aabb || solid instanceof Obb;
    }

    private static RigidIntervalBox rigidBox(Solid3d solid, RigidTransform3d transform) {
        if (solid instanceof Aabb box) return RigidIntervalBox.fromAabb(box, transform);
        return RigidIntervalBox.fromObb((Obb) solid, transform);
    }

    private static boolean isIdentity(RigidTransform3d transform) {
        return transform.equals(RigidTransform3d.identity());
    }
}
