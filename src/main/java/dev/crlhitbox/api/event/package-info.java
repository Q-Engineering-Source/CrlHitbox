/**
 * Explicit update entries for entity collider caches.
 *
 * <p>{@link dev.crlhitbox.api.event.EntityColliderUpdateEvent} lets another mod recompute collider
 * shapes from its own state and publish them into the cache. It is an update entry rather than a
 * collision callback: collision queries never post events, and this event never triggers networking,
 * persistence, world mutation, or damage by itself.</p>
 */
package dev.crlhitbox.api.event;
