package dev.crlhitbox.api.geometry;

import java.io.File;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeometryApiSurfacePhase1BTest {
    private static final String PACKAGE_PATH = "dev/crlhitbox/api/geometry";
    private static final ClassFile CLASS_FILE = ClassFile.of();

    @Test
    void distancesExposeExactlyTheFrozenSixPublicStaticMethods() throws Exception {
        assertEquals(Set.of(
                        "pointToSegmentSquared(Vec3d,Segment3d)",
                        "segmentToSegmentSquared(Segment3d,Segment3d)",
                        "pointToAabbSquared(Vec3d,Aabb)",
                        "segmentToAabbSquared(Segment3d,Aabb)",
                        "pointToObbSquared(Vec3d,Obb)",
                        "segmentToObbSquared(Segment3d,Obb)"),
                publicStaticMethods(classModel("GeometryDistances")));
    }

    @Test
    void intersectionsExposeExactlyTheFrozenTwentyFourTypedOverloads() throws Exception {
        assertEquals(Set.of(
                        "intersects(Aabb,Aabb)", "intersects(Sphere,Sphere)", "intersects(Sphere,Aabb)", "intersects(Aabb,Sphere)",
                        "intersects(Sphere,Obb)", "intersects(Obb,Sphere)", "intersects(Obb,Obb)", "intersects(Aabb,Obb)", "intersects(Obb,Aabb)",
                        "intersects(Capsule,Sphere)", "intersects(Sphere,Capsule)", "intersects(Capsule,Capsule)", "intersects(Capsule,Aabb)", "intersects(Aabb,Capsule)",
                        "intersects(Capsule,Obb)", "intersects(Obb,Capsule)", "intersects(Segment3d,Sphere)", "intersects(Sphere,Segment3d)",
                        "intersects(Segment3d,Aabb)", "intersects(Aabb,Segment3d)", "intersects(Segment3d,Obb)", "intersects(Obb,Segment3d)",
                        "intersects(Segment3d,Capsule)", "intersects(Capsule,Segment3d)"),
                publicStaticMethods(classModel("GeometryIntersections")));
    }

    @Test
    void geometryPackageHasNoAdditionalPublicTypeOrMutableArrayCollectionReturn() throws Exception {
        Set<String> expectedTypes = Set.of(
                "Aabb", "Bounded3d", "Capsule", "GeometryDistances", "GeometryIntersections",
                "Obb", "Rotation3d", "Segment3d", "Sphere", "Vec3d");
        Path packageDirectory = outputRoot().resolve(PACKAGE_PATH);
        Set<String> publicTypes = new TreeSet<>();
        try (Stream<Path> files = Files.list(packageDirectory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .toList()) {
                String simpleName = file.getFileName().toString().replaceFirst("\\.class$", "");
                if (!simpleName.equals("package-info") && isPublic(CLASS_FILE.parse(file))) publicTypes.add(simpleName);
            }
        }
        assertEquals(expectedTypes, publicTypes, "frozen public geometry type inventory");

        for (String simpleName : expectedTypes) {
            for (MethodModel method : classModel(simpleName).methods()) {
                if (!isPublic(method) || isSynthetic(method)) continue;
                ClassDesc returnType = method.methodTypeSymbol().returnType();
                String returnDescriptor = returnType.descriptorString();
                assertFalse(returnDescriptor.startsWith("["), () -> simpleName + "." + method.methodName().stringValue() + " returns an array");
                assertFalse(returnDescriptor.equals("Ljava/lang/Object;"), () -> simpleName + "." + method.methodName().stringValue() + " returns Object");
                assertFalse(isCollectionOrMap(returnType, new TreeSet<>()), () -> simpleName + "." + method.methodName().stringValue() + " returns a collection or map surface");
            }
        }
    }

    private static Set<String> publicStaticMethods(ClassModel type) {
        Set<String> signatures = new TreeSet<>();
        for (MethodModel method : type.methods()) {
            if (isPublic(method) && isStatic(method) && !isSynthetic(method)) signatures.add(signature(method));
        }
        return signatures;
    }

    private static String signature(MethodModel method) {
        MethodTypeDesc type = method.methodTypeSymbol();
        return method.methodName().stringValue() + "(" + type.parameterList().stream().map(GeometryApiSurfacePhase1BTest::simpleName).reduce((left, right) -> left + "," + right).orElse("") + ")";
    }

    private static String simpleName(ClassDesc type) {
        String displayName = type.displayName();
        int packageSeparator = displayName.lastIndexOf('.');
        return packageSeparator < 0 ? displayName : displayName.substring(packageSeparator + 1);
    }

    private static ClassModel classModel(String simpleName) throws Exception { return CLASS_FILE.parse(outputRoot().resolve(PACKAGE_PATH).resolve(simpleName + ".class")); }

    private static Path outputRoot() throws Exception {
        String resource = PACKAGE_PATH + "/Vec3d.class";
        return Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(Path::of)
                .filter(path -> Files.isRegularFile(path.resolve(resource)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not locate " + resource + " from java.class.path"));
    }

    private static boolean isCollectionOrMap(ClassDesc type, Set<String> visited) throws Exception {
        String descriptor = type.descriptorString();
        if (descriptor.length() == 1 || descriptor.startsWith("[")) return false;
        String internalName = descriptor.substring(1, descriptor.length() - 1);
        if (internalName.equals("java/util/Collection") || internalName.equals("java/util/Map")) return true;
        if (!visited.add(internalName)) return false;
        ClassModel model = CLASS_FILE.parse(classPath(internalName));
        for (var implemented : model.interfaces()) if (isCollectionOrMap(implemented.asSymbol(), visited)) return true;
        return model.superclass().isPresent() && isCollectionOrMap(model.superclass().orElseThrow().asSymbol(), visited);
    }

    private static Path classPath(String internalName) throws Exception {
        Path outputClass = outputRoot().resolve(internalName + ".class");
        if (Files.isRegularFile(outputClass)) return outputClass;
        Path javaBaseClass = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("/modules/java.base/" + internalName + ".class");
        if (Files.isRegularFile(javaBaseClass)) return javaBaseClass;
        throw new IllegalStateException("Could not locate class bytes for " + internalName);
    }

    private static boolean isPublic(ClassModel model) { return (model.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0; }
    private static boolean isPublic(MethodModel model) { return (model.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0; }
    private static boolean isStatic(MethodModel model) { return (model.flags().flagsMask() & ClassFile.ACC_STATIC) != 0; }
    private static boolean isSynthetic(MethodModel model) { return (model.flags().flagsMask() & ClassFile.ACC_SYNTHETIC) != 0; }
}
