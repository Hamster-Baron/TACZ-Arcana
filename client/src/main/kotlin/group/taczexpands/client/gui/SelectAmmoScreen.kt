package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IAmmo
import com.tacz.guns.api.item.IAmmoBox
import com.tacz.guns.api.item.builder.AmmoItemBuilder
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.index.CommonAmmoIndex
import com.tacz.guns.resource.pojo.data.gun.Bolt
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.network.c2s.C2SSwitchAmmo
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import kotlin.jvm.optionals.getOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class SelectAmmoScreen(ammo: List<ResourceLocation>) : Screen(Component.literal("Ammo Selection")) {
    private val items: MutableList<Pair<ResourceLocation, CommonAmmoIndex>> = ArrayList()
    private val radius = 80

    private var selectedIndex = -1

    init {
        ammo.forEach {
            TimelessAPI.getCommonAmmoIndex(it).ifPresent { index ->
                items.add(it to index)
            }
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(graphics, mouseX, mouseY, partialTicks)
        selectedIndex = -1

        val centerX = width / 2
        val centerY = height / 2

        val segmentsPerSlot = 20
        val innerRadius = (radius - 32).toFloat()
        val outerRadius = (radius + 32).toFloat()

        val currentAmmoId = getCurrentAmmoId()
        val currentAmmoAmount = getCurrentAmmoAmount()
        for (i in items.indices) {
            val angleStep = 2 * Math.PI / items.size
            val startAngle = angleStep * i - Math.PI / 2 - angleStep / 2
            val endAngle = startAngle + angleStep

            val isSelected = isMouseOverSlot(
                mouseX, mouseY, centerX, centerY,
                innerRadius, outerRadius, startAngle, endAngle
            )
            if (isSelected) {
                selectedIndex = i
            }

            val color = if (isSelected) -0x3f000001 else 0x40FFFFFF

            drawSector(
                graphics.pose(),
                centerX,
                centerY,
                innerRadius,
                outerRadius,
                startAngle,
                endAngle,
                segmentsPerSlot,
                color
            )

            val itemAngle = (startAngle + endAngle) / 2
            val x = (centerX + radius * cos(itemAngle)).toInt()
            val y = (centerY + radius * sin(itemAngle)).toInt()

            val renderAmmoId = items[i].first
            val ammoItem = AmmoItemBuilder.create().setId(renderAmmoId).build()

            val itemScale = 2.0f
            val centerX = x
            val centerY = y

            val scaledSize = 16f * itemScale
            val offsetX = centerX - scaledSize / 2
            val offsetY = centerY - scaledSize / 2
            val poseStack = graphics.pose()

            poseStack.pushPose()
            poseStack.translate(centerX.toDouble(), centerY.toDouble(), 0.0)
            poseStack.scale(itemScale, itemScale, itemScale)


            graphics.renderItem(ammoItem, -8, -8)
            poseStack.popPose()

            val infiniteAmmo = minecraft?.player?.isCreative ?: false
            val ammoAmount = if (infiniteAmmo) {
                9999
            } else if (currentAmmoId != null && currentAmmoId == renderAmmoId) {
                currentAmmoAmount + getInventoryAmmoAmount(renderAmmoId)
            } else {
                getInventoryAmmoAmount(renderAmmoId)
            }


            val renderText = "$ammoAmount"
            val textWidth = font.width(renderText)
            val textHeight = font.lineHeight

            val scale = 1.0f


            poseStack.pushPose()
            poseStack.translate(x.toDouble(), y + 12.0 + 8, 0.0)
            poseStack.scale(scale, scale, 1.0f)
            graphics.drawString(
                font,
                renderText,
                -textWidth / 2,
                -textHeight / 2,
                (if (ammoAmount <= 0) 0xffff0000 else 0xffffffff).toInt(),
                true
            )
            poseStack.popPose()

            if (isSelected) {
                val name = ammoItem.hoverName.string.replace(Regex("§[0-9a-fk-or]"), "")
                val nameWidth = font.width(name)
                graphics.drawString(font, name, width / 2 - nameWidth / 2, height - 48 - 8, (if (ammoAmount <= 0) 0xffff0000 else 0xffffffff).toInt(), true)
            }

        }

    }

    fun getCurrentAmmoId(): ResourceLocation? {
        val player = minecraft?.player ?: return null
        val mainHand = player.mainHandItem
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (mainHand.item != gunItem) return null
        val index = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return null
        return IAccessorGunData.getCurrentAmmoId(index.gunData, mainHand)
    }

    fun getCurrentAmmoAmount(): Int {
        val player = minecraft?.player ?: return 0
        val mainHand = player.mainHandItem
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (mainHand.item != gunItem) return 0
        val index = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return 0

        return gunItem.getCurrentAmmoCount(mainHand) + if (gunItem.hasBulletInBarrel(mainHand) && index.gunData.bolt != Bolt.OPEN_BOLT) 1 else 0
    }

    private fun getInventoryAmmoAmount(ammoId: ResourceLocation): Int {
        val player = minecraft?.player ?: return 0
        val inventory = player.inventory
        var amount = 0
        for (i in 0..<inventory.containerSize) {
            val inventoryItem = inventory.getItem(i)
            val iAmmo = inventoryItem.item
            if (iAmmo is IAmmo && iAmmo.getAmmoId(inventoryItem) == ammoId) {
                amount += inventoryItem.count
            }
            val iAmmoBox = inventoryItem.item
            if (iAmmoBox is IAmmoBox) {
                if (iAmmoBox.isAllTypeCreative(inventoryItem)) {
                    amount = 9999
                    return amount
                }

                if (iAmmoBox.getAmmoId(inventoryItem) == ammoId) {
                    if (iAmmoBox.isCreative(inventoryItem)) {
                        amount = 9999
                        return amount
                    }
                }
                amount += iAmmoBox.getAmmoCount(inventoryItem)
            }
        }
        return amount
    }


    private fun isMouseOverSlot(
        mouseX: Int, mouseY: Int, centerX: Int, centerY: Int,
        innerR: Float, outerR: Float,
        startAngle: Double, endAngle: Double
    ): Boolean {
        var startAngle = startAngle
        var endAngle = endAngle
        val dx = (mouseX - centerX).toDouble()
        val dy = (mouseY - centerY).toDouble()

        val distSq = dx * dx + dy * dy
        val dist = sqrt(distSq)

        if (dist < innerR || dist > outerR) {
            return false
        }

        var angle = atan2(dy, dx)
        if (angle < -Math.PI / 2) angle += 2 * Math.PI

        if (startAngle < -Math.PI / 2) startAngle += 2 * Math.PI
        if (endAngle < -Math.PI / 2) endAngle += 2 * Math.PI

        return if (endAngle < startAngle) {
            angle >= startAngle || angle <= endAngle
        } else {
            angle >= startAngle && angle <= endAngle
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            doClose()
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    fun doClose() {
        if (selectedIndex >= 0)
            NetworkManager.sendToServer(C2SSwitchAmmo(items[selectedIndex].first))

        onClose()
    }

    private fun drawSector(
        pose: PoseStack, centerX: Int, centerY: Int,
        innerR: Float, outerR: Float,
        startRad: Double, endRad: Double,
        segments: Int, color: Int
    ) {
        val tesselator = Tesselator.getInstance()
        val buffer = tesselator.builder

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { GameRenderer.getPositionColorShader() }

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)

        for (i in 0..segments) {
            val angle = startRad + (endRad - startRad) * i / segments
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()

            val x1 = centerX + outerR * cos
            val y1 = centerY + outerR * sin
            val x2 = centerX + innerR * cos
            val y2 = centerY + innerR * sin

            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = (color) and 0xFF

            buffer.vertex(pose.last().pose(), x1, y1, 0f).color(r, g, b, a).endVertex()
            buffer.vertex(pose.last().pose(), x2, y2, 0f).color(r, g, b, a).endVertex()
        }

        tesselator.end()

        RenderSystem.disableBlend()
    }
}