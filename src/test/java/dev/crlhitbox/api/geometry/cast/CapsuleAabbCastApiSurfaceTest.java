package dev.crlhitbox.api.geometry.cast;

import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.ArrayList;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapsuleAabbCastApiSurfaceTest {
    private static final Path OUTPUT = Path.of("build/classes/java/main/dev/crlhitbox/api/geometry/cast");

    @Test
    void classfileInventoryExposesOnlyTwoOwnersAndEightApprovedNestedValues() throws Exception {
        var actual = new TreeSet<String>();
        try (var files = Files.list(OUTPUT)) {
            for (var file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassModel model = ClassFile.of().parse(file);
                if ((model.flags().flagsMask() & ClassFile.ACC_PUBLIC) != 0) {
                    actual.add(file.getFileName().toString().replace(".class", ""));
                    assertEquals(69, model.majorVersion());
                    assertTrue((model.flags().flagsMask() & ClassFile.ACC_FINAL) != 0);
                }
            }
        }
        assertEquals(Set.of("CapsuleAabbCast3d", "CapsuleAabbCastResult3d", "CapsuleAabbCastResult3d$Kind",
                "CapsuleAabbCastResult3d$StartRelation", "CapsuleAabbCastResult3d$UnresolvedReason",
                "CapsuleAabbCastResult3d$NormalSource", "CapsuleAabbCastResult3d$BandBracket",
                "CapsuleAabbCastResult3d$NormalEstimate", "CapsuleAabbCastResult3d$QueryScale", "CapsuleAabbCastResult3d$WorkCounts"), actual);
    }

    @Test
    void facadeAndResultHaveExactlyTheAdoptedPublicMethodsAndNoPublicConstructor() throws Exception {
        assertEquals(Set.of("contractRevision()int", "cast(Capsule,Vec3d,Aabb)CapsuleAabbCastResult3d"), methods("CapsuleAabbCast3d"));
        assertEquals(Set.of("kind()Kind", "startRelation()StartRelation", "safePrefixEnd()OptionalDouble",
                "bandBracket()Optional", "normalEstimate()Optional", "scale()Optional", "reason()Optional", "work()WorkCounts"),
                methods("CapsuleAabbCastResult3d"));
    }

    @Test
    void publicValueConstructorsEnforceFractionWorkAndScaleInvariants() {
        var bracket = new CapsuleAabbCastResult3d.BandBracket(-0.0, 1, 1e-8, -0.0, true);
        assertEquals(0L, Double.doubleToRawLongBits(bracket.lastOutsideBandFraction()));
        assertEquals(0L, Double.doubleToRawLongBits(bracket.bandEntryPathWidth()));
        assertThrows(IllegalArgumentException.class, () -> new CapsuleAabbCastResult3d.BandBracket(0.5, 0.5, 1e-8, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new CapsuleAabbCastResult3d.BandBracket(0, Double.NaN, 1e-8, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new CapsuleAabbCastResult3d.WorkCounts(74, 32, 40));
        assertThrows(IllegalArgumentException.class, () -> new CapsuleAabbCastResult3d.WorkCounts(2, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CapsuleAabbCastResult3d.QueryScale(1e-8, 7e-8, 7e-8));
    }

    private static Set<String> methods(String type) throws Exception {
        Set<String> result = new TreeSet<>();
        for (MethodModel method : ClassFile.of().parse(OUTPUT.resolve(type + ".class")).methods()) {
            if ((method.flags().flagsMask() & ClassFile.ACC_PUBLIC) == 0) continue;
            var signature = method.methodTypeSymbol();
            var parameters = new ArrayList<String>();
            for (var parameter : signature.parameterList()) parameters.add(parameter.displayName());
            String returnName = signature.returnType().displayName();
            returnName = returnName.substring(returnName.lastIndexOf('$') + 1);
            result.add(method.methodName().stringValue() + "(" + String.join(",", parameters) + ")" + returnName);
        }
        return result;
    }

    @Test
    void runtimeEntryReportsTheAdoptedContractRevision() {
        assertEquals(1, CapsuleAabbCast3d.contractRevision());
    }
}
