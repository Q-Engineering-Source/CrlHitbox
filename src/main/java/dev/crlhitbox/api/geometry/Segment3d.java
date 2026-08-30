package dev.crlhitbox.api.geometry;

import java.util.Objects;

/** An immutable finite closed segment whose endpoint order is canonicalized lexicographically. */
public final class Segment3d implements Bounded3d {
    private final Vec3d start;
    private final Vec3d end;
    private final Vec3d delta;
    private final Aabb bounds;

    /**
     * Creates a finite segment. Endpoint subtraction must produce finite components so that
     * {@link #delta()} remains total; this is a representability precondition, not a ban on zero length.
     */
    public Segment3d(Vec3d first, Vec3d second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (compare(first, second) <= 0) { this.start = first; this.end = second; } else { this.start = second; this.end = first; }
        this.delta = end.subtract(start);
        this.bounds = new Aabb(start.min(end), start.max(end));
    }

    /** Returns the canonical first endpoint. */
    public Vec3d start() { return start; }
    /** Returns the canonical second endpoint. */
    public Vec3d end() { return end; }
    /** Returns {@code end - start}. */
    public Vec3d delta() { return delta; }
    /** Returns the closed bounds containing both endpoints. */
    @Override public Aabb bounds() { return bounds; }

    private static int compare(Vec3d first, Vec3d second) {
        int result = Double.compare(first.x(), second.x());
        if (result != 0) return result;
        result = Double.compare(first.y(), second.y());
        return result != 0 ? result : Double.compare(first.z(), second.z());
    }
    @Override public boolean equals(Object object) { return this == object || object instanceof Segment3d other && start.equals(other.start) && end.equals(other.end); }
    @Override public int hashCode() { return 31 * start.hashCode() + end.hashCode(); }
    @Override public String toString() { return "Segment3d[start=" + start + ", end=" + end + "]"; }
}
