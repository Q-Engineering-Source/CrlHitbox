package dev.crlhitbox.api.entity;

import dev.crlhitbox.api.event.EntityColliderUpdateEvent;
import dev.crlhitbox.internal.entity.EntityColliderThreads;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice F structural acceptance for the explicit update entry.
 *
 * <p>The event is posted for a live Entity, so its runtime behaviour needs a dedicated-server run and
 * is not claimed here. These tests cover what can be established without a game instance: argument
 * validation before any world access, the non-cancellable Forge event contract, and the
 * package-private reentrancy marker.</p>
 */
class EntityColliderUpdateTest {
    private static final ResourceLocation REASON = new ResourceLocation("crlhitbox", "pose_changed");

    @Test
    void requestUpdateRejectsNullArgumentsBeforeTouchingTheWorld() {
        assertThrows(NullPointerException.class, () -> EntityColliders.requestUpdate(null, REASON));
    }

    @Test
    void logicalThreadCheckRejectsANullEntityBeforeReadingWorldState() {
        assertThrows(NullPointerException.class,
                () -> EntityColliderThreads.requireLogicalThread(null));
    }

    @Test
    void updateEventIsAPlainForgeEventAndNotCancellable() {
        assertTrue(Event.class.isAssignableFrom(EntityColliderUpdateEvent.class),
                "the update entry is a Forge event");
        assertFalse(EntityColliderUpdateEvent.class.isAnnotationPresent(Cancelable.class),
                "a listener changes the outcome by publishing a snapshot, not by cancelling");
    }

    @Test
    void updateEntryExposesOnlyFactAccessors() {
        assertTrue(EntityColliderUpdateEvent.class.getDeclaredMethods().length > 0);
        for (var method : EntityColliderUpdateEvent.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertTrue(method.getName().startsWith("get"),
                    "only read accessors are public: " + method.getName());
            assertTrue(method.getParameterCount() == 0,
                    "no accessor takes arguments: " + method.getName());
        }
    }

    @Test
    void reentrancyMarkerIsNotPublicApi() throws Exception {
        Field marker = EntityColliderHolder.class.getDeclaredField("updateInProgress");

        assertEquals(boolean.class, marker.getType());
        assertFalse(Modifier.isPublic(marker.getModifiers()),
                "the reentrancy marker is package-private");
        assertFalse(Modifier.isStatic(marker.getModifiers()),
                "the marker is per cache, never static global state");
    }
}
