package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.create
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument.Floats
import net.minecraft.world.entity.Entity

@Serializable
@SerialName("Variables")
data class Variables(val variables: List<Variable>, val selector: SelectorData? = null) : Condition {
    companion object {
        val EXAMPLE = Variables(listOf(Variable()))
    }

    override fun check(context: Context): Boolean {

        val target = selector.create(context).getTarget(context) ?: context.self
        for (variable in variables) {
            if (!variable.check(context, target)) return false
        }
        return true
    }
}


@Serializable
data class Variable(val name: String? = null, val range: String? = null, val expression: ExpressionData? = null) {
    @Transient
    val valueRange = range?.let { Floats().parse(StringReader(range)) }

    fun check(context: Context, target: Entity?): Boolean {
        if (expression == null && name != null && range != null) {
            val value = ExpressionHelper.initExpression(name, context, target).evaluate().numberValue.toDouble()
            return valueRange!!.matches(value)
        } else if (expression != null) {
            return expression.create(context).get(context, target).booleanValue
        } else throw Exception("Unknown variable type")
    }
}