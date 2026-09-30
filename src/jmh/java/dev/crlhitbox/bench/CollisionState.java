package dev.crlhitbox.bench;

import dev.crlhitbox.api.collider.ColliderQueries;
import dev.crlhitbox.api.collider.ColliderSnapshot;
import dev.crlhitbox.api.collider.MutableCapsuleCollider;
import dev.crlhitbox.api.collider.MutableObbCollider;
import dev.crlhitbox.api.collider.SolidColliderSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Obb;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Rotation3d;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Sphere;
import dev.crlhitbox.api.geometry.Vec3d;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Frozen benchmark inputs for the six collider throughput targets.
 *
 * <p>The data set is built once per trial, outside the measurement, and holds a fixed mix of
 * intersecting and separated pairs so that a benchmark cannot score by always taking an early exit.
 * Inputs are read out by index, so the measured operation allocates nothing of its own; the two
 * rotated items intentionally rebuild a collider through its public setter and take a fresh snapshot
 * inside the measured operation, as the roadmap requires.</p>
 *
 * <p><strong>The mix is asserted, not assumed.</strong> A previous revision of this file claimed a
 * mixed workload while every pair was in fact separated, which inflated the measured throughput by
 * letting every query take its cheapest early exit. Setup now runs each prepared pair through the
 * public query entry and fails the whole run if a pair does not produce the result its index
 * promises, and it verifies that each rotated sequence really contains both outcomes. A silent
 * regression of the inputs is therefore impossible.</p>
 */
@State(Scope.Thread)
public class CollisionState {
    /** Number of prepared pairs per shape; a power of two so indexing can mask instead of divide. */
    static final int FIXTURES = 64;

    private static final int ROTATION_STEPS = 64;

    /**
     * Centre distance of the intersecting fixtures.
     *
     * <p>Half extents and radii are 0.5 throughout, so two axis-aligned boxes still overlap while
     * their centres are at most 1.0 apart on an axis, and two spheres still overlap while their
     * centres are at most 1.0 apart in total. A distance of 0.5 leaves real margin on every shape:
     * box axes 0.5 < 1.0, sphere centres 0.559 < 1.0, capsule centre lines 0.25 apart against a
     * combined radius of 0.5.</p>
     */
    private static final double SEPARATION_INTERSECTING = 0.5D;

    /** Centre distance of the separated fixtures, far outside every touching range above. */
    private static final double SEPARATION_SEPARATED = 6.0D;

    private ColliderSnapshot[] aabbFirst;
    private ColliderSnapshot[] aabbSecond;
    private ColliderSnapshot[] sphereFirst;
    private ColliderSnapshot[] sphereSecond;
    private ColliderSnapshot[] obbFirst;
    private ColliderSnapshot[] obbSecond;
    private ColliderSnapshot[] capsuleFirst;
    private ColliderSnapshot[] capsuleSecond;

    private MutableObbCollider rotatingObb;
    private ColliderSnapshot rotatingObbPartner;
    private MutableCapsuleCollider rotatingCapsule;
    private ColliderSnapshot rotatingCapsulePartner;
    private Rotation3d[] orientations;

    private int aabbIndex;
    private int sphereIndex;
    private int obbIndex;
    private int capsuleIndex;
    private int rotationIndex;

    @Setup(Level.Trial)
    public void setup() {
        aabbFirst = new ColliderSnapshot[FIXTURES];
        aabbSecond = new ColliderSnapshot[FIXTURES];
        sphereFirst = new ColliderSnapshot[FIXTURES];
        sphereSecond = new ColliderSnapshot[FIXTURES];
        obbFirst = new ColliderSnapshot[FIXTURES];
        obbSecond = new ColliderSnapshot[FIXTURES];
        capsuleFirst = new ColliderSnapshot[FIXTURES];
        capsuleSecond = new ColliderSnapshot[FIXTURES];
        orientations = new Rotation3d[ROTATION_STEPS];

        for (int index = 0; index < FIXTURES; index++) {
            double offset = (index - FIXTURES / 2) * 0.25D;
            // Even indices must intersect, odd indices must be clearly separated.
            double separation = (index % 2 == 0) ? SEPARATION_INTERSECTING : SEPARATION_SEPARATED;
            Vec3d firstCenter = new Vec3d(offset, offset * 0.5D, -offset * 0.25D);
            Vec3d secondCenter = firstCenter.add(new Vec3d(separation, separation * 0.5D, 0.0D));

            aabbFirst[index] = solid(new Aabb(
                    firstCenter.subtract(new Vec3d(0.5D, 0.5D, 0.5D)),
                    firstCenter.add(new Vec3d(0.5D, 0.5D, 0.5D))));
            aabbSecond[index] = solid(new Aabb(
                    secondCenter.subtract(new Vec3d(0.5D, 0.5D, 0.5D)),
                    secondCenter.add(new Vec3d(0.5D, 0.5D, 0.5D))));

            sphereFirst[index] = solid(new Sphere(firstCenter, 0.5D));
            sphereSecond[index] = solid(new Sphere(secondCenter, 0.5D));

            obbFirst[index] = solid(new Obb(firstCenter, new Vec3d(0.5D, 0.5D, 0.5D),
                    Rotation3d.identity()));
            obbSecond[index] = solid(new Obb(secondCenter, new Vec3d(0.5D, 0.5D, 0.5D),
                    Rotation3d.identity()));

            capsuleFirst[index] = solid(new Capsule(new Segment3d(
                    firstCenter.subtract(new Vec3d(0.5D, 0.0D, 0.0D)),
                    firstCenter.add(new Vec3d(0.5D, 0.0D, 0.0D))), 0.25D));
            capsuleSecond[index] = solid(new Capsule(new Segment3d(
                    secondCenter.subtract(new Vec3d(0.5D, 0.0D, 0.0D)),
                    secondCenter.add(new Vec3d(0.5D, 0.0D, 0.0D))), 0.25D));
        }

        for (int index = 0; index < ROTATION_STEPS; index++) {
            double half = Math.PI * index / ROTATION_STEPS;
            orientations[index] = new Rotation3d(0.0D, 0.0D, Math.sin(half), Math.cos(half));
        }

        // Anisotropic half extents, so rotating the box really changes the volume it covers; a cube
        // would occupy the same region at every angle and make the rotated item meaningless. The
        // partner sits far enough out that some orientations miss it and some hit it, which
        // verifyRotationMix asserts.
        rotatingObb = new MutableObbCollider(
                new Vec3d(0.0D, 0.0D, 0.0D), new Vec3d(0.5D, 0.25D, 0.5D), Rotation3d.identity());
        rotatingObbPartner = solid(new Obb(
                new Vec3d(0.85D, 0.2D, 0.0D), new Vec3d(0.5D, 0.5D, 0.5D), Rotation3d.identity()));
        rotatingCapsule = new MutableCapsuleCollider(
                new Vec3d(0.0D, 0.0D, 0.0D), 1.0D, 0.25D, Rotation3d.identity());
        rotatingCapsulePartner = solid(new Capsule(new Segment3d(
                new Vec3d(0.7D, -0.5D, 0.0D), new Vec3d(0.7D, 0.5D, 0.0D)), 0.25D));

        verifyFixtureMix();
        verifyRotationMix();
    }

    /** Fails the run when a prepared pair does not produce the outcome its index promises. */
    private void verifyFixtureMix() {
        for (int index = 0; index < FIXTURES; index++) {
            boolean expected = index % 2 == 0;
            requireResult(aabbFirst[index], aabbSecond[index], expected, "aabb[]", index);
            requireResult(sphereFirst[index], sphereSecond[index], expected, "sphere[]", index);
            requireResult(obbFirst[index], obbSecond[index], expected, "obb[]", index);
            requireResult(capsuleFirst[index], capsuleSecond[index], expected, "capsule[]", index);
        }
    }

    /** Fails the run when a rotated sequence is degenerate, i.e. always hitting or never hitting. */
    private void verifyRotationMix() {
        requireMixed(countObbHits(), "rotatedObbPair");
        requireMixed(countCapsuleHits(), "rotatedCapsulePair");
    }

    private int countObbHits() {
        int hits = 0;
        for (int index = 0; index < ROTATION_STEPS; index++) {
            rotatingObb.setOrientation(orientations[index]);
            if (ColliderQueries.intersects(rotatingObb.snapshot(), rotatingObbPartner)) {
                hits++;
            }
        }
        return hits;
    }

    private int countCapsuleHits() {
        int hits = 0;
        for (int index = 0; index < ROTATION_STEPS; index++) {
            rotatingCapsule.setOrientation(orientations[index]);
            if (ColliderQueries.intersects(
                    rotatingCapsule.snapshot(), rotatingCapsulePartner)) {
                hits++;
            }
        }
        return hits;
    }

    private static void requireResult(
            ColliderSnapshot first,
            ColliderSnapshot second,
            boolean expected,
            String shape,
            int index
    ) {
        boolean actual = ColliderQueries.intersects(first, second);
        if (actual != expected) {
            throw new IllegalStateException("benchmark input " + shape + index
                    + " expected intersecting=" + expected + " but the public query returned " + actual);
        }
    }

    private static void requireMixed(int hits, String item) {
        if (hits == 0 || hits == ROTATION_STEPS) {
            throw new IllegalStateException("benchmark input " + item
                    + " is degenerate: " + hits + " of " + ROTATION_STEPS
                    + " orientations produced a hit, so the item has no mixed workload");
        }
    }

    int nextAabbIndex() {
        return aabbIndex = (aabbIndex + 1) & (FIXTURES - 1);
    }

    ColliderSnapshot aabbFirst(int index) {
        return aabbFirst[index];
    }

    ColliderSnapshot aabbSecond(int index) {
        return aabbSecond[index];
    }

    int nextSphereIndex() {
        return sphereIndex = (sphereIndex + 1) & (FIXTURES - 1);
    }

    ColliderSnapshot sphereFirst(int index) {
        return sphereFirst[index];
    }

    ColliderSnapshot sphereSecond(int index) {
        return sphereSecond[index];
    }

    int nextObbIndex() {
        return obbIndex = (obbIndex + 1) & (FIXTURES - 1);
    }

    ColliderSnapshot obbFirst(int index) {
        return obbFirst[index];
    }

    ColliderSnapshot obbSecond(int index) {
        return obbSecond[index];
    }

    int nextCapsuleIndex() {
        return capsuleIndex = (capsuleIndex + 1) & (FIXTURES - 1);
    }

    ColliderSnapshot capsuleFirst(int index) {
        return capsuleFirst[index];
    }

    ColliderSnapshot capsuleSecond(int index) {
        return capsuleSecond[index];
    }

    Rotation3d nextOrientation() {
        return orientations[rotationIndex = (rotationIndex + 1) & (ROTATION_STEPS - 1)];
    }

    MutableObbCollider rotatingObb() {
        return rotatingObb;
    }

    ColliderSnapshot rotatingObbPartner() {
        return rotatingObbPartner;
    }

    MutableCapsuleCollider rotatingCapsule() {
        return rotatingCapsule;
    }

    ColliderSnapshot rotatingCapsulePartner() {
        return rotatingCapsulePartner;
    }

    private static ColliderSnapshot solid(dev.crlhitbox.api.geometry.Solid3d solid) {
        return new SolidColliderSnapshot(solid, RigidTransform3d.identity(), true);
    }
}
