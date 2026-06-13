package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

@Serializable
@SerialName("Frostbite")
data class Frostbite(
    val time: Int,
    val cancelWalk: Boolean = true,
    val cancelJump: Boolean = true,
    val cancelRotation: Boolean = true,
    val showOverlay: Boolean = true,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Frostbite(5)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach
            val identifier = UUID.randomUUID().toString()
            if (cancelWalk)
                ActionManager.cancelAction(target, S2CCancelAction.Action.Walk, time, identifier)
            if (cancelJump)
                ActionManager.cancelAction(target, S2CCancelAction.Action.Jump, time, identifier)
            if (cancelRotation)
                ActionManager.cancelAction(target, S2CCancelAction.Action.Rotate, time, identifier)
            if (showOverlay)
                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.Frostbite, time), target)
        }
    }
}
