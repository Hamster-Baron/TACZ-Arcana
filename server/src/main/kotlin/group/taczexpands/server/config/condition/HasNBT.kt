package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.arguments.NbtPathArgument
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack

@Serializable
@SerialName("HasNBT")
data class HasNBT(val path: String = "{}", val data: String, val pathIsExpression: Boolean = false, val dataIsExpression: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = HasNBT("GunId", "tacz:example")
    }

    override fun check(context: Context): Boolean {
        val mainHand = context.self.mainHandItem
        return checkItemStack(context, mainHand)
    }

    fun checkItemStack(context: Context, stack: ItemStack): Boolean {
        val rootTag = stack.orCreateTag
        val realPath = if (pathIsExpression) ExpressionHelper.initExpression(path, context, context.self).evaluate().stringValue else path


        val nbtPath = NbtPathArgument.nbtPath().parse(StringReader(realPath))
        val sources = nbtPath.get(rootTag)

        val realData = if (dataIsExpression) ExpressionHelper.initExpression(this.data, context, context.self).evaluate().stringValue else this.data
        val target = TagParser(StringReader(realData)).readValue()

        return sources.all { it == target }
    }
}