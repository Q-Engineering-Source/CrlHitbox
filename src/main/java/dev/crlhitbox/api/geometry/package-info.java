/**
 * Immutable, JDK-only, double-precision pointwise geometry values.
 *
 * <p>Coordinates, extents, radii, and lengths use one caller-chosen linear unit; squared-distance
 * results use the corresponding squared unit. All inputs and mandatory bounds are finite; signed zero
 * is canonicalized and equality is exact over canonical stored values. Shapes are closed sets, so
 * boundary points are contained. This package has no platform, rendering, networking, or
 * certified-geometry dependency.</p>
 */
package dev.crlhitbox.api.geometry;
