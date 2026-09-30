package dev.crlhitbox.api.collider;

import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Solid3d;
import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;

/**
 * Shared mutable state for the solid shape colliders: enable flag, placement, checked revision and
 * the cached immutable snapshot.
 *
 * <p>Every effective change builds and validates the candidate snapshot first and only then publishes
 * it, so an invalid input, an unrepresentable placement or a revision overflow leaves the previous
 * state and snapshot untouched. A change that produces an equal snapshot is a no-op that neither
 * allocates nor advances the revision.</p>
 */
abstract class AbstractMutableCollider implements MutableCollider {
    private long revision;
    private boolean enabled = true;
    private RigidTransform3d localToParent = RigidTransform3d.identity();
    private SolidColliderSnapshot snapshot;

    AbstractMutableCollider(Solid3d initialSolid) {
        this.snapshot = new SolidColliderSnapshot(
                Objects.requireNonNull(initialSolid, "initialSolid"), localToParent, enabled);
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
        SolidColliderSnapshot candidate =
                new SolidColliderSnapshot(currentSolid(), localToParent, enabled);
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
        SolidColliderSnapshot candidate =
                new SolidColliderSnapshot(currentSolid(), checked, enabled);
        this.localToParent = checked;
        this.snapshot = candidate;
        this.revision = nextRevision;
        return true;
    }

    /** Rebuilds the current geometric value from this collider's shape parameters. */
    abstract Solid3d currentSolid();

    /**
     * Publishes one already validated shape value as a new snapshot.
     *
     * @return whether the published snapshot differs from the previous one
     */
    final boolean publishSolid(Solid3d solid) {
        SolidColliderSnapshot candidate = new SolidColliderSnapshot(
                Objects.requireNonNull(solid, "solid"), localToParent, enabled);
        if (candidate.equals(snapshot)) {
            return false;
        }
        long nextRevision = checkedNextRevision();
        snapshot = candidate;
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
