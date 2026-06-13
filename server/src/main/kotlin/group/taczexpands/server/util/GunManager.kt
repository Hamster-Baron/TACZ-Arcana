package group.taczexpands.server.util

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IAttachment
import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.builder.AmmoItemBuilder
import com.tacz.guns.network.NetworkHandler
import com.tacz.guns.network.message.ServerMessageRefreshRefitScreen
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import com.tacz.guns.resource.pojo.data.gun.Bolt
import group.taczexpands.common.accessor.IAccessorAttachmentData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.UnderBarrel
import group.taczexpands.common.nbt.AttachmentExtras
import group.taczexpands.common.nbt.GunExtras.getBuiltinUnderBarrel
import group.taczexpands.common.nbt.GunExtras.getGunDataUnderBarrel
import group.taczexpands.common.nbt.GunExtras.deleteUnderBarrelRootTag
import group.taczexpands.common.nbt.GunExtras.getUnderBarrel
import group.taczexpands.common.nbt.GunExtras.getUsingUnderBarrel
import group.taczexpands.common.nbt.GunExtras.setCurrentExtraAmmoId
import group.taczexpands.common.nbt.GunExtras.setUsingUnderBarrel
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.context.AttachmentChangeContext
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.NetworkManager.sendToPlayer
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemHandlerHelper
import kotlin.jvm.optionals.getOrNull

