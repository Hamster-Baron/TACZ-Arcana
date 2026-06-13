package group.taczexpands.client.compat

import net.minecraftforge.fml.ModList
import kotlin.jvm.optionals.getOrNull

object CompatHelper {
    fun hasIris(): Boolean {
        if (ModList.get().isLoaded("oculus"))
            return true
        return false
    }

    fun hasTACZTweaksV3(): Boolean {
        val modList = ModList.get()
        if (!modList.isLoaded("tacztweaks")) return false

        val container = modList.getModContainerById("tacztweaks").getOrNull() ?: return false
        if (container.modInfo.version.majorVersion < 3) return false

        return true
    }
}