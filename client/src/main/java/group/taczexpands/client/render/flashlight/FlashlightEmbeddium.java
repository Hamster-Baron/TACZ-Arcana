package group.taczexpands.client.render.flashlight;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.Consumer;

/** Draws the existing terrain and block-entity lists without the world-stage callbacks. */
public final class FlashlightEmbeddium {
    private FlashlightEmbeddium() {}

    public static void drawLayer(RenderType layer, PoseStack pose, Vec3 camera) {
        RenderDevice.enterManagedCode();
        try {
            SodiumWorldRenderer.instance().drawChunkLayer(layer, pose, camera.x, camera.y, camera.z);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    public static void forEachVisible(Consumer<BlockEntity> draw) {
        SodiumWorldRenderer.instance().forEachVisibleBlockEntity(draw);
    }
}
