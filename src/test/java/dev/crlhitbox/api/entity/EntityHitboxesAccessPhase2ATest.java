package dev.crlhitbox.api.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EntityHitboxesAccessPhase2ATest {
    @Test
    void findAndRequireRejectNullBeforeCapabilityLookup() {
        assertThrows(NullPointerException.class, () -> EntityHitboxes.find(null));
        assertThrows(NullPointerException.class, () -> EntityHitboxes.require(null));
    }
}
