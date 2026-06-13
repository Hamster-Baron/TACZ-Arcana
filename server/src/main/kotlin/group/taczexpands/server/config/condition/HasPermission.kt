package group.taczexpands.server.config.condition

import group.taczexpands.server.bukkit.BukkitForge
import group.taczexpands.server.bukkit.BukkitHelper
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("HasPermission")
data class HasPermission(val node: String) : Condition {
    companion object {
        val EXAMPLE = HasPermission("taczexpands.test")
    }

    override fun check(context: Context): Boolean {
        if (BukkitHelper.hasBukkit) {
            return BukkitForge.hasPermission(context.self, node)
        }
        return false
    }
}
