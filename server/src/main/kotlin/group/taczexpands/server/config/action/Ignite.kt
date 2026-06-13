package group.taczexpands.server.config.action

import group.taczexpands.server.accessor.IAccessorEntity
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Ignite")
data class Ignite(
    val time: Int,
    val extraDamage: Float = 0.0f,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Ignite(5)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target.remainingFireTicks = time
            (target as IAccessorEntity).`taczexpands$setIgniteExtraDamage`(extraDamage)
        }
    }
}
