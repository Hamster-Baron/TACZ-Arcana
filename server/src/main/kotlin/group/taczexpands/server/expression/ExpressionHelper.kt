package group.taczexpands.server.expression

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import com.ezylang.evalex.data.EvaluationValue
import com.ezylang.evalex.functions.AbstractFunction
import com.ezylang.evalex.functions.FunctionIfc
import com.ezylang.evalex.functions.FunctionParameter
import com.ezylang.evalex.parser.Token
import com.mojang.brigadier.StringReader
import com.tacz.guns.init.CapabilityRegistry
import group.taczexpands.common.nbt.unwrap
import group.taczexpands.server.bukkit.BukkitHelper
import group.taczexpands.server.bukkit.placeholderapi.PlaceholderAPIForge
import group.taczexpands.server.bukkit.placeholderapi.PlaceholderAPIHelper
import group.taczexpands.server.config.GLOBAL_VALUES
import group.taczexpands.server.context.Context
import group.taczexpands.server.nbt.DataStorage
import group.taczexpands.server.nbt.DataType
import group.taczexpands.server.skill.PackValuesManager
import group.taczexpands.server.skill.SignalManager
import group.taczexpands.server.util.infer
import net.minecraft.commands.arguments.NbtPathArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.*

object ExpressionHelper {
    private fun functionEntry(key: String, value: FunctionIfc): Map.Entry<String, FunctionIfc> {
        return AbstractMap.SimpleEntry(key, value) as Map.Entry<String, FunctionIfc>
    }

    val expressionConfig = ExpressionConfiguration.builder()
        .allowOverwriteConstants(true)
        .arraysAllowed(true)
        .binaryAllowed(true)
        .build()
        .withAdditionalFunctions(
            functionEntry("str", StrFunction),
            functionEntry("double", DoubleFunction),
            functionEntry("int", IntFunction),
            functionEntry("bool", BoolFunction),
            functionEntry("signalLife", SignalLifeFunction),
            functionEntry("getMainHandNBT", GetMainHandNBTFunction),
            functionEntry("getSelfNBT", GetSelfNBTFunction),

            functionEntry("genUUID", GenUUIDFunction)


        )

    fun parse(str: String?, args: List<String>?, context: Context, target: Entity?): String? {
        var copy = str ?: return null
        if (args != null && args.isNotEmpty()) {
            IntRange(0, args.size - 1).map { it to "\$${it + 1}" }.forEach {
                if (copy.contains(it.second)) {
                    copy = copy.replace(
                        it.second,
                        initExpression(args[it.first], context, target).evaluate().stringValue
                    )
                }
            }
        }
        return copy
    }

    fun parseNew(str: String?, args: List<ExpressionInstance>?, context: Context, target: Entity?): String? {
        var copy = str ?: return null
        if (args != null && args.isNotEmpty()) {
            IntRange(0, args.size - 1).map { it to "\$${it + 1}" }.forEach {
                if (copy.contains(it.second)) {
                    copy = copy.replace(
                        it.second,
                        args[it.first].get(context, target).stringValue
                    )
                }
            }
        }
        return copy
    }

