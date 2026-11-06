package group.taczexpands.client.input;

import net.minecraft.util.Mth;

/** Easing and shortest yaw interpolation shared by main and auxiliary cameras. */
public final class CameraInterpolation {
    private CameraInterpolation() {}

    public static float smoothstep(float progress) {
        float t = Mth.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static float rotation(float progress, float from, float to) {
        return Mth.rotLerp(progress, from, to);
    }
}
