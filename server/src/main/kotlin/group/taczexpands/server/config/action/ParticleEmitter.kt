package group.taczexpands.server.config.action

import group.taczexpands.common.network.v2.s2c.S2CParticleEffect
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
    val maxNum: Int = 0,
    val duration: Int,
    val startDelay: Int,
    val blockSight: Boolean,
    @SerialName("gravity")
    val gravityValue: Float = 0.0f,
    val args: List<String>? = null,
    override val delay: Int? = null,
    val variables: Map<String, String>? = null,
    val identifier: String? = null,
    val follow: Boolean = false,
    val facing: Boolean = false
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
        val selected = data.selector.getTargets(context)
        val targets = if (mode == Emitter.Mode.SNOWSTORM) selected.distinct() else selected

        targets.forEach { target ->
            val level = target.level() as? ServerLevel ?: return@forEach
            val x = ExpressionHelper.parse(x, args, context, target)?.toDoubleOrNull() ?: 0.0
            val y = ExpressionHelper.parse(y, args, context, target)?.toDoubleOrNull() ?: 0.0
            val z = ExpressionHelper.parse(z, args, context, target)?.toDoubleOrNull() ?: 0.0
            val location = Vec3(x, y, z)

            if (mode == Emitter.Mode.SNOWSTORM) {
                val values = mutableMapOf("radius" to radius, "duration" to duration / 20f,
                    "start_delay" to startDelay / 20f)
                values.putAll(particleVariables(variables, args, context, target))
                ParticleEmitterManager.sendEffect(S2CParticleEffect(type, identifier, target.id, x, y, z, values,
                    follow, if (facing) target.getViewYRot(1f) else null,
                    if (facing) target.getViewXRot(1f) else null), level, location)
                if (!blockSight) {
                    if (identifier != null) ParticleEmitterManager.removeNamed(identifier, target)
                    return@forEach
                }
            }

            ParticleEmitterManager.addEmitter(
                Emitter(
                    type,
                    extraData,
                    level,
                    location,
                    mode,
                    radius.toDouble(),
                    maxNum,
                    duration,
                    startDelay,
                    blockSight,
                    gravityValue,
                ),
                if (mode == Emitter.Mode.SNOWSTORM) identifier else null,
                target
            )
        }
    }
}
