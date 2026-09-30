package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Mutable, editable compound of immutable collider snapshots.
 *
 * <p>Children are snapshots, so the compound holds snapshot-style ownership: adding
 * {@code childCollider.snapshot()} means later mutation of the source collider cannot change this
 * compound, and a change must be published explicitly through {@link #setChild(int, ColliderSnapshot)}
 * or {@link #replaceChildren(List)}. Insertion order is preserved, an empty compound is legal and
 * represents the empty set, and children may themselves be compounds so multi-level trees are
 * expressed by replacing one level with a new child snapshot at a time.</p>
 *
 * <p>Every effective edit validates the complete candidate first and publishes it in one step, so a
 * rejected index, a {@code null} child or a revision overflow leaves the previous children, snapshot
 * and revision untouched. Queries traverse deep trees iteratively in the query dispatcher, so no
 * user-controlled recursion is introduced here.</p>
 */
public final class MutableCompoundCollider extends AbstractMutableCollider {
    private ColliderSnapshot[] children;

    /** Creates an empty compound; an empty compound intersects nothing and is not drawn. */
    public MutableCompoundCollider() {
        this(new ColliderSnapshot[0]);
    }

    /** Creates a compound from the given ordered child snapshots. */
    public MutableCompoundCollider(List<? extends ColliderSnapshot> children) {
        this(ColliderSnapshotFactory.copyChildren(children));
    }

    private MutableCompoundCollider(ColliderSnapshot[] children) {
        super(new CompoundColliderSnapshot(
                Arrays.asList(children), RigidTransform3d.identity(), true));
        this.children = children;
    }

    /** Returns the number of direct children. */
    public int childCount() {
        return children.length;
    }

    /** Returns the direct child at {@code index} in insertion order. */
    public ColliderSnapshot child(int index) {
        Objects.checkIndex(index, children.length);
        return children[index];
    }

    /** Appends one child as a single effective change. */
    public void addChild(ColliderSnapshot child) {
        Objects.requireNonNull(child, "child");
        ColliderSnapshot[] next = Arrays.copyOf(children, children.length + 1);
        next[children.length] = child;
        publishChildren(next);
    }

    /**
     * Replaces one child.
     *
     * @return whether this call changed the state
     */
    public boolean setChild(int index, ColliderSnapshot child) {
        Objects.requireNonNull(child, "child");
        Objects.checkIndex(index, children.length);
        if (children[index].equals(child)) {
            return false;
        }
        ColliderSnapshot[] next = children.clone();
        next[index] = child;
        return publishChildren(next);
    }

    /**
     * Removes one child as a single effective change.
     *
     * @return the removed child
     */
    public ColliderSnapshot removeChild(int index) {
        Objects.checkIndex(index, children.length);
        ColliderSnapshot removed = children[index];
        ColliderSnapshot[] next = new ColliderSnapshot[children.length - 1];
        System.arraycopy(children, 0, next, 0, index);
        System.arraycopy(children, index + 1, next, index, children.length - index - 1);
        publishChildren(next);
        return removed;
    }

    /**
     * Replaces every child after validating the whole candidate list.
     *
     * @return whether this call changed the state
     */
    public boolean replaceChildren(List<? extends ColliderSnapshot> children) {
        return publishChildren(ColliderSnapshotFactory.copyChildren(children));
    }

    /**
     * Removes every child as one effective change.
     *
     * @return whether this call changed the state
     */
    public boolean clearChildren() {
        if (children.length == 0) {
            return false;
        }
        return publishChildren(new ColliderSnapshot[0]);
    }

    @Override
    ColliderSnapshot buildSnapshot(boolean enabled, RigidTransform3d localToParent) {
        return new CompoundColliderSnapshot(Arrays.asList(children), localToParent, enabled);
    }

    private boolean publishChildren(ColliderSnapshot[] next) {
        ColliderSnapshot candidate = new CompoundColliderSnapshot(
                Arrays.asList(next), localToParent(), enabled());
        if (candidate.equals(snapshot())) {
            return false;
        }
        if (!publishSnapshot(candidate)) {
            return false;
        }
        children = next;
        return true;
    }
}
