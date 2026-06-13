package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.create
import group.taczexpands.server.util.ScheduledTask
import group.taczexpands.server.util.schedule
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Timer")
data class Timer(
    val timeExpression: ExpressionData,
    val recalculateEveryTick: Boolean = false,
    val action: ChainAction,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Timer(ExpressionData("0"), action = ChainAction.EXAMPLE)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), timeExpression.create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            var until = data.dataList[1].expression.get(context, target).numberValue.toInt()

            val taskBlock: (ScheduledTask) -> Unit = {
                action.perform(skill, context)
            }

            if (recalculateEveryTick) {
                schedule(skill, context.self, 0, taskBlock) {
                    it.time++
                    until = data.dataList[1].expression.get(context, target).numberValue.toInt()
                    if (it.time >= until) {
                        it.time = 0
                    }
                }
            } else {
                schedule(skill, context.self, until, taskBlock)
            }
        }
    }
}