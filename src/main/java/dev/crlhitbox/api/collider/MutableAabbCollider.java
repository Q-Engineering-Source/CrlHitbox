package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Vec3d;

/**
 * Mutable axis-aligned box collider.
 *
 * <p>The interval pair is published in one step through {@link #setBounds(Vec3d, Vec3d)}; there is
 * deliberately no setter that could leave the value with {@code min > max} in between. Zero-width
 * axes are valid closed sets, while an inverted or non-finite interval is rejected without changing
 * the previous state, revision or snapshot.</p>
 */
public final class MutableAabbCollider extends AbstractMutableCollider {
    private Vec3d min;
    private Vec3d max;

    /** Creates one box collider from its local interval endpoints. */
    public MutableAabbCollider(Vec3d min, Vec3d max) {
        super(new Aabb(requirePoint(min, "min"), requirePoint(max, "max")));
        this.min = requirePoint(min, "min");
        this.max = requirePoint(max, "max");
    }

    /** Returns the local minimum endpoint. */
    public Vec3d min() {
        return min;
    }

    /** Returns the local maximum endpoint. */
    public Vec3d max() {
        return max;
    }

    /**
     * Replaces both endpoints in one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean setBounds(Vec3d min, Vec3d max) {
        Vec3d checkedMin = requirePoint(min, "min");
        Vec3d checkedMax = requirePoint(max, "max");
        if (checkedMin.equals(this.min) && checkedMax.equals(this.max)) {
            return false;
        }
        if (!publishSolid(new Aabb(checkedMin, checkedMax))) {
            return false;
        }
        this.min = checkedMin;
        this.max = checkedMax;
        return true;
    }

    @Override
    Solid3d currentSolid() {
        return new Aabb(min, max);
    }
}
