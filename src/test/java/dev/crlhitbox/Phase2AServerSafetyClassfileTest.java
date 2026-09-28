package dev.crlhitbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/** Bytecode-only dedicated-server linkage audit for every production CRL Hitbox class. */
class Phase2AServerSafetyClassfileTest {
    private static final Path PRODUCTION_CLASSES = Path.of(
            "build", "classes", "java", "main", "dev", "crlhitbox");
    private static final String CAPABILITY_STORAGE =
            "dev/crlhitbox/internal/entity/EntityHitboxCapability$1";
    /**
     * Phase 2B allows Netty/IMessage references only inside the internal wire package. Geometry,
     * {@code EntityHitboxHolder}, and {@code EntityHitboxSnapshot} live outside it and therefore
     * remain fully covered by the network prohibitions below.
     */
    private static final String INTERNAL_NETWORK_PACKAGE = "dev/crlhitbox/internal/network/";
    private static final String INTERNAL_CLIENT_PACKAGE = "dev/crlhitbox/internal/client/";

    @Test
    void productionClassfilesHaveNoClientOrExternalMathLinkage() throws IOException {
        for (ProductionClass productionClass : productionClasses()) {
            for (PoolEntry entry : productionClass.model().constantPool()) {
                String value = entryValue(entry);
                rejectPrefix(productionClass, entry, value, "net/minecraft/client/",
                        "Minecraft client class linkage");
                rejectPrefix(productionClass, entry, value, "net/minecraftforge/client/",
                        "Forge client class linkage");
                rejectPrefix(productionClass, entry, value, "org/lwjgl/",
                        "LWJGL rendering linkage");
                rejectPrefix(productionClass, entry, value, "org/joml/",
                        "JOML math linkage");
                rejectPrefix(productionClass, entry, value, "javax/vecmath/",
                        "javax.vecmath linkage");
                rejectPrefix(productionClass, entry, value, "com/mojang/math/",
                        "Mojang client math linkage");
                rejectPrefix(productionClass, entry, value, "com/mojang/blaze3d/",
                        "Blaze3D rendering linkage");
                rejectPrefix(productionClass, entry, value, "net/minecraft/util/math/",
                        "Minecraft math linkage");
                rejectContains(productionClass, entry, value, "Tessellator",
                        "Tessellator rendering indicator");
                rejectContains(productionClass, entry, value, "BufferBuilder",
                        "BufferBuilder rendering indicator");
                rejectContains(productionClass, entry, value, "GlStateManager",
                        "GlStateManager rendering indicator");
            }
        }
    }

    @Test
    void onlyInternalClientClassesMayLinkTheClientImplementationPackage() throws IOException {
        for (ProductionClass productionClass : productionClasses()) {
            if (productionClass.name().startsWith(INTERNAL_CLIENT_PACKAGE)) continue;
            for (PoolEntry entry : productionClass.model().constantPool()) {
                rejectPrefix(productionClass, entry, entryValue(entry), INTERNAL_CLIENT_PACKAGE,
                        "common/server linkage to a client-only implementation class");
            }
        }
    }

