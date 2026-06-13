package group.taczexpands.server.util

import group.taczexpands.server.skill.Skill
import net.minecraft.server.level.ServerPlayer

class ScheduledTask(val skill: Skill?, val player: ServerPlayer?, val task: (ScheduledTask) -> Unit, var time: Int, var cancelled: Boolean = false, var tick: ((ScheduledTask) -> Unit)? = null) {
    fun tick(): Boolean {
        if (cancelled)
            return true
        if (tick == null) {
            time--
        } else {
            tick!!(this)
        }
        if (time <= 0) {
            task(this)
            if (time <= 0)
                return true
        }
        return false
    }

    fun cancel() {
        cancelled = true
    }
}