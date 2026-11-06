package group.taczexpands.server.config.action

import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.performAction
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.DefinitionScope
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.util.schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Chain")
data class ChainAction(
    val actions: List<Action>? = null,
    val from: String? = null,
    val repeat: Int = 0,
    val repeatDelay: Int? = 0,
    override val delay: Int? = null,
) : Action {
    companion object {
        val EXAMPLE = ChainAction(listOf())
    }

    @Transient
    private val definitionScope = DefinitionScope.current

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val runBlock = {
            actions?.forEach {
                it.performAction(skill, context)
            }
            if (from != null) {
                SkillManager.resolveChainAction(from, definitionScope)?.actions?.forEach {
                    it.performAction(skill, context)
                }
            }
        }

        if (repeat > 0) {
            repeat(repeat) {
                if (repeatDelay != null && repeatDelay > 0) {
                    schedule(skill, context.self, repeatDelay * it) { runBlock() }
                } else {
                    runBlock()
                }
            }
        } else {
            runBlock()
        }
    }
}
