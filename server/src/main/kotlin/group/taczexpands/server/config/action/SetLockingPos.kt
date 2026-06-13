package group.taczexpands.server.config.action

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.init.ModItems
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.util.taczPick
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

@Serializable
@SerialName("SetLockingPos")
data class SetLockingPos(
    val clear: Boolean = false,
    val fromSimpleMode: Boolean = false,
    val posX: String? = "VarTargetPos[0]",
    val posY: String? = "VarTargetPos[1]",
    val posZ: String? = "VarTargetPos[2]",
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = SetLockingPos()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {

        data.selector.getTargets(context).forEach { player ->
            if (player !is ServerPlayer) return@forEach

            if (clear) {
                PlayerListener.getPlayerStates(player).skillLockingPos = null
                return@forEach
            }

            if (fromSimpleMode) {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                val mainHand = player.mainHandItem
                if (mainHand.item != gunItem) {
                    return@forEach
                }

                if (!IGunOperator.fromLivingEntity(player).synIsAiming) {
                    return@forEach
                }

                val gunID = gunItem.getGunId(mainHand)

                val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                if (gunIndex == null) {
                    return@forEach
                }

                val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

                val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

                if (!extraBulletData.missileData.isMissile) return@forEach

                val result = player.taczPick(extraBulletData.missileData.maxLockingDistance.toDouble(), 1.0f, false)
                val location = if (result.type != HitResult.Type.MISS) {
                    result.location
                } else null

                PlayerListener.getPlayerStates(player).skillLockingPos = location

            } else {
                val x = ExpressionHelper.initExpression(posX!!, context, player).evaluate().numberValue.toDouble()
                val y = ExpressionHelper.initExpression(posY!!, context, player).evaluate().numberValue.toDouble()
                val z = ExpressionHelper.initExpression(posZ!!, context, player).evaluate().numberValue.toDouble()
                PlayerListener.getPlayerStates(player).skillLockingPos = Vec3(x, y, z)
            }
        }

    }
}