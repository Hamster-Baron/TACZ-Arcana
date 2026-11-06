package group.taczexpands.server.bukkit.mythicmobs

import com.mojang.logging.LogUtils
import org.bukkit.Bukkit

object MythicMobsHelper {
    private val LOGGER = LogUtils.getLogger()

    val hasMythicMobs: Boolean by lazy {
        try {
            Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
        } catch (e: Exception) {
            LOGGER.debug("MythicMobs is unavailable", e)
            false
        }
    }
}
