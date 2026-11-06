package group.taczexpands.common.rotation;

import java.util.ArrayList;
import java.util.List;

/** Computes angle limits and intersections for rotation locks. */
public final class RotationMath {
    private RotationMath() {}

    public static double wrap(double angle) {
        double wrapped = angle % 360;
        return wrapped >= 180 ? wrapped - 360 : wrapped < -180 ? wrapped + 360 : wrapped;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /** Intersect active ranges, retaining the previous angles if the intersection is empty. */
    public static double[] apply(List<RotationLimit> limits, double oldYaw, double oldPitch,
                                 double requestedYaw, double requestedPitch) {
        if (!Double.isFinite(oldYaw) || !Double.isFinite(oldPitch)
                || !Double.isFinite(requestedYaw) || !Double.isFinite(requestedPitch)) {
            return new double[] {oldYaw, oldPitch};
        }

        List<double[]> allowedYaw = new ArrayList<>();
        allowedYaw.add(new double[] {oldYaw - 360, oldYaw + 360});
        double minimumPitch = -90;
        double maximumPitch = 90;

        for (RotationLimit limit : limits) {
            minimumPitch = Math.max(minimumPitch, limit.pitchMin());
            maximumPitch = Math.min(maximumPitch, limit.pitchMax());
            if (limit.yawMax() - limit.yawMin() >= 360) continue;

            double center = oldYaw + wrap(limit.yawCenter() - oldYaw);
            List<double[]> intersection = new ArrayList<>();
            // Adjacent turns cover intervals that cross the -180/180 boundary.
            for (double[] interval : allowedYaw) {
                for (int turn = -1; turn <= 1; turn++) {
                    double minimum = Math.max(interval[0], center + limit.yawMin() + 360 * turn);
                    double maximum = Math.min(interval[1], center + limit.yawMax() + 360 * turn);
                    if (minimum <= maximum) intersection.add(new double[] {minimum, maximum});
                }
            }
            allowedYaw = intersection;
        }

        if (allowedYaw.isEmpty() || minimumPitch > maximumPitch) return new double[] {oldYaw, oldPitch};

        double[] selected = allowedYaw.get(0);
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (double[] interval : allowedYaw) {
            double distance = Math.abs(clamp(oldYaw, interval[0], interval[1]) - oldYaw);
            if (distance < nearestDistance) {
                selected = interval;
                nearestDistance = distance;
            }
        }
        return new double[] {
            clamp(requestedYaw, selected[0], selected[1]),
            clamp(requestedPitch, minimumPitch, maximumPitch)
        };
    }
}
