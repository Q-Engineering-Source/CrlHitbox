package dev.crlhitbox.internal.entity;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local, non-persistent allocator for provider/incarnation holder generations.
 *
 * <p>Each attached provider receives one positive {@code long} generation that is immutable for
 * that provider and distinct from every other provider until exhaustion. Allocation is monotonic
 * inside one physical JVM process; it is never random, never wall-clock derived, never derived from
 * an Entity ID or UUID, never persisted, and retains no Entity, holder, provider, world, or snapshot
 * reference. A server restart may restart the allocator, and client cleanup makes ordering
 * session-local, so neither global uniqueness nor persistence is claimed.</p>
 *
 * <p>The allocator fails <em>before</em> returning a nonpositive or wrapped value: exhaustion throws
 * instead of silently wrapping to a negative generation.</p>
 */
final class HolderGenerationAllocator {
    private static final AtomicLong PROCESS_COUNTER = new AtomicLong();

    private HolderGenerationAllocator() {
    }

    /** Allocates the next positive process-local generation. */
    static long allocate() {
        return allocateFrom(PROCESS_COUNTER);
    }

    /**
     * Allocation core over an explicit counter.
     *
     * <p>The counter is a parameter so that near-exhaustion and exhaustion behavior can be tested
     * without adding a production reset hook or a second allocator.</p>
     */
    static long allocateFrom(AtomicLong counter) {
        while (true) {
            long current = counter.get();
            if (current == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "holder generation allocator exhausted at " + current);
            }
            long next = current + 1L;
            if (next <= 0L) {
                throw new IllegalStateException(
                        "holder generation allocator would wrap past " + current);
            }
            if (counter.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
