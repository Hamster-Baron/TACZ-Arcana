package group.taczexpands.server.compat

import net.minecraftforge.fml.ModList
import kotlin.jvm.optionals.getOrNull

object CompatHelper {
    fun hasTACZAddon(): Boolean {
        return ModList.get().isLoaded("taczaddon")
    }

    fun hasSuperbWarfare(): Boolean {
        return ModList.get().isLoaded("superbwarfare")
    }

    fun hasTACZTweaks(): Boolean {
        val modList = ModList.get()
        if (!modList.isLoaded("tacztweaks")) return false

        val container = modList.getModContainerById("tacztweaks").getOrNull() ?: return false
        if (container.modInfo.version.majorVersion >= 3) return false

        return true
    }
}