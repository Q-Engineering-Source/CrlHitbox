package dev.crlhitbox.api.geometry.cast;

import dev.crlhitbox.api.geometry.Aabb;
import dev.crlhitbox.api.geometry.Capsule;
import dev.crlhitbox.api.geometry.Vec3d;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.BandBracket;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.Kind;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.NormalEstimate;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.NormalSource;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.QueryScale;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.StartRelation;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.UnresolvedReason;
import dev.crlhitbox.api.geometry.cast.CapsuleAabbCastResult3d.WorkCounts;

import java.util.OptionalDouble;

import static dev.crlhitbox.api.geometry.cast.CastEnclosure.point;

/** Per-call control state. All safety decisions use composed support bounds, never pair distance. */
final class CapsuleAabbCastKernel {
    private CapsuleAabbCastKernel() {
    }

    static CapsuleAabbCastResult3d cast(Capsule capsule, Vec3d delta, Aabb obstacle) {
        Run run = new Run();
        try {
            run.context = Context.create(capsule, delta, obstacle);
            if (run.context == null) return run.unresolved(UnresolvedReason.UNSUPPORTED_DOMAIN);
            return run.solve();
        } catch (ArithmeticException unavailableEnclosure) {
            // This boundary maps an explicit arithmetic failure to the contract's uncertainty result.
            // Illegal argument / state invariant violations deliberately remain fail-fast.
            return run.unresolved(UnresolvedReason.ARITHMETIC_UNCERTAINTY);
        }
    }

    private static final class Run {
        private final CapsuleAabbWitness.Candidate witness = new CapsuleAabbWitness.Candidate();
        private final double[] approximateStart = new double[3];
        private Context context;
        private int evaluations;
        private int advances;
        private int refinements;
        private StartRelation start = StartRelation.UNCLASSIFIED;
        private boolean hasPrefix;
        private double prefixEnd;
        private Evaluation lastOutside;
        private NormalEstimate lastNormal;

        CapsuleAabbCastResult3d solve() {
            Evaluation current = evaluate(0.0);
            if (current.pairDistance.upper() < point(context.radius).subtract(context.scale.penetrationSlop()).lower()) {
                start = StartRelation.OVERLAP;
                return result(Kind.INITIAL_OVERLAP, OptionalDouble.empty(), null, null, null);
            }
            if (!current.outsideBand) return initial(current);
            start = StartRelation.SEPARATED_OUTSIDE_BAND;
            hasPrefix = true;
            prefixEnd = 0.0;
            retainOutside(current);

            while (true) {
                if (current.insideBand) return refine(lastOutside, current);
                if (!current.hasSeparator()) return unresolved(current.axis == null
                        ? UnresolvedReason.NO_VALID_AXIS : UnresolvedReason.ARITHMETIC_UNCERTAINTY);
                if (current.outsideBand) retainOutside(current);
                if (current.closing.upper() <= 0.0 || current.endGap.lower() > 0.0 || current.time == 1.0) {
                    return result(Kind.CLEAR_SEPARATED, OptionalDouble.of(1.0), null, current.normal, null);
                }
                if (advances == 32) return unresolved(UnresolvedReason.ADVANCEMENT_LIMIT);
                advances++;
                double closure = point(current.gap.lower()).divide(current.closing.upper()).lower();
                double step = point(0.75).multiply(closure).lower();
                if (!(step > 0.0)) return unresolved(UnresolvedReason.NO_PROGRESS);
                // A lower bound >=1 proves the permitted step covers the endpoint before clamping.
                double nextLower = point(current.time).add(step).lower();
                if (!(nextLower > current.time)) return unresolved(UnresolvedReason.NO_PROGRESS);
                double next = Math.min(1.0, nextLower);
                // The preceding fixed axis proves every point of this step, not just its endpoint.
                prefixEnd = next;
                current = evaluate(next);
                if (next == 1.0 && !current.insideBand && !current.hasSeparator()) {
                    return unresolved(UnresolvedReason.ENDPOINT_AMBIGUOUS);
                }
            }
        }

