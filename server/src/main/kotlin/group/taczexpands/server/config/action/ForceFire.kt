package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("ForceFire")
data class ForceFire(
    val mode: Mode = Mode.SHOOT,
    val triggerAnimation: Boolean = true,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    @Serializable
    enum class Mode {
        @SerialName("shoot")
        SHOOT,

        @SerialName("melee_attack")
        MELEE_ATTACK
    }

    companion object {
        val EXAMPLE = ForceFire()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target as? ServerPlayer ?: return@forEach

            if(mode == Mode.SHOOT) {
                PlayerListener.getPlayerStates(target).forceShoot = true
                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.ForceShoot, if (triggerAnimation) 0 else 1), target)
            } else {
                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.MeleeAttack, if (triggerAnimation) 0 else 1), target)
            }
        }
    }
}
