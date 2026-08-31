/**
 * Immutable, JDK-only, double-precision pointwise geometry values.
 *
 * <p>Coordinates, extents, radii, and lengths use one caller-chosen linear unit; squared-distance
 * results use the corresponding squared unit. All inputs and mandatory bounds are finite; signed zero
 * is canonicalized and equality is exact over canonical stored values. Shapes are closed sets, so
 * boundary points are contained. This package has no platform, rendering, networking, or
 * certified-geometry dependency.</p>
 *
 * <p>{@link dev.crlhitbox.api.geometry.Solid3d} is the sealed closed-solid abstraction;
 * {@link dev.crlhitbox.api.geometry.Segment3d} remains bounded but is not a solid.
 * {@link dev.crlhitbox.api.geometry.Composite} represents a nonempty finite union whose nested
 * inputs are flattened into an ordered primitive-leaf sequence. Composite equality is exact
 * structural equality of that canonical sequence, not general geometric-set equality.</p>
 *
 * <p>{@link dev.crlhitbox.api.geometry.RigidTransform3d} is an active source/local-to-target/parent
 * rigid transform: points map as {@code R(p) + t}, while vectors omit translation. Composition is
 * ordered as {@code a.andThen(b) = b ∘ a}. This package does not yet define transformed-solid
 * wrappers or production projection of exact solid representations.</p>
 */
package dev.crlhitbox.api.geometry;
