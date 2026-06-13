package group.taczexpands.server.bukkit.placeholderapi

import group.taczexpands.server.bukkit.toBukkit
import me.clip.placeholderapi.PlaceholderAPI

object PlaceholderAPIForge {
    fun setPlaceholders(player: net.minecraft.world.entity.player.Player, message: String): String {
        return PlaceholderAPI.setPlaceholders(player.toBukkit() as org.bukkit.entity.Player, message)
    }
}