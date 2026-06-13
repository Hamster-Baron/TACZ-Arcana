package group.taczexpands.server.config.action

import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.util.schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val runBlock = {
            actions?.forEach {
                try {
                    it.perform(skill, context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (from != null) {
                SkillManager.LOADED_CHAIN_ACTIONS[from]?.actions?.forEach {
                    try {
                        it.perform(skill, context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
