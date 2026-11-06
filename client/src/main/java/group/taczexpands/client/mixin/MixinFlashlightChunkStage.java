package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import group.taczexpands.client.render.Depth;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelRenderer.class, priority = 1100)
public abstract class MixinFlashlightChunkStage {
    @WrapOperation(method = "renderChunkLayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V", remap = false))
    private void taczexpands$depthChunkStage(RenderType type, LevelRenderer renderer, PoseStack pose,
            Matrix4f projection, int frame, Camera camera, Frustum frustum, Operation<Void> original) {
        if (!Depth.INSTANCE.getDepthRendering())
            original.call(type, renderer, pose, projection, frame, camera, frustum);
    }
}