    @Test
    void productionClassfilesHaveNoNetworkPersistenceLifecycleOrGlobalStateLinkage()
            throws IOException {
        for (ProductionClass productionClass : productionClasses()) {
            assertNoStaticRegistryField(productionClass);
            boolean internalWireCode = productionClass.name().startsWith(INTERNAL_NETWORK_PACKAGE);
            for (PoolEntry entry : productionClass.model().constantPool()) {
                String value = entryValue(entry);
                if (!internalWireCode) {
                    rejectPrefix(productionClass, entry, value, "net/minecraft/network/",
                            "Minecraft network linkage");
                    rejectPrefix(productionClass, entry, value, "net/minecraftforge/fml/common/network/",
                            "Forge network linkage");
                    rejectPrefix(productionClass, entry, value, "io/netty/",
                            "Netty network linkage");
                    rejectContains(productionClass, entry, value, "SimpleNetworkWrapper",
                            "SimpleNetworkWrapper indicator");
                    rejectContains(productionClass, entry, value, "IMessage",
                            "IMessage indicator");
                    rejectContains(productionClass, entry, value, "IMessageHandler",
                            "IMessageHandler indicator");
                    rejectContains(productionClass, entry, value, "ByteBuf",
                            "ByteBuf indicator");
                    rejectContains(productionClass, entry, value, "PacketBuffer",
                            "PacketBuffer indicator");
                }

                rejectContains(productionClass, entry, value, "ICapabilitySerializable",
                        "capability serialization interface");
                rejectContains(productionClass, entry, value, "INBTSerializable",
                        "NBT serialization interface");
                rejectContains(productionClass, entry, value, "NBTTag",
                        "persistent NBT schema indicator");
                rejectNbtBaseOutsideInertStorageSignature(productionClass, entry, value);

                rejectLifecycleReference(productionClass, entry, value);
                rejectContains(productionClass, entry, value, "org/spongepowered/asm/mixin/",
                        "Mixin linkage");
                rejectContains(productionClass, entry, value, "IFMLLoadingPlugin",
                        "coremod loading-plugin indicator");
                rejectContains(productionClass, entry, value, "IClassTransformer",
                        "coremod transformer indicator");
                rejectContains(productionClass, entry, value, "AccessTransformer",
                        "access-transformer indicator");

                rejectPrefix(productionClass, entry, value, "java/lang/reflect/",
                        "reflection linkage");
                rejectMethodHandleReference(productionClass, entry);
                rejectContains(productionClass, entry, value, "java/util/ServiceLoader",
                        "ServiceLoader reflection indicator");
                rejectContains(productionClass, entry, value, "java/lang/ThreadLocal",
                        "ThreadLocal indicator");
                rejectContains(productionClass, entry, value, "java/util/Random",
                        "Random indicator");
                rejectContains(productionClass, entry, value, "java/util/SplittableRandom",
                        "SplittableRandom indicator");
                rejectContains(productionClass, entry, value, "java/util/concurrent/ThreadLocalRandom",
                        "ThreadLocalRandom indicator");
                rejectContains(productionClass, entry, value, "java/util/UUID",
                        "UUID generation indicator");
                rejectPrefix(productionClass, entry, value, "java/time/",
                        "wall-clock time linkage");
                rejectMember(productionClass, entry, "java/lang/System", "currentTimeMillis",
                        "currentTimeMillis indicator");
                rejectMember(productionClass, entry, "java/lang/System", "nanoTime",
                        "nanoTime indicator");
                rejectMember(productionClass, entry, "java/lang/Class", "forName",
                        "Class.forName reflection indicator");
                rejectMember(productionClass, entry, "java/lang/Class", "getDeclared",
                        "Class declared-member reflection indicator");
            }
        }
    }