        private CapsuleAabbCastResult3d initial(Evaluation initial) {
            if (!initial.insideBand) {
                start = initial.hasSeparator() ? StartRelation.SEPARATED_BAND_UNRESOLVED : StartRelation.AMBIGUOUS_START;
                return unresolved(UnresolvedReason.AMBIGUOUS_START);
            }
            if (initial.axis == null) {
                start = StartRelation.AMBIGUOUS_START;
                return unresolved(UnresolvedReason.NO_VALID_AXIS);
            }
            CastEnclosure slopSupport = point(context.scale.penetrationSlop()).multiply(initial.axisNorm.lower());
            if (point(initial.gap.lower()).add(slopSupport).lower() < 0.0) {
                start = StartRelation.AMBIGUOUS_START;
                return unresolved(UnresolvedReason.AMBIGUOUS_START);
            }
            start = StartRelation.NEAR_CONTACT;
            if (initial.closing.lower() > 0.0) {
                return result(Kind.HIT_INITIAL_INWARD, OptionalDouble.empty(), null, initial.normal, null);
            }
            boolean wholeSlop = point(Math.min(initial.gap.lower(), initial.endGap.lower())).add(slopSupport).lower() >= 0.0;
            if (wholeSlop && initial.closing.upper() < 0.0) {
                return result(Kind.CLEAR_INITIAL_OUTWARD, OptionalDouble.empty(), null, initial.normal, null);
            }
            if (wholeSlop && initial.closing.lower() <= 0.0 && initial.closing.upper() >= 0.0) {
                return result(Kind.CLEAR_INITIAL_TANGENT, OptionalDouble.empty(), null, initial.normal, null);
            }
            return result(Kind.HIT_INITIAL_NEAR_BAND, OptionalDouble.empty(), null, initial.normal, null);
        }

        private CapsuleAabbCastResult3d refine(Evaluation low, Evaluation high) {
            if (low == null || !low.outsideBand || !high.insideBand || !(low.time < high.time)) {
                throw new IllegalStateException("missing certified band endpoints");
            }
            boolean limited = false;
            while (pathWidth(low.time, high.time) > 4.0 * context.scale.epsilon()) {
                if (refinements == 40) { limited = true; break; }
                refinements++;
                double midpoint = low.time + (high.time - low.time) * 0.5;
                if (!(midpoint > low.time && midpoint < high.time)) { limited = true; break; }
                Evaluation middle;
                try {
                    middle = evaluate(midpoint);
                } catch (ArithmeticException ambiguousMidpoint) {
                    // The existing bracket remains valid; this failure cannot erase its evidence.
                    limited = true;
                    break;
                }
                if (middle.insideBand) high = middle;
                else if (middle.outsideBand) { low = middle; retainOutside(middle); }
                else { limited = true; break; }
            }
            BandBracket bracket = new BandBracket(low.time, high.time, context.scale.contactBandWidth(),
                    pathWidth(low.time, high.time), limited);
            NormalEstimate normal = high.normal != null ? high.normal : previousNormal();
            return result(Kind.HIT_AFTER_CLEAR, OptionalDouble.of(low.time), bracket, normal, null);
        }

        private double pathWidth(double low, double high) {
            return point(context.deltaNorm.upper()).multiply(point(high).subtract(low)).upper();
        }

        private void retainOutside(Evaluation evaluation) {
            lastOutside = evaluation;
            if (evaluation.normal != null) lastNormal = evaluation.normal;
        }

        private NormalEstimate previousNormal() {
            return lastNormal == null ? null : new NormalEstimate(lastNormal.direction(), lastNormal.sourceFraction(), NormalSource.LAST_SEPARATED);
        }

        private CapsuleAabbCastResult3d unresolved(UnresolvedReason reason) {
            return result(Kind.UNRESOLVED, hasPrefix ? OptionalDouble.of(prefixEnd) : OptionalDouble.empty(), null,
                    hasPrefix ? previousNormal() : null, reason);
        }

        private CapsuleAabbCastResult3d result(Kind kind, OptionalDouble prefix, BandBracket bracket,
                                              NormalEstimate normal, UnresolvedReason reason) {
            return new CapsuleAabbCastResult3d(kind, start, prefix, bracket, normal,
                    context == null ? null : context.scale, reason, new WorkCounts(evaluations, advances, refinements));
        }

