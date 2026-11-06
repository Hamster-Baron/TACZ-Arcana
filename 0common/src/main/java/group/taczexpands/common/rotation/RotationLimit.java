package group.taczexpands.common.rotation;

/** A resolved yaw interval and absolute pitch interval shared by both game sides. */
public record RotationLimit(double yawCenter, double yawMin, double yawMax, double pitchMin, double pitchMax) {
    public RotationLimit {
        for (double value : new double[] {yawCenter, yawMin, yawMax, pitchMin, pitchMax}) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("nonfinite limit");
            }
        }
        if (yawMin < -180 || yawMax > 180 || yawMin > yawMax
                || pitchMin < -90 || pitchMax > 90 || pitchMin > pitchMax) {
            throw new IllegalArgumentException("invalid limit");
        }
    }
}
