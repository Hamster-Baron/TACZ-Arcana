package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.SignalManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument

@Serializable
@SerialName("SignalState")
data class SignalState(val signal: String, val range: String) : Condition {
    companion object {
        val EXAMPLE = SignalState("name", "0..")
    }

    @Transient
    val timeRange = RangeArgument.Ints().parse(StringReader(range))

    override fun check(context: Context): Boolean {
        val duration = SignalManager.getSignalLife(signal)
        return timeRange.matches(duration)
    }
}