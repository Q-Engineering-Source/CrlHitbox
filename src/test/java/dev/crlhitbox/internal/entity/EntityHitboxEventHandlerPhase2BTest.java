package dev.crlhitbox.internal.entity;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static wiring evidence for the automatic delivery handlers.
 *
 * <p>The handlers themselves require live server objects, so this suite verifies the exact
 * subscription surface: one {@code @SubscribeEvent} method per required event, with the precise
 * parameter type and no static state that could retain an Entity, holder, or world.</p>
 */
class EntityHitboxEventHandlerPhase2BTest {
    @Test
    void startTrackingHandlerSubscribesOnlyToTheForgeStartTrackingEvent() throws Exception {
        Map<String, String> subscriptions = subscribedEvents(EntityHitboxTrackingHandler.class);

        assertEquals(Map.of("onStartTracking", PlayerEvent.StartTracking.class.getName()),
                subscriptions);
    }

    @Test
    void playerLifecycleHandlerSubscribesToLoginRespawnAndDimensionChange() throws Exception {
        Map<String, String> subscriptions =
                subscribedEvents(EntityHitboxPlayerLifecycleHandler.class);

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("onPlayerLoggedIn", PlayerLoggedInEvent.class.getName());
        expected.put("onPlayerRespawn", PlayerRespawnEvent.class.getName());
        expected.put("onPlayerChangedDimension", PlayerChangedDimensionEvent.class.getName());
        assertEquals(expected, subscriptions, "exactly the three required player lifecycle events");
    }

    @Test
    void handlersHoldNoStaticStateAndExposeNoOtherPublicMethods() throws Exception {
        for (Class<?> handler : new Class<?>[] {
                EntityHitboxTrackingHandler.class, EntityHitboxPlayerLifecycleHandler.class}) {
            for (var field : handler.getDeclaredFields()) {
                assertTrue(!Modifier.isStatic(field.getModifiers()),
                        handler.getSimpleName() + " must not hold static field " + field.getName());
            }
            for (Method method : handler.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers())) {
                    assertTrue(method.isAnnotationPresent(SubscribeEvent.class),
                            handler.getSimpleName() + "." + method.getName()
                                    + " must be an event subscriber");
                    assertEquals(1, method.getParameterCount(),
                            "event subscribers take exactly one event parameter");
                    assertEquals(void.class, method.getReturnType(),
                            "event subscribers return void");
                }
            }
        }
    }

    private static Map<String, String> subscribedEvents(Class<?> handler) {
        Map<String, String> subscriptions = new LinkedHashMap<>();
        for (Method method : handler.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;
            assertEquals(1, method.getParameterCount());
            subscriptions.put(method.getName(), method.getParameterTypes()[0].getName());
        }
        return subscriptions;
    }
}
