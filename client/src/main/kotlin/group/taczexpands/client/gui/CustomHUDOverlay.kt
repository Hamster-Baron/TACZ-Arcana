package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import group.taczexpands.client.override.BeamRendererOverride
import group.taczexpands.common.network.v2.s2c.S2CAddHUD
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import org.joml.Matrix4f
import org.joml.Vector4f

object CustomHUDOverlay : IGuiOverlay {
    val hudMap = mutableMapOf<String, S2CAddHUD>()

    override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        val poseStack = guiGraphics.pose()

        hudMap.values.forEach { hud ->
            var targetX = 0f
            var targetY = 0f
            val finalScale = hud.scale

            when (hud.renderSpace) {
                S2CAddHUD.RenderSpace.WORLD -> {
                    val camera = mc.gameRenderer.mainCamera
                    val cameraPos = camera.position

                    val rx = hud.x - cameraPos.x
                    val ry = hud.y - cameraPos.y
                    val rz = hud.z - cameraPos.z

                    if (rx * rx + ry * ry + rz * rz > 128 * 128) return@forEach

                    val modelViewMatrix = Matrix4f(BeamRendererOverride.lastViewMatrix)
                    val projectionMatrix = Matrix4f(BeamRendererOverride.lastProjectionMatrix)

                    val screenPos = Vector4f(rx.toFloat(), ry.toFloat(), rz.toFloat(), 1.0f)
                    screenPos.mul(modelViewMatrix).mul(projectionMatrix)

                    if (screenPos.w <= 0.0f) return@forEach

                    val ndcX = screenPos.x / screenPos.w
                    val ndcY = screenPos.y / screenPos.w
                    targetX = (ndcX + 1.0f) * 0.5f * screenWidth
                    targetY = (1.0f - ndcY) * 0.5f * screenHeight
                }

                S2CAddHUD.RenderSpace.SCREEN -> {
                    targetX = hud.x.toFloat() + screenWidth / 2.0f
                    targetY = hud.y.toFloat() + screenHeight / 2.0f
                }
            }

            poseStack.pushPose()
            poseStack.translate(targetX, targetY, 0f)
            poseStack.scale(finalScale, finalScale, 1.0f)

            if (hud.imagePath != null) {
                val textureLocation = ResourceLocation.tryParse(hud.imagePath)
                if (textureLocation != null) {
                    val imgWidth = hud.imageWidth
                    val imgHeight = hud.imageHeight

                    val imgOffsetX = when (hud.alignment) {
                        S2CAddHUD.Alignment.LEFT -> 0f
                        S2CAddHUD.Alignment.CENTER -> -imgWidth / 2f
                        S2CAddHUD.Alignment.RIGHT -> -imgWidth.toFloat()
                    }
                    val imgOffsetY = -imgHeight / 2f

                    poseStack.pushPose()
                    poseStack.translate(imgOffsetX, imgOffsetY, 0f)

                    RenderSystem.setShader { GameRenderer.getPositionTexShader() }
                    RenderSystem.setShaderTexture(0, textureLocation)
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

                    guiGraphics.blit(textureLocation, 0, 0, 0f, 0f, imgWidth, imgHeight, imgWidth, imgHeight)

                    poseStack.popPose()
                }
            }

            if (hud.text != null) {
                val font = mc.font
                val textWidth = font.width(hud.text)
                val textHeight = 8

                val textOffsetX = when (hud.alignment) {
                    S2CAddHUD.Alignment.LEFT -> 0f
                    S2CAddHUD.Alignment.CENTER -> -textWidth / 2f
                    S2CAddHUD.Alignment.RIGHT -> -textWidth.toFloat()
                }
                val textOffsetY = -textHeight / 2f

                poseStack.pushPose()
                poseStack.translate(textOffsetX, textOffsetY, 0f)

                guiGraphics.drawString(font, hud.text, 0, 0, 0xFFFFFF, true)

                poseStack.popPose()
            }

            poseStack.popPose()
        }
    }

    fun reset() {
        hudMap.clear()
    }
}