package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.bullet.LockingTargetResolver
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("SetLockingTarget")
data class SetLockingTarget(
    val fromRawSimpleMode: Boolean = false,
    val fromSimpleMode: Boolean = false,
    val target: SelectorData? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = SetLockingTarget()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(listOf(SelectorPrepareData(selector.create(context)), SelectorPrepareData(target.create(context))))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { player ->
            if (player !is ServerPlayer) return@forEach
            val lockingTarget = if (fromRawSimpleMode)
                LockingTargetResolver.resolve(player, LockingTargetResolver.Mode.RAW)
            else if (fromSimpleMode)
                LockingTargetResolver.resolve(player, LockingTargetResolver.Mode.SIMPLE)
            else data.dataList[1].selector.getTarget(context)
            PlayerListener.getPlayerStates(player).skillLockingTarget = lockingTarget
        }
    }
}
