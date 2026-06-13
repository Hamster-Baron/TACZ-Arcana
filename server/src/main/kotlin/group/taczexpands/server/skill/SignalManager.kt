package group.taczexpands.server.skill

import group.taczexpands.server.context.SignalContext
import group.taczexpands.server.skill.TriggerType
import net.minecraftforge.server.ServerLifecycleHooks

object SignalManager {
    private val SIGNAL_MAP = mutableMapOf<String, Int>()
    fun dispatchSignal(signal: String, duration: Int) {
        if (duration >= 0) {
            val expireTick = ServerLifecycleHooks.getCurrentServer().tickCount + duration
            SIGNAL_MAP[signal] = expireTick
        }

        try {
            ServerLifecycleHooks.getCurrentServer().playerList.players.forEach {
                (0..it.inventory.containerSize - 1).forEach { index ->
                    SkillManager.trigger(TriggerType.ON_SIGNAL, SignalContext(it, signal), index)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSignalActive(signal: String): Boolean {
        val expireTick = SIGNAL_MAP[signal] ?: return false
        return ServerLifecycleHooks.getCurrentServer().tickCount <= expireTick
    }

    fun getSignalLife(signal: String): Int {
        val expireTick = SIGNAL_MAP[signal] ?: return -1
        return expireTick - ServerLifecycleHooks.getCurrentServer().tickCount
    }

    fun onServerTick() {
        val currentTick = ServerLifecycleHooks.getCurrentServer().tickCount
        SIGNAL_MAP.entries.removeIf { entry ->
            entry.value <= currentTick
        }
    }


}