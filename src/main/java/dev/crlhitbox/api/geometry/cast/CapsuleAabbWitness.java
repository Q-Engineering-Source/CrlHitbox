package dev.crlhitbox.api.geometry.cast;

import java.util.Arrays;

/** Bounded active-set candidate selection. No candidate distance is safety authority. */
final class CapsuleAabbWitness {
    private CapsuleAabbWitness() {
    }

    /** Query-owned scratch; no Evaluation or public result retains these mutable arrays. */
    static final class Candidate {
        private final double[] breaks = new double[8];
        private final double[] boxPoint = new double[3];
        private final double[] residual = new double[3];
        private double parameter;
        private double distanceEstimate;
        private boolean available;

        double parameter() { return parameter; }
        double[] boxPoint() { return boxPoint; }
        double[] residual() { return residual; }
    }

    static Candidate find(double[] start, double[] direction, double[] boxMaximum,
                          double[] origin, double[] worldMaximum, Candidate best) {
        best.available = false;
        double[] breaks = best.breaks;
        int count = 2;
        breaks[0] = 0.0;
        breaks[1] = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            if (direction[axis] == 0.0) continue;
            count = crossing(breaks, count, -start[axis] / direction[axis]);
            count = crossing(breaks, count, (boxMaximum[axis] - start[axis]) / direction[axis]);
        }
        Arrays.sort(breaks, 0, count);
        int unique = 1;
        for (int i = 1; i < count; i++) if (breaks[i] != breaks[unique - 1]) breaks[unique++] = breaks[i];
        for (int i = 0; i < unique; i++) consider(breaks[i], start, direction, origin, worldMaximum, best);
        for (int i = 0; i + 1 < unique; i++) {
            double low = breaks[i];
            double high = breaks[i + 1];
            double middle = low + (high - low) * 0.5;
            double quadratic = 0.0;
            double linear = 0.0;
            for (int axis = 0; axis < 3; axis++) {
                double position = start[axis] + middle * direction[axis];
                if (position < 0.0 || position > boxMaximum[axis]) {
                    double offset = position < 0.0 ? start[axis] : start[axis] - boxMaximum[axis];
                    quadratic += direction[axis] * direction[axis];
                    linear += direction[axis] * offset;
                }
            }
            if (quadratic == 0.0) continue;
            double parameter = -linear / quadratic;
            if (Double.isFinite(parameter) && parameter > low && parameter < high) {
                consider(parameter, start, direction, origin, worldMaximum, best);
            }
        }
        if (!best.available) throw new ArithmeticException("no finite feasible witness candidate");
        return best;
    }

    private static int crossing(double[] values, int count, double value) {
        if (Double.isFinite(value) && value >= 0.0 && value <= 1.0) values[count++] = value == 0.0 ? 0.0 : value;
        return count;
    }

    private static void consider(double parameter, double[] start, double[] direction,
                                 double[] origin, double[] worldMaximum, Candidate best) {
        double x = start[0] + parameter * direction[0];
        double y = start[1] + parameter * direction[1];
        double z = start[2] + parameter * direction[2];
        // q is clamped in original world coordinates, hence is a feasible binary64 box point.
        double qx = Math.max(origin[0], Math.min(worldMaximum[0], origin[0] + x));
        double qy = Math.max(origin[1], Math.min(worldMaximum[1], origin[1] + y));
        double qz = Math.max(origin[2], Math.min(worldMaximum[2], origin[2] + z));
        double rx = x - (qx - origin[0]);
        double ry = y - (qy - origin[1]);
        double rz = z - (qz - origin[2]);
        double estimate = Math.hypot(Math.hypot(rx, ry), rz);
        if (!Double.isFinite(estimate) || !better(best, parameter, qx, qy, qz, estimate)) return;
        best.available = true;
        best.parameter = parameter;
        best.distanceEstimate = estimate;
        best.boxPoint[0] = qx; best.boxPoint[1] = qy; best.boxPoint[2] = qz;
        best.residual[0] = rx; best.residual[1] = ry; best.residual[2] = rz;
    }

    private static boolean better(Candidate current, double parameter, double x, double y, double z, double estimate) {
        if (!current.available || estimate < current.distanceEstimate) return true;
        if (estimate > current.distanceEstimate) return false;
        if (parameter < current.parameter) return true;
        if (parameter > current.parameter) return false;
        int comparison = Double.compare(x, current.boxPoint[0]);
        if (comparison != 0) return comparison < 0;
        comparison = Double.compare(y, current.boxPoint[1]);
        if (comparison != 0) return comparison < 0;
        return Double.compare(z, current.boxPoint[2]) < 0;
    }
}
