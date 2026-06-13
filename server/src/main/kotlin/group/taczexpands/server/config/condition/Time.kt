package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument

@Serializable
@SerialName("Time")
data class Time(val range: String) : Condition {
    companion object {
        val EXAMPLE = Time("0..")
    }

    @Transient
    val timeRange = RangeArgument.Ints().parse(StringReader(range))

    override fun check(context: Context): Boolean {
        return timeRange.matches(context.self.level().dayTime.toInt())
    }
}