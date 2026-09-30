package dev.crlhitbox.bench;

import dev.crlhitbox.api.collider.ColliderQueries;
import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Diagnostic decomposition of the two rotated benchmark items.
 *
 * <p>These are diagnostic, not part of the six targets. They measure each step the rotated items
 * perform inside their measured region — segment construction, shape construction, shape bounds, full
 * snapshot construction, the setter, and the query alone — so a decision about touching frozen
 * geometry constructors can be based on measurements rather than on a guess.</p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(1)
public class RotationDiagnostics {
    private static final double CENTERLINE_LENGTH = 1.0D;
    private static final double RADIUS = 0.25D;
    private static final RigidTransform3d IDENTITY = RigidTransform3d.identity();

    @Benchmark
    public Segment3d segmentConstruct(CollisionState state) {
        Rotation3d orientation = state.nextOrientation();
        Vec3d half = orientation.rotate(new Vec3d(0.0D, 1.0D, 0.0D))
                .multiply(CENTERLINE_LENGTH * 0.5D);
        return new Segment3d(half.negate(), half);
    }

    @Benchmark
    public Capsule capsuleConstruct(CollisionState state) {
        Rotation3d orientation = state.nextOrientation();
        Vec3d half = orientation.rotate(new Vec3d(0.0D, 1.0D, 0.0D))
                .multiply(CENTERLINE_LENGTH * 0.5D);
        return new Capsule(new Segment3d(half.negate(), half), RADIUS);
    }

    @Benchmark
    public Aabb capsuleBounds(CollisionState state) {
        Rotation3d orientation = state.nextOrientation();
        Vec3d half = orientation.rotate(new Vec3d(0.0D, 1.0D, 0.0D))
                .multiply(CENTERLINE_LENGTH * 0.5D);
        Capsule capsule = new Capsule(new Segment3d(half.negate(), half), RADIUS);
        return capsule.bounds();
    }

    @Benchmark
    public Aabb obbBounds(CollisionState state) {
        Rotation3d orientation = state.nextOrientation();
        Obb obb = new Obb(new Vec3d(0.0D, 0.0D, 0.0D),
                new Vec3d(0.5D, 0.5D, 0.5D), orientation);
        return obb.bounds();
    }

    /** Everything the rotated capsule item builds per operation, without the query. */
    @Benchmark
    public ColliderSnapshot capsuleSnapshotBuild(CollisionState state) {
        Rotation3d orientation = state.nextOrientation();
        Vec3d half = orientation.rotate(new Vec3d(0.0D, 1.0D, 0.0D))
                .multiply(CENTERLINE_LENGTH * 0.5D);
        Capsule capsule = new Capsule(new Segment3d(half.negate(), half), RADIUS);
        return new SolidColliderSnapshot(capsule, IDENTITY, true);
    }

    /** The setter alone, exactly as the rotated item uses it. */
    @Benchmark
    public boolean capsuleSetOrientation(CollisionState state) {
        return state.rotatingCapsule().setOrientation(state.nextOrientation());
    }

    /** The query alone over an already built snapshot pair. */
    @Benchmark
    public boolean capsuleQueryOnly(CollisionState state) {
        return ColliderQueries.intersects(
                state.rotatingCapsule().snapshot(), state.rotatingCapsulePartner());
    }
}
