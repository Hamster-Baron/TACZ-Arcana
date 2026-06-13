package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.TriggerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("CancelAction")
data class CancelAction(
    val action: S2CCancelAction.Action,
    val duration: Int? = 1,
    val status: TriggerType? = null,
    val identifier: String? = null,
    val remove: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = CancelAction(S2CCancelAction.Action.Sprint)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {

        data.selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer) {
                if (status != null) {
                    if (remove) {
                        ActionManager.removeCancel(target, action, status, identifier)
                    } else {
                        ActionManager.cancelAction(target, action, status, identifier)
                    }
                } else if (duration != null) {
                    if (remove) {
                        ActionManager.removeCancel(target, action, identifier)
                    } else {
                        if (duration >= 0) {
                            ActionManager.cancelAction(target, action, duration, identifier)
                        }
                    }
                }
            }
        }

    }
}