package group.taczexpands.server.bukkit.mythicmobs

import org.bukkit.Bukkit

object MythicMobsHelper {
    val hasMythicMobs: Boolean by lazy {
        try {
            return@lazy Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@lazy false
    }
}