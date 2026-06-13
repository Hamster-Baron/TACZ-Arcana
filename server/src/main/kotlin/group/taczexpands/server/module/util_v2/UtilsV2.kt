package group.taczexpands.server.module.util_v2

import com.tacz.guns.api.item.IGun
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

fun getGun(livingEntity: LivingEntity): GunContext? {
    val mainHandItem = livingEntity.mainHandItem
    val iGun = IGun.getIGunOrNull(mainHandItem) ?: return null
    return GunContext(mainHandItem, iGun)
}

fun getGun(itemStack: ItemStack): GunContext? {
    val iGun = IGun.getIGunOrNull(itemStack) ?: return null
    return GunContext(itemStack, iGun)
}