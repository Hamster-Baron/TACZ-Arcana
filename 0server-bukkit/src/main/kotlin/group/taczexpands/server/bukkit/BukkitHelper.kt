package group.taczexpands.server.bukkit

import com.mojang.logging.LogUtils

object BukkitHelper {
    private val LOGGER = LogUtils.getLogger()

    val hasBukkit: Boolean by lazy {
        try {
            Class.forName("org.bukkit.Bukkit")
            return@lazy true
        } catch (e: Exception) {
            LOGGER.debug("Bukkit API is unavailable", e)
        }
        return@lazy false
    }
}
