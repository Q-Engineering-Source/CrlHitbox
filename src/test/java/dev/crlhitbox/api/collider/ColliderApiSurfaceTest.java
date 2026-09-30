package dev.crlhitbox.api.collider;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.reflect.AccessFlag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Executable closure for the open collider layer: the exact public type set, the sealed snapshot
 * hierarchy, the approved public methods, and the absence of platform linkage.
 */
class ColliderApiSurfaceTest {
    private static final Path PRODUCTION_CLASSES = Path.of("build", "classes", "java", "main");
    private static final Path PACKAGE_DIRECTORY = PRODUCTION_CLASSES.resolve("dev/crlhitbox/api/collider");
    private static final ClassFile CLASS_FILE = ClassFile.of();

    @Test
    void colliderPackageContainsExactlyTheApprovedPublicTypes() throws IOException {
        Set<String> publicTypes = new TreeSet<>();
        try (Stream<Path> files = Files.list(PACKAGE_DIRECTORY)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .toList()) {
                String simpleName = file.getFileName().toString().replaceFirst("\\.class$", "");
                if (simpleName.equals("package-info")) {
                    continue;
                }
                if (CLASS_FILE.parse(file).flags().has(AccessFlag.PUBLIC)) {
                    publicTypes.add(simpleName);
                }
            }
        }

        assertEquals(Set.of(
                "Collider",
                "MutableCollider",
                "ColliderSnapshot",
                "SolidColliderSnapshot",
                "RayColliderSnapshot",
                "CompoundColliderSnapshot",
                "Ray3d"), publicTypes);
    }

    @Test
    void colliderSnapshotIsSealedToExactlyTheThreeSnapshotValues() {
        Set<String> permitted = new TreeSet<>();
        for (Class<?> type : ColliderSnapshot.class.getPermittedSubclasses()) {
            permitted.add(type.getSimpleName());
        }

        assertEquals(Set.of(
                "SolidColliderSnapshot", "RayColliderSnapshot", "CompoundColliderSnapshot"), permitted);
        assertNull(MutableCollider.class.getPermittedSubclasses(),
                "MutableCollider is an open interface, not a sealed hierarchy");
    }

    @Test
    void colliderInterfacesExposeOnlyTheApprovedMethods() throws IOException {
        assertEquals(Set.of("enabled", "localToParent", "revision", "snapshot"),
                publicMethodNames("Collider"));
        assertEquals(Set.of("setEnabled", "setLocalToParent"),
                publicMethodNames("MutableCollider"));
        assertEquals(Set.of("bounds", "enabled", "localToParent"),
                publicMethodNames("ColliderSnapshot"));
    }

    @Test
    void colliderSnapshotsExposeOnlyTheApprovedMethods() throws IOException {
        Set<String> common = Set.of("bounds", "enabled", "localToParent", "equals", "hashCode", "toString");

        Set<String> solid = new TreeSet<>(common);
        solid.add("solid");
        assertEquals(solid, publicMethodNames("SolidColliderSnapshot"));

        Set<String> ray = new TreeSet<>(common);
        ray.add("ray");
        assertEquals(ray, publicMethodNames("RayColliderSnapshot"));

        Set<String> compound = new TreeSet<>(common);
        compound.add("child");
        compound.add("childCount");
        assertEquals(compound, publicMethodNames("CompoundColliderSnapshot"));
    }

    @Test
    void rayValueExposesOnlyTheApprovedMethods() throws IOException {
        assertEquals(Set.of("asSegment", "bounds", "direction", "end", "equals", "hashCode",
                "length", "origin", "toString"), publicMethodNames("Ray3d"));
    }

    @Test
    void colliderLayerHasNoPlatformLinkage() throws IOException {
        try (Stream<Path> files = Files.list(PACKAGE_DIRECTORY)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .toList()) {
                String constantPoolText = new String(
                        Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                assertFalse(constantPoolText.contains("net/minecraft"),
                        file.getFileName() + " must not reference Minecraft classes");
                assertFalse(constantPoolText.contains("net/minecraftforge"),
                        file.getFileName() + " must not reference Forge classes");
                assertFalse(constantPoolText.contains("io/netty"),
                        file.getFileName() + " must not reference Netty classes");
                assertFalse(constantPoolText.contains("org/joml"),
                        file.getFileName() + " must not reference JOML");
            }
        }
    }

    private static Set<String> publicMethodNames(String simpleName) throws IOException {
        ClassModel model = CLASS_FILE.parse(PACKAGE_DIRECTORY.resolve(simpleName + ".class"));
        Set<String> names = new TreeSet<>();
        for (MethodModel method : model.methods()) {
            String name = method.methodName().stringValue();
            if (name.startsWith("<")) {
                continue;
            }
            if (method.flags().has(AccessFlag.PUBLIC)
                    && !method.flags().has(AccessFlag.SYNTHETIC)) {
                names.add(name);
            }
        }
        return names;
    }

    @Test
    void colliderSnapshotsExposeExactlyOnePublicConstructorEach() throws IOException {
        for (String type : Set.of("SolidColliderSnapshot", "RayColliderSnapshot",
                "CompoundColliderSnapshot", "Ray3d")) {
            ClassModel model = CLASS_FILE.parse(PACKAGE_DIRECTORY.resolve(type + ".class"));
            int constructors = 0;
            for (MethodModel method : model.methods()) {
                if (method.methodName().stringValue().equals("<init>")
                        && method.flags().has(AccessFlag.PUBLIC)) {
                    constructors++;
                }
            }
            assertEquals(1, constructors, type + " must expose exactly one public constructor");
        }
    }
}