object GunManager {
    fun refitAttachment(attachmentType: AttachmentType, attachmentItem: ItemStack, attachmentSlotIndex: Int?, gunItem: ItemStack, player: ServerPlayer, enforce: Boolean = false, inventory: Inventory): Boolean {
        val iGun = IGun.getIGunOrNull(gunItem) ?: return false
        if (!enforce && !iGun.allowAttachment(gunItem, attachmentItem)) return false

        var shouldRefresh = false
        var shouldTriggerSwitch = false

        val oldAttachmentItem = iGun.getAttachment(gunItem, attachmentType)

        if (attachmentType == AttachmentType.GRIP) {
            val oldUnderBarrel = getUnderBarrel(gunItem)
            val newUnderBarrel = getUnderBarrelAfterGripRefit(gunItem, attachmentItem)
            if (!isSameUnderBarrel(oldUnderBarrel, newUnderBarrel)) {
                shouldRefresh = oldUnderBarrel != null || newUnderBarrel != null
                if (oldUnderBarrel != null) {
                    shouldTriggerSwitch = unloadActiveUnderBarrelAmmo(iGun, gunItem, player)
                }
            }
        }

        val gunData = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem)).getOrNull()?.gunData
        val prevAmmoId = gunData?.let { IAccessorGunData.getCurrentAmmoId(it, gunItem) }
        val prevBaseAmmo = gunData?.let { IAccessorGunData.getCurrentBaseAmmo(it, gunItem) }
        val prevIsBaseAmmo = prevBaseAmmo != null && prevAmmoId != null && prevBaseAmmo.ammoId == prevAmmoId

        if (attachmentType == AttachmentType.EXTENDED_MAG) {
            iGun.dropAllAmmo(player, gunItem)
            if(gunData != null) {
                if (iGun.hasBulletInBarrel(gunItem)) {
                    iGun.setBulletInBarrel(gunItem, false)
                    if (gunData.getBolt() != Bolt.OPEN_BOLT && !player.isCreative()) {
                        val ammoItem = AmmoItemBuilder.create().setId(prevAmmoId).setCount(1).build()
                        ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                    }
                }
            }
        }

        iGun.installAttachment(gunItem, attachmentItem)
        AttachmentPropertyManager.postChangeEvent(player, gunItem)

        val currentAmmoId = gunData?.let { IAccessorGunData.getCurrentAmmoId(it, gunItem) }
        val currentBaseAmmo = gunData?.let { IAccessorGunData.getCurrentBaseAmmo(it, gunItem) }
        val currentIsBaseAmmo = currentBaseAmmo != null && currentAmmoId != null && currentBaseAmmo.ammoId == prevAmmoId

        if (prevAmmoId != null && !IAccessorGunData.isValidAmmoId(gunData, gunItem, prevAmmoId)
            || prevIsBaseAmmo && prevBaseAmmo!!.isTriggerBaseAmmoReloadEvent
            || currentIsBaseAmmo && currentBaseAmmo!!.isTriggerBaseAmmoReloadEvent) {

            iGun.installAttachment(gunItem, oldAttachmentItem)
            AttachmentPropertyManager.postChangeEvent(player, gunItem)

            iGun.dropAllAmmo(player, gunItem)
            if (iGun.hasBulletInBarrel(gunItem)) {
                iGun.setBulletInBarrel(gunItem, false)
                if (gunData.getBolt() != Bolt.OPEN_BOLT && !player.isCreative()) {
                    val ammoItem = AmmoItemBuilder.create().setId(prevAmmoId).setCount(1).build()
                    ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                }
            }

            iGun.installAttachment(gunItem, attachmentItem)
            AttachmentPropertyManager.postChangeEvent(player, gunItem)

            setCurrentExtraAmmoId(gunItem, null)
            SkillManager.trigger(TriggerType.ON_SWITCH_AMMO, Context(player))
        }

        if (!AttachmentExtras.getIsGenerated(oldAttachmentItem)) {
            if (attachmentSlotIndex != null) {
                inventory.setItem(attachmentSlotIndex, oldAttachmentItem)
            } else {
                if (!inventory.add(oldAttachmentItem)) {
                    player.drop(oldAttachmentItem, true)
                }
            }
        }


        player.inventoryMenu.broadcastChanges()
        NetworkHandler.sendToClientPlayer(ServerMessageRefreshRefitScreen(), player)
        if (shouldRefresh) {
            sendToPlayer(S2CAction(S2CAction.Action.Draw, 0), player)
            sendToPlayer(S2CAction(S2CAction.Action.Draw, 3), player)
        }
        if (shouldTriggerSwitch) {
            SkillManager.triggerReverse(TriggerType.ON_SWITCH_TO_UNDER_BARREL, Context(player), -1)
            SkillManager.trigger(TriggerType.ON_SWITCH_TO_GUN, Context(player), -1)
        }

        val iAttachmentOld = IAttachment.getIAttachmentOrNull(oldAttachmentItem)
        val prevId = if (iAttachmentOld != null) {
            iAttachmentOld.getAttachmentId(oldAttachmentItem).toString()
        } else null

        val iAttachmentNew = IAttachment.getIAttachmentOrNull(attachmentItem)
        val newId = if (iAttachmentNew != null) {
            iAttachmentNew.getAttachmentId(attachmentItem).toString()
        } else null


        SkillManager.trigger(TriggerType.ON_CHANGE_ATTACHMENT, AttachmentChangeContext(player, prevId, newId), -1)

        return true
    }

    fun unloadAttachment(attachmentType: AttachmentType, gunItem: ItemStack, player: ServerPlayer, inventory: Inventory): Boolean {
        val iGun = IGun.getIGunOrNull(gunItem) ?: return false

        val attachmentItem = iGun.getAttachment(gunItem, attachmentType)
        if (attachmentItem.isEmpty) return false

        var shouldRefresh = false
        var shouldTriggerSwitch = false

        if (attachmentType == AttachmentType.GRIP) {
            val oldUnderBarrel = getUnderBarrel(gunItem)
            val newUnderBarrel = getUnderBarrelAfterGripUnload(gunItem)
            if (!isSameUnderBarrel(oldUnderBarrel, newUnderBarrel)) {
                shouldRefresh = oldUnderBarrel != null || newUnderBarrel != null
                if (oldUnderBarrel != null) {
                    shouldTriggerSwitch = unloadActiveUnderBarrelAmmo(iGun, gunItem, player)
                }
            }
        }

        val gunData = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem)).getOrNull()?.gunData
        val prevAmmoId = gunData?.let { IAccessorGunData.getCurrentAmmoId(it, gunItem) }
        val prevBaseAmmo = gunData?.let { IAccessorGunData.getCurrentBaseAmmo(it, gunItem) }
        val prevIsBaseAmmo = prevBaseAmmo != null && prevAmmoId != null && prevBaseAmmo.ammoId == prevAmmoId

        if (attachmentType == AttachmentType.EXTENDED_MAG) {
            iGun.dropAllAmmo(player, gunItem)
            if (gunData != null) {
                if (iGun.hasBulletInBarrel(gunItem)) {
                    iGun.setBulletInBarrel(gunItem, false)
                    if (gunData.getBolt() != Bolt.OPEN_BOLT && !player.isCreative()) {
                        val ammoItem = AmmoItemBuilder.create().setId(prevAmmoId).setCount(1).build()
                        ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                    }
                }
            }
        }

        iGun.unloadAttachment(gunItem, attachmentType)
        AttachmentPropertyManager.postChangeEvent(player, gunItem)

        if (prevAmmoId != null && !IAccessorGunData.isValidAmmoId(gunData, gunItem, prevAmmoId)
            || prevIsBaseAmmo && prevBaseAmmo!!.isTriggerBaseAmmoReloadEvent) {

            iGun.installAttachment(gunItem, attachmentItem)
            AttachmentPropertyManager.postChangeEvent(player, gunItem)

            iGun.dropAllAmmo(player, gunItem)
            if (iGun.hasBulletInBarrel(gunItem)) {
                iGun.setBulletInBarrel(gunItem, false)
                if (gunData.getBolt() != Bolt.OPEN_BOLT && !player.isCreative()) {
                    val ammoItem = AmmoItemBuilder.create().setId(prevAmmoId).setCount(1).build()
                    ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                }
            }

            iGun.unloadAttachment(gunItem, attachmentType)
            AttachmentPropertyManager.postChangeEvent(player, gunItem)

            setCurrentExtraAmmoId(gunItem, null)
            SkillManager.trigger(TriggerType.ON_SWITCH_AMMO, Context(player))

        }

        if (!AttachmentExtras.getIsGenerated(attachmentItem)) {
            if (!inventory.add(attachmentItem)) {
                player.drop(attachmentItem, true)
            }
        }

        player.inventoryMenu.broadcastChanges()
        NetworkHandler.sendToClientPlayer(ServerMessageRefreshRefitScreen(), player)
        if (shouldRefresh) {
            sendToPlayer(S2CAction(S2CAction.Action.Draw, 0), player)
            sendToPlayer(S2CAction(S2CAction.Action.Draw, 3), player)
        }
        if (shouldTriggerSwitch) {
            SkillManager.triggerReverse(TriggerType.ON_SWITCH_TO_UNDER_BARREL, Context(player), -1)
            SkillManager.trigger(TriggerType.ON_SWITCH_TO_GUN, Context(player), -1)
        }


        val iAttachmentNew = IAttachment.getIAttachmentOrNull(attachmentItem)
        val prevId = if (iAttachmentNew != null) {
            iAttachmentNew.getAttachmentId(attachmentItem).toString()
        } else null


        SkillManager.trigger(TriggerType.ON_CHANGE_ATTACHMENT, AttachmentChangeContext(player, prevId, null), -1)

        return true
    }

    private fun getAttachmentUnderBarrel(attachmentItem: ItemStack): UnderBarrel? {
        val iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem) ?: return null
        return IAccessorAttachmentData.getExtraHolder(iAttachment.getAttachmentId(attachmentItem))?.underBarrel
    }

    private fun getUnderBarrelAfterGripRefit(gunItem: ItemStack, attachmentItem: ItemStack): UnderBarrel? {
        return getAttachmentUnderBarrel(attachmentItem) ?: getBuiltinUnderBarrel(gunItem) ?: getGunDataUnderBarrel(gunItem)
    }

    private fun getUnderBarrelAfterGripUnload(gunItem: ItemStack): UnderBarrel? {
        return getBuiltinUnderBarrel(gunItem) ?: getGunDataUnderBarrel(gunItem)
    }

    private fun isSameUnderBarrel(first: UnderBarrel?, second: UnderBarrel?): Boolean {
        if (first === second) return true
        if (first == null || second == null) return false
        return first.gunId == second.gunId
                && first.gunDisplayNamespace == second.gunDisplayNamespace
                && first.gunDisplayPrefix == second.gunDisplayPrefix
                && first.ignoreSilencer == second.ignoreSilencer
                && first.animationLockTime == second.animationLockTime
    }

    private fun unloadActiveUnderBarrelAmmo(iGun: IGun, gunItem: ItemStack, player: ServerPlayer): Boolean {
        val wasUsingUnderBarrel = getUsingUnderBarrel(gunItem)
        setUsingUnderBarrel(gunItem, true)
        if (iGun.hasBulletInBarrel(gunItem)) {
            val gunData = TimelessAPI.getCommonGunIndex(iGun.getGunId(gunItem)).orElse(null)
            if (gunData != null && gunData.gunData.bolt != Bolt.OPEN_BOLT) {
                iGun.setCurrentAmmoCount(gunItem, iGun.getCurrentAmmoCount(gunItem) + 1)
            }
        }
        iGun.dropAllAmmo(player, gunItem)
        setUsingUnderBarrel(gunItem, false)
        deleteUnderBarrelRootTag(gunItem)
        return wasUsingUnderBarrel
    }

}
