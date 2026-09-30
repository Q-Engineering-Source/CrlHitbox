package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.RigidTransform3d;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of an ordered compound of other snapshots.
 *
 * <p>Children are already immutable snapshots, so a compound owns them defensively and never holds a
 * live reference to a mutable child: mutating the mutable object that produced a child cannot change
 * this compound. Each child's {@code localToParent} places it into this compound's local frame, and
 * the compound's own {@code localToParent} is applied once on top, so an entity or world transform is
 * never stacked twice.</p>
 *
 * <p>An empty compound, a disabled compound, or a compound whose enabled leaves all represent the
 * empty set has no bounds; that is reported as {@link Optional#empty()} and means "intersects
 * nothing", not "unbounded" or "unknown".</p>
 */
public final class CompoundColliderSnapshot implements ColliderSnapshot {
    private final ColliderSnapshot[] children;
    private final RigidTransform3d localToParent;
    private final boolean enabled;
    private final Aabb bounds;

    /**
     * Creates one compound snapshot.
     *
     * @param children the ordered child snapshots; never {@code null} and never containing {@code null}
     * @param localToParent the placement of the whole compound into the parent frame
     * @param enabled whether the compound participates in queries and rendering
     * @throws IllegalArgumentException when an enabled compound has no representable finite
     *         parent-frame bounds
     */
    public CompoundColliderSnapshot(
            List<? extends ColliderSnapshot> children,
            RigidTransform3d localToParent,
            boolean enabled
    ) {
        this.children = ColliderSnapshotFactory.copyChildren(children);
        this.localToParent = ColliderSnapshotFactory.requireTransform(localToParent);
        this.enabled = enabled;
        this.bounds = ColliderSnapshotFactory.placeBounds(
                ColliderSnapshotFactory.unionOfChildren(this.children), localToParent, enabled);
    }

    /** Returns the number of direct children. */
    public int childCount() {
        return children.length;
    }

    /** Returns the child at {@code index} in insertion order. */
    public ColliderSnapshot child(int index) {
        return children[index];
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public RigidTransform3d localToParent() {
        return localToParent;
    }

    @Override
    public Optional<Aabb> bounds() {
        return Optional.ofNullable(bounds);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof CompoundColliderSnapshot other
                && enabled == other.enabled
                && java.util.Arrays.equals(children, other.children)
                && localToParent.equals(other.localToParent);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * java.util.Arrays.hashCode(children) + localToParent.hashCode())
                + Boolean.hashCode(enabled);
    }

    @Override
    public String toString() {
        return "CompoundColliderSnapshot[children=" + children.length
                + ", localToParent=" + localToParent
                + ", enabled=" + enabled
                + ", bounds=" + bounds + "]";
    }
}
