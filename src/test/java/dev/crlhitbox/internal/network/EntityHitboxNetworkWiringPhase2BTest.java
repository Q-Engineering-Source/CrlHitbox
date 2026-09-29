package dev.crlhitbox.internal.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static evidence for the frozen channel contract: one client-bound message on the {@code crlhitbox}
 * channel and no client-to-server path.
 *
 * <p>The channel itself cannot be registered without a live Forge runtime, so this suite asserts the
 * frozen constants directly and inspects the compiled class for the registration direction and for
 * the absence of any server-bound send call.</p>
 */
class EntityHitboxNetworkWiringPhase2BTest {
    private static final Path PRODUCTION_CLASSES = Path.of(
            "build", "classes", "java", "main", "dev", "crlhitbox");

    @Test
    void channelAndDiscriminatorMatchTheFrozenContract() {
        assertEquals("crlhitbox", EntityHitboxNetwork.CHANNEL_NAME, "channel name");
        assertEquals(0, FullSnapshotProtocol.FULL_SNAPSHOT_DISCRIMINATOR, "discriminator");
        assertEquals(1, FullSnapshotProtocol.PROTOCOL_VERSION, "protocol version");
    }

    @Test
    void channelRegistersOnlyAClientBoundMessage() throws IOException {
        ClassModel model = classModel("internal/network/EntityHitboxNetwork");
        List<String> sideFields = memberFieldNames(model, "net/minecraftforge/fml/relauncher/Side");

        assertTrue(sideFields.contains("CLIENT"), "the message is registered for Side.CLIENT");
        assertFalse(sideFields.contains("SERVER"), "no Side.SERVER registration may exist");
    }

    @Test
    void channelHasNoClientToServerSendOrBroadcastPath() throws IOException {
        ClassModel model = classModel("internal/network/EntityHitboxNetwork");
        List<String> memberNames = memberNames(model);

        assertFalse(memberNames.contains("sendToServer"), "sendToServer must never be called");
        assertFalse(memberNames.contains("sendToAll"), "no broadcast send path is authorized");
        assertFalse(memberNames.contains("sendToDimension"),
                "no dimension broadcast send path is authorized");
    }

    @Test
    void publicSyncFacadeExposesOnlyTheTwoSendMethods() throws IOException {
        ClassModel model = classModel("api/entity/EntityHitboxSync");
        List<String> memberNames = memberNames(model);

        assertFalse(memberNames.contains("sendToServer"), "the public facade has no C2S path");
        assertTrue(memberNames.contains("sendFullSnapshot"), "explicit recipient send exists");
        assertTrue(memberNames.contains("sendFullSnapshotToTrackingAndSelf"),
                "tracking and self resend exists");
    }

    private static ClassModel classModel(String relativePath) throws IOException {
        Path classFile = PRODUCTION_CLASSES.resolve(relativePath + ".class");
        assertTrue(Files.isRegularFile(classFile), "missing compiled class " + classFile);
        return ClassFile.of().parse(classFile);
    }

    private static List<String> memberFieldNames(ClassModel model, String owner) {
        List<String> names = new ArrayList<>();
        for (PoolEntry entry : model.constantPool()) {
            if (entry instanceof MemberRefEntry member
                    && member.owner().asInternalName().equals(owner)) {
                names.add(member.name().stringValue());
            }
        }
        return names;
    }

    private static List<String> memberNames(ClassModel model) {
        List<String> names = new ArrayList<>();
        for (PoolEntry entry : model.constantPool()) {
            if (entry instanceof MemberRefEntry member) {
                names.add(member.name().stringValue());
            } else if (entry instanceof ClassEntry classEntry) {
                names.add(classEntry.asInternalName());
            }
        }
        return names;
    }
}
