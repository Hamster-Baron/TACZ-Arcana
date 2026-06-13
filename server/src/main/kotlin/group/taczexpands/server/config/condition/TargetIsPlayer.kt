package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitEntityContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.player.Player

@Serializable
@SerialName("TargetIsPlayer")
data class TargetIsPlayer(val `is`: Boolean) : Condition {
    companion object {
        val EXAMPLE = TargetIsPlayer(true)
    }

    override fun check(context: Context): Boolean {
        if (context !is HitEntityContext) {
            throw Exception("Trigger type has no target param. ")
        }
        if (`is` && context.target is Player) return true
        if (!`is` && context.target !is Player) return true
        return false
    }
}