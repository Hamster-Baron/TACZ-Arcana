package group.taczexpands.client.render

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import group.taczexpands.client.accessor.IAccessorGameRenderer
import group.taczexpands.client.accessor.IAccessorLevelRenderer
import group.taczexpands.client.compat.iris.IrisCompat
import group.taczexpands.client.compat.RendererPresence
import group.taczexpands.client.render.flashlight.FlashlightDepthState
import net.minecraft.client.Minecraft
import org.joml.Matrix3f
import org.joml.Matrix4f

object Depth {
    var depthRendering = false
    var depthRenderTarget: RenderTarget? = null

    fun call(flashlight: FlashlightData) {
        val previousRendering = depthRendering
        val previousTarget = depthRenderTarget
        val state = FlashlightDepthState.begin()
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushPose()
        modelView.setIdentity()
        RenderSystem.applyModelViewMatrix()
        try {
            renderDepth(flashlight)
        } finally {
            try {
                try {
                    modelView.popPose()
                } finally {
                    state.restore()
                }
            } finally {
                depthRenderTarget = previousTarget
                depthRendering = previousRendering
            }
        }
    }

    private fun renderDepth(flashlight: FlashlightData) {
        flashlight.prepareRenderTarget()
        depthRenderTarget = flashlight.depthRenderTarget.textureTarget
        depthRendering = true
        flashlight.depthRenderTarget.textureTarget.bindWrite(true)

        val mc = Minecraft.getInstance()
        val gameRenderer = mc.gameRenderer
        gameRenderer as IAccessorGameRenderer

        val levelRenderer = mc.levelRenderer

        val camera = flashlight.camera
        val partialTick = Minecraft.getInstance().partialTick
        val projMat = Matrix4f().perspective(
            Math.toRadians(maxOf(70.0, minOf(178.0, flashlight.angle * 2.0 + 2.0))).toFloat(),
            1.0f, 0.05f, flashlight.range)

        gameRenderer.resetProjectionMatrix(projMat)
        flashlight.updateCamera(partialTick)
        val poseStack = PoseStack()

        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot))
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.yRot + 180.0f))

        val invViewRotationMatrix = Matrix3f(poseStack.last().normal()).invert()
        RenderSystem.setInverseViewRotationMatrix(invViewRotationMatrix)

        levelRenderer.prepareCullFrustum(poseStack, camera.position, projMat)
        val prevRenderHand = (gameRenderer as IAccessorGameRenderer).`taczexpands$getRenderHand`()
        gameRenderer.setRenderHand(false)
        val iris = RendererPresence.OCULUS_PRESENT
        val previousLayout = if (iris) IrisCompat.beginLightweightLayout() else false
        val previousShaderHook = iris && IrisCompat.setHookUsingShaderPack()
        try {
            (levelRenderer as IAccessorLevelRenderer).`taczexpands$renderDepth`(flashlight, poseStack, partialTick, camera, projMat)
        } finally {
            if (iris) {
                try {
                    IrisCompat.restoreHookUsingShaderPack(previousShaderHook)
                } finally {
                    IrisCompat.endLightweightLayout(previousLayout)
                }
            }
        }
        gameRenderer.setRenderHand(prevRenderHand)
        flashlight.flashlightCameraToClip.set(projMat)
        flashlight.worldToFlashlightCamera.set(poseStack.last().pose())
    }
}
