package dev.crlhitbox.api.geometry;

/** A closed solid geometry value with finite world-axis-aligned bounds. */
public sealed interface Solid3d extends Bounded3d
        permits Aabb, Sphere, Obb, Capsule, Composite {
}
