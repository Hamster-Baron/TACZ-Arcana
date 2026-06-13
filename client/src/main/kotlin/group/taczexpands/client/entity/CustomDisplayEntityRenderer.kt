package group.taczexpands.client.entity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import group.taczexpands.common.entity.CustomDisplayEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemDisplayContext

class CustomDisplayEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<CustomDisplayEntity>(context) {
    init {
        shadowRadius = 0f
    }

    override fun shouldShowName(pEntity: CustomDisplayEntity): Boolean {
        return false
    }

    override fun getTextureLocation(entity: CustomDisplayEntity): ResourceLocation {
        return CustomDisplayManager.tryGetTextureID(entity) ?: ResourceLocation("")
    }

    override fun render(entity: CustomDisplayEntity, entityYaw: Float, partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        try {
            val instance = CustomDisplayManager.getInstance(entity) ?: return
            poseStack.pushPose()

            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.yRot) - 180.0f))
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.xRot)))
            poseStack.translate(0.0, 1.5, 0.0)
            poseStack.scale(-1f, -1f, 1f)
            instance.controller?.update()
            instance.model.render(poseStack,
                ItemDisplayContext.GROUND,
                RenderType.entityTranslucent(instance.textureID),
                packedLight,
                OverlayTexture.NO_OVERLAY)

            poseStack.popPose()
        } catch (e: Exception) {
        }
    }


}