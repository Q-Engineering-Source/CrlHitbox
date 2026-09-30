package dev.crlhitbox.internal.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice G structural acceptance for the client-only debug renderer.
 *
 * <p>The overlay itself needs a real GPU client run, which this environment does not provide, so the
 * executed evidence covers the class-level contract only: the client layer stays inside its package,
 * the event handler subscribes to exactly the world-last render event, the renderer saves and
 * restores GL state, and no platform drawing class leaks into common or server code.</p>
 */
class ColliderDebugRenderStructureTest {
    private static final Path PRODUCTION_CLASSES = Path.of("build", "classes", "java", "main");
    private static final Path PACKAGE_DIRECTORY =
            PRODUCTION_CLASSES.resolve("dev/crlhitbox/internal/client");
    private static final String SUBSCRIBE_EVENT =
            "Lnet/minecraftforge/fml/common/eventhandler/SubscribeEvent;";
    private static final ClassFile CLASS_FILE = ClassFile.of();

    @Test
    void clientLayerContainsExactlyTheApprovedTypes() throws IOException {
        Set<String> types = new TreeSet<>();
        try (Stream<Path> files = Files.list(PACKAGE_DIRECTORY)) {
            for (Path file : files
                    .filter(path -> path.getFileName().toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .toList()) {
                types.add(file.getFileName().toString().replaceFirst("\\.class$", ""));
            }
        }

        assertEquals(Set.of(
                "ClientSideSnapshotDispatcher",
                "ColliderDebugRenderHandler",
                "ColliderDebugRenderer",
                "EntityColliderRenderFrames"), types);
    }

    @Test
    void debugHandlerSubscribesToExactlyTheWorldLastRenderEvent() throws IOException {
        ClassModel model = CLASS_FILE.parse(
                PACKAGE_DIRECTORY.resolve("ColliderDebugRenderHandler.class"));
        Set<String> subscribed = new TreeSet<>();
        for (MethodModel method : model.methods()) {
            if (!hasSubscribeEvent(method)) {
                continue;
            }
            assertEquals(1, method.methodTypeSymbol().parameterCount(),
                    "an event subscriber takes exactly one parameter");
            subscribed.add(method.methodName().stringValue() + ":"
                    + method.methodTypeSymbol().parameterList().get(0).displayName());
        }

        assertEquals(Set.of("onRenderWorldLast:RenderWorldLastEvent"), subscribed);
    }

    @Test
    void rendererSavesAndRestoresGlState() throws IOException {
        ClassModel model = CLASS_FILE.parse(PACKAGE_DIRECTORY.resolve("ColliderDebugRenderer.class"));
        Set<String> glStateCalls = new TreeSet<>();
        for (PoolEntry entry : model.constantPool()) {
            if (entry instanceof MemberRefEntry member
                    && member.owner().asInternalName()
                    .equals("net/minecraft/client/renderer/GlStateManager")) {
                glStateCalls.add(member.name().stringValue());
            }
        }

        assertTrue(glStateCalls.contains("pushMatrix"),
                "the renderer saves the matrix before drawing");
        assertTrue(glStateCalls.contains("popMatrix"),
                "the renderer restores the matrix after drawing");
        assertTrue(glStateCalls.contains("depthMask") && glStateCalls.contains("enableDepth"),
                "depth state is saved and restored");
        assertTrue(glStateCalls.contains("enableCull") && glStateCalls.contains("disableCull"),
                "cull state is saved and restored");
        assertTrue(glStateCalls.contains("enableTexture2D")
                        && glStateCalls.contains("disableTexture2D"),
                "texture state is saved and restored");
    }

    @Test
    void rendererUsesRealOutlinesInsteadOfOnlyBoundingBoxes() throws IOException {
        String constantPoolText = new String(
                Files.readAllBytes(PACKAGE_DIRECTORY.resolve("ColliderDebugRenderer.class")),
                StandardCharsets.ISO_8859_1);

        assertTrue(constantPoolText.contains("localToWorld"),
                "oriented boxes are drawn from their own axes");
        assertTrue(constantPoolText.contains("centerline"),
                "capsules are drawn from their centerline");
        assertTrue(constantPoolText.contains("asSegment")
                        || constantPoolText.contains("origin"),
                "rays are drawn at their finite length");
    }

    private static boolean hasSubscribeEvent(MethodModel method) {
        return method.findAttribute(Attributes.runtimeVisibleAnnotations())
                .map(annotations -> annotations.annotations().stream()
                        .anyMatch(annotation -> annotation.className().stringValue()
                                .equals(SUBSCRIBE_EVENT)))
                .orElse(false);
    }
}
