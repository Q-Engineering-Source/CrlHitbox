package dev.crlhitbox.bench;

import dev.crlhitbox.api.collider.ColliderQueries;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.GeometryIntersections;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Diagnostic benchmarks that decompose the cost of one collider query.
 *
 * <p>These are <em>not</em> part of the six roadmap targets. They exist to answer two questions with
 * evidence: how much of the measured cost belongs to the frozen typed kernel itself, and how much to
 * the public snapshot layer above it. The constant-input pair is a deliberate validity control — if
 * it scores orders of magnitude above the real inputs, the harness can be folded and its numbers must
 * not be compared with anything.</p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(1)
public class ColliderDiagnostics {
    private static final Aabb CONSTANT_FIRST =
            new Aabb(new Vec3d(-0.5D, -0.5D, -0.5D), new Vec3d(0.5D, 0.5D, 0.5D));
    private static final Aabb CONSTANT_SECOND =
            new Aabb(new Vec3d(0.25D, 0.25D, 0.25D), new Vec3d(1.25D, 1.25D, 1.25D));

    @Benchmark
    public boolean rawAabbPair(CollisionState state) {
        int index = state.nextAabbIndex();
        return GeometryIntersections.intersects(
                (Aabb) ((SolidColliderSnapshot) state.aabbFirst(index)).solid(),
                (Aabb) ((SolidColliderSnapshot) state.aabbSecond(index)).solid());
    }

    @Benchmark
    public boolean snapshotAabbPair(CollisionState state) {
        int index = state.nextAabbIndex();
        return ColliderQueries.intersects(state.aabbFirst(index), state.aabbSecond(index));
    }

    @Benchmark
    public boolean rawSpherePair(CollisionState state) {
        int index = state.nextSphereIndex();
        return GeometryIntersections.intersects(
                (Sphere) ((SolidColliderSnapshot) state.sphereFirst(index)).solid(),
                (Sphere) ((SolidColliderSnapshot) state.sphereSecond(index)).solid());
    }

    @Benchmark
    public boolean snapshotSpherePair(CollisionState state) {
        int index = state.nextSphereIndex();
        return ColliderQueries.intersects(state.sphereFirst(index), state.sphereSecond(index));
    }

    /** Validity control: constant inputs, so folding would show up as an implausible score. */
    @Benchmark
    public boolean constantAabbPair() {
        return GeometryIntersections.intersects(CONSTANT_FIRST, CONSTANT_SECOND);
    }
}
