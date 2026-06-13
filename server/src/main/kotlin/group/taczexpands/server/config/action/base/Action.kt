package group.taczexpands.server.config.action.base

import group.taczexpands.server.config.SelectorInstance
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionInstance
import group.taczexpands.server.util.schedule
import group.taczexpands.server.skill.*

interface Action {
    val delay: Int?

    fun perform(skill: Skill, context: Context) {
        val data = prepare(skill, context)
        if (delay == null || delay!! <= 0) {
            execute(skill, context, data)
        } else {
            schedule(skill, context.self, delay!!) {
                execute(skill, context, data)
            }
        }
    }

    fun prepare(skill: Skill, context: Context): PrepareData {
        return PrepareData.EMPTY
    }

    fun execute(skill: Skill, context: Context, data: PrepareData)
}

open class PrepareData() {
    companion object {
        val EMPTY = PrepareData()
    }

    val selector: SelectorInstance
        get() {
            return (this as SelectorPrepareData)._selector
        }

    val expression: ExpressionInstance
        get() {
            return (this as ExpressionPrepareData)._expression
        }

    val args: List<ExpressionInstance>
        get() {
            return (this as ArgsPrepareData)._args
        }

    val dataList: List<PrepareData>
        get() {
            return (this as ListPrepareData)._dataList
        }
}

class ListPrepareData(val _dataList: List<PrepareData>) : PrepareData() {
    constructor(vararg data: PrepareData) : this(data.toList())
}

class ExpressionPrepareData(val _expression: ExpressionInstance) : PrepareData()

class ArgsPrepareData(val _args: List<ExpressionInstance>) : PrepareData()

class SelectorPrepareData(val _selector: SelectorInstance) : PrepareData()

fun List<ExpressionInstance>.toData(): ArgsPrepareData {
    return ArgsPrepareData(this)
}

fun SelectorInstance.toData(): SelectorPrepareData {
    return SelectorPrepareData(this)
}

fun ExpressionInstance.toData(): ExpressionPrepareData {
    return ExpressionPrepareData(this)
}




