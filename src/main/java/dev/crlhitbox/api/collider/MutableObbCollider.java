package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Vec3d;

/**
 * Mutable oriented box collider.
 *
 * <p>The shape orientation is internal to the box and is deliberately distinct from the whole
 * collider's {@code localToParent} placement: a caller can rotate the box inside its own frame
 * without touching where the collider sits, and vice versa. Zero extents on one or more axes are
 * valid closed sets; a negative or non-finite extent is rejected without changing any state.</p>
 */
public final class MutableObbCollider extends AbstractMutableSolidCollider {
    private Vec3d center;
    private Vec3d halfExtents;
    private Rotation3d orientation;

    /** Creates one oriented box collider from a local center, half extents and orientation. */
    public MutableObbCollider(Vec3d center, Vec3d halfExtents, Rotation3d orientation) {
        super(new Obb(
                requirePoint(center, "center"),
                requirePoint(halfExtents, "halfExtents"),
                requireRotation(orientation)));
        this.center = requirePoint(center, "center");
        this.halfExtents = requirePoint(halfExtents, "halfExtents");
        this.orientation = requireRotation(orientation);
    }

    /** Returns the local center. */
    public Vec3d center() {
        return center;
    }

    /** Returns the symmetric local half extents. */
    public Vec3d halfExtents() {
        return halfExtents;
    }

    /** Returns the shape's own orientation. */
    public Rotation3d orientation() {
        return orientation;
    }

    /** Replaces the local center. */
    public boolean setCenter(Vec3d center) {
        Vec3d checked = requirePoint(center, "center");
        if (checked.equals(this.center)) {
            return false;
        }
        if (!publishSolid(new Obb(checked, halfExtents, orientation))) {
            return false;
        }
        this.center = checked;
        return true;
    }

    /** Replaces the symmetric local half extents. */
    public boolean setHalfExtents(Vec3d halfExtents) {
        Vec3d checked = requirePoint(halfExtents, "halfExtents");
        if (checked.equals(this.halfExtents)) {
            return false;
        }
        if (!publishSolid(new Obb(center, checked, orientation))) {
            return false;
        }
        this.halfExtents = checked;
        return true;
    }

    /** Replaces the shape's own orientation. */
    public boolean setOrientation(Rotation3d orientation) {
        Rotation3d checked = requireRotation(orientation);
        if (checked.equals(this.orientation)) {
            return false;
        }
        if (!publishSolid(new Obb(center, halfExtents, checked))) {
            return false;
        }
        this.orientation = checked;
        return true;
    }

    /**
     * Replaces center, half extents and orientation as one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean setShape(Vec3d center, Vec3d halfExtents, Rotation3d orientation) {
        Vec3d checkedCenter = requirePoint(center, "center");
        Vec3d checkedExtents = requirePoint(halfExtents, "halfExtents");
        Rotation3d checkedOrientation = requireRotation(orientation);
        if (checkedCenter.equals(this.center)
                && checkedExtents.equals(this.halfExtents)
                && checkedOrientation.equals(this.orientation)) {
            return false;
        }
        if (!publishSolid(new Obb(checkedCenter, checkedExtents, checkedOrientation))) {
            return false;
        }
        this.center = checkedCenter;
        this.halfExtents = checkedExtents;
        this.orientation = checkedOrientation;
        return true;
    }

    @Override
    Solid3d currentSolid() {
        return new Obb(center, halfExtents, orientation);
    }
}
