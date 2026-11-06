package group.taczexpands.client.render.surface;

import group.taczexpands.client.input.CameraInterpolation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** One resolved world pose and lens, consumed together by the renderer. */
public record SurfaceCameraPose(Vec3 position, float yaw, float pitch, float roll, float fov,
                                float distance, String vision, boolean shaders) {
    SurfaceCameraPose blendFrom(SurfaceCameraPose start, float progress) {
        float t = CameraInterpolation.smoothstep(progress);
        return new SurfaceCameraPose(start.position.lerp(position, t),
                CameraInterpolation.rotation(t, start.yaw, yaw), Mth.lerp(t, start.pitch, pitch),
                CameraInterpolation.rotation(t, start.roll, roll), Mth.lerp(t, start.fov, fov),
                Mth.lerp(t, start.distance, distance), vision, shaders);
    }
}
