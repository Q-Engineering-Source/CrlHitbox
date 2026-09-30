/**
 * Open collider API: mutable facades, immutable snapshots and the unified query entry point.
 *
 * <p>This layer exists so that other mods can build and update colliders, publish immutable
 * snapshots, attach them to an Entity, and query them through public API only. It is pure JDK and
 * depends only on {@code dev.crlhitbox.api.geometry}; it never references Minecraft, Forge, Netty,
 * rendering or JMH types, and it is compiled by the isolated verification task with an empty external
 * classpath.</p>
 *
 * <p>Queries read snapshots and never call provider code, so a mutable collider cannot change shape
 * during a query and a query never fires an event, applies damage, or touches the world.</p>
 */
package dev.crlhitbox.api.collider;
