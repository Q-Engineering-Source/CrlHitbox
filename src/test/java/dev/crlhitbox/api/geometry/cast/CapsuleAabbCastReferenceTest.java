package dev.crlhitbox.api.geometry.cast;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Segment3d;
import dev.crlhitbox.api.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;
import java.util.Arrays;
import java.util.TreeSet;
import java.lang.management.ManagementFactory;
import com.sun.management.ThreadMXBean;

import static dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.Kind.CLEAR_SEPARATED;
import static dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.Kind.HIT_AFTER_CLEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapsuleAabbCastReferenceTest {
    private static final Vec3d ZERO = new Vec3d(0, 0, 0);
    private static final Aabb UNIT_BOX = new Aabb(ZERO, new Vec3d(1, 1, 1));
    private static volatile CapsuleAabbCastResult3d measurementSink;

    /** Standalone bounded diagnostic/measurement entry in the already approved test path. */
    public static void main(String[] args) {
        if (CapsuleAabbCast3d.contractRevision() != 1) throw new IllegalStateException("incompatible runtime contract");
        if (args.length == 1 && args[0].equals("compatibility-signature")) {
            printCompatibilitySignature();
            return;
        }
        if (args.length == 1 && args[0].equals("near-band-probe")) {
            // Finite analytic family around the radius-16 corner's negative slop edge.
            double center = (16.0 - Math.nextUp(8.0 * 1e-10)) / Math.sqrt(2.0);
            long centerBits = Double.doubleToRawLongBits(center);
            for (int offset = -32; offset <= 32; offset++) {
                double h = Double.longBitsToDouble(centerBits + offset);
                var p = new Vec3d(-h, -h, 0.5);
                var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), 16), new Vec3d(16, -16, 0), UNIT_BOX);
                System.out.println("near-band h=" + Double.toHexString(h) + " kind=" + result.kind()
                        + " reason=" + result.reason() + " start=" + result.startRelation() + " work=" + result.work());
            }
            return;
        }
        if (args.length == 1 && args[0].equals("batch-benchmark")) { measureBatches(); return; }
        if (args.length == 1 && args[0].equals("work-probe")) {
            var random = new Random(0x434150534c494dL);
            int maxWork = 0;
            int found = 0;
            for (int i = 0; i < 30_000; i++) {
                double radius = Math.scalb(0.5 + random.nextDouble(), random.nextInt(10) - 4);
                double x = 1 + 29 * random.nextDouble();
                double y = -radius + (random.nextDouble() - 0.5) * 2e-8;
                var a = new Vec3d(-x, y, random.nextDouble());
                var b = new Vec3d(a.x() + (random.nextDouble() - 0.5) * radius, a.y() - random.nextDouble() * radius, a.z() + random.nextDouble());
                var delta = new Vec3d(x * 2, (random.nextDouble() - 0.5) * radius, (random.nextDouble() - 0.5) * 2);
                var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(a, b), radius), delta, UNIT_BOX);
                maxWork = Math.max(maxWork, result.work().advancementAttempts());
                if (result.kind() == CapsuleAabbCastResult3d.Kind.UNRESOLVED && result.safePrefixEnd().isPresent()) {
                    System.out.println("work-probe i=" + i + " r=" + radius + " a=" + a + " b=" + b + " d=" + delta
                            + " reason=" + result.reason() + " work=" + result.work() + " prefix=" + result.safePrefixEnd());
                    if (++found == 5) break;
                }
            }
            System.out.println("work-probe maxAdvance=" + maxWork + " found=" + found);
            return;
        }
        if (args.length == 1 && args[0].equals("probe")) {
            for (double r : new double[]{0.000001, 0.001, 0.25, 1, 16, 60}) {
                for (double x : new double[]{1, 10, 30}) for (double offset : new double[]{0, 7.9e-10, 8e-10, 8.1e-10, 1e-9}) {
                    var p = new Vec3d(-x, -r - offset, 0.5);
                    var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), r), new Vec3d(2 * x, 0, 0), UNIT_BOX);
                    if (result.kind() == CapsuleAabbCastResult3d.Kind.UNRESOLVED || result.bandBracket().map(CapsuleAabbCastResult3d.BandBracket::refinementLimited).orElse(false)) {
                        System.out.println("probe r=" + r + " x=" + x + " offset=" + offset + " kind=" + result.kind()
                                + " reason=" + result.reason() + " work=" + result.work() + " prefix=" + result.safePrefixEnd() + " bracket=" + result.bandBracket());
                    }
                }
            }
            for (double x : new double[]{-0.25 - 8e-10, -0.25 + 8e-10, -0.25 + Math.nextDown(8e-10)}) {
                var p = new Vec3d(x, 0.5, 0.5);
                var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), 0.25), new Vec3d(0, 1, 0), UNIT_BOX);
                System.out.println("start x=" + Double.toHexString(x) + " kind=" + result.kind() + " reason=" + result.reason() + " start=" + result.startRelation());
            }
            return;
        }
        if (args.length != 1 || !args[0].equals("benchmark")) throw new IllegalArgumentException("use probe or benchmark");
        var baseBean = ManagementFactory.getThreadMXBean();
        if (!(baseBean instanceof ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unavailable");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        System.out.println("JVM=" + System.getProperty("java.vm.name") + " " + System.getProperty("java.runtime.version")
                + " vendor=" + System.getProperty("java.vendor") + " processors=" + Runtime.getRuntime().availableProcessors());
        for (String name : new String[]{"ordinary_clear", "thin_wall", "plane_tangent", "profile_near_boundary"}) {
            var a = name.equals("plane_tangent") ? new Vec3d(-0.25, 0.5, 0.5) : new Vec3d(-1, -2, 0);
            var b = name.equals("plane_tangent") ? a : new Vec3d(-1, 2, 0);
            var box = name.equals("plane_tangent") ? UNIT_BOX : new Aabb(new Vec3d(0, -0.5, -0.5), new Vec3d(0.015625, 0.5, 0.5));
            var delta = switch (name) {
                case "ordinary_clear" -> new Vec3d(-0.125, 0, 0);
                case "plane_tangent" -> new Vec3d(0, 0.125, 0);
                default -> new Vec3d(2, 0, 0);
            };
            if (name.equals("profile_near_boundary")) {
                var shift = new Vec3d(33_554_428, 0, 0);
                a = a.add(shift); b = b.add(shift);
                box = new Aabb(box.min().add(shift), box.max().add(shift));
            }
            var capsule = new Capsule(new Segment3d(a, b), 0.25);
            for (int i = 0; i < 10_000; i++) measurementSink = CapsuleAabbCast3d.cast(capsule, delta, box);
            long[] nanos = new long[5_000];
            long thread = Thread.currentThread().threadId();
            long before = bean.getThreadAllocatedBytes(thread);
            for (int i = 0; i < nanos.length; i++) {
                long started = System.nanoTime();
                measurementSink = CapsuleAabbCast3d.cast(capsule, delta, box);
                nanos[i] = System.nanoTime() - started;
            }
            long allocated = bean.getThreadAllocatedBytes(thread) - before;
            Arrays.sort(nanos);
            var result = measurementSink;
            System.out.println(name + " warmup=10000 samples=5000 p50ns=" + nanos[2500] + " p95ns=" + nanos[4750]
                    + " p99ns=" + nanos[4950] + " maxns=" + nanos[4999] + " bytesPerCall=" + (allocated / 5000.0)
                    + " kind=" + result.kind() + " work=" + result.work());
        }
    }

    private record Fixture(Capsule capsule, Vec3d delta, Aabb box) {
        CapsuleAabbCastResult3d cast() { return CapsuleAabbCast3d.cast(capsule, delta, box); }
    }

    /** Differential characterization against the pinned prior jar, not a geometric oracle. */
    private static void printCompatibilitySignature() {
        int index = 0;
        for (double origin : new double[]{0, 30_000_000, -30_000_000})
            for (double x : new double[]{-2, -0.25, -0x1.0000000dbe6ffp-2, 0.5})
                for (double y : new double[]{-0.125, 0.5})
                    for (double radius : new double[]{0, 0.25, 16})
                        for (Vec3d delta : new Vec3d[]{ZERO, new Vec3d(2, 0, 0), new Vec3d(-1, 0, 0), new Vec3d(0, 0.125, 0)})
                            for (double length : new double[]{0, 4}) {
                                var a = new Vec3d(origin + x, origin + y, origin + 0.5);
                                var b = new Vec3d(origin + x, origin + y + length, origin + 0.5 + length);
                                var box = new Aabb(new Vec3d(origin, origin, origin), new Vec3d(origin + 1, origin + 1, origin + 1));
                                printResultBits(index++, CapsuleAabbCast3d.cast(new Capsule(new Segment3d(a, b), radius), delta, box));
                            }
        for (double h : new double[]{0x1.6a09e667a5fdcp3, 0x1.6a09e667a5fddp3, 0x1.6a09e667a5fdep3}) {
            var p = new Vec3d(-h, -h, 0.5);
            printResultBits(index++, CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), 16), new Vec3d(16, -16, 0), UNIT_BOX));
        }
        var p = new Vec3d(-1, -16, 0.5);
        printResultBits(index, CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), 16), new Vec3d(2, 0, 0), UNIT_BOX));
    }

    private static void printResultBits(int index, CapsuleAabbCastResult3d result) {
        var line = new StringBuilder().append(index).append('|').append(result.kind()).append('|').append(result.startRelation());
        line.append("|prefix:").append(result.safePrefixEnd().isPresent() ? bits(result.safePrefixEnd().getAsDouble()) : "absent");
        line.append("|bracket:");
        result.bandBracket().ifPresent(b -> line.append(bits(b.lastOutsideBandFraction())).append(',')
                .append(bits(b.insideContactBandFraction())).append(',').append(bits(b.contactClearanceUpper())).append(',')
                .append(bits(b.bandEntryPathWidth())).append(',').append(b.refinementLimited()));
        line.append("|normal:");
        result.normalEstimate().ifPresent(n -> line.append(bits(n.direction().x())).append(',').append(bits(n.direction().y()))
                .append(',').append(bits(n.direction().z())).append(',').append(bits(n.sourceFraction())).append(',').append(n.source()));
        line.append("|scale:");
        result.scale().ifPresent(s -> line.append(bits(s.epsilon())).append(',').append(bits(s.contactBandWidth()))
                .append(',').append(bits(s.penetrationSlop())));
        line.append("|reason:").append(result.reason()).append("|work:").append(result.work());
        System.out.println(line);
    }

    private static String bits(double value) { return Long.toHexString(Double.doubleToRawLongBits(value)); }

    private static void measureBatches() {
        var baseBean = ManagementFactory.getThreadMXBean();
        if (!(baseBean instanceof ThreadMXBean bean) || !bean.isThreadAllocatedMemorySupported()) {
            throw new IllegalStateException("thread allocation measurement unavailable");
        }
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        System.out.println("synthetic representative batches; runtime=" + System.getProperty("java.runtime.version")
                + " processors=" + Runtime.getRuntime().availableProcessors() + " contract=" + CapsuleAabbCast3d.contractRevision());
        for (String name : new String[]{"mixed", "all_tangent", "all_clear", "stress_all_hit"}) {
            for (int size : new int[]{8, 32, 64}) {
                if (name.equals("stress_all_hit") && size == 8) continue;
                if (name.equals("all_clear") && size != 32) continue;
                Fixture[] fixtures = new Fixture[size];
                for (int i = 0; i < size; i++) {
                    double offset = (i % 4) * 0.125;
                    var a = new Vec3d(-1 - offset, -2, 0);
                    var b = new Vec3d(-1 - offset, 2, 0);
                    var box = new Aabb(new Vec3d(0, -0.5, -0.5), new Vec3d(0.015625, 0.5, 0.5));
                    var delta = new Vec3d(-0.125, 0, 0);
                    if (name.equals("all_tangent")) {
                        a = new Vec3d(-0.25, 0.25 + offset, 0.5); b = a;
                        box = UNIT_BOX; delta = new Vec3d(0, 0.125, 0);
                    } else if (name.equals("stress_all_hit") || name.equals("mixed") && i == 0) {
                        delta = new Vec3d(2, 0, 0);
                    }
                    fixtures[i] = new Fixture(new Capsule(new Segment3d(a, b), 0.25), delta, box);
                }
                for (int i = 0; i < 1000; i++) for (var fixture : fixtures) measurementSink = fixture.cast();
                long[] nanos = new long[1000];
                long thread = Thread.currentThread().threadId();
                long before = bean.getThreadAllocatedBytes(thread);
                for (int i = 0; i < nanos.length; i++) {
                    long started = System.nanoTime();
                    for (var fixture : fixtures) measurementSink = fixture.cast();
                    nanos[i] = System.nanoTime() - started;
                }
                long allocated = bean.getThreadAllocatedBytes(thread) - before;
                Arrays.sort(nanos);
                var work = new TreeSet<String>();
                for (var fixture : fixtures) {
                    var result = fixture.cast();
                    work.add(result.kind() + ":" + result.work());
                }
                System.out.println(name + " size=" + size + " warmupBatches=1000 samples=1000 p50ns=" + nanos[500]
                        + " p95ns=" + nanos[950] + " p99ns=" + nanos[990] + " maxns=" + nanos[999]
                        + " bytesPerBatch=" + (allocated / 1000.0) + " bytesPerCast=" + (allocated / (1000.0 * size)) + " work=" + work);
            }
        }
    }

    @Test
    void ambiguousRefinementKeepsItsValidCoarserBracket() {
        var p = new Vec3d(-1, -16, 0.5);
        var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(p, p), 16), new Vec3d(2, 0, 0), UNIT_BOX);
        assertEquals(HIT_AFTER_CLEAR, result.kind());
        var bracket = result.bandBracket().orElseThrow();
        assertTrue(bracket.refinementLimited());
        assertTrue(bracket.bandEntryPathWidth() > 4 * result.scale().orElseThrow().epsilon());
        BigDecimal squaredBandRadius = exact(16).add(exact(result.scale().orElseThrow().contactBandWidth())).pow(2);
        BigDecimal lowX = exact(-1).add(exact(2).multiply(exact(bracket.lastOutsideBandFraction())));
        BigDecimal highX = exact(-1).add(exact(2).multiply(exact(bracket.insideContactBandFraction())));
        assertTrue(lowX.pow(2).add(exact(256)).compareTo(squaredBandRadius) > 0);
        assertTrue(highX.pow(2).add(exact(256)).compareTo(squaredBandRadius) <= 0);
        assertWorkAndFields(result);
    }

    @Test
    void faceEdgeAndCornerBracketsContainTheIndependentExactSquaredBandCrossing() {
        for (double[] offsets : new double[][]{{0.5, 0.5}, {-0.125, 0.5}, {-0.125, -0.125}}) {
            Vec3d center = new Vec3d(-1, offsets[0], offsets[1]);
            var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(center, center), 0.25), new Vec3d(2, 0, 0), UNIT_BOX);
            assertEquals(HIT_AFTER_CLEAR, result.kind());
            var bracket = result.bandBracket().orElseThrow();
            BigDecimal threshold = exact(0.25).add(exact(result.scale().orElseThrow().contactBandWidth())).pow(2);
            assertTrue(approachSquared(bracket.lastOutsideBandFraction(), offsets).compareTo(threshold) > 0);
            assertTrue(approachSquared(bracket.insideContactBandFraction(), offsets).compareTo(threshold) <= 0);
            assertTrue(bracket.insideContactBandFraction() < 0.5);
            assertWorkAndFields(result);
        }
    }

    @Test
    void grazingAPlaneFindsTheContactBandBeforePhysicalTangency() {
        var center = new Vec3d(-1, -0.25, 0.5);
        var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(center, center), 0.25), new Vec3d(3, 0, 0), UNIT_BOX);
        assertEquals(HIT_AFTER_CLEAR, result.kind());
        var bracket = result.bandBracket().orElseThrow();
        BigDecimal threshold = exact(0.25).add(exact(result.scale().orElseThrow().contactBandWidth())).pow(2);
        for (boolean high : new boolean[]{false, true}) {
            double t = high ? bracket.insideContactBandFraction() : bracket.lastOutsideBandFraction();
            BigDecimal x = exact(-1).add(exact(t).multiply(exact(3)));
            BigDecimal squared = x.pow(2).add(exact(0.25).pow(2));
            assertTrue(high ? squared.compareTo(threshold) <= 0 : squared.compareTo(threshold) > 0);
            assertTrue(x.signum() < 0, "contact-band bracket precedes x=0 physical tangency");
        }
        assertWorkAndFields(result);
    }

    @Test
    void arbitraryFixedCenterlineAndSixCoordinateDirectionsFindTheCylindricalMiddle() {
        for (int axis = 0; axis < 3; axis++) for (int sign : new int[]{-1, 1}) {
            var start = permute(new Vec3d(0, -2, -2), axis, sign);
            var end = permute(new Vec3d(0, 2, 2), axis, sign);
            Vec3d c0 = permute(new Vec3d(1, -0.5, -0.5), axis, sign);
            Vec3d c1 = permute(new Vec3d(1.015625, 0.5, 0.5), axis, sign);
            var box = new Aabb(c0.min(c1), c0.max(c1));
            var delta = permute(new Vec3d(2, 0, 0), axis, sign);
            var forward = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(start, end), 0.25), delta, box);
            var reversed = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(end, start), 0.25), delta, box);
            assertAxialBracket(forward, 0.75, 2);
            assertAxialBracket(reversed, 0.75, 2);
        }
    }

    @Test
    void largeCommonTranslationKeepsExactAxialGuaranteesWithItsOwnWorldAwareBand() {
        for (double origin : new double[]{0, 30_000_000, -30_000_000}) {
            var a = new Vec3d(origin, origin - 2, origin);
            var b = new Vec3d(origin, origin + 2, origin);
            var box = new Aabb(new Vec3d(origin + 1, origin - 0.5, origin - 0.5),
                    new Vec3d(origin + 1.015625, origin + 0.5, origin + 0.5));
            var result = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(a, b), 0.25), new Vec3d(2, 0, 0), box);
            assertAxialBracket(result, 0.75, 2);
            if (origin != 0) assertTrue(result.scale().orElseThrow().epsilon() >= 64 * Math.ulp(origin));
        }
    }

    @Test
    void seededDyadicFaceCasesAndSplitRequestsCheckGuaranteesRatherThanEqualDecisions() {
        var random = new Random(0x43484220260912L);
        for (int i = 0; i < 128; i++) {
            double radius = (1 + random.nextInt(8)) / 16.0;
            double gap = (1 + random.nextInt(16)) / 16.0;
            var a = new Vec3d(-radius - gap, 0.25, 0.5);
            var b = new Vec3d(-radius - gap, 0.75, 0.5);
            var capsule = new Capsule(new Segment3d(a, b), radius);
            var whole = CapsuleAabbCast3d.cast(capsule, new Vec3d(3, 0, 0), UNIT_BOX);
            assertAxialBracket(whole, gap, 3);
            double firstLength = gap / 2;
            var first = CapsuleAabbCast3d.cast(capsule, new Vec3d(firstLength, 0, 0), UNIT_BOX);
            assertEquals(CLEAR_SEPARATED, first.kind());
            var shift = new Vec3d(firstLength, 0, 0);
            var remainder = CapsuleAabbCast3d.cast(new Capsule(new Segment3d(a.add(shift), b.add(shift)), radius),
                    new Vec3d(3 - firstLength, 0, 0), UNIT_BOX);
            assertAxialBracket(remainder, gap - firstLength, 3 - firstLength);
        }
    }

    @Test
    void optionalTwentyFourPointFiveUnitHeadlessCrossingDoesNotSamplePastAThinWall() {
        var a = new Vec3d(0, -2, 0);
        var b = new Vec3d(0, 2, 0);
        var wall = new Aabb(new Vec3d(1, -0.5, -0.5), new Vec3d(1.015625, 0.5, 0.5));
        assertAxialBracket(CapsuleAabbCast3d.cast(new Capsule(new Segment3d(a, b), 0.25), new Vec3d(24.5, 0, 0), wall), 0.75, 24.5);
    }

    private static Vec3d permute(Vec3d v, int axis, int sign) {
        return switch (axis) {
            case 0 -> new Vec3d(sign * v.x(), v.y(), v.z());
            case 1 -> new Vec3d(v.z(), sign * v.x(), v.y());
            case 2 -> new Vec3d(v.y(), v.z(), sign * v.x());
            default -> throw new IllegalArgumentException("axis");
        };
    }

    private static BigDecimal approachSquared(double t, double[] offsets) {
        BigDecimal result = exact(-1).add(exact(t).multiply(exact(2))).pow(2);
        for (double offset : offsets) if (offset < 0) result = result.add(exact(offset).pow(2));
        return result;
    }

    private static void assertAxialBracket(CapsuleAabbCastResult3d result, double gap, double delta) {
        assertEquals(HIT_AFTER_CLEAR, result.kind());
        var bracket = result.bandBracket().orElseThrow();
        BigDecimal band = exact(result.scale().orElseThrow().contactBandWidth());
        BigDecimal low = exact(gap).subtract(exact(delta).multiply(exact(bracket.lastOutsideBandFraction())));
        BigDecimal high = exact(gap).subtract(exact(delta).multiply(exact(bracket.insideContactBandFraction())));
        assertTrue(low.compareTo(band) > 0, "analytic low clearance must exceed fixed band");
        assertTrue(high.compareTo(band) <= 0, "analytic high clearance must be within fixed band");
        // Clearance decreases affinely here, so this endpoint inequality proves the whole prefix.
        assertTrue(low.signum() > 0);
        BigDecimal exactWidth = exact(delta).multiply(exact(bracket.insideContactBandFraction()).subtract(exact(bracket.lastOutsideBandFraction())));
        assertTrue(exact(bracket.bandEntryPathWidth()).compareTo(exactWidth) >= 0);
        assertWorkAndFields(result);
    }

    private static void assertWorkAndFields(CapsuleAabbCastResult3d result) {
        assertEquals(CapsuleAabbCastResult3d.StartRelation.SEPARATED_OUTSIDE_BAND, result.startRelation());
        assertEquals(result.bandBracket().orElseThrow().lastOutsideBandFraction(), result.safePrefixEnd().orElseThrow());
        var work = result.work();
        assertTrue(work.evaluationCount() <= 73 && work.advancementAttempts() <= 32 && work.refinementAttempts() <= 40);
        assertTrue(work.evaluationCount() <= 1 + work.advancementAttempts() + work.refinementAttempts());
        assertTrue(result.reason().isEmpty());
    }

    private static BigDecimal exact(double value) { return new BigDecimal(value); }
}