    fun initExpression(
        expressionStr: String,
        context: Context,
        target: Entity?,
        newSelf: Entity? = null,
        processDelegate: ((Expression) -> Expression)? = null
    ): Expression {
        val innerExpression = Expression(expressionStr, expressionConfig)
        val expression = processDelegate?.invoke(innerExpression) ?: innerExpression
        expression.with("__BUILTIN_CONTEXT__", context)
        expression.with("__BUILTIN_TARGET__", target)
        expression.with("__BUILTIN_NEW_SELF__", newSelf)
        val scoreboard = ServerLifecycleHooks.getCurrentServer().scoreboard
        expression.undefinedVariables.forEach { variable ->
            if (variable.startsWith("Val_")) {
                val valueName = variable.removePrefix("Val_")

                val packValue = PackValuesManager.getValue(valueName)
                if (packValue != null) {
                    expression.with(variable, packValue)
                    return@forEach
                }


                if (!GLOBAL_VALUES.values.containsKey(valueName)) {
                    return@forEach
                }

                val scalar = GLOBAL_VALUES.values[valueName]!!

                expression.with(variable, scalar.infer())
                return@forEach
            }

            if (variable.startsWith("VarFloat_")) {
                val entity = target ?: context.self
                val objectiveName = variable.removePrefix("VarFloat_")
                val scoreData = DataStorage.get().getOrCreateScore(DataType.FLOAT, objectiveName)
                val value = scoreData.getValue(entity)
                expression.with(variable, value)
            } else if (variable.startsWith("VarString_")) {
                val entity = target ?: context.self
                val objectiveName = variable.removePrefix("VarString_")
                val scoreData = DataStorage.get().getOrCreateScore(DataType.STRING, objectiveName)
                val value = scoreData.getValue(entity)
                expression.with(variable, value)
            } else if (variable.startsWith("Var_")) {
                val entity = target ?: context.self
                val objectiveName = "tacz_${variable.removePrefix("Var_")}"
                if (!scoreboard.hasObjective(objectiveName)) {
                    scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.DUMMY.defaultRenderType)
                }
                val objective = scoreboard.getOrCreateObjective(objectiveName)
                val score = scoreboard.getOrCreatePlayerScore(entity.scoreboardName, objective).score
                expression.with(variable, score)
            } else {
                val result = if (variable.startsWith("PAPI_")) {
                    val player = target as? Player
                    if (player != null && BukkitHelper.hasBukkit && PlaceholderAPIHelper.hasPlaceholderAPI) {
                        PlaceholderAPIForge.setPlaceholders(player, variable.removePrefix("PAPI_"))
                    } else variable
                } else if (variable.startsWith("VarPAPI_")) {
                    val player = target as? Player
                    if (player != null && BukkitHelper.hasBukkit && PlaceholderAPIHelper.hasPlaceholderAPI) {
                        PlaceholderAPIForge.setPlaceholders(player, variable.removePrefix("VarPAPI_"))
                    } else variable
                } else if (variable.startsWith("Self")) {
                    val self = newSelf ?: context.self
                    Variables.getBuiltinVariable<Any?>(variable.removePrefix("Self"))?.get(context, self)
                } else if (variable.startsWith("Target")) {
                    val newTarget = context.target
                    Variables.getBuiltinVariable<Any?>(variable.removePrefix("Target"))?.get(context, newTarget)
                } else if (variable.startsWith("VarSelf")) {
                    val self = newSelf ?: context.self
                    Variables.getBuiltinVariable<Any?>(variable.removePrefix("VarSelf"))?.get(context, self)
                } else if (variable.startsWith("VarTarget")) {
                    val newTarget = context.target
                    Variables.getBuiltinVariable<Any?>(variable.removePrefix("VarTarget"))?.get(context, newTarget)
                } else if (variable.startsWith("Var")) {
                    Variables.getBuiltinVariable<Any?>(variable.removePrefix("Var"))?.get(context, target)
                } else {
                    Variables.getBuiltinVariable<Any?>(variable)?.get(context, target)
                }

                expression.with(variable, result)
            }
        }
        return expression
    }
}

object GenUUIDFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        return expression.convertValue(UUID.randomUUID().toString())
    }
}

@FunctionParameter(name = "value", isLazy = false)
object StrFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val param = params[0]
        if (param.isStringValue) {
            val str = param.stringValue
            return expression.convertValue(str)
        } else if (param.isNumberValue) {
            return expression.convertValue(param.numberValue.toPlainString())
        } else {
            return expression.convertValue(param.value.toString())
        }
    }
}

@FunctionParameter(name = "value", isLazy = false)
object DoubleFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val param = params[0]
        if (param.isStringValue) {
            val str = param.stringValue
            return expression.convertValue(str.toDoubleOrNull() ?: str.toInt().toDouble())
        } else if (param.isBooleanValue) {
            return expression.convertValue(if (param.booleanValue) 1.0 else 0.0)
        } else {
            return expression.convertValue(param.numberValue.toDouble())
        }
    }
}

