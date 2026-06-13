package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CShake
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("Shake")
data class Shake(
    val stop: Boolean = false,
    val amplitudeYRot: Float = 0.0f,
    val amplitudeXRot: Float = 0.0f,
    val durationMillis: Long = 20,
    val frequencyHz: Float = 0.5f,
    val randomness: Float = 0.1f,
    val dynamicPhaseOffset: Boolean = true,
    val phaseOffsetYRot: Float? = null,
    val phaseOffsetXRot: Float? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {

    companion object {
        val EXAMPLE = Shake()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer)
                if (stop) {
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.StopShake), target)
                } else {
                    NetworkManager.sendToPlayer(S2CShake(amplitudeYRot,
                        amplitudeXRot,
                        durationMillis,
                        frequencyHz,
                        randomness,
                        dynamicPhaseOffset,
                        phaseOffsetYRot,
                        phaseOffsetXRot), target)
                }
        }
    }
}