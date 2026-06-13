package group.taczexpands.server.api

import group.taczexpands.common.api.GunScriptAPICommon
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.Parameter
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.nbt.DataStorage
import group.taczexpands.server.nbt.DataType
import group.taczexpands.server.expression.create
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.skill.SignalManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import net.minecraftforge.server.ServerLifecycleHooks

object GunScriptAPIServer {
    fun init() {
        GunScriptAPICommon.getPlayerScoreIntServerDelegate = ::getInt
        GunScriptAPICommon.getPlayerScoreFloatServerDelegate = ::getFloat
        GunScriptAPICommon.getPlayerScoreStringServerDelegate = ::getString


        GunScriptAPICommon.setPlayerScoreIntDelegate = ::setInt
        GunScriptAPICommon.setPlayerScoreFloatDelegate = ::setFloat
        GunScriptAPICommon.setPlayerScoreStringDelegate = ::setString

        GunScriptAPICommon.dispatchSignalDelegate = ::dispatchSignal

        GunScriptAPICommon.parseExpressionDelegate = ::parseExpression

        GunScriptAPICommon.modifyDelegate = ::modify

        GunScriptAPICommon.storeLockingTarget = ::storeLockingTarget
    }

    fun getInt(entity: LivingEntity, scoreName: String): Int {
        if (entity is ServerPlayer) {
            val context = Context(entity)
            return ExpressionData(scoreName, target = SelectorData.SELF).create(context).get(context, entity).numberValue.toInt()
        }
        return 0
    }

    fun getFloat(entity: LivingEntity, scoreName: String): Float {
        if (entity is ServerPlayer) {
            val context = Context(entity)
            return ExpressionData(scoreName, target = SelectorData.SELF).create(context).get(context, entity).numberValue.toFloat()
        }
        return 0.0f
    }

    fun getString(entity: LivingEntity, scoreName: String): String {
        if (entity is ServerPlayer) {
            val context = Context(entity)
            return ExpressionData(scoreName, target = SelectorData.SELF).create(context).get(context, entity).stringValue
        }
        return ""
    }


    fun setInt(entity: LivingEntity, scoreName: String, value: Int): Boolean {
        if (entity is ServerPlayer) {
            if (scoreName.startsWith("Var_")) {
                val scoreboard = ServerLifecycleHooks.getCurrentServer().scoreboard
                val objectiveName = "tacz_${scoreName.removePrefix("Var_")}"
                if (!scoreboard.hasObjective(objectiveName)) {
                    scoreboard.addObjective(
                        objectiveName,
                        ObjectiveCriteria.DUMMY,
                        Component.literal(objectiveName),
                        ObjectiveCriteria.DUMMY.defaultRenderType
                    )
                }
                val objective = scoreboard.getOrCreateObjective(objectiveName)
                val score = scoreboard.getOrCreatePlayerScore(entity.scoreboardName, objective)

                score.score = value
                return true
            }
            return false
        }
        return false
    }

    fun setFloat(entity: LivingEntity, scoreName: String, value: Float): Boolean {
        if (entity is ServerPlayer) {
            if (scoreName.startsWith("VarFloat_")) {
                val objectiveName = scoreName.removePrefix("VarFloat_")
                val scoreData = DataStorage.get().getOrCreateScore(DataType.FLOAT, objectiveName)
                scoreData.setValue(entity, value)
                return true
            }
            return false
        }
        return false
    }

    fun setString(entity: LivingEntity, scoreName: String, value: String): Boolean {
        if (entity is ServerPlayer) {
            if (scoreName.startsWith("VarString_")) {
                val objectiveName = scoreName.removePrefix("VarString_")
                val scoreData = DataStorage.get().getOrCreateScore(DataType.STRING, objectiveName)
                scoreData.setValue(entity, value)
                return true
            }
            return false
        }
        return false
    }

    fun dispatchSignal(entity: LivingEntity, signal: String, duration: Int): Boolean {
        if (entity is ServerPlayer) {
            SignalManager.dispatchSignal(signal, duration)
            return true
        }
        return false
    }

    fun parseExpression(entity: LivingEntity, expression: String): String {
        if (entity is ServerPlayer) {
            val context = Context(entity)
            return ExpressionData(expression, target = SelectorData.SELF).create(context).get(context, entity).stringValue
        }
        return ""
    }

    fun modify(entity: LivingEntity, name: String, action: String, expression: String) {
        if (entity is ServerPlayer) {
            val context = Context(entity)

            val parameter = Parameter(name, action, SelectorData.SELF, ExpressionData(expression))
            parameter.execute(context, SelectorPrepareData(parameter.selector.create(context)))
        }
    }

    fun storeLockingTarget(entity: LivingEntity) {
        if (entity is ServerPlayer) {
            val playerState = PlayerListener.getPlayerStates(entity)
            playerState.scriptStoredTarget = playerState.lockingTarget
        }
    }
}