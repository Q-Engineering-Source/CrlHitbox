package dev.crlhitbox.api.geometry;

/** A finite geometry value that exposes a closed world-axis-aligned bound. */
public interface Bounded3d {
    /** Returns this value's finite, closed world-axis-aligned bounds. */
    Aabb bounds();
}
