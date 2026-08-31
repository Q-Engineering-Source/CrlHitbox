package dev.crlhitbox.internal.entity;

import dev.crlhitbox.Reference;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Forge event listener that gives every Entity a fresh, empty holder provider. */
public final class EntityHitboxAttachmentHandler {
    private static final ResourceLocation ATTACHMENT_ID =
            new ResourceLocation(Reference.MOD_ID, "entity_hitboxes");

    /** Attaches one fresh provider without inspecting Entity state. */
    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<Entity> event) {
        event.addCapability(ATTACHMENT_ID, new EntityHitboxProvider());
    }
}
