package group.taczexpands.server.config.action

import group.taczexpands.common.network.v2.s2c.S2CParticleEffect
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.ParticleEmitterManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

internal fun particleVariables(variables: Map<String, String>?, args: List<String>?, context: Context,
    target: Entity): Map<String, Float> = variables?.mapValues { (_, value) ->
    ExpressionHelper.parse(value, args, context, target)?.toFloatOrNull() ?: 0f
} ?: emptyMap()

@Serializable
@SerialName("ParticleUpdate")
data class ParticleUpdate(
    val identifier: String,
    val selector: SelectorData? = null,
    val variables: Map<String, String>? = null,
    val remove: Boolean = false,
    val args: List<String>? = null,
    override val delay: Int? = null
) : Action {
    override fun prepare(skill: Skill, context: Context): PrepareData = SelectorPrepareData(selector.create(context))

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).distinct().forEach { target ->
            val level = target.level() as? ServerLevel ?: return@forEach
            if (remove) ParticleEmitterManager.removeNamed(identifier, target)
            ParticleEmitterManager.sendEffect(S2CParticleEffect(identifier = identifier, owner = target.id,
                variables = if (remove) null else particleVariables(variables, args, context, target), remove = remove),
                level, null)
        }
    }
}
