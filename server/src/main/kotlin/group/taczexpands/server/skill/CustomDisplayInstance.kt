package group.taczexpands.server.skill

import com.ezylang.evalex.Expression
import group.taczexpands.common.entity.CustomDisplayEntity
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.SelectorInstance
import group.taczexpands.server.config.action.ChainAction
import group.taczexpands.server.config.action.Sound
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt


class CustomDisplayInstance(
    val entity: CustomDisplayEntity,
    val target: Entity,
    val context: Context,
    val x: String?,
    val y: String?,
    val z: String?,
    val yaw: String?,
    val pitch: String?,
    val life: Int,
    val skill: Skill,
    val onDeath: ChainAction?,
    dimensionWidth: String?,
    dimensionHeight: String?,
    maxHealth: String?,
    val viewers: SelectorInstance?,
    val ambientSound: Sound?,
    val ambientSoundInterval: Int,
) {

    init {
        val pos = if (x != null && y != null && z != null) {
            Vec3(
                x.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toDouble() },
                y.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toDouble() },
                z.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toDouble() })
        } else target.position()

        val yawValue =
            yaw?.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toFloat() } ?: target.yHeadRot
        val pitchValue =
            pitch?.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toFloat() } ?: target.xRot
        entity.setPos(pos)
        entity.yRot = yawValue
        entity.xRot = pitchValue


        val dimensionWidth =
            dimensionWidth?.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toFloat() }
        val dimensionHeight =
            dimensionHeight?.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toFloat() }
        val maxHealth = maxHealth?.let { ExpressionHelper.initExpression(it, context, target, null, ::processExpression).evaluate().numberValue.toDouble() }

        entity.renderData = entity.renderData.apply {
            if (dimensionWidth != null && dimensionHeight != null) {
                putFloat("dimensionWidth", dimensionWidth)
                putFloat("dimensionHeight", dimensionHeight)
            }

            if (maxHealth != null) {
                putDouble("maxHealth", maxHealth)
                entity.getAttribute(Attributes.MAX_HEALTH)?.baseValue = maxHealth
                entity.health = maxHealth.toFloat()
                entity.isInvulnerable = false
            }
        }
    }

    fun isBroadcastTo(player: Player): Boolean? {
        if (viewers == null) {
            return null
        }

        if (viewers.getTargets(context).contains(player)) {
            return true
        } else {
            return false
        }
    }

    fun processExpression(expression: Expression): Expression {
        expression.undefinedVariables.forEach { variable ->
            when (variable) {
                "VarDisplayPos", "DisplayPos" -> {
                    val pos = entity.position()
                    expression.with(variable, arrayOf(pos.x, pos.y, pos.z, entity.yRot.toDouble(), entity.xRot.toDouble()))
                }

                "VarLookTargetEyeRot", "LookTargetEyeRot" -> {
                    expression.with(variable, getLookAtAngles(target.eyePosition))
                }

                "VarLookSelfEyeRot", "LookSelfEyeRot" -> {
                    expression.with(variable, getLookAtAngles(context.self.eyePosition))
                }

                "VarLookTargetRot", "LookTargetRot" -> {
                    expression.with(variable, getLookAtAngles(target.position()))
                }

                "VarLookSelfRot", "LookSelfRot" -> {
                    expression.with(variable, getLookAtAngles(context.self.position()))
                }

                "VarTickCount", "TickCount" -> {
                    expression.with(variable, entity.tickCount)
                }
            }
        }
        return expression
    }

    fun getLookAtAngles(facePos: Vec3): Array<Double> {
        val sourcePos = entity.position()

        val dx = facePos.x - sourcePos.x
        val dy = facePos.y - sourcePos.y - 0.5
        val dz = facePos.z - sourcePos.z

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Mth.wrapDegrees(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG - 90.0)
        val pitch = Mth.wrapDegrees(-Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG)

        return arrayOf(yaw, pitch)
    }

    fun getExpressionValue(expressionStr: String?, default: Double): Double {
        if (expressionStr != null) {
            try {
                return ExpressionHelper.initExpression(expressionStr, context, target, null, ::processExpression)
                    .evaluate().numberValue.toDouble()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return default
    }

    fun getExpressionValue(expressionStr: String?, default: Float): Float {
        if (expressionStr != null) {
            try {
                return ExpressionHelper.initExpression(expressionStr, context, target, null, ::processExpression)
                    .evaluate().numberValue.toFloat()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return default
    }

    init {
        CustomDisplayManager.instances.put(entity, this)
    }

    fun tick() {
        if (life >= 0) {
            if (entity.tickCount > life) {
                entity.discard()
                CustomDisplayManager.instances.remove(entity)
                return
            }
        }

        val xPos = getExpressionValue(x, entity.x)
        val yPos = getExpressionValue(y, entity.y)
        val zPos = getExpressionValue(z, entity.z)


        val yawValue = getExpressionValue(yaw, entity.yRot)
        val pitchValue = getExpressionValue(pitch, entity.xRot)

        entity.moveTo(xPos, yPos, zPos, yawValue, pitchValue)

        if(ambientSound != null && entity.tickCount % ambientSoundInterval == 0) {
            ambientSound.perform(skill, context)
        }
    }

    private var triggeredOnDeath = false

    fun onDeath() {
        if (!triggeredOnDeath) {
            triggeredOnDeath = true
            onDeath?.perform(skill, context)
        }
    }
}