@FunctionParameter(name = "value", isLazy = false)
object IntFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val param = params[0]
        if (param.isStringValue) {
            val str = param.stringValue
            return expression.convertValue(str.toIntOrNull() ?: str.toDouble().toInt())
        } else if (param.isBooleanValue) {
            return expression.convertValue(if (param.booleanValue) 1 else 0)
        } else {
            return expression.convertValue(param.numberValue.toInt())
        }
    }
}

@FunctionParameter(name = "value", isLazy = false)
object BoolFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val param = params[0]
        if (param.isStringValue) {
            val str = param.stringValue
            return expression.convertValue(str.lowercase() != "false")
        } else if (param.isBooleanValue) {
            return expression.convertValue(param.booleanValue)
        } else {
            return expression.convertValue(param.numberValue.toInt() != 0)
        }
    }
}

@FunctionParameter(name = "value", isLazy = false)
object SignalLifeFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val param = params[0]
        val duration = if (param.isStringValue) {
            val str = param.stringValue
            SignalManager.getSignalLife(str)
        } else -1
        return expression.convertValue(duration)
    }
}

enum class TargetType(val typeName: String) {
    SELF("Self"),
    TARGET("Target"),
}

fun Expression.getData(variable: String): Any? {
    return this.dataAccessor.getData(variable).value
}

fun getTarget(expression: Expression, type: TargetType): Entity {
    when (type) {
        TargetType.SELF -> {
            val newSelf = expression.getData("__BUILTIN_NEW_SELF__")
            if (newSelf is Entity) return newSelf

            val context = expression.getData("__BUILTIN_CONTEXT__")
            if (context is Context) return context.self
        }

        TargetType.TARGET -> {
            val target = expression.getData("__BUILTIN_TARGET__")
            if (target is Entity) return target

            val context = expression.getData("__BUILTIN_CONTEXT__")
            if (context is Context) {
                val target = context.target
                if (target is Entity) return target
            }
        }
    }

    throw NullPointerException("Target result is null")
}

@FunctionParameter(name = "targetType", isLazy = false)
@FunctionParameter(name = "path", isLazy = false)
object GetMainHandNBTFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val targetType = params[0]
        val path = params[1]

        val target = getTarget(expression, TargetType.entries.first { it.typeName.equals(targetType.stringValue, ignoreCase = true) })
        if (target !is LivingEntity) throw IllegalStateException("${targetType.stringValue} is not a LivingEntity")

        val mainHandItem = target.mainHandItem

        val rootTag = mainHandItem.orCreateTag
        val nbtPath = NbtPathArgument.nbtPath().parse(StringReader(path.stringValue))
        val sources = nbtPath.get(rootTag)

        if (sources.isEmpty()) throw NullPointerException("NBT result is null")
        if (sources.size > 1) throw IllegalStateException("NBT result has more than one")

        return expression.convertValue(sources[0].unwrap())
    }
}

@FunctionParameter(name = "targetType", isLazy = false)
@FunctionParameter(name = "path", isLazy = false)
object GetSelfNBTFunction : AbstractFunction() {
    override fun evaluate(
        expression: Expression, p1: Token, vararg params: EvaluationValue
    ): EvaluationValue {
        val targetType = params[0]
        val path = params[1]

        val target = getTarget(expression, TargetType.entries.first { it.typeName.equals(targetType.stringValue, ignoreCase = true) })

        val rootTag = target.saveWithoutId(CompoundTag())

        val nbtPath = NbtPathArgument.nbtPath().parse(StringReader(path.stringValue))
        val sources = nbtPath.get(rootTag)

        if (sources.isEmpty()) throw NullPointerException("NBT result is null")
        if (sources.size > 1) throw IllegalStateException("NBT result has more than one")

        return expression.convertValue(sources[0].unwrap())
    }
}