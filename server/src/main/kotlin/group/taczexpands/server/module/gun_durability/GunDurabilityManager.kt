package group.taczexpands.server.module.gun_durability

import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.nbt.GunExtras.getDurabilityDamage
import group.taczexpands.common.nbt.GunExtras.setDurabilityDamage
import group.taczexpands.server.module.util_v2.getGun
import group.taczexpands.server.util.schedule
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object GunDurabilityManager {
    fun isNotBroken(gun: ItemStack): Boolean {
        val extra = IAccessorGunData.getExtraHolder(gun) ?: return true
        if (extra.durability <= 0) return true
        val currentDamage = getDurabilityDamage(gun)
        if (currentDamage < extra.durability) {
            return true
        }
        return false
    }

    fun updateRemove(player: ServerPlayer) {
        val gunContext = getGun(player)
        if (gunContext != null) {
            if (!isNotBroken(gunContext.gunItemStack)) {
                if (gunContext.extra?.removeOnDamaged ?: false) {
                    val removeDelay = gunContext.extra?.removeDelay ?: 0

                    schedule(removeDelay) {
                        gunContext.gunItemStack.shrink(1)
                    }
                }
            }
        }
    }

    fun damage(gun: ItemStack, damage: Int) {
        val gunContext = getGun(gun) ?: return
        if ((gunContext.extra?.durability ?: 0) <= 0) return
        var currentDamage = getDurabilityDamage(gun)
        currentDamage += damage
        setDurabilityDamage(gun, currentDamage)
    }

}