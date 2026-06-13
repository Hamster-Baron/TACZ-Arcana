package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CSound
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3

@Serializable
enum class SoundAction {
    @SerialName("play")
    PLAY,

    @SerialName("stop")
    STOP
}

@Serializable
@SerialName("Sound")
data class Sound(
    val action: SoundAction = SoundAction.PLAY,
    val group: String = "default",
    val name: String = "tacz:dummy",
    val sourceType: String = "PLAYERS",
    val distance: Int = 128,
    val volume: Float = 1.0f,
    val minVolume: Float = 0.0f,
    val pitch: Float = 1.0f,
    val useAudienceSource: Boolean = false,
    val sourceX: String? = null,
    val sourceY: String? = null,
    val sourceZ: String? = null,
    val mono: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {

    companion object {
        val EXAMPLE = Sound()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target as? ServerPlayer ?: return@forEach

            when (action) {
                SoundAction.PLAY -> playSound(target, skill, context, data)
                SoundAction.STOP -> {
                    NetworkManager.sendToPlayer(
                        S2CSound(action.name.lowercase(), group, "", 0, 0, 0.0f, 0.0f, true, Vec3.ZERO, -1),
                        target
                    )
                }
            }
        }
    }

    private fun playSound(target: ServerPlayer, skill: Skill, context: Context, data: PrepareData) {
        val soundSource = resolveSoundSource()
        val safeDistance = distance.coerceAtLeast(0)
        val safeVolume = volume.coerceAtLeast(0.0f)
        val safeMinVolume = minVolume.coerceIn(0.0f, 1.0f)

        val selfSource = data.selector.source?.getTarget(context) ?: context.self
        val useVariablePos = sourceX != null && sourceY != null && sourceZ != null
        val sourcePos = resolveSourcePos(target, context, useVariablePos, selfSource.position())
        val sourceEntity = if (useVariablePos) {
            -1
        } else if (useAudienceSource) {
            target.id
        } else {
            selfSource.id
        }

        if (safeMinVolume > 0.0f && safeDistance > 0) {
            val maxDistance = safeDistance * (1.0f - safeMinVolume)
            val maxDistanceSqr = maxDistance * maxDistance
            if (target.distanceToSqr(sourcePos) > maxDistanceSqr) {
                sendPacket(target, soundSource, 0, safeMinVolume, sourcePos, sourceEntity)
                return
            }
        }

        sendPacket(target, soundSource, safeDistance, safeVolume, sourcePos, sourceEntity)
    }

    private fun resolveSourcePos(target: ServerPlayer, context: Context, useVariablePos: Boolean, defaultPos: Vec3): Vec3 {
        return if (useVariablePos) {
            val x = ExpressionHelper.initExpression(sourceX!!, context, target).evaluate().numberValue.toDouble()
            val y = ExpressionHelper.initExpression(sourceY!!, context, target).evaluate().numberValue.toDouble()
            val z = ExpressionHelper.initExpression(sourceZ!!, context, target).evaluate().numberValue.toDouble()
            Vec3(x, y, z)
        } else if (useAudienceSource) {
            target.position()
        } else {
            defaultPos
        }
    }

    private fun resolveSoundSource(): SoundSource {
        return runCatching {
            SoundSource.valueOf(sourceType.uppercase())
        }.getOrElse {
            TACZExpandsCommon.LOGGER.warn(
                "[TACZExpands Sound] Invalid sourceType '{}', fallback to PLAYERS",
                sourceType
            )
            SoundSource.PLAYERS
        }
    }

    private fun sendPacket(
        target: ServerPlayer,
        soundSource: SoundSource,
        soundDistance: Int,
        soundVolume: Float,
        sourcePos: Vec3,
        sourceEntity: Int
    ) {
        NetworkManager.sendToPlayer(
            S2CSound(
                action.name.lowercase(),
                group,
                name,
                soundSource.ordinal,
                soundDistance,
                soundVolume,
                pitch,
                mono,
                sourcePos,
                sourceEntity
            ),
            target
        )
    }
}
