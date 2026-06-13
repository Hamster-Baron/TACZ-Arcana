package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.render.Depth;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    public void hookRenderToBuffer(EntityModel instance, PoseStack poseStack, VertexConsumer vertexConsumer, int i1, int i2, float v1, float v2, float v3, float v4) {
        if (CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse() && TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            instance.renderToBuffer(poseStack, vertexConsumer, LightTexture.pack(15, 15), OverlayTexture.RED_OVERLAY_V, 1.0f, v2 * 0.2f, v3 * 0.2f, v4);
            return;
        }
        instance.renderToBuffer(poseStack, vertexConsumer, i1, i2, v1, v2, v3, v4);
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
    public void redirectRenderLayerCall(RenderLayer instance, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Entity t, float v1, float v2, float v3, float v4, float v5, float v6, Operation<Void> original) {
        if (Depth.INSTANCE.getDepthRendering()) return;
        original.call(instance, poseStack, multiBufferSource, i, t, v1, v2, v3, v4, v5, v6);
    }

}
