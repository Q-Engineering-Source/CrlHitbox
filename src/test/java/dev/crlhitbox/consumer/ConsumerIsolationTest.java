package dev.crlhitbox.consumer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consumer-isolation acceptance for the independent example fixture.
 *
 * <p>The fixture is compiled from its own source set against the production output and the platform
 * only. These assertions read its compiled classes and require that it depends on the public API
 * alone: no internal package, no reflection, no class-name loading, and no framework type beyond what
 * the entity entry points expose.</p>
 */
class ConsumerIsolationTest {
    private static final Path EXAMPLE_CLASSES =
            Path.of("build", "classes", "java", "example");

    @Test
    void consumerFixtureExposesThePublicEntryPointsItUses() throws IOException {
        String constantPoolText = allConsumerBytes();

        assertTrue(constantPoolText.contains("dev/crlhitbox/api/collider/MutableCapsuleCollider"),
                "the fixture uses the mutable capsule collider");
        assertTrue(constantPoolText.contains("dev/crlhitbox/api/collider/MutableRayCollider"),
                "the fixture uses the mutable ray collider");
        assertTrue(constantPoolText.contains("dev/crlhitbox/api/collider/ColliderQueries"),
                "the fixture uses the unified query entry");
        assertTrue(constantPoolText.contains("dev/crlhitbox/api/entity/EntityColliders"),
                "the fixture uses the public entity cache facade");
        assertTrue(constantPoolText.contains("dev/crlhitbox/api/entity/EntityColliderFrames"),
                "the fixture uses the public coordinate adapter");
        assertTrue(constantPoolText.contains("dev/crlhitbox/api/event/EntityColliderUpdateEvent"),
                "the fixture subscribes to the public update entry");
    }

    @Test
    void consumerFixtureNeverTouchesInternalsOrReflection() throws IOException {
        for (Path classFile : consumerClasses()) {
            String constantPoolText = new String(
                    Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
            assertFalse(constantPoolText.contains("dev/crlhitbox/internal"),
                    classFile.getFileName() + " must not reference internal implementation");
            assertFalse(constantPoolText.contains("java/lang/reflect"),
                    classFile.getFileName() + " must not use reflection");
            assertFalse(constantPoolText.contains("Class/forName")
                            || constantPoolText.contains("java/lang/Class"),
                    classFile.getFileName() + " must not load classes by name");
            assertFalse(constantPoolText.contains("sun/misc/Unsafe"),
                    classFile.getFileName() + " must not use Unsafe");
        }
    }

    @Test
    void consumerFixtureStaysOutOfTheShippedArtifact() {
        assertTrue(Files.isDirectory(EXAMPLE_CLASSES),
                "the fixture is compiled into its own output directory");
        assertFalse(Files.exists(Path.of("build", "classes", "java", "main", "dev", "crlhitbox",
                        "example")),
                "the fixture must not be compiled into the production output");
    }

    private static String allConsumerBytes() throws IOException {
        StringBuilder text = new StringBuilder();
        for (Path classFile : consumerClasses()) {
            text.append(new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1));
        }
        return text.toString();
    }

    private static List<Path> consumerClasses() throws IOException {
        assertTrue(Files.isDirectory(EXAMPLE_CLASSES),
                "the consumer fixture must be compiled before this test runs");
        try (Stream<Path> files = Files.walk(EXAMPLE_CLASSES)) {
            List<Path> classes = new ArrayList<>();
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                classes.add(file);
            }
            return classes;
        }
    }
}
