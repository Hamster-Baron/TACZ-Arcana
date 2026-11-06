package group.taczexpands.client.api

import group.taczexpands.client.gui.VariableManager
import group.taczexpands.common.api.GunScriptAPICommon
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity

object GunScriptAPIClient {
    fun init() {
        GunScriptAPICommon.getPlayerScoreIntClientDelegate = ::getInt
        GunScriptAPICommon.getPlayerScoreFloatClientDelegate = ::getFloat
        GunScriptAPICommon.getPlayerScoreStringClientDelegate = ::getString

        GunScriptAPICommon.refreshVariableDelegate = ::refreshVariable
        GunScriptAPICommon.refreshAllVariableDelegate = ::refreshAllVariable
        GunScriptAPICommon.windowWidthClientDelegate = ::getWindowWidth
        GunScriptAPICommon.windowHeightClientDelegate = ::getWindowHeight
        GunScriptAPICommon.guiScaledWidthClientDelegate = ::getGuiScaledWidth
        GunScriptAPICommon.guiScaledHeightClientDelegate = ::getGuiScaledHeight
        GunScriptAPICommon.guiScaleClientDelegate = ::getGuiScale
    }

    fun getInt(entity: LivingEntity, scoreName: String): Int {
        if (entity != null && entity == Minecraft.getInstance().player) {
            return VariableManager.processString(scoreName).toIntOrNull() ?: 0
        }
        return 0
    }

    fun getFloat(entity: LivingEntity, scoreName: String): Float {
        if (entity != null && entity == Minecraft.getInstance().player) {
            return VariableManager.processString(scoreName).toFloatOrNull() ?: 0.0f
        }
        return 0.0f
    }

    fun getString(entity: LivingEntity, scoreName: String): String {
        if (entity != null && entity == Minecraft.getInstance().player) {
            return VariableManager.processString(scoreName)
        }
        return ""
    }

    fun refreshVariable(entity: LivingEntity, variable: String) {
        if (entity != null && entity == Minecraft.getInstance().player) {
            VariableManager.invalidate(variable)
        }
    }

    fun refreshAllVariable(entity: LivingEntity) {
        if (entity != null && entity == Minecraft.getInstance().player) {
            VariableManager.invalidateAll()
        }
    }

    fun getWindowWidth(): Int = Minecraft.getInstance().window.width

    fun getWindowHeight(): Int = Minecraft.getInstance().window.height

    fun getGuiScaledWidth(): Int = Minecraft.getInstance().window.guiScaledWidth

    fun getGuiScaledHeight(): Int = Minecraft.getInstance().window.guiScaledHeight

    fun getGuiScale(): Double = Minecraft.getInstance().window.guiScale
}
