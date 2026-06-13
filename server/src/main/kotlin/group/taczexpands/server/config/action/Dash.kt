package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ArgsPrepareData
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.create
import group.taczexpands.server.module.dash.DashInstance
import group.taczexpands.server.module.dash.DashManager
import group.taczexpands.server.module.dash.DashManager.activeDashes
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("Dash")
data class Dash(
    val mode: Mode,
    val yaw: ExpressionData,
    val pitch: ExpressionData,
    val speed: ExpressionData,
    val drag: ExpressionData,
    val applyEnvironmentModifiers: Boolean,
    val impactDamage: ExpressionData,
    val selfImpactDamage: ExpressionData,
    val damageType: String,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Dash(
            Mode.GROUND_FOLLOW,
            ExpressionData("Pos[3]"),
            ExpressionData("Pos[4]"),
            ExpressionData("2.0"),
            ExpressionData("0.95"),
            true,
            ExpressionData("1.0"),
            ExpressionData("1.0"),
            "minecraft:in_wall"
        )
    }

    @Serializable
    enum class Mode {
        @SerialName("omni_directional")
        OMNI_DIRECTIONAL,

        @SerialName("ground_follow")
        GROUND_FOLLOW,

        @SerialName("blink")
        BLINK,
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), listOf(yaw, pitch, speed, drag, impactDamage, selfImpactDamage).create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach

            val yaw = data.dataList[1].args[0].get(context, target).numberValue.toFloat()
            val pitch = data.dataList[1].args[1].get(context, target).numberValue.toFloat()
            val speed = data.dataList[1].args[2].get(context, target).numberValue.toFloat()
            val drag = data.dataList[1].args[3].get(context, target).numberValue.toFloat()
            val impactDamage = data.dataList[1].args[4].get(context, target).numberValue.toFloat()
            val selfImpactDamage = data.dataList[1].args[5].get(context, target).numberValue.toFloat()


            val usePitch = mode == Mode.OMNI_DIRECTIONAL
            val direction = DashManager.getDirectionVec(yaw, if (usePitch) pitch else 0f)

            activeDashes[target] = DashInstance(
                target,
                mode,
                direction,
                speed.toDouble(),
                drag.toDouble(),
                applyEnvironmentModifiers,
                ResourceLocation(damageType),
                impactDamage,
                selfImpactDamage
            )

        }
    }
}