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
 */
package dev.crlhitbox.api.geometry;