        private Evaluation evaluate(double time) {
            if (++evaluations > 73) throw new IllegalStateException("evaluation budget exceeded");
            for (int i = 0; i < 3; i++) approximateStart[i] = context.rawStart[i] + time * context.rawDelta[i];
            var candidate = CapsuleAabbWitness.find(approximateStart, context.rawLine,
                    context.rawBoxMaximum, context.origin, context.worldMaximum, witness);
            CastEnclosure ax = context.start[0].add(context.delta[0].multiply(time));
            CastEnclosure ay = context.start[1].add(context.delta[1].multiply(time));
            CastEnclosure az = context.start[2].add(context.delta[2].multiply(time));
            CastEnclosure bx = context.end[0].add(context.delta[0].multiply(time));
            CastEnclosure by = context.end[1].add(context.delta[1].multiply(time));
            CastEnclosure bz = context.end[2].add(context.delta[2].multiply(time));
            CastEnclosure rx = residual(ax, candidate, 0);
            CastEnclosure ry = residual(ay, candidate, 1);
            CastEnclosure rz = residual(az, candidate, 2);
            CastEnclosure distance = CastEnclosure.norm(rx, ry, rz);
            boolean inside = distance.subtract(context.radius).upper() <= context.scale.contactBandWidth();
            double magnitude = Math.max(Math.abs(candidate.residual()[0]), Math.max(Math.abs(candidate.residual()[1]), Math.abs(candidate.residual()[2])));
            if (magnitude == 0.0) return new Evaluation(time, distance, null, null, null, null, null, null, inside, false);
            double[] axis = {candidate.residual()[0] / magnitude, candidate.residual()[1] / magnitude, candidate.residual()[2] / magnitude};
            CastEnclosure norm = CastEnclosure.norm(point(axis[0]), point(axis[1]), point(axis[2]));
            if (!(norm.lower() > 0.0)) throw new ArithmeticException("axis norm not bounded positive");
            CastEnclosure gap = context.gap(ax, ay, az, bx, by, bz, axis, norm);
            CastEnclosure endGap = context.gap(context.finishStart, context.finishEnd, axis, norm);
            CastEnclosure closing = CastEnclosure.dot(context.delta, axis).negate();
            boolean outside = gap.lower() > 0.0
                    && point(gap.lower()).divide(norm.upper()).lower() > context.scale.contactBandWidth();
            if (inside && outside) throw new IllegalStateException("contradictory proximity and separator bounds");
            NormalEstimate normal = normal(time, distance, axis);
            return new Evaluation(time, distance, axis, norm, gap, endGap, closing, normal, inside, outside);
        }

        private CastEnclosure residual(CastEnclosure translatedStart, CapsuleAabbWitness.Candidate candidate, int axis) {
            CastEnclosure feasiblePoint = translatedStart.add(context.line[axis].multiply(candidate.parameter()));
            CastEnclosure boxPoint = point(candidate.boxPoint()[axis]).subtract(context.origin[axis]);
            return feasiblePoint.subtract(boxPoint);
        }

        private NormalEstimate normal(double time, CastEnclosure pairDistance, double[] axis) {
            if (!(pairDistance.lower() > 4.0 * context.scale.contactBandWidth())) return null;
            double length = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
            Vec3d direction = new Vec3d(axis[0] / length, axis[1] / length, axis[2] / length);
            CastEnclosure unit = CastEnclosure.norm(point(direction.x()), point(direction.y()), point(direction.z()));
            if (unit.lower() < 1.0 - 0x1.0p-48 || unit.upper() > 1.0 + 0x1.0p-48) return null;
            return new NormalEstimate(direction, time, NormalSource.CURRENT_WITNESS);
        }
    }

    private record Evaluation(double time, CastEnclosure pairDistance, double[] axis, CastEnclosure axisNorm,
                              CastEnclosure gap, CastEnclosure endGap, CastEnclosure closing, NormalEstimate normal,
                              boolean insideBand, boolean outsideBand) {
        boolean hasSeparator() { return axis != null && gap.lower() > 0.0; }
    }

    /** Exact-input enclosures and one frozen scale, never reused between independent calls. */
    static final class Context {
        final double radius;
        final double[] origin;
        final double[] worldMaximum;
        final double[] rawStart;
        final double[] rawLine;
        final double[] rawDelta;
        final double[] rawBoxMaximum;
        final CastEnclosure[] start;
        final CastEnclosure[] end;
        final CastEnclosure[] line;
        final CastEnclosure[] delta;
        final CastEnclosure[] boxMaximum;
        final CastEnclosure[] finishStart;
        final CastEnclosure[] finishEnd;
        final CastEnclosure deltaNorm;
        final QueryScale scale;

        private Context(double radius, double[] origin, double[] worldMaximum, double[] rawStart,
                        double[] rawLine, double[] rawDelta, double[] rawBoxMaximum, CastEnclosure[] start,
                        CastEnclosure[] end, CastEnclosure[] line, CastEnclosure[] delta, CastEnclosure[] boxMaximum,
                        CastEnclosure[] finishStart, CastEnclosure[] finishEnd, CastEnclosure deltaNorm, QueryScale scale) {
            this.radius = radius;
            this.origin = origin;
            this.worldMaximum = worldMaximum;
            this.rawStart = rawStart;
            this.rawLine = rawLine;
            this.rawDelta = rawDelta;
            this.rawBoxMaximum = rawBoxMaximum;
            this.start = start;
            this.end = end;
            this.line = line;
            this.delta = delta;
            this.boxMaximum = boxMaximum;
            this.finishStart = finishStart;
            this.finishEnd = finishEnd;
            this.deltaNorm = deltaNorm;
            this.scale = scale;
        }

