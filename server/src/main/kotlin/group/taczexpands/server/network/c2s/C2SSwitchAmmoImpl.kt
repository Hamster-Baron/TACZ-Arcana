package group.taczexpands.server.network.c2s

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.api.item.IAmmo
import com.tacz.guns.api.item.IAmmoBox
import com.tacz.guns.api.item.builder.AmmoItemBuilder
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import com.tacz.guns.resource.pojo.data.gun.Bolt
import com.tacz.guns.util.AttachmentDataUtils
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.SwitchAmmoCondition
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.c2s.C2SSwitchAmmo
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.config.ServerConfig
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.items.ItemHandlerHelper
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object C2SSwitchAmmoImpl {

    @JvmStatic
    fun getHandler(): BiConsumer<C2SSwitchAmmo, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: C2SSwitchAmmo, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                val player = it.sender ?: return@enqueueWork
                setAmmoType(player, packet.ammoType)
            }
            it.packetHandled = true
        }
    }

    fun setAmmoType(player: ServerPlayer, ammoType: ResourceLocation, bypassAmmoCondition: Boolean = false) {
        val mainHand = player.mainHandItem
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (mainHand.item != gunItem) return
        val gunId = gunItem.getGunId(mainHand)

        if (IGunOperator.fromLivingEntity(player).synReloadState.stateType.isReloading) return
        TimelessAPI.getCommonGunIndex(gunId).ifPresent { index ->
            val gunData = index.gunData
            val currentAmmoId = IAccessorGunData.getCurrentAmmoId(gunData, mainHand)
            if (ammoType != currentAmmoId) {
                if (IAccessorGunData.isValidAmmoId(gunData, mainHand, ammoType)) {
                    val prevAmmo = gunItem.getCurrentAmmoCount(mainHand)
                    val switchCondition = IAccessorGunData.getSwitchAmmoCondition(gunData, mainHand)
                    if (switchCondition != SwitchAmmoCondition.NONE) {
                        if (switchCondition == SwitchAmmoCondition.NOT_FULL) {
                            val maxAmmo = AttachmentDataUtils.getAmmoCountWithAttachment(mainHand, gunData)
                            if (prevAmmo >= maxAmmo) {
                                return@ifPresent
                            }
                        } else if (switchCondition == SwitchAmmoCondition.EMPTY) {
                            if (prevAmmo > 0 || (gunItem.hasBulletInBarrel(mainHand) && gunData.bolt != Bolt.OPEN_BOLT)) {
                                return@ifPresent
                            }
                        }
                    }

                    val ammoAmount = getAmmoAmount(ammoType, player)

                    if (!player.isCreative && ammoAmount.first <= 0 && !bypassAmmoCondition) {
                        gunItem.setCurrentAmmoCount(mainHand, prevAmmo)
                        return@ifPresent
                    }


                    if (!ammoAmount.second || ServerConfig.returnAmmoAllTypeCreativeAmmoBox.get()) {
                        gunItem.dropAllAmmo(player, mainHand)
                    } else {
                        gunItem.setCurrentAmmoCount(mainHand, 0)
                    }

                    if (gunItem.hasBulletInBarrel(mainHand)) {
                        gunItem.setBulletInBarrel(mainHand, false)
                        if (gunData.bolt != Bolt.OPEN_BOLT && !player.isCreative && (!ammoAmount.second || ServerConfig.returnAmmoAllTypeCreativeAmmoBox.get())) {
                            val ammoItem = AmmoItemBuilder.create().setId(currentAmmoId).setCount(1).build()
                            ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                        }
                    }

                    if (ammoType == gunData.ammoId) {
                        GunExtras.setCurrentExtraAmmoId(mainHand, null)
                    } else {
                        GunExtras.setCurrentExtraAmmoId(mainHand, ammoType)
                    }

                    if (player.isCreative) {
                        gunItem.setCurrentAmmoCount(mainHand, 0)
                    }

                    AttachmentPropertyManager.postChangeEvent(player, mainHand)
                    player.inventoryMenu.broadcastChanges()
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.RefreshCache), player)
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.Reload), player)
                    SkillManager.trigger(TriggerType.ON_SWITCH_AMMO, Context(player))
                }
            }
        }
    }

    private fun getAmmoAmount(ammoId: ResourceLocation, player: ServerPlayer): Pair<Int, Boolean> {
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
                    return amount to true
                }

                if (iAmmoBox.getAmmoId(inventoryItem) == ammoId) {
                    if (iAmmoBox.isCreative(inventoryItem)) {
                        amount = 9999
                        return amount to false
                    }
                }
                amount += iAmmoBox.getAmmoCount(inventoryItem)
            }
        }
        return amount to false
    }
}