package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CFlash
import group.taczexpands.server.accessor.IAccessorLivingEntity
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
import net.minecraft.world.entity.Mob
import net.minecraftforge.server.ServerLifecycleHooks

@Serializable
@SerialName("FlashEmitter")
data class FlashEmitter(
    val duration: Int = 20 * 5,
    val fadeIn: Int = 4,
    val fadeOut: Int = 20,
    val strength: Int = 100,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = FlashEmitter()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer) {
                NetworkManager.sendToPlayer(S2CFlash(duration * 50.toLong(),
                    fadeIn * 50.toLong(),
                    fadeOut * 50.toLong(), strength), target)
            } else if (target is Mob) {
                target.target = null
                target.sensing.tick()
                (target as IAccessorLivingEntity).`taczexpands$setBlindTime`(duration)
            }
        }

    }

}