package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CAnimation
import group.taczexpands.common.network.s2c.S2CSendVariable
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
enum class PlayMode {
    @SerialName("once_hold")
    ONCE_HOLD,

    @SerialName("once_stop")
    ONCE_STOP,

    @SerialName("loop")
    LOOP
}

@Serializable
@SerialName("Animation")
data class Animation(
    val action: S2CAnimation.Action? = null,
    val track: Int = 0,
    val name: String = "",
    val to: String = "",
    val blending: Boolean = true,
    val playMode: PlayMode = PlayMode.ONCE_STOP,
    val transitionTime: Float = 0.0f,
    val selector: SelectorData? = null,
    val volatile: Boolean? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Animation()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target as? ServerPlayer ?: return@forEach

            if (action != null) {
                NetworkManager.sendToPlayer(
                    S2CAnimation(action, track, name, to, blending, playMode.ordinal, transitionTime),
                    context.self
                )
            }

            if (volatile != null) {
                NetworkManager.sendToPlayer(S2CSendVariable(S2CSendVariable.Type.VOLATILE, "animation_redirect", if (volatile) "true" else "false"), target)
            }
        }
    }
}