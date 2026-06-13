package group.taczexpands.client.gui

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IAmmo
import com.tacz.guns.api.item.IGun
import com.tacz.guns.client.gui.GunSmithTableScreen
import com.tacz.guns.util.RenderDistance
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.accessor.IAccessorPreview
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import kotlin.jvm.optionals.getOrNull


object ItemPreviewManager {
    val PREVIEW_BACK_TEXTURE = ResourceLocation(TACZExpandsCommon.MODID, "textures/gui/preview_back.png")
    val PREVIEW_FRONT_TEXTURE = ResourceLocation(TACZExpandsCommon.MODID, "textures/gui/preview_front.png")

    @SubscribeEvent
    fun onScreenRenderPost(event: ScreenEvent.Render.Post) {
        val screen = event.screen
        if (screen is AbstractContainerScreen<*>) {
            val slot = screen.slotUnderMouse ?: return
            val itemStack = slot.item

            var preview: IAccessorPreview? = null
            val iGun = IGun.getIGunOrNull(itemStack)
            if (iGun != null) {
                preview = TimelessAPI.getCommonGunIndex(iGun.getGunId(itemStack)).getOrNull()?.pojo as? IAccessorPreview
            }

            if (preview == null) {
                val iAmmo = IAmmo.getIAmmoOrNull(itemStack)
                if (iAmmo != null) {
                    preview = TimelessAPI.getCommonAmmoIndex(iAmmo.getAmmoId(itemStack)).getOrNull()?.pojo as? IAccessorPreview
                }
            }

            if (preview == null || !preview.`taczexpands$shouldPreview`()) return


            doRender(screen, event, itemStack, preview)

        }
    }

    fun doRender(screen: AbstractContainerScreen<*>, event: ScreenEvent.Render.Post, itemStack: ItemStack, preview: IAccessorPreview) {
        val panelWidth = 134
        val panelHeight = 210

        val x = screen.guiLeft - panelWidth - 2
        val y = screen.guiTop - 50
        val guiGraphics = event.guiGraphics
        val poseStack = guiGraphics.pose()
        val mc = Minecraft.getInstance()
        val font = mc.font

        poseStack.pushPose()
        poseStack.translate(0.0f, 0.0f, 1000.0f)

        guiGraphics.blit(PREVIEW_BACK_TEXTURE, x, y, 0, 0, panelWidth, panelHeight)

        renderLeftModel(poseStack, itemStack, x, y, preview.`taczexpands$getPreviewScale`(), preview.`taczexpands$getPreviewOffset`())

        poseStack.pushPose()
        poseStack.translate(0.0f, 0.0f, 1000.0f)
        guiGraphics.blit(PREVIEW_FRONT_TEXTURE, x, y, 0, 0, panelWidth, panelHeight)
        GunSmithTableScreen.drawModCenteredString(guiGraphics,
            font,
            Component.translatable("gui.tacz.gun_smith_table.preview"),
            x + 108,
            y + 5,
            0x555555)

        val maxPhysicalWidth = 122

        val titleScale = 0.75f
        val previewTitle = getHardLines(preview.`taczexpands$getPreviewTitle`(), titleScale, maxPhysicalWidth, font)

        val detailScale = 0.5f
        val previewDetail = getHardLines(preview.`taczexpands$getPreviewDetail`(), detailScale, maxPhysicalWidth, font)

        val bufferSource = mc.renderBuffers().bufferSource()

        val startX = x + 6.0f
        val startY = y + 72.0f

        val lineSpacing = 2.0f


        poseStack.pushPose()

        poseStack.translate(startX.toDouble(), startY.toDouble(), 0.0)
        poseStack.scale(titleScale, titleScale, 1.0f)

        var titleTotalHeight = 0.0f
        for (line in previewTitle) {
            font.drawInBatch(
                line, 0.0f, titleTotalHeight, -0x1,
                false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 15728880
            )
            titleTotalHeight += font.lineHeight
        }
        bufferSource.endBatch()
        poseStack.popPose()


        val detailPhysicalY = startY + (titleTotalHeight * titleScale) + lineSpacing

        poseStack.pushPose()

        poseStack.translate(startX.toDouble(), detailPhysicalY.toDouble(), 0.0)
        poseStack.scale(detailScale, detailScale, 1.0f)

        var detailTotalHeight = 0.0f
        for (line in previewDetail) {
            font.drawInBatch(
                line, 0.0f, detailTotalHeight, -0x1,
                false, poseStack.last().pose(), bufferSource,
                Font.DisplayMode.NORMAL, 0, 15728880
            )
            detailTotalHeight += font.lineHeight
        }
        bufferSource.endBatch()
        poseStack.popPose()

        poseStack.popPose()
        poseStack.popPose()
    }

    fun getHardLines(rawText: String?, scale: Float, maxWidth: Int, font: Font): List<FormattedCharSequence> {
        val adjustedWidth = (maxWidth / scale).toInt()
        if (rawText == null) return font.split(Component.translatable("gui.tacz.gun_smith_table.error"), adjustedWidth)

        val component = try {
            Component.Serializer.fromJson(rawText)
        } catch (e: Exception) {
            null
        }

        if (component != null) {
            return font.split(component, adjustedWidth)
        }


        return font.split(Component.translatable(rawText), adjustedWidth)
    }


    private fun renderLeftModel(poseStack: PoseStack, stack: ItemStack, leftPos: Int, topPos: Int, scale: Float, offset: FloatArray) {
        RenderDistance.markGuiRenderTimestamp()

        val rotationPeriod = 8f
        val xPos: Int = leftPos + 60
        val yPos: Int = topPos + 27
        val startX: Int = leftPos + 3
        val startY: Int = topPos + 3
        val width = 128
        val height = 63
        val rotPitch = 15f

        val window = Minecraft.getInstance().window
        val windowGuiScale = window.guiScale
        val scissorX = (startX * windowGuiScale).toInt()
        val scissorY = (window.height - ((startY + height) * windowGuiScale)).toInt()
        val scissorW = (width * windowGuiScale).toInt()
        val scissorH = (height * windowGuiScale).toInt()
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH)

        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false)
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS)
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        poseStack.pushPose()
        poseStack.translate(xPos.toFloat(), yPos.toFloat(), 100.0f)
        poseStack.translate(8.0, 8.0, 0.0)
        poseStack.translate(offset[0], offset[1], offset[2])
        poseStack.scale(1.0f, -1.0f, 1.0f)
        poseStack.scale(scale, scale, scale)
        val rot = (System.currentTimeMillis() % (rotationPeriod * 1000).toInt()) * (360f / (rotationPeriod * 1000))
        poseStack.mulPose(Axis.XP.rotationDegrees(rotPitch))
        poseStack.mulPose(Axis.YP.rotationDegrees(rot))
        RenderSystem.applyModelViewMatrix()

        val bufferSource = Minecraft.getInstance().renderBuffers().bufferSource()
        Lighting.setupForFlatItems()

        Minecraft.getInstance()
            .getItemRenderer()
            .renderStatic(stack, ItemDisplayContext.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0)

        bufferSource.endBatch()
        RenderSystem.enableDepthTest()
        Lighting.setupFor3DItems()
        poseStack.popPose()
        RenderSystem.applyModelViewMatrix()

        RenderSystem.disableScissor()
    }
}