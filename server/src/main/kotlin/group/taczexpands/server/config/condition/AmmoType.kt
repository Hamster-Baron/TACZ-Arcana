package group.taczexpands.server.config.condition

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.init.ModItems
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("AmmoType")
data class AmmoType(val types: List<String>, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = AmmoType(listOf("tacz:.*"), true)
    }

    override fun check(context: Context): Boolean {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = context.self.mainHandItem
        if (mainHand.item != gunItem) return false
        val gunId = gunItem.getGunId(mainHand)
        val index = TimelessAPI.getCommonGunIndex(gunId).orElse(null) ?: return false
        val currentAmmoId = IAccessorGunData.getCurrentAmmoId(index.gunData, mainHand).toString()
        if (types.checkContains(currentAmmoId, useRegex)) {
            return true
        }
        return false
    }
}