        static Context create(Capsule capsule, Vec3d movement, Aabb box) {
            double radius = capsule.radius();
            if (radius > 64.0) return null;
            double[] a = coordinates(capsule.centerline().start());
            double[] b = coordinates(capsule.centerline().end());
            double[] min = coordinates(box.min());
            double[] max = coordinates(box.max());
            double[] v = coordinates(movement);
            for (int i = 0; i < 3; i++) {
                if (Math.abs(a[i]) > 0x1.0p25 || Math.abs(b[i]) > 0x1.0p25
                        || Math.abs(min[i]) > 0x1.0p25 || Math.abs(max[i]) > 0x1.0p25 || Math.abs(v[i]) > 64.0) return null;
            }
            CastEnclosure[] start = new CastEnclosure[3];
            CastEnclosure[] end = new CastEnclosure[3];
            CastEnclosure[] line = new CastEnclosure[3];
            CastEnclosure[] delta = new CastEnclosure[3];
            CastEnclosure[] boxMax = new CastEnclosure[3];
            CastEnclosure[] finishA = new CastEnclosure[3];
            CastEnclosure[] finishB = new CastEnclosure[3];
            double[] rawStart = new double[3];
            double[] rawLine = new double[3];
            double[] rawMax = new double[3];
            double ulp = Math.ulp(radius);
            for (int i = 0; i < 3; i++) {
                delta[i] = point(v[i]);
                CastEnclosure worldFinishA = point(a[i]).add(delta[i]);
                CastEnclosure worldFinishB = point(b[i]).add(delta[i]);
                if (worldFinishA.absoluteUpper() > 0x1.0p25 || worldFinishB.absoluteUpper() > 0x1.0p25) return null;
                start[i] = point(a[i]).subtract(point(min[i]));
                end[i] = point(b[i]).subtract(point(min[i]));
                line[i] = point(b[i]).subtract(point(a[i]));
                boxMax[i] = point(max[i]).subtract(point(min[i]));
                if (boxMax[i].upper() > 128.0) return null;
                finishA[i] = start[i].add(delta[i]);
                finishB[i] = end[i].add(delta[i]);
                rawStart[i] = a[i] - min[i];
                rawLine[i] = b[i] - a[i];
                rawMax[i] = max[i] - min[i];
                double[] coordinateLike = {a[i], b[i], min[i], max[i], v[i], worldFinishA.lower(), worldFinishA.upper(), worldFinishB.lower(), worldFinishB.upper()};
                for (double coordinate : coordinateLike) ulp = Math.max(ulp, Math.ulp(coordinate));
            }
            CastEnclosure deltaNorm = CastEnclosure.norm(delta);
            double lineNorm = CastEnclosure.norm(line).upper();
            if (deltaNorm.upper() > 64.0 || lineNorm > 128.0) return null;
            double local = Math.max(Math.max(CastEnclosure.norm(start).upper(), CastEnclosure.norm(end).upper()),
                    Math.max(CastEnclosure.norm(finishA).upper(), CastEnclosure.norm(finishB).upper()));
            if (local > 512.0) return null;
            double lengthScale = Math.max(1.0, Math.max(radius, Math.max(lineNorm,
                    Math.max(CastEnclosure.norm(boxMax).upper(), Math.max(deltaNorm.upper(), local)))));
            double epsilon = Math.max(1e-10, Math.max(point(64.0).multiply(point(ulp)).upper(),
                    point(1e-12).multiply(point(lengthScale)).upper()));
            if (epsilon > 1.0 / 4096.0) return null;
            double band = point(8.0).multiply(point(epsilon)).upper();
            return new Context(radius, min, max, rawStart, rawLine, v, rawMax, start, end, line, delta, boxMax,
                    finishA, finishB, deltaNorm, new QueryScale(epsilon, band, band));
        }

        CastEnclosure[] at(CastEnclosure[] initial, double time) {
            CastEnclosure t = point(time);
            return new CastEnclosure[]{initial[0].add(delta[0].multiply(t)), initial[1].add(delta[1].multiply(t)),
                    initial[2].add(delta[2].multiply(t))};
        }

        CastEnclosure gap(CastEnclosure[] a, CastEnclosure[] b, double[] axis, CastEnclosure norm) {
            return gap(a[0], a[1], a[2], b[0], b[1], b[2], axis, norm);
        }

        CastEnclosure gap(CastEnclosure ax, CastEnclosure ay, CastEnclosure az,
                          CastEnclosure bx, CastEnclosure by, CastEnclosure bz, double[] axis, CastEnclosure norm) {
            CastEnclosure support = CastEnclosure.ZERO;
            for (int i = 0; i < 3; i++) if (axis[i] > 0.0) support = support.add(boxMaximum[i].multiply(axis[i]));
            return CastEnclosure.minimum(CastEnclosure.dot(ax, ay, az, axis), CastEnclosure.dot(bx, by, bz, axis))
                    .subtract(support).subtract(point(radius).multiply(norm));
        }

        private static double[] coordinates(Vec3d value) {
            return new double[]{value.x(), value.y(), value.z()};
        }
    }
}
