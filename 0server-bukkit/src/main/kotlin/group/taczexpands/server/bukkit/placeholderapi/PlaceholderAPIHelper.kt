package group.taczexpands.server.bukkit.placeholderapi

import org.bukkit.Bukkit

object PlaceholderAPIHelper {
    val hasPlaceholderAPI: Boolean by lazy {
        try {
            return@lazy Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@lazy false
    }
}