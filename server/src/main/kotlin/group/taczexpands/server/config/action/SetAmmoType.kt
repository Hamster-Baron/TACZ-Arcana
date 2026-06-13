package group.taczexpands.server.config.action

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.init.ModItems
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.c2s.C2SSwitchAmmoImpl
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import kotlin.jvm.optionals.getOrNull

@Serializable
@SerialName("SetAmmoType")
data class SetAmmoType(
    val index: Int? = null,
    val ammoType: String? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = SetAmmoType(null, "tacz:9mm")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target as? ServerPlayer ?: return@forEach
            if (ammoType != null) {
                C2SSwitchAmmoImpl.setAmmoType(target, ResourceLocation(ammoType), true)
            } else if (index != null) {
                val mainHand = target.mainHandItem
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                if (gunItem == mainHand.item) {
                    val gunIndex = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return@forEach
                    val ammoList = IAccessorGunData.getExtraAmmoList(gunIndex.gunData, mainHand)
                    val realIndex = index - 1
                    if (realIndex == -1) {
                        C2SSwitchAmmoImpl.setAmmoType(target, gunIndex.gunData.ammoId, true)
                    } else if (realIndex >= 0) {
                        if (ammoList.isEmpty()) return@forEach
                        val ammoType = ammoList[realIndex % ammoList.size].ammoId
                        C2SSwitchAmmoImpl.setAmmoType(target, ammoType, true)
                    }
                }
            }
        }
    }
}