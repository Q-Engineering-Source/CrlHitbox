package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;

/**
 * Shared mutable state for the collider facades: enable flag, placement, checked revision and the
 * cached immutable snapshot.
 *
 * <p>Every effective change builds and validates the candidate snapshot first and only then publishes
 * it, so an invalid input, an unrepresentable placement or a revision overflow leaves the previous
 * state and snapshot untouched. A change that produces an equal snapshot is a no-op that neither
 * allocates nor advances the revision, and {@link #snapshot()} reuses the cached value instead of
 * rebuilding a tree per query.</p>
 */
abstract class AbstractMutableCollider implements MutableCollider {
    private long revision;
    private boolean enabled = true;
    private RigidTransform3d localToParent = RigidTransform3d.identity();
    private ColliderSnapshot snapshot;

    AbstractMutableCollider(ColliderSnapshot initialSnapshot) {
        this.snapshot = Objects.requireNonNull(initialSnapshot, "initialSnapshot");
    }

    @Override
    public final long revision() {
        return revision;
    }

    @Override
    public final boolean enabled() {
        return enabled;
    }

    @Override
    public final RigidTransform3d localToParent() {
        return localToParent;
    }

    @Override
    public final ColliderSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public final boolean setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        ColliderSnapshot candidate = buildSnapshot(enabled, localToParent);
        this.enabled = enabled;
        this.snapshot = candidate;
        this.revision = nextRevision;
        return true;
    }

    @Override
    public final boolean setLocalToParent(RigidTransform3d transform) {
        RigidTransform3d checked = Objects.requireNonNull(transform, "localToParent");
        if (checked.equals(localToParent)) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        ColliderSnapshot candidate = buildSnapshot(enabled, checked);
        this.localToParent = checked;
        this.snapshot = candidate;
        this.revision = nextRevision;
        return true;
    }

    /**
     * Builds the snapshot for a candidate enable state and placement.
     *
     * <p>The parameters are passed explicitly because the candidate is validated before the state is
     * published.</p>
     */
    abstract ColliderSnapshot buildSnapshot(boolean enabled, RigidTransform3d localToParent);

    /**
     * Publishes one already validated candidate snapshot.
     *
     * @return whether the published snapshot differs from the previous one
     */
    final boolean publishSnapshot(ColliderSnapshot candidate) {
        ColliderSnapshot checked = Objects.requireNonNull(candidate, "candidate");
        if (checked.equals(snapshot)) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        snapshot = checked;
        revision = nextRevision;
        return true;
    }

    private long checkedNextRevision() {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("collider revision overflow");
        }
        return revision + 1L;
    }

    static Vec3d requirePoint(Vec3d point, String name) {
        return Objects.requireNonNull(point, name);
    }

    static Rotation3d requireRotation(Rotation3d rotation) {
        return Objects.requireNonNull(rotation, "orientation");
    }

    static double requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(
                    name + " must be finite and nonnegative: " + value);
        }
        return value;
    }
}
