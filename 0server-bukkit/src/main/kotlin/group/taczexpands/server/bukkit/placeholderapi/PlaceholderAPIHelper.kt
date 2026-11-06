package group.taczexpands.server.bukkit.placeholderapi

import com.mojang.logging.LogUtils
import org.bukkit.Bukkit

object PlaceholderAPIHelper {
    private val LOGGER = LogUtils.getLogger()

    val hasPlaceholderAPI: Boolean by lazy {
        try {
            Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
        } catch (e: Exception) {
            LOGGER.debug("PlaceholderAPI is unavailable", e)
            false
        }
    }
}
