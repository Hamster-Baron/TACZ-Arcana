package group.taczexpands.server.config.action

import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.condition.ChainCondition
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Branch")
data class Branch(
    val chainCondition: ChainCondition,
    val succeeded: ChainAction? = null,
    val failed: ChainAction? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Branch(ChainCondition.EXAMPLE)
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        if (chainCondition.check(context))
            succeeded?.perform(skill, context)
        else failed?.perform(skill, context)
    }
}
