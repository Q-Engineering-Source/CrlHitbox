package dev.crlhitbox.internal.bench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Frozen collider throughput targets and their checker.
 *
 * <p>The six absolute values come from the reference README table as recorded in the handover
 * roadmap section 5.1. They are per-item gates: no average, no total score and no multi-thread
 * aggregate may compensate for a slow item, and the reference error column is never subtracted from
 * the target.</p>
 *
 * <p>The checker reads one {@code benchmark=score} line per executed benchmark — the benchmark
 * workflow extracts those from the raw JMH JSON — and reports every unmet or missing target. It only
 * inspects results; it never runs or replaces a measurement, and a missing item is reported as not
 * executed rather than as zero.</p>
 */
public final class BenchmarkThresholds {
    /** One roadmap target: its id, its benchmark method name and its minimum throughput in ops/s. */
    public record Target(String id, String benchmark, double minimumOpsPerSecond) {
    }

    private static final Target[] TARGETS = {
            new Target("P01", "aabbPair", 3_561_837_865.930D),
            new Target("P02", "capsulePair", 22_350_681.198D),
            new Target("P03", "rotatedCapsulePair", 16_842_309.523D),
            new Target("P04", "obbPair", 5_528_493.224D),
            new Target("P05", "rotatedObbPair", 4_648_963.750D),
            new Target("P06", "spherePair", 119_556_589.705D)};

    private BenchmarkThresholds() {
    }

    /** Returns the frozen targets in roadmap order. */
    public static List<Target> targets() {
        return List.of(TARGETS);
    }

    /**
     * Compares measured throughput against every target.
     *
     * @param measuredOpsPerSecond measured ops/s keyed by benchmark method name
     * @return one message per unmet, missing or invalid target; empty when every item passes
     */
    public static List<String> verify(Map<String, Double> measuredOpsPerSecond) {
        List<String> problems = new ArrayList<>();
        for (Target target : TARGETS) {
            Double score = measuredOpsPerSecond.get(target.benchmark());
            if (score == null) {
                problems.add(target.id() + " " + target.benchmark() + ": NOT EXECUTED");
                continue;
            }
            if (!Double.isFinite(score) || score <= 0.0D) {
                problems.add(target.id() + " " + target.benchmark()
                        + ": invalid score " + score);
                continue;
            }
            if (score < target.minimumOpsPerSecond()) {
                double shortfall = (target.minimumOpsPerSecond() - score)
                        / target.minimumOpsPerSecond() * 100.0D;
                problems.add(String.format(Locale.ROOT,
                        "%s %s: %.3f ops/s is below the target %.3f ops/s (shortfall %.2f%%)",
                        target.id(), target.benchmark(), score,
                        target.minimumOpsPerSecond(), shortfall));
            }
        }
        return problems;
    }

    /**
     * Parses a {@code benchmark=score} text file and prints the verdict.
     *
     * <p>Exit code 0 means every target passed, 1 means at least one item failed, was missing or was
     * not executed.</p>
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: BenchmarkThresholds <measured-ops-per-second.txt>");
            System.exit(2);
            return;
        }
        Map<String, Double> measured = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\uFEFF")) {
                // A byte-order mark at the start of the file is not part of the benchmark name.
                trimmed = trimmed.substring(1);
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                System.err.println("malformed line: " + trimmed);
                System.exit(2);
                return;
            }
            String name = trimmed.substring(0, separator).trim();
            try {
                measured.put(name, Double.parseDouble(trimmed.substring(separator + 1).trim()));
            } catch (NumberFormatException failure) {
                System.err.println("malformed score for " + name + ": " + failure.getMessage());
                System.exit(2);
                return;
            }
        }
        List<String> problems = verify(measured);
        for (Target target : TARGETS) {
            Double score = measured.get(target.benchmark());
            System.out.println(String.format(Locale.ROOT, "%-4s %-20s target=%15.3f measured=%s",
                    target.id(), target.benchmark(), target.minimumOpsPerSecond(),
                    score == null ? "NOT EXECUTED" : String.format(Locale.ROOT, "%.3f", score)));
        }
        if (problems.isEmpty()) {
            System.out.println("RESULT: all six targets met");
            return;
        }
        for (String problem : problems) {
            System.out.println("FAIL: " + problem);
        }
        System.out.println("RESULT: " + problems.size() + " item(s) not met");
        System.exit(1);
    }
}
