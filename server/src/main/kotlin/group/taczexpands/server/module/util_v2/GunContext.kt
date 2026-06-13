package group.taczexpands.server.module.util_v2

import com.tacz.guns.api.item.IGun
import com.tacz.guns.resource.pojo.data.gun.GunData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.GunExtraHolder
import net.minecraft.world.item.ItemStack

data class GunContext(val gunItemStack: ItemStack, val iGun: IGun) {
    val gunData: GunData?
        get() = IAccessorGunData.getGunData(gunItemStack)

    val extra: GunExtraHolder?
        get() = IAccessorGunData.getExtraHolder(gunItemStack)
}
