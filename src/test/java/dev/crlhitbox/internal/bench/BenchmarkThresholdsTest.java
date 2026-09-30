package dev.crlhitbox.internal.bench;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Acceptance for the frozen benchmark thresholds and their checker. */
class BenchmarkThresholdsTest {
    @Test
    void targetsMatchTheRoadmapTable() {
        List<BenchmarkThresholds.Target> targets = BenchmarkThresholds.targets();

        assertEquals(6, targets.size());
        assertEquals(List.of("P01", "P02", "P03", "P04", "P05", "P06"),
                targets.stream().map(BenchmarkThresholds.Target::id).toList());
        assertEquals(List.of("aabbPair", "capsulePair", "rotatedCapsulePair", "obbPair",
                "rotatedObbPair", "spherePair"),
                targets.stream().map(BenchmarkThresholds.Target::benchmark).toList());
        assertEquals(3_561_837_865.930D, targets.get(0).minimumOpsPerSecond(), 1.0E-3D);
        assertEquals(119_556_589.705D, targets.get(5).minimumOpsPerSecond(), 1.0E-3D);
    }

    @Test
    void everyTargetAtOrAboveItsValuePasses() {
        Map<String, Double> measured = new LinkedHashMap<>();
        for (BenchmarkThresholds.Target target : BenchmarkThresholds.targets()) {
            measured.put(target.benchmark(), target.minimumOpsPerSecond());
        }

        assertTrue(BenchmarkThresholds.verify(measured).isEmpty(),
                "exactly reaching a target passes; there is no error allowance");
    }

    @Test
    void missingItemsAreReportedAsNotExecutedInsteadOfZero() {
        List<String> problems = BenchmarkThresholds.verify(Map.of());

        assertEquals(6, problems.size());
        for (String problem : problems) {
            assertTrue(problem.contains("NOT EXECUTED"), problem);
        }
    }

    @Test
    void invalidScoresAreRejected() {
        Map<String, Double> measured = new LinkedHashMap<>();
        measured.put("aabbPair", Double.NaN);
        measured.put("capsulePair", -1.0D);
        measured.put("rotatedCapsulePair", Double.POSITIVE_INFINITY);

        List<String> problems = BenchmarkThresholds.verify(measured);

        assertTrue(problems.stream().anyMatch(problem -> problem.contains("aabbPair")
                && problem.contains("invalid score")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("capsulePair")
                && problem.contains("invalid score")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("rotatedCapsulePair")
                && problem.contains("invalid score")));
    }

    @Test
    void slowItemsReportTheirShortfall() {
        Map<String, Double> measured = new LinkedHashMap<>();
        for (BenchmarkThresholds.Target target : BenchmarkThresholds.targets()) {
            measured.put(target.benchmark(), target.minimumOpsPerSecond());
        }
        measured.put("obbPair", 5_528_493.224D / 2.0D);

        List<String> problems = BenchmarkThresholds.verify(measured);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("P04"), problems.get(0));
        assertTrue(problems.get(0).contains("shortfall 50.00%"), problems.get(0));
    }

    @Test
    void aStrongItemNeverCompensatesForAFailingItem() {
        Map<String, Double> measured = new LinkedHashMap<>();
        for (BenchmarkThresholds.Target target : BenchmarkThresholds.targets()) {
            measured.put(target.benchmark(), target.minimumOpsPerSecond() * 1000.0D);
        }
        measured.put("spherePair", 1.0D);

        List<String> problems = BenchmarkThresholds.verify(measured);

        assertFalse(problems.isEmpty(), "an overall average must not hide a failing item");
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("P06"), problems.get(0));
    }
}
