package dev.crlhitbox.api.geometry;

import java.util.Objects;

/**
 * An immutable local solid placed, without materialization, into a caller-defined parent frame.
 * It represents every parent point {@code localToParent.transformPoint(p)} for a point {@code p}
 * in {@code localSolid}. Both placed-query operands must target the same parent frame; this value
 * intentionally carries no runtime frame identifier.
 */
public final class PlacedSolid3d implements Bounded3d {
    private final Solid3d localSolid;
    private final RigidTransform3d localToParent;
    private final Aabb bounds;

    /** Creates a placement of {@code localSolid} into its parent frame. */
    public PlacedSolid3d(Solid3d localSolid, RigidTransform3d localToParent) {
        this.localSolid = Objects.requireNonNull(localSolid, "localSolid");
        this.localToParent = Objects.requireNonNull(localToParent, "localToParent");
        this.bounds = placedBounds(localSolid, localToParent);
    }

    /** Returns the solid expressed in its local frame. */
    public Solid3d localSolid() {
        return localSolid;
    }

    /** Returns the local-to-parent rigid transform. */
    public RigidTransform3d localToParent() {
        return localToParent;
    }

    /** Returns eagerly stored conservative parent-frame bounds used only for broad-phase rejection. */
    @Override
    public Aabb bounds() {
        return bounds;
    }

    static Aabb placedBounds(Solid3d localSolid, RigidTransform3d localToParent) {
        if (localToParent.equals(RigidTransform3d.identity())) return localSolid.bounds();
        return switch (localSolid) {
            case Aabb box -> primitiveBounds(box, localToParent);
            case Sphere sphere -> primitiveBounds(sphere, localToParent);
            case Obb box -> primitiveBounds(box, localToParent);
            case Capsule capsule -> primitiveBounds(capsule, localToParent);
            case Composite composite -> compositeBounds(composite, localToParent);
        };
    }

    private static Aabb primitiveBounds(Solid3d primitive, RigidTransform3d localToParent) {
        return switch (primitive) {
            case Aabb box -> RigidIntervalBox.fromAabb(box, localToParent).conservativeBounds();
            case Sphere sphere -> new Sphere(
                    localToParent.transformPoint(sphere.center()),
                    sphere.radius()).bounds();
            case Obb box -> obbBounds(box, localToParent);
            case Capsule capsule -> capsuleBounds(capsule, localToParent);
            case Composite ignored -> throw new AssertionError("Composite must be flattened before primitive bounds");
        };
    }

    private static Aabb capsuleBounds(Capsule capsule, RigidTransform3d localToParent) {
        Vec3d first = localToParent.transformPoint(capsule.centerline().start());
        Vec3d second = localToParent.transformPoint(capsule.centerline().end());
        double radius = capsule.radius();
        Vec3d minimum = first.min(second);
        Vec3d maximum = first.max(second);
        return new Aabb(
                new Vec3d(minimum.x() - radius, minimum.y() - radius, minimum.z() - radius),
                new Vec3d(maximum.x() + radius, maximum.y() + radius, maximum.z() + radius));
    }

    private static Aabb obbBounds(Obb box, RigidTransform3d localToParent) {
        Aabb rigidBounds = RigidIntervalBox.fromObb(box, localToParent).conservativeBounds();
        Vec3d minimum = rigidBounds.min();
        Vec3d maximum = rigidBounds.max();
        Vec3d half = box.halfExtents();
        for (int signs = 0; signs < 8; signs++) {
            Vec3d intrinsicCorner = box.localToWorld(new Vec3d(
                    (signs & 1) == 0 ? -half.x() : half.x(),
                    (signs & 2) == 0 ? -half.y() : half.y(),
                    (signs & 4) == 0 ? -half.z() : half.z()));
            Vec3d parentCorner = localToParent.transformPoint(intrinsicCorner);
            minimum = minimum.min(parentCorner);
            maximum = maximum.max(parentCorner);
        }
        return new Aabb(minimum, maximum);
    }

    private static Aabb compositeBounds(Composite composite, RigidTransform3d localToParent) {
        Aabb first = primitiveBounds(composite.child(0), localToParent);
        Vec3d minimum = first.min();
        Vec3d maximum = first.max();
        for (int index = 1; index < composite.childCount(); index++) {
            Aabb childBounds = primitiveBounds(composite.child(index), localToParent);
            minimum = minimum.min(childBounds.min());
            maximum = maximum.max(childBounds.max());
        }
        return new Aabb(minimum, maximum);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof PlacedSolid3d other
                && localSolid.equals(other.localSolid)
                && localToParent.equals(other.localToParent);
    }

    @Override
    public int hashCode() {
        return 31 * localSolid.hashCode() + localToParent.hashCode();
    }

    @Override
    public String toString() {
        return "PlacedSolid3d[localSolid=" + localSolid + ", localToParent=" + localToParent + "]";
    }
}
