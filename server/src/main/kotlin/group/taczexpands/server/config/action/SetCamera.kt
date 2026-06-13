package group.taczexpands.server.config.action

import group.taczexpands.common.data.CoordinateType
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.network.v2.s2c.S2CSendCameraPath
import group.taczexpands.common.util.JSON
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity

@Serializable
@SerialName("SetCamera")
data class SetCamera(
    val paths: List<CameraPath>,
    val lockMove: Boolean = true,
    val lockRotate: Boolean = true,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    @Serializable
    data class CameraPath(
        val x: String?,
        val y: String?,
        val z: String?,
        val yaw: String?,
        val pitch: String?,
        val roll: String?,
        val durationMillis: String?,
        val coordinateType: CoordinateType
    ) {
        fun toCommon(context: Context, target: LivingEntity?): group.taczexpands.common.data.CameraPath {
            return group.taczexpands.common.data.CameraPath(
                x?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toDouble() },
                y?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toDouble() },
                z?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toDouble() },
                yaw?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toFloat() },
                pitch?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toFloat() },
                roll?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toFloat() },
                durationMillis?.let { ExpressionHelper.initExpression(it, context, target).evaluate().numberValue.toLong() } ?: 0L,
                coordinateType)
        }
    }

    companion object {
        val EXAMPLE = SetCamera(listOf())
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach
            NetworkManager.sendToPlayer(S2CSendCameraPath(!lockMove, !lockRotate, paths.map { it.toCommon(context, target) }).create(), target)
        }
    }
}