package dev.crlhitbox.api.geometry.cast;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Vec3d;

import java.util.Objects;

/** Entry point for the separately versioned capsule/AABB translation contract. */
public final class CapsuleAabbCast3d {
    private CapsuleAabbCast3d() {
    }

    /** Runtime capability check; deliberately a method rather than an inlined constant. */
    public static int contractRevision() {
        return 1;
    }

    /**
     * Queries the entire fixed-orientation translation against one static box.
     * HIT brackets contact-band entry, not exact physical TOI; absent prefixes
     * are not safe motion. See the result kind and the versioned contract.
     */
    public static CapsuleAabbCastResult3d cast(Capsule capsule, Vec3d delta, Aabb obstacle) {
        Objects.requireNonNull(capsule, "capsule");
        Objects.requireNonNull(delta, "delta");
        Objects.requireNonNull(obstacle, "obstacle");
        return CapsuleAabbCastKernel.cast(capsule, delta, obstacle);
    }
}
