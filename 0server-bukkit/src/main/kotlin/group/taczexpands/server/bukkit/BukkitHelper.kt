package group.taczexpands.server.bukkit

object BukkitHelper {
    val hasBukkit: Boolean by lazy {
        try {
            Class.forName("org.bukkit.Bukkit")
            return@lazy true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@lazy false
    }
}