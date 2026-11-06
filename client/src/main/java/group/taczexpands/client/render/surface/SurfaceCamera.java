package group.taczexpands.client.render.surface;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class SurfaceCamera extends Camera {
    static SurfaceCameraPose resolve(SurfaceDefinition.Camera spec, Integer anchorId, float partialTick) {
        var mc = Minecraft.getInstance();
        float yaw = spec.yaw(), pitch = spec.pitch();
        Vec3 pos = new Vec3(spec.x(), spec.y(), spec.z());
        if (spec.space().equals("player") || spec.space().equals("entity")) {
            var anchor = spec.space().equals("player") ? mc.player
                    : mc.level == null || anchorId == null ? null : mc.level.getEntity(anchorId);
            if (anchor == null || anchor.isRemoved()) return null;
            float py = anchor.getViewYRot(partialTick), pp = anchor.getViewXRot(partialTick);
            Vec3 forward = Vec3.directionFromRotation(pp, py);
            Vec3 right = Vec3.directionFromRotation(0, py + 90);
            Vec3 up = right.cross(forward);
            pos =
                    anchor
                            .getEyePosition(partialTick)
                            .add(right.scale(pos.x))
                            .add(up.scale(pos.y))
                            .add(forward.scale(pos.z));
            yaw += py;
            pitch += pp;
        }
        return new SurfaceCameraPose(pos, yaw, pitch, spec.roll(), spec.fov(), spec.distance(), spec.vision(), spec.shaders());
    }

    public void configure(SurfaceCameraPose pose, float partialTick, boolean boneCamera) {
        var mc = Minecraft.getInstance();
        setup(mc.level, mc.player, !boneCamera && pose.position().distanceToSqr(mc.player.getEyePosition(partialTick)) > 1e-10,
                false, partialTick);
        setPosition(pose.position());
        setRotation(pose.yaw(), pose.pitch());
    }
}
