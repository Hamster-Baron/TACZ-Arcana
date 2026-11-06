package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import group.taczexpands.client.override.BeamRendererOverride
import group.taczexpands.client.render.surface.HudRouting
import group.taczexpands.client.render.surface.SurfaceEngine
import group.taczexpands.client.render.surface.SurfaceRenderer
import group.taczexpands.common.network.v2.s2c.S2CAddHUD
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import org.joml.Vector4f
import java.util.Collections

object CustomHUDOverlay : IGuiOverlay {
    private val components = linkedMapOf<String, S2CAddHUD>()
    val hudMap: Map<String, S2CAddHUD> = Collections.unmodifiableMap(components)
    private val byFeed = mutableMapOf<String, MutableMap<String, S2CAddHUD>>()
    fun put(hud: S2CAddHUD) {
        remove(hud.identifier)
        components[hud.identifier] = hud
        index(hud)
    }
    private fun index(hud: S2CAddHUD) {
        val owner = HudRouting.owner(hud.identifier, SurfaceEngine.feedIds()) ?: return
        byFeed.getOrPut(owner) { linkedMapOf() }[hud.identifier] = hud
    }
    fun remove(identifier: String?) {
        if (identifier == null) {
            reset()
            return
        }
        components.remove(identifier)
        val owner = HudRouting.owner(identifier, SurfaceEngine.feedIds()) ?: return
        byFeed[owner]?.remove(identifier)
        if (byFeed[owner]?.isEmpty() == true) byFeed.remove(owner)
    }
    fun reindexFeeds() {
        byFeed.clear()
        components.values.forEach(::index)
    }
    fun forFeed(identifier: String): Collection<S2CAddHUD> = byFeed[identifier]?.values ?: emptyList()

    override fun render(gui: ForgeGui, guiGraphics: GuiGraphics, partialTick: Float, screenWidth: Int, screenHeight: Int) {
        var elements: Collection<S2CAddHUD> = hudMap.values
        if (elements.stream().anyMatch { it.identifier.startsWith("surface:") }) {
            elements = elements.stream().filter { !it.identifier.startsWith("surface:") }.toList()
        }
        renderElements(elements, guiGraphics, screenWidth, screenHeight)
    }

    fun renderElements(elements: Collection<S2CAddHUD>, guiGraphics: GuiGraphics, screenWidth: Int, screenHeight: Int) {
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.level == null) return
        val poseStack = guiGraphics.pose()
        for (hud in elements) {
            if (hud.feed != null && hud.feed == SurfaceRenderer.activeFeed()?.id()) continue
            val targetX: Float
            val targetY: Float
            if (hud.renderSpace == S2CAddHUD.RenderSpace.WORLD) {
                val cameraPos = mc.gameRenderer.mainCamera.position
                val rx = hud.x - cameraPos.x
                val ry = hud.y - cameraPos.y
                val rz = hud.z - cameraPos.z
                if (rx * rx + ry * ry + rz * rz > 16384) continue
                val screenPos = Vector4f(rx.toFloat(), ry.toFloat(), rz.toFloat(), 1f)
                    .mul(BeamRendererOverride.lastViewMatrix)
                    .mul(BeamRendererOverride.lastProjectionMatrix)
                if (screenPos.w <= 0f) continue
                targetX = (screenPos.x / screenPos.w + 1f) * 0.5f * screenWidth
                targetY = (1f - screenPos.y / screenPos.w) * 0.5f * screenHeight
            } else {
                targetX = hud.x.toFloat() + getAnchorX(hud.anchor, screenWidth)
                targetY = hud.y.toFloat() + getAnchorY(hud.anchor, screenHeight)
            }
            val pivot = hud.pivot ?: when (hud.alignment) {
                S2CAddHUD.Alignment.LEFT -> S2CAddHUD.Anchor.CENTER_LEFT
                S2CAddHUD.Alignment.CENTER -> S2CAddHUD.Anchor.CENTER
                S2CAddHUD.Alignment.RIGHT -> S2CAddHUD.Anchor.CENTER_RIGHT
            }
            poseStack.pushPose()
            try {
                poseStack.translate(targetX, targetY, 0f)
                poseStack.scale(hud.scale, hud.scale, 1f)
                val textureLocation = if (hud.feed != null) SurfaceEngine.hudTexture(hud.feed)
                    else hud.imagePath?.let { ResourceLocation.tryParse(it) }
                if (textureLocation != null) {
                    val imgWidth = hud.imageWidth
                    val imgHeight = hud.imageHeight
                    poseStack.pushPose()
                    try {
                        poseStack.translate(-getAnchorX(pivot, imgWidth), -getAnchorY(pivot, imgHeight), 0f)
                        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
                        RenderSystem.setShaderTexture(0, textureLocation)
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        if (hud.feed != null) {
                            // Framebuffers store their first row at the bottom.
                            guiGraphics.blit(textureLocation, 0, 0, imgWidth, imgHeight,
                                0f, imgHeight.toFloat(), imgWidth, -imgHeight, imgWidth, imgHeight)
                        } else guiGraphics.blit(textureLocation, 0, 0, 0f, 0f, imgWidth, imgHeight, imgWidth, imgHeight)
                    } finally {
                        poseStack.popPose()
                    }
                }
                if (hud.text != null) {
                    poseStack.pushPose()
                    try {
                        poseStack.translate(-getAnchorX(pivot, mc.font.width(hud.text)), -getAnchorY(pivot, 8), 0f)
                        guiGraphics.drawString(mc.font, hud.text, 0, 0, 0xFFFFFF, true)
                    } finally {
                        poseStack.popPose()
                    }
                }
            } finally {
                poseStack.popPose()
            }
        }
    }

    fun reset() {
        components.clear()
        byFeed.clear()
    }

    private fun getAnchorX(anchor: S2CAddHUD.Anchor, screenWidth: Int): Float = when (anchor) {
        S2CAddHUD.Anchor.TOP_LEFT, S2CAddHUD.Anchor.CENTER_LEFT, S2CAddHUD.Anchor.BOTTOM_LEFT -> 0f
        S2CAddHUD.Anchor.TOP_CENTER, S2CAddHUD.Anchor.CENTER, S2CAddHUD.Anchor.BOTTOM_CENTER -> screenWidth / 2f
        S2CAddHUD.Anchor.TOP_RIGHT, S2CAddHUD.Anchor.CENTER_RIGHT, S2CAddHUD.Anchor.BOTTOM_RIGHT -> screenWidth.toFloat()
    }

    private fun getAnchorY(anchor: S2CAddHUD.Anchor, screenHeight: Int): Float = when (anchor) {
        S2CAddHUD.Anchor.TOP_LEFT, S2CAddHUD.Anchor.TOP_CENTER, S2CAddHUD.Anchor.TOP_RIGHT -> 0f
        S2CAddHUD.Anchor.CENTER_LEFT, S2CAddHUD.Anchor.CENTER, S2CAddHUD.Anchor.CENTER_RIGHT -> screenHeight / 2f
        S2CAddHUD.Anchor.BOTTOM_LEFT, S2CAddHUD.Anchor.BOTTOM_CENTER, S2CAddHUD.Anchor.BOTTOM_RIGHT -> screenHeight.toFloat()
    }

}
