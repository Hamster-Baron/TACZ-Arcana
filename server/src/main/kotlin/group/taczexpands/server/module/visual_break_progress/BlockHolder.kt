package group.taczexpands.server.module.visual_break_progress

import net.minecraftforge.server.ServerLifecycleHooks

class BlockHolder() {
    var lastUpdated: Int = 0
    var progress: Float = 0.0f
        set(value) {
            lastUpdated = ServerLifecycleHooks.getCurrentServer().tickCount
            field = value
        }

    fun getStage(): Int {
        if (progress !in 0.0f..<1.0f) {
            return -1
        }
        return (progress * 10).toInt().coerceIn(0, 9)
    }
}