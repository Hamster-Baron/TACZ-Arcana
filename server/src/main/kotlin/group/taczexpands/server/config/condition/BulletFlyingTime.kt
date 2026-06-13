package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument

@Serializable
@SerialName("BulletFlyingTime")
data class BulletFlyingTime(val range: String) : Condition {
    companion object {
        val EXAMPLE = BulletFlyingTime("0..")
    }

    @Transient
    val timeRange = RangeArgument.Ints().parse(StringReader(range))

    override fun check(context: Context): Boolean {
        val tickCount = context.bullet?.tickCount ?: throw Exception("Unknown bulletflyingtime type. ")
        return timeRange.matches(tickCount)
    }
}