package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

object FrostbiteOverlay : IGuiOverlay {
    val POWDER_SNOW_OUTLINE_LOCATION: ResourceLocation = ResourceLocation("textures/misc/powder_snow_outline.png")

    var time = 0
    fun onClientTick() {
        if (time > 0) {
            time--
        }
    }

    override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        val player = Minecraft.getInstance().player ?: return
        if (player.ticksFrozen > 0) return
        if (time <= 0) return
        renderTextureOverlay(guiGraphics, POWDER_SNOW_OUTLINE_LOCATION, 1.0f, screenWidth, screenHeight)
    }

    fun renderTextureOverlay(p_282304_: GuiGraphics, p_281622_: ResourceLocation, p_281504_: Float, screenWidth: Int, screenHeight: Int) {
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        p_282304_.setColor(1.0f, 1.0f, 1.0f, p_281504_)
        p_282304_.blit(p_281622_, 0, 0, -90, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight)
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        p_282304_.setColor(1.0f, 1.0f, 1.0f, 1.0f)
    }
}