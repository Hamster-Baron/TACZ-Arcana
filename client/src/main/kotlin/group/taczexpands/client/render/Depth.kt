package group.taczexpands.client.render

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import group.taczexpands.client.accessor.IAccessorGameRenderer
import group.taczexpands.client.compat.iris.IrisCompat
import group.taczexpands.client.compat.CompatHelper
import group.taczexpands.client.mixin.accessor.IAccessorCamera
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f

object Depth {
    var depthRendering = false
    var depthRenderTarget: RenderTarget? = null
    var outputScreenshot = false

    fun call(flashlight: FlashlightData) {
        val prevProjMatrix = RenderSystem.getProjectionMatrix()
        val prevInvViewRotationMat = RenderSystem.getInverseViewRotationMatrix()

        flashlight.prepareRenderTarget()
        depthRenderTarget = flashlight.depthRenderTarget.textureTarget
        depthRendering = true
        flashlight.depthRenderTarget.textureTarget.bindWrite(false)
        flashlight.depthRenderTarget.textureTarget.bindRead()

        val mc = Minecraft.getInstance()
        val gameRenderer = mc.gameRenderer
        gameRenderer as IAccessorGameRenderer

        val levelRenderer = mc.levelRenderer

        val projPostStack = PoseStack()
        val camera = flashlight.camera
        camera as IAccessorCamera
        val partialTick = Minecraft.getInstance().partialTick


        projPostStack.mulPoseMatrix(gameRenderer.getProjectionMatrix(70.0))

        val projMat = projPostStack.last().pose()

        gameRenderer.resetProjectionMatrix(projMat)
        if (!flashlight.isFirstPerson) {
            camera.setEyeHeight(flashlight.entity.eyeHeight)
            camera.setEyeHeightOld(flashlight.entity.eyeHeight)
        } else {
            camera.setEyeHeight(0.0f)
            camera.setEyeHeightOld(0.0f)
        }
        camera.setup(flashlight.entity.level(), flashlight.entity, false, false, partialTick)
        if (flashlight.isFirstPerson) {
            val pos = flashlight.firstPersonCameraWorldPosition
            val rot = flashlight.firstPersonCameraWorldRotation
            camera.setPosition(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
            camera.setRotation(rot.x, rot.y)
        } else {
            camera.move(0.5, 0.0, 0.0)
        }
        val poseStack = PoseStack()

        poseStack.mulPose(Axis.XP.rotationDegrees(camera.xRot))
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.yRot + 180.0f))

        val invViewRotationMatrix = Matrix3f(poseStack.last().normal()).invert()
        RenderSystem.setInverseViewRotationMatrix(invViewRotationMatrix)

        levelRenderer.prepareCullFrustum(poseStack, camera.position, gameRenderer.getProjectionMatrix(70.0))
        val prevRenderHand = (gameRenderer as IAccessorGameRenderer).`taczexpands$getRenderHand`()
        gameRenderer.setRenderHand(false)
        if (CompatHelper.hasIris()) {
            IrisCompat.setHookUsingShaderPack()
        }
        levelRenderer.renderLevel(poseStack, partialTick, 0L, false, camera, gameRenderer, gameRenderer.lightTexture(), projMat)
        if (CompatHelper.hasIris()) {
            IrisCompat.unSetHookUsingShaderPack()
        }
        gameRenderer.setRenderHand(prevRenderHand)
        flashlight.flashlightCameraToClip.set(RenderSystem.getProjectionMatrix())
        flashlight.worldToFlashlightCamera.set(Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose()))

        gameRenderer.resetProjectionMatrix(prevProjMatrix)
        RenderSystem.setInverseViewRotationMatrix(prevInvViewRotationMat)

        depthRendering = false
        depthRenderTarget = null
        Minecraft.getInstance().mainRenderTarget.bindWrite(false)
        Minecraft.getInstance().mainRenderTarget.bindRead()
    }
}