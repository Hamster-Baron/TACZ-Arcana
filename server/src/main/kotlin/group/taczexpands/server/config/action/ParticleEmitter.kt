package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.Emitter
import group.taczexpands.server.skill.ParticleEmitterManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

@Serializable
@SerialName("ParticleEmitter")
data class ParticleEmitter(
    val type: String,
    val extraData: String = "",
    val mode: Emitter.Mode,
    val selector: SelectorData? = null,
    val x: String,
    val y: String,
    val z: String,
    val radius: Float,
    val maxNum: Int,
    val duration: Int,
    val startDelay: Int,
    val blockSight: Boolean,
    @SerialName("gravity")
    val gravityValue: Float = 0.0f,
    val args: List<String>? = null,
    override val delay: Int? = null
) : Action {

    companion object {
        val EXAMPLE = ParticleEmitter("minecraft:dust",
            " ",
            mode = Emitter.Mode.BALL,
            x = "",
            y = "",
            z = "",
            radius = 5.0f,
            maxNum = 50,
            duration = 20,
            startDelay = 0,
            blockSight = true)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val targets = data.selector.getTargets(context)

        targets.forEach { target ->
            val level = target.level() as? ServerLevel ?: return@forEach
            val x = ExpressionHelper.parse(x, args, context, target)?.toDoubleOrNull() ?: 0.0
            val y = ExpressionHelper.parse(y, args, context, target)?.toDoubleOrNull() ?: 0.0
            val z = ExpressionHelper.parse(z, args, context, target)?.toDoubleOrNull() ?: 0.0

            ParticleEmitterManager.addEmitter(
                Emitter(
                    type,
                    extraData,
                    level,
                    Vec3(x, y, z),
                    mode,
                    radius.toDouble(),
                    maxNum,
                    duration,
                    startDelay,
                    blockSight,
                    gravityValue,
                )
            )
        }
    }
}