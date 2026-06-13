package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.tacz.guns.api.item.IAmmo
import com.tacz.guns.api.item.IAttachment
import com.tacz.guns.api.item.IGun
import com.tacz.guns.client.resource.ClientAssetsManager
import com.tacz.guns.client.resource.pojo.PackInfo
import com.tacz.guns.init.ModCreativeTabs
import group.taczexpands.client.accessor.IAccessorClientAssetsManager
import group.taczexpands.client.mixin.accessor.IAccessorCreativeModeInventoryScreen
import group.taczexpands.common.TACZExpandsCommon
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.joml.Matrix4f
import kotlin.random.Random


object CreativeTabManager {
    val tabs by lazy {
        listOf(
            ModCreativeTabs.AMMO_TAB,
            ModCreativeTabs.ATTACHMENT_SCOPE_TAB,
            ModCreativeTabs.ATTACHMENT_MUZZLE_TAB,
            ModCreativeTabs.ATTACHMENT_STOCK_TAB,
            ModCreativeTabs.ATTACHMENT_GRIP_TAB,
            ModCreativeTabs.ATTACHMENT_EXTENDED_MAG_TAB,
            ModCreativeTabs.ATTACHMENT_LASER_TAB,
            TACZExpandsCommon.MODULE_TAB,
            ModCreativeTabs.GUN_PISTOL_TAB,
            ModCreativeTabs.GUN_SNIPER_TAB,
            ModCreativeTabs.GUN_RIFLE_TAB,
            ModCreativeTabs.GUN_SHOTGUN_TAB,
            ModCreativeTabs.GUN_SMG_TAB,
            ModCreativeTabs.GUN_RPG_TAB,
            ModCreativeTabs.GUN_MG_TAB,
            TACZExpandsCommon.MISC_TAB
        ).map { it.get() }
    }


    private val ICONS: ResourceLocation = ResourceLocation(TACZExpandsCommon.MODID, "textures/gui/icons.png")
    private var startIndex: Int = 0

    private var filters: MutableList<Filter> = mutableListOf()
    private var buttons: MutableList<FilterButton> = mutableListOf()
    private var btnScrollUp: Button? = null
    private var btnScrollDown: Button? = null
    private var guiLeft = 0
    private var guiTop = 0

    private var currentTab: CreativeModeTab = CreativeModeTabs.getDefaultTab()

    var rendering = false

    private val filtersStatus: MutableMap<String, Boolean> = mutableMapOf()

    fun onSelectTab(tab: CreativeModeTab, screen: CreativeModeInventoryScreen) {
        onSwitchCreativeTab(tab, screen)
    }

