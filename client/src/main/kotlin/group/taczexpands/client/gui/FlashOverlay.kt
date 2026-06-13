package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import kotlin.math.max
import kotlin.math.min

object FlashOverlay : IGuiOverlay {
    private var isActive = false
    private var startTime = 0L
    private var durationMillis = 0L
    private var fadeInMillis = 0L
    private var fadeOutMillis = 0L
    private var strength = 100


    fun startEffect(duration: Long, fadeIn: Long, fadeOut: Long, strength: Int) {
        val currentTime = System.currentTimeMillis()

        if (isActive) {
            val elapsedTime = currentTime - startTime
            if (elapsedTime < fadeInMillis) {
                val currentAlphaProgress = elapsedTime.toFloat() / fadeInMillis
                this.startTime = currentTime - (fadeIn * currentAlphaProgress).toLong()
            } else {
                this.startTime = currentTime - fadeIn
            }
        } else {
            this.startTime = currentTime
            this.isActive = true
        }

        this.durationMillis = duration + fadeIn + fadeOut
        this.fadeInMillis = fadeIn
        this.fadeOutMillis = fadeOut
        this.strength = strength.coerceIn(1, 100)
    }

    override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        if (!isActive) return

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - startTime

        if (elapsedTime > durationMillis) {
            isActive = false
            return
        }

        val alpha = when {
            elapsedTime < fadeInMillis -> {
                elapsedTime.toFloat() / fadeInMillis
            }
            elapsedTime > durationMillis - fadeOutMillis -> {
                val remainingTime = durationMillis - elapsedTime
                remainingTime.toFloat() / fadeOutMillis
            }
            else -> 1.0f
        } * (strength / 100.0f)

        val clampedAlpha = max(0.0f, min(1.0f, alpha))

        val alphaInt = (clampedAlpha * 255).toInt()

        val color = (alphaInt shl 24) or 0xFFFFFF

        val mc = Minecraft.getInstance()
        val screenWidth = mc.window.guiScaledWidth
        val screenHeight = mc.window.guiScaledHeight

        RenderSystem.disableDepthTest()
        guiGraphics.fill(0, 0, screenWidth, screenHeight, color)
        RenderSystem.enableDepthTest()
    }

    fun reset() {
        isActive = false
    }
}