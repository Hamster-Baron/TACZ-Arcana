package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.SkillManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("CancelSkill")
data class CancelSkill(
    val skillName: String? = null,
    val duration: Int = 0,
    val runRemainingTasks: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = CancelSkill("skill")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer) {
                val skillName = skillName ?: skill.config.name
                SkillManager.cancelSkill(target, skillName, duration, runRemainingTasks)
            }
        }
    }
}