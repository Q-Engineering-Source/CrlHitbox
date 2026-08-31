package dev.crlhitbox.internal.entity;

import dev.crlhitbox.api.entity.EntityHitboxHolder;
import dev.crlhitbox.api.entity.EntityHitboxSnapshot;
import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.PlacedSolid3d;
import dev.crlhitbox.api.geometry.RigidTransform3d;
import dev.crlhitbox.api.geometry.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityHitboxCapabilityArchitecturePhase2ATest {
    @Test
    void inertStorageWritesNullAndReadDoesNotMutateHolder() {
        EntityHitboxHolder holder = new EntityHitboxHolder();
        Vec3d point = new Vec3d(1.0D, 2.0D, 3.0D);
        holder.put(
                new ResourceLocation("crlhitbox", "core"),
                new PlacedSolid3d(new Aabb(point, point), RigidTransform3d.identity()));
        EntityHitboxSnapshot before = holder.snapshot();

        assertNull(EntityHitboxCapability.STORAGE.writeNBT(null, holder, null));
        EntityHitboxCapability.STORAGE.readNBT(null, holder, null, null);

        assertEquals(before, holder.snapshot());
    }

    @Test
    void attachmentHandlerAddsOneFreshProviderUnderTheStableKey() {
        EntityHitboxAttachmentHandler handler = new EntityHitboxAttachmentHandler();
        AttachCapabilitiesEvent<Entity> firstEvent =
                new AttachCapabilitiesEvent<>(Entity.class, null);
        AttachCapabilitiesEvent<Entity> secondEvent =
                new AttachCapabilitiesEvent<>(Entity.class, null);

        handler.attach(firstEvent);
        handler.attach(secondEvent);

        ResourceLocation key = new ResourceLocation("crlhitbox", "entity_hitboxes");
        assertEquals(1, firstEvent.getCapabilities().size());
        assertEquals(1, secondEvent.getCapabilities().size());
        ICapabilityProvider first = firstEvent.getCapabilities().get(key);
        ICapabilityProvider second = secondEvent.getCapabilities().get(key);
        assertInstanceOf(EntityHitboxProvider.class, first);
        assertInstanceOf(EntityHitboxProvider.class, second);
        assertNotSame(first, second);
    }

    @Test
    void modEntryPointDeclaresTheStandardPreInitializationHandler() throws IOException {
        MethodModel preInit = method(
                classModel("dev/crlhitbox/CrlHitbox"),
                "preInit",
                "(Lnet/minecraftforge/fml/common/event/FMLPreInitializationEvent;)V");

        assertTrue((preInit.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0);
        assertTrue(hasRuntimeAnnotation(
                preInit,
                "Lnet/minecraftforge/fml/common/Mod$EventHandler;"));
    }

    @Test
    void providerImplementsOnlyTheNonSerializableCapabilityInterface() throws IOException {
        ClassModel provider = classModel("dev/crlhitbox/internal/entity/EntityHitboxProvider");

        assertEquals(
                Set.of("net/minecraftforge/common/capabilities/ICapabilityProvider"),
                provider.interfaces().stream().map(type -> type.asInternalName()).collect(java.util.stream.Collectors.toSet()));
        assertFalse(provider.interfaces().stream()
                .map(type -> type.asInternalName())
                .anyMatch(name -> name.contains("ICapabilitySerializable")
                        || name.contains("INBTSerializable")));
        assertFalse(provider.methods().stream()
                .map(method -> method.methodName().stringValue())
                .anyMatch(name -> name.equals("serializeNBT") || name.equals("deserializeNBT")));
    }

    @Test
    void providerConstructorCreatesOneFreshNonStaticHolderAndNoOwnerField() throws IOException {
        ClassModel provider = classModel("dev/crlhitbox/internal/entity/EntityHitboxProvider");
        var holderFields = provider.fields().stream()
                .filter(field -> field.fieldType().stringValue()
                        .equals("Ldev/crlhitbox/api/entity/EntityHitboxHolder;"))
                .toList();

        assertEquals(1, holderFields.size());
        assertFalse((holderFields.getFirst().flags().flagsMask() & ClassFile.ACC_STATIC) != 0);
        assertFalse(provider.fields().stream().anyMatch(field -> {
            String descriptor = field.fieldType().stringValue();
            return descriptor.contains("Lnet/minecraft/entity/Entity;")
                    || descriptor.contains("Lnet/minecraft/world/World;");
        }));
        assertTrue(method(provider, "<init>", "()V").code().orElseThrow().elementStream()
                .filter(NewObjectInstruction.class::isInstance)
                .map(NewObjectInstruction.class::cast)
                .anyMatch(instruction -> instruction.className().asInternalName()
                        .equals("dev/crlhitbox/api/entity/EntityHitboxHolder")));
    }

    @Test
    void capabilityInjectionFieldIsPrivateStaticAndNeverPubliclyReturned() throws IOException {
        ClassModel capability = classModel("dev/crlhitbox/internal/entity/EntityHitboxCapability");
        FieldModel injected = capability.fields().stream()
                .filter(field -> hasRuntimeAnnotation(
                        field,
                        "Lnet/minecraftforge/common/capabilities/CapabilityInject;"))
                .findFirst()
                .orElseThrow();

        assertEquals(
                "Lnet/minecraftforge/common/capabilities/Capability;",
                injected.fieldType().stringValue());
        assertTrue((injected.flags().flagsMask() & ClassFile.ACC_PRIVATE) != 0);
        assertTrue((injected.flags().flagsMask() & ClassFile.ACC_STATIC) != 0);
        assertFalse(capability.methods().stream()
                .filter(method -> (method.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0)
                .anyMatch(method -> method.methodTypeSymbol().returnType().descriptorString()
                        .equals("Lnet/minecraftforge/common/capabilities/Capability;")));
    }

    private static ClassModel classModel(String internalName) throws IOException {
        return ClassFile.of().parse(Path.of(
                "build", "classes", "java", "main", internalName + ".class"));
    }

    private static MethodModel method(ClassModel model, String name, String descriptor) {
        return model.methods().stream()
                .filter(method -> method.methodName().stringValue().equals(name))
                .filter(method -> method.methodType().stringValue().equals(descriptor))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasRuntimeAnnotation(MethodModel method, String descriptor) {
        return method.findAttribute(Attributes.runtimeVisibleAnnotations())
                .stream()
                .flatMap(attribute -> attribute.annotations().stream())
                .anyMatch(annotation -> annotation.className().stringValue().equals(descriptor));
    }

    private static boolean hasRuntimeAnnotation(FieldModel field, String descriptor) {
        return field.findAttribute(Attributes.runtimeVisibleAnnotations())
                .stream()
                .flatMap(attribute -> attribute.annotations().stream())
                .anyMatch(annotation -> annotation.className().stringValue().equals(descriptor));
    }
}
