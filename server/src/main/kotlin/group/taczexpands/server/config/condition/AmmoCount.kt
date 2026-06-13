package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.pojo.data.gun.Bolt
import group.taczexpands.server.config.action.TriggerSignal
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument


@Serializable
@SerialName("AmmoCount")
data class AmmoCount(val range: String) : Condition {
    companion object {
        val EXAMPLE = AmmoCount("0..")
    }

    @Transient
    val ammoRange = RangeArgument.Ints().parse(StringReader(range))

    override fun check(context: Context): Boolean {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = context.self.mainHandItem
        if (mainHand.item != gunItem) return false
        val boltAmount = if (gunItem.hasBulletInBarrel(mainHand)) {
            val gunData = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).orElse(null)
            if (gunData != null && gunData.getGunData().getBolt() != Bolt.OPEN_BOLT) {
                1
            } else 0
        } else 0
        if (!ammoRange.matches(gunItem.getCurrentAmmoCount(mainHand) + boltAmount)) return false
        return true
    }
}