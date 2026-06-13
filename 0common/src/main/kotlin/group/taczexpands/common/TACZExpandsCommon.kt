package group.taczexpands.common

import com.mojang.logging.LogUtils
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.builder.AttachmentItemBuilder
import com.tacz.guns.api.item.builder.GunItemBuilder
import com.tacz.guns.init.ModCreativeTabs
import com.tacz.guns.resource.index.CommonAttachmentIndex
import com.tacz.guns.resource.index.CommonGunIndex
import group.taczexpands.common.accessor.IAccessorMiscPOJO
import group.taczexpands.common.entity.CustomDisplayEntity
import group.taczexpands.common.event.CommonListener
import net.minecraft.client.Minecraft
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityAttributeCreationEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.function.Supplier
import java.util.function.ToIntFunction


@Mod(TACZExpandsCommon.MODID)
class TACZExpandsCommon {
    companion object {
        const val MODID = "taczexpands"
        val LOGGER = LogUtils.getLogger()
        val ENTITYTYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID)
        val ENTITYTYPE_CUSTOM_DISPLAY = ENTITYTYPES.register("custom_display") {
            EntityType.Builder.of<CustomDisplayEntity>(
                ::CustomDisplayEntity,
                MobCategory.MISC
            ).sized(1.0f, 1.0f).updateInterval(1).build("custom_display")
        }

        val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID)
        val MISC_TAB_ITEM = ITEMS.register("misc_tab") {
            Item(Item.Properties())
        }

        val MODULE_TAB_ITEM = ITEMS.register("module_tab") {
            Item(Item.Properties())
        }


        var TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)

        val MODULE_TAB: RegistryObject<CreativeModeTab> = TABS.register<CreativeModeTab>("module", Supplier {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.tab.taczexpands.module"))
                .withTabsBefore(ModCreativeTabs.ATTACHMENT_LASER_TAB.id)
                .icon { ItemStack(MODULE_TAB_ITEM.get()) }
                .displayItems { parameters, output ->
                    output.acceptAll(generateModuleItems())
                }
                .build()
        })

        val MISC_TAB: RegistryObject<CreativeModeTab> = TABS.register<CreativeModeTab>("misc", Supplier {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.tab.taczexpands.misc"))
                .withTabsBefore(ModCreativeTabs.GUN_MG_TAB.id)
                .icon { ItemStack(MISC_TAB_ITEM.get()) }
                .displayItems { parameters, output ->
                    output.acceptAll(generateMiscItems())
                }
                .build()
        })

        private fun generateModuleItems(): NonNullList<ItemStack> {
            val stacks = NonNullList.create<ItemStack>()

            TimelessAPI.getAllCommonAttachmentIndex()
                .stream()
                .sorted(Comparator.comparingInt(ToIntFunction { m: MutableMap.MutableEntry<ResourceLocation, CommonAttachmentIndex> -> m.value.sort }))
                .forEach { entry ->
                    if (entry!!.value!!.pojo.isHidden) {
                        return@forEach
                    }
                    if (IAccessorMiscPOJO.isMisc(entry.value!!.pojo)) {
                        return@forEach
                    }

                    if(entry.value!!.pojo.type != AttachmentType.valueOf("MODULE")) {
                        return@forEach
                    }

                    val itemStack = AttachmentItemBuilder.create().setId(entry.key).setCount(1).build()
                    stacks.add(itemStack)
                }
            return stacks
        }


        private fun generateMiscItems(): NonNullList<ItemStack> {
            val stacks = NonNullList.create<ItemStack>()
            TimelessAPI.getAllCommonGunIndex()
                .stream()
                .sorted(Comparator.comparingInt(ToIntFunction { m: MutableMap.MutableEntry<ResourceLocation, CommonGunIndex> -> m.value.sort }))
                .forEach { entry ->
                    val index: CommonGunIndex = entry.value
                    val gunData = index.gunData
                    if (IAccessorMiscPOJO.isHidden(index.pojo)) {
                        return@forEach
                    }
                    if (!IAccessorMiscPOJO.isMisc(index.pojo)) {
                        return@forEach
                    }
                    val itemStack = GunItemBuilder.create()
                        .setId(entry.key)
                        .setFireMode(gunData.getFireModeSet().get(0))
                        .setAmmoCount(gunData.getAmmoAmount())
                        .setHeatData(gunData.hasHeatData())
                        .setAmmoInBarrel(true)
                        .setCount(1)
                        .build()
                    stacks.add(itemStack)
                }

            TimelessAPI.getAllCommonAttachmentIndex()
                .stream()
                .sorted(Comparator.comparingInt(ToIntFunction { m: MutableMap.MutableEntry<ResourceLocation, CommonAttachmentIndex> -> m.value.sort }))
                .forEach { entry ->
                    if (entry!!.value!!.pojo.isHidden) {
                        return@forEach
                    }
                    if (!IAccessorMiscPOJO.isMisc(entry.value!!.pojo)) {
                        return@forEach
                    }

                    val itemStack = AttachmentItemBuilder.create().setId(entry.key).setCount(1).build()
                    stacks.add(itemStack)
                }
            return stacks
        }
    }

    fun onAttributeCreate(event: EntityAttributeCreationEvent) {
        event.put(
            ENTITYTYPE_CUSTOM_DISPLAY.get(), LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .build()
        )
    }

    init {
        ENTITYTYPES.register(MOD_BUS)
        ITEMS.register(MOD_BUS)
        TABS.register(MOD_BUS)
        MOD_BUS.addListener(::onAttributeCreate)
        MinecraftForge.EVENT_BUS.register(CommonListener)

        try {
            val clazz = Class.forName("group.taczexpands.server.TACZExpandsServer")
            clazz.getDeclaredConstructor().newInstance()
            LOGGER.info("Server init.")
        } catch (e: ClassNotFoundException) {
        }

        try {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                Runnable {
                    try {
                        val clazz = Class.forName("group.taczexpands.client.TACZExpandsClient")
                        clazz.getDeclaredConstructor().newInstance()
                        LOGGER.info("Client init.")
                    } catch (e: ClassNotFoundException) {
                    }
                }
            }
        } catch (e: Throwable) {
        }

        try {
            val clazz = Class.forName("group.taczexpands.hybrid.TACZExpandsHybrid")
            clazz.getDeclaredConstructor().newInstance()
            LOGGER.info("Hybrid init.")
        } catch (e: ClassNotFoundException) {
        }
    }
}