    @SubscribeEvent
    fun onPlayerLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        filters.clear()
        rendering = false
    }

    @SubscribeEvent
    fun onScreenInitPost(event: ScreenEvent.Init.Post) {
        val screen = event.screen
        if (screen is CreativeModeInventoryScreen) {
            this.createFilters()

            this.guiLeft = screen.guiLeft
            this.guiTop = screen.guiTop
            this.buttons.clear()

            this.buttons.addAll(this.createFilterButtons())
            this.buttons.forEach { event.addListener(it) }

            event.addListener(IconButton(this.guiLeft - 22, this.guiTop - 12, { button ->
                if (startIndex > 0) startIndex--
                this.updateFilterButtons()
            }, ICONS, 0, 0).also { this.btnScrollUp = it })

            event.addListener(IconButton(this.guiLeft - 22, this.guiTop + 127, { button ->
                if (startIndex <= filters.size - 4 - 1) startIndex++
                this.updateFilterButtons()
            }, ICONS, 16, 0).also { this.btnScrollDown = it })

            this.onSwitchCreativeTab(IAccessorCreativeModeInventoryScreen.getSelectedTab(), screen)
        } else {
            rendering = false
        }
    }

    @SubscribeEvent
    fun onScreenRenderPre(event: ScreenEvent.Render.Pre) {
        val screen = event.screen
        if (screen is CreativeModeInventoryScreen) {
            this.guiLeft = screen.guiLeft
            this.guiTop = screen.guiTop

        } else {
            rendering = false
        }
    }

    private fun onSwitchCreativeTab(tab: CreativeModeTab, screen: CreativeModeInventoryScreen) {
        currentTab = tab
        if (tabs.contains(tab)) {
            rendering = true
            this.btnScrollUp?.visible = true
            this.btnScrollDown?.visible = true

            startIndex = 0
            filters.forEach { it.enabled = true }
            buttons.forEach { it.sync() }

            this.updateFilterButtons()
            this.updateItems(screen)
        } else {
            rendering = false
            this.btnScrollUp?.visible = false
            this.btnScrollDown?.visible = false
            this.buttons.forEach { it.visible = false }
        }
    }

    private fun createFilterButtons(): List<FilterButton> {
        return filters.map {
            FilterButton(this.guiLeft - 28, this.guiTop, it) {
                val screen = Minecraft.getInstance().screen
                if (screen is CreativeModeInventoryScreen) {
                    updateItems(screen)
                }
            }.also { it.visible = false }
        }
    }

    private fun updateFilterButtons() {
        this.buttons.forEach { it.visible = false }

        val validButtons = buttons.filter {
            it.filter.getItems(currentTab.displayItems).size > 0
        }

        var i = startIndex
        while (i < startIndex + 4 && i < validButtons.size) {
            val button = validButtons[i]
            button.y = this.guiTop + 29 * (i - startIndex) + 11
            button.visible = true
            button.enabled = filtersStatus.getOrDefault(button.filter.namespace, true)
            button.filter.enabled = button.enabled
            i++
        }
        this.btnScrollUp?.active = startIndex > 0
        this.btnScrollDown?.active = startIndex <= this.filters.size - 4 - 1
    }

    private fun updateItems(screen: CreativeModeInventoryScreen) {
        val menu = screen.getMenu()
        menu.items.clear()
        for (filter in this.filters) {
            if (filter.enabled) {
                menu.items.addAll(filter.getItems(currentTab.displayItems))
            }
        }
        menu.items.sortWith(Comparator.comparingInt { o -> Item.getId(o.item) })
        menu.scrollTo(0f)
    }

    private fun createFilters() {
        filters.clear()

        val allPacks = (ClientAssetsManager.INSTANCE as IAccessorClientAssetsManager).`taczexpands$getAllPackInfo`()
        this.filters.addAll(allPacks.map { Filter(it.key, it.value) })
    }

    class Filter(val namespace: String, val packInfo: PackInfo) {
        val name: Component = Component.translatable(packInfo.name)
        val iconTexture = ResourceLocation(namespace, "textures/pack_icon.png").let {
            if (Minecraft.getInstance().resourceManager.getResource(it).isPresent)
                it else null
        }
        var enabled: Boolean = true

        var iconItems: List<ItemStack> = listOf()

        fun getItems(input: Collection<ItemStack>): List<ItemStack> {
            val result = input.filter {
                val iGun = IGun.getIGunOrNull(it)
                if (iGun != null) {
                    return@filter iGun.getGunId(it).namespace == namespace
                }

                val iAttachment = IAttachment.getIAttachmentOrNull(it)
                if (iAttachment != null)
                    return@filter iAttachment.getAttachmentId(it).namespace == namespace

                val iAmmo = IAmmo.getIAmmoOrNull(it)
                if (iAmmo != null)
                    return@filter iAmmo.getAmmoId(it).namespace == namespace

                val holder = it.itemHolder
                if (holder !is Holder.Reference<Item>) return@filter false
                return@filter holder.key().location().namespace == namespace
            }
            iconItems = result
            return result
        }
    }

    class FilterButton(x: Int, y: Int, val filter: Filter, onPress: OnPress) : Button(x, y, 32, 26, Component.literal(""), onPress, DEFAULT_NARRATION) {
        companion object {
            private val CREATIVE_TABS_LOCATION = ResourceLocation("textures/gui/container/creative_inventory/tabs.png")
        }

        var enabled = filter.enabled

        init {
            tooltip = Tooltip.create(filter.name)
        }

        fun sync() {
            enabled = filter.enabled
        }

        override fun onPress() {
            enabled = !enabled
            filter.enabled = enabled
            filtersStatus[filter.namespace] = enabled
            super.onPress()
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            val width = if (enabled) 32 else 28
            val textureX = 26
            val textureY = if (enabled) 32 else 0
            RenderSystem.setShaderTexture(0, CREATIVE_TABS_LOCATION)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, this.alpha)
            this.drawRotatedTexture(pGuiGraphics.pose().last().pose(), this.x, this.y, textureX, textureY, width, 26)
            val iconTexture = filter.iconTexture
            if (iconTexture != null) {
                pGuiGraphics.blit(iconTexture, this.x + 8, this.y + 5, 0, 0, 16, 16)
            } else {
                val iconList = filter.iconItems
                if (iconList.isNotEmpty()) {
                    val icon = iconList[Random(System.currentTimeMillis() / 1000L).nextInt(iconList.size)]
                    pGuiGraphics.renderItem(icon, this.x + 8, this.y + 5)
                }
            }
        }


        private fun drawRotatedTexture(matrix4f: Matrix4f, x: Int, y: Int, textureX: Int, textureY: Int, width: Int, height: Int) {
            val scaleX = 0.00390625f
            val scaleY = 0.00390625f
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            val builder = Tesselator.getInstance().builder
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            builder.vertex(matrix4f, x.toFloat(), (y + height).toFloat(), 0.0f)
                .uv(((textureX + height).toFloat() * scaleX), ((textureY).toFloat() * scaleY))
                .endVertex()
            builder.vertex(matrix4f, (x + width).toFloat(), (y + height).toFloat(), 0.0f)
                .uv(((textureX + height).toFloat() * scaleX), ((textureY + width).toFloat() * scaleY))
                .endVertex()
            builder.vertex(matrix4f, (x + width).toFloat(), y.toFloat(), 0.0f)
                .uv(((textureX).toFloat() * scaleX), ((textureY + width).toFloat() * scaleY))
                .endVertex()
            builder.vertex(matrix4f, x.toFloat(), y.toFloat(), 0.0f).uv(((textureX).toFloat() * scaleX), ((textureY).toFloat() * scaleY)).endVertex()
            BufferUploader.drawWithShader(builder.end())
        }
    }

    class IconButton(x: Int, y: Int, onPress: OnPress, val icon: ResourceLocation, val iconU: Int, val iconV: Int) : Button(
        x,
        y,
        20,
        20,
        Component.literal(""),
        onPress,
        DEFAULT_NARRATION
    ) {

        override fun createTooltipPositioner(): ClientTooltipPositioner {
            return DefaultTooltipPositioner.INSTANCE
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
            var prevShaderColor: FloatArray? = null
            if (!active) {
                prevShaderColor = RenderSystem.getShaderColor().copyOf()
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1f)
            }
            pGuiGraphics.blit(icon, x + 2, y + 2, iconU, iconV, 16, 16)
            if (prevShaderColor != null) {
                RenderSystem.setShaderColor(prevShaderColor[0], prevShaderColor[1], prevShaderColor[2], prevShaderColor[3])
            }
        }

    }
}