    private static List<ProductionClass> productionClasses() throws IOException {
        if (!Files.isDirectory(PRODUCTION_CLASSES)) {
            fail("Missing production class directory for classfile audit: "
                    + PRODUCTION_CLASSES.toAbsolutePath());
        }
        try (var paths = Files.walk(PRODUCTION_CLASSES)) {
            return paths
                    .filter(path -> path.toString().endsWith(".class"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(Phase2AServerSafetyClassfileTest::readProductionClass)
                    .toList();
        }
    }

    private static ProductionClass readProductionClass(Path path) {
        try {
            ClassModel model = ClassFile.of().parse(path);
            return new ProductionClass(model.thisClass().asInternalName(), model);
        } catch (IOException exception) {
            throw new AssertionError("Unable to parse production classfile " + path, exception);
        }
    }

    private static void assertNoStaticRegistryField(ProductionClass productionClass) {
        for (FieldModel field : productionClass.model().fields()) {
            if (!field.flags().has(AccessFlag.STATIC)) continue;
            String type = field.fieldType().stringValue();
            for (String indicator : List.of(
                    "Ljava/util/Map;", "Ljava/util/HashMap;", "Ljava/util/LinkedHashMap;",
                    "Ljava/util/WeakHashMap;", "Ljava/util/concurrent/ConcurrentMap;",
                    "Ljava/util/concurrent/ConcurrentHashMap;", "Ljava/util/Collection;",
                    "Ljava/util/List;", "Ljava/util/Set;",
                    "Ldev/crlhitbox/api/entity/EntityHitboxHolder;",
                    "Lnet/minecraft/entity/Entity;", "Lnet/minecraft/world/World;")) {
                if (type.contains(indicator)) {
                    fail(productionClass.name() + " field " + field.fieldName().stringValue()
                            + " descriptor " + type + " violates static registry indicator " + indicator);
                }
            }
        }
    }

    private static void rejectNbtBaseOutsideInertStorageSignature(
            ProductionClass productionClass, PoolEntry entry, String value) {
        if (!value.contains("net/minecraft/nbt/NBTBase")) return;
        if (!productionClass.name().equals(CAPABILITY_STORAGE)
                || !hasOnlyInertNbtStorageSignatures(productionClass.model())) {
            fail(productionClass.name() + " constant-pool entry " + describe(entry)
                    + " violates NBTBase restriction; only EntityHitboxCapability's inert IStorage "
                    + "writeNBT/readNBT signatures may reference NBTBase");
        }
    }

    private static boolean hasOnlyInertNbtStorageSignatures(ClassModel model) {
        boolean writeNbt = false;
        boolean readNbt = false;
        for (MethodModel method : model.methods()) {
            String name = method.methodName().stringValue();
            String type = method.methodType().stringValue();
            if (!type.contains("Lnet/minecraft/nbt/NBTBase;")) continue;
            if (name.equals("writeNBT") && type.endsWith("Lnet/minecraft/nbt/NBTBase;")) {
                writeNbt = true;
            } else if (name.equals("readNBT") && type.endsWith("V")) {
                readNbt = true;
            } else {
                return false;
            }
        }
        return writeNbt && readNbt;
    }

    private static void rejectLifecycleReference(
            ProductionClass productionClass, PoolEntry entry, String value) {
        String lifecyclePackage = "net/minecraftforge/fml/common/event/";
        if (value.contains(lifecyclePackage)
                && !value.contains(lifecyclePackage + "FMLPreInitializationEvent")) {
            fail(productionClass.name() + " constant-pool entry " + describe(entry)
                    + " violates forbidden lifecycle indicator");
        }
    }

    private static void rejectPrefix(
            ProductionClass productionClass, PoolEntry entry, String value, String prefix, String rule) {
        if (value.contains(prefix)) reject(productionClass, entry, rule);
    }

    private static void rejectContains(
            ProductionClass productionClass, PoolEntry entry, String value, String indicator, String rule) {
        if (value.contains(indicator)) reject(productionClass, entry, rule);
    }

    private static void rejectMember(
            ProductionClass productionClass, PoolEntry entry, String owner, String methodPrefix, String rule) {
        if (entry instanceof MemberRefEntry member
                && member.owner().asInternalName().equals(owner)
                && member.name().stringValue().startsWith(methodPrefix)) {
            reject(productionClass, entry, rule);
        }
    }

    private static void rejectMethodHandleReference(ProductionClass productionClass, PoolEntry entry) {
        if (entry instanceof ClassEntry classEntry
                && (classEntry.asInternalName().equals("java/lang/invoke/MethodHandle")
                || classEntry.asInternalName().equals("java/lang/invoke/VarHandle"))) {
            reject(productionClass, entry, "method-handle linkage");
        }
        if (entry instanceof MemberRefEntry member
                && member.owner().asInternalName().startsWith("java/lang/invoke/")
                && !isCompilerBootstrap(member)) {
            reject(productionClass, entry, "method-handle linkage");
        }
    }

    private static boolean isCompilerBootstrap(MemberRefEntry member) {
        String owner = member.owner().asInternalName();
        return owner.equals("java/lang/invoke/StringConcatFactory")
                || owner.equals("java/lang/invoke/LambdaMetafactory")
                || owner.equals("java/lang/runtime/SwitchBootstraps");
    }

    private static void reject(ProductionClass productionClass, PoolEntry entry, String rule) {
        fail(productionClass.name() + " constant-pool entry " + describe(entry)
                + " violates " + rule);
    }

    private static String entryValue(PoolEntry entry) {
        if (entry instanceof ClassEntry classEntry) return classEntry.asInternalName();
        if (entry instanceof MemberRefEntry member) {
            return member.owner().asInternalName() + "." + member.name().stringValue()
                    + member.type().stringValue();
        }
        if (entry instanceof Utf8Entry utf8) return utf8.stringValue();
        return entry.toString();
    }

    private static String describe(PoolEntry entry) {
        return "#" + entry.index() + " " + entryValue(entry);
    }

    private record ProductionClass(String name, ClassModel model) {
    }
}
