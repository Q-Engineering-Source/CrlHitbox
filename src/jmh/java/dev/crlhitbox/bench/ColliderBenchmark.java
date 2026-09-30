package dev.crlhitbox.bench;

import dev.crlhitbox.api.collider.ColliderQueries;
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
 * The six collider throughput benchmarks of the handover roadmap (section 13.9).
 *
 * <p>Every item measures the public production entry {@link ColliderQueries#intersects} over
 * pre-built immutable snapshots, so no shortcut and no private kernel is used to obtain a score. The
 * two rotated items rebuild a collider through its public setter and take a fresh snapshot inside the
 * measured operation, which is why their expected throughput is lower; that allocation is part of the
 * measurement and is never moved into the setup.</p>
 *
 * <p>Protocol, frozen before the first run: throughput in ops/s, single thread, 3 JVM forks, 5 warm-up
 * iterations and 5 measurement iterations of one second each, with a fixed intersecting/separated
 * input mix.</p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@Threads(1)
public class ColliderBenchmark {
    @Benchmark
    public boolean aabbPair(CollisionState state) {
        int index = state.nextAabbIndex();
        return ColliderQueries.intersects(state.aabbFirst(index), state.aabbSecond(index));
    }

    @Benchmark
    public boolean spherePair(CollisionState state) {
        int index = state.nextSphereIndex();
        return ColliderQueries.intersects(state.sphereFirst(index), state.sphereSecond(index));
    }

    @Benchmark
    public boolean obbPair(CollisionState state) {
        int index = state.nextObbIndex();
        return ColliderQueries.intersects(state.obbFirst(index), state.obbSecond(index));
    }

    @Benchmark
    public boolean capsulePair(CollisionState state) {
        int index = state.nextCapsuleIndex();
        return ColliderQueries.intersects(state.capsuleFirst(index), state.capsuleSecond(index));
    }

    @Benchmark
    public boolean rotatedObbPair(CollisionState state) {
        state.rotatingObb().setOrientation(state.nextOrientation());
        return ColliderQueries.intersects(state.rotatingObb().snapshot(), state.rotatingObbPartner());
    }

    @Benchmark
    public boolean rotatedCapsulePair(CollisionState state) {
        state.rotatingCapsule().setOrientation(state.nextOrientation());
        return ColliderQueries.intersects(
                state.rotatingCapsule().snapshot(), state.rotatingCapsulePartner());
    }
}
