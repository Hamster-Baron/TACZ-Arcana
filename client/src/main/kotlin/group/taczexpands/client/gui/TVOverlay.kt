package group.taczexpands.client.gui

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.client.input.InputManager
import group.taczexpands.common.entity.EntityKineticBulletShared
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import kotlin.math.abs


object TVOverlay : IGuiOverlay {
    var xCenterPos: Float = 0.0f
    var yCenterPos: Float = 0.0f

    var yCompassBar: Float = 0.0f
    var compassBarLength: Float = 0.0f
    var compassBarWidth: Float = 0.0f

    var heightBarLength: Float = 0.0f
    var heightBarWidth: Float = 0.0f

    var speedBarLength: Float = 0.0f
    var speedBarWidth: Float = 0.0f

    val cardinalDirections = setOf(0, 90, 180, 270)

    override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTicks: Float, screenWidth: Int, screenHeight: Int) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val cameraEntity = mc.cameraEntity
        if (cameraEntity !is EntityKineticBullet) return

        val camera = mc.gameRenderer.mainCamera

        gui.setupOverlayRenderState(true, false)

        val textureID = cameraEntity.entityData.get(EntityKineticBulletShared.OVERLAY_TEXTURE_DATA_ACCESSOR)
        if (!textureID.isNullOrEmpty()) {
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
            guiGraphics.blit(ResourceLocation(textureID), 0, 0, -90, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight)
            RenderSystem.depthMask(true)
            RenderSystem.enableDepthTest()
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f)
        }

        var yaw = camera.yRot % 360
        if (yaw < 0) yaw += 360

        xCenterPos = mc.window.guiScaledWidth / 2.0f
        yCenterPos = mc.window.guiScaledHeight / 2.0f

        speedBarLength = mc.window.guiScaledHeight / 2.0f
        speedBarWidth = 8.0f

        heightBarLength = mc.window.guiScaledHeight / 2.0f
        heightBarWidth = 8.0f


        val xCenterSpeedBar = xCenterPos - xCenterPos / 2.0f
        fillRect(guiGraphics, xCenterSpeedBar, yCenterPos - speedBarLength / 2.0f + 0.5f, xCenterSpeedBar + 2.0f, yCenterPos + speedBarLength / 2.0f - 0.5f, 0xffffffff.toInt())
        fillRect(guiGraphics,
            xCenterSpeedBar,
            yCenterPos - speedBarLength / 2.0f - 0.5f,
            xCenterSpeedBar + 2.0f + speedBarWidth / 2.0f,
            yCenterPos - speedBarLength / 2.0f + 0.5f,
            0xffffffff.toInt())
        fillRect(guiGraphics,
            xCenterSpeedBar,
            yCenterPos + speedBarLength / 2.0f - 0.5f,
            xCenterSpeedBar + 2.0f + speedBarWidth / 2.0f,
            yCenterPos + speedBarLength / 2.0f + 0.5f,
            0xffffffff.toInt())

        (1..7).forEach {
            fillRect(guiGraphics,
                xCenterSpeedBar + 2.0f,
                yCenterPos - speedBarLength / 2.0f + it * speedBarLength / 8.0f - 0.5f,
                xCenterSpeedBar + 2.0f + speedBarWidth / 2.0f,
                yCenterPos - speedBarLength / 2.0f + it * speedBarLength / 8.0f + 0.5f,
                0xffffffff.toInt())
        }

        val currentSpeed = cameraEntity.entityData.get(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR) * 20.0f
        val maxSpeed = cameraEntity.entityData.get(EntityKineticBulletShared.MAX_SPEED_DATA_ACCESSOR) * 20.0f

        val speedDrawPercent = 1.0f - (currentSpeed / maxSpeed).coerceIn(0.0f, 1.0f)

        val currentSpeedYPos = yCenterPos - speedBarLength / 2.0f + speedBarLength * speedDrawPercent

        fillRect(guiGraphics,
            xCenterSpeedBar + 2.0f,
            currentSpeedYPos - 0.5f,
            xCenterSpeedBar + 2.0f + speedBarWidth,
            currentSpeedYPos + 0.5f,
            0xffffffff.toInt())

        drawYCenteredString(guiGraphics, mc.font, String.format("%.2f", currentSpeed), xCenterSpeedBar + 2.0f + speedBarWidth + 2.0f, currentSpeedYPos, 0xffffff)


        val xCenterHeightBar = xCenterPos + xCenterPos / 2.0f

        fillRect(guiGraphics, xCenterHeightBar - 2.0f, yCenterPos - heightBarLength / 2.0f + 0.5f, xCenterHeightBar, yCenterPos + heightBarLength / 2.0f - 0.5f, 0xffffffff.toInt())

        fillRect(guiGraphics,
            xCenterHeightBar - 2.0f - heightBarWidth / 2.0f,
            yCenterPos - heightBarLength / 2.0f - 0.5f,
            xCenterHeightBar,
            yCenterPos - heightBarLength / 2.0f + 0.5f,
            0xffffffff.toInt())

        fillRect(guiGraphics,
            xCenterHeightBar - 2.0f - heightBarWidth / 2.0f,
            yCenterPos + heightBarLength / 2.0f - 0.5f,
            xCenterHeightBar,
            yCenterPos + heightBarLength / 2.0f + 0.5f,
            0xffffffff.toInt())

        (1..7).forEach {
            fillRect(guiGraphics,
                xCenterHeightBar - 2.0f - heightBarWidth / 2.0f,
                yCenterPos - heightBarLength / 2.0f + it * heightBarLength / 8.0f - 0.5f,
                xCenterHeightBar - 2.0f,
                yCenterPos - heightBarLength / 2.0f + it * heightBarLength / 8.0f + 0.5f,
                0xffffffff.toInt())
        }

        val topBlockYPos = player.level().getHeight(Heightmap.Types.WORLD_SURFACE, cameraEntity.blockX, cameraEntity.blockZ)
        val relativeYPos = cameraEntity.y - topBlockYPos

        val heightDrawPercent = 1.0f - (relativeYPos.toFloat() / 100.0f).coerceIn(0.0f, 1.0f)
        val currentHeightYPos = yCenterPos - heightBarLength / 2.0f + heightBarLength * heightDrawPercent

        fillRect(guiGraphics,
            xCenterHeightBar - 2.0f - heightBarWidth,
            currentHeightYPos - 0.5f,
            xCenterHeightBar - 2.0f,
            currentHeightYPos + 0.5f,
            0xffffffff.toInt())

        val yPosStr = String.format("%.2f", relativeYPos)
        drawYCenteredString(guiGraphics, mc.font, yPosStr, xCenterHeightBar - 2.0f - heightBarWidth - 2.0f - mc.font.width(yPosStr), currentHeightYPos, 0xffffff)

        yCompassBar = yCenterPos - yCenterPos / 2.0f - 32.0f

        compassBarLength = mc.window.guiScaledWidth / 2.0f
        compassBarWidth = 8.0f


        drawFullSizeDirection(guiGraphics, yaw, 0.0f, "S")
        drawFullSizeDirection(guiGraphics, yaw, 90.0f, "W")
        drawFullSizeDirection(guiGraphics, yaw, 180.0f, "N")
        drawFullSizeDirection(guiGraphics, yaw, 270.0f, "E")

        ((0 until 360) step 5).filter { it !in cardinalDirections }.forEach {
            if (it % 10 == 0)
                drawFullSizeDirection(guiGraphics, yaw, it.toFloat(), null)
            else
                drawHalfSizeDirection(guiGraphics, yaw, it.toFloat())
        }

        fillRect(guiGraphics,
            xCenterPos - 0.5f,
            yCompassBar - compassBarWidth / 2.0f,
            xCenterPos + 0.5f,
            yCompassBar + compassBarWidth + compassBarWidth / 2.0f,
            0xffffffff.toInt())
        fillRect(guiGraphics,
            xCenterPos - compassBarLength / 2.0f - 0.5f,
            yCompassBar,
            xCenterPos - compassBarLength / 2.0f + 0.5f,
            yCompassBar + compassBarWidth,
            0xffffffff.toInt())
        fillRect(guiGraphics,
            xCenterPos + compassBarLength / 2.0f - 0.5f,
            yCompassBar,
            xCenterPos + compassBarLength / 2.0f + 0.5f,
            yCompassBar + compassBarWidth,
            0xffffffff.toInt())

        drawXCenteredString(guiGraphics, mc.font, yaw.toInt().toString(), xCenterPos, yCompassBar + compassBarWidth + compassBarWidth / 2.0f, 0xffffff)
    }

    private fun drawFullSizeDirection(graphics: GuiGraphics, yaw: Float, angle: Float, text: String?) {
        val nDist = angleDistance(yaw, angle)
        if (abs(nDist) <= 90) {
            val nPos = xCenterPos + nDist * (compassBarLength / 180.0f)
            fillRect(graphics, nPos - 0.5f, yCompassBar, nPos + 0.5f, yCompassBar + compassBarWidth, 0xffffffff.toInt())
            if (text != null) {
                val mc = Minecraft.getInstance()
                drawXCenteredString(graphics, mc.font, text, nPos, yCompassBar - 9.0f, 0xFFFFFF)
            }
        }
    }

    private fun drawHalfSizeDirection(graphics: GuiGraphics, yaw: Float, angle: Float) {
        val nDist = angleDistance(yaw, angle)
        if (abs(nDist) <= 90) {
            val nPos = xCenterPos + nDist * (compassBarLength / 180.0f)
            fillRect(graphics, nPos - 0.5f, yCompassBar + compassBarWidth / 4.0f, nPos + 0.5f, yCompassBar + compassBarWidth / 4.0f * 3.0f, 0xffffffff.toInt())
        }
    }

    fun drawXCenteredString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        if (Minecraft.getInstance().options.backgroundForChatOnly().get())
            drawXCenteredShadowString(graphics, font, text, x, y, color)
        else drawXCenteredBoxedString(graphics, font, text, x, y, color)
    }

    fun drawXCenteredShadowString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        val width = font.width(text).toFloat()
        graphics.drawString(font, text, (x - width / 2), y, color, true)
    }

    fun drawXCenteredBoxedString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        val mc = Minecraft.getInstance()
        val width = font.width(text)
        val height = font.lineHeight
        val width1 = width + 4.0f
        val height1 = height + 3.0f
        val x0 = x - width1 / 2.0f

        val backgroundColor = (Mth.clamp(mc.options.textBackgroundOpacity().get() * ((color shr 24) and 0xFF), 0.0, 255.0).toInt()) shl 24
        fillRect(graphics, x0, y, x0 + width1, y + height1, backgroundColor)

        graphics.drawString(font, text, x - width / 2, y + 2, color, false)

        RenderSystem.enableBlend()
    }

    fun drawYCenteredString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        if (Minecraft.getInstance().options.backgroundForChatOnly().get())
            drawYCenteredShadowString(graphics, font, text, x, y, color)
        else drawYCenteredBoxedString(graphics, font, text, x, y, color)
    }

    fun drawYCenteredShadowString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        val height = font.lineHeight
        graphics.drawString(font, text, x, y - height / 2.0f, color, true)
    }

    fun drawYCenteredBoxedString(graphics: GuiGraphics, font: Font, text: String, x: Float, y: Float, color: Int) {
        val mc = Minecraft.getInstance()
        val width = font.width(text)
        val height = font.lineHeight
        val width1 = width + 4.0f
        val height1 = height + 3.0f
        val x0 = x - width1 / 2.0f

        val backgroundColor = (Mth.clamp(mc.options.textBackgroundOpacity().get() * ((color shr 24) and 0xFF), 0.0, 255.0).toInt()) shl 24
        fillRect(graphics, x0, y - height / 2.0f, x0 + width1, y - height / 2.0f + height1, backgroundColor)

        graphics.drawString(font, text, x, y - height / 2.0f + 2, color, false)

        RenderSystem.enableBlend()
    }

    private fun angleDistance(yaw: Float, other: Float): Float {
        val dist = other - yaw
        if (dist > 0) {
            return if (dist > 180) (dist - 360) else dist
        } else {
            return if (dist < -180) (dist + 360) else dist
        }
    }

    private fun fillRect(graphics: GuiGraphics, x0: Float, y0: Float, x1: Float, y1: Float, color: Int) {
        RenderSystem.enableBlend()

        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

        val a = (color shr 24 and 255)
        val r = (color shr 16 and 255)
        val g = (color shr 8 and 255)
        val b = (color and 255)
        val tess = Tesselator.getInstance()
        val builder = tess.builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        val matrix = graphics.pose().last().pose()
        builder.vertex(matrix, x0, y1, 0.0f).color(r, g, b, a).endVertex()
        builder.vertex(matrix, x1, y1, 0.0f).color(r, g, b, a).endVertex()
        builder.vertex(matrix, x1, y0, 0.0f).color(r, g, b, a).endVertex()
        builder.vertex(matrix, x0, y0, 0.0f).color(r, g, b, a).endVertex()
        tess.end()

        RenderSystem.defaultBlendFunc()
    }
}