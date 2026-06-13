package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.common.util.CompoundTagSerializer
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.NbtPathArgument
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import kotlin.collections.all

@Serializable
@SerialName("HasItem")
data class HasItem(val items: List<ItemData>) : Condition {
    companion object {
        val EXAMPLE = HasItem(listOf(ItemData("minecraft:dirt", 1)))
    }


    @Transient
    val itemList = items.groupBy { it.itemType }.map { (item, data) -> ItemStack(item, data.sumOf { it.count }) to data }.toMap().also {
        if (it.any { (stack, data) -> data.size != 1 }) throw IllegalArgumentException("Illegal itemstacks. ")
    }

    override fun check(context: Context): Boolean {
        val inventory = context.self.inventory

        for ((stack, data) in itemList) {
            val count =
                inventory.clearOrCountMatchingItems({
                    it.`is`(stack.item) && data.all { data -> data.nbt.all { nbt -> nbt.checkItemStack(context, it) } }
                }, 0, context.self.inventoryMenu.craftSlots)
            if (stack.count > count) return false
        }
        return true

    }
}

@Serializable
@SerialName("ConsumeItem")
data class ConsumeItem(val items: List<ItemData>) : Condition {
    companion object {
        val EXAMPLE = HasItem(listOf(ItemData("minecraft:dirt", 1)))
    }

    @Transient
    val itemList = items.groupBy { it.itemType }.map { (item, data) -> ItemStack(item, data.sumOf { it.count }) to data }.toMap().also {
        if (it.any { (stack, data) -> data.size != 1 }) throw IllegalArgumentException("Illegal itemstacks. ")
    }

    override fun check(context: Context): Boolean {
        val inventory = context.self.inventory

        for ((stack, data) in itemList) {
            val count =
                inventory.clearOrCountMatchingItems({
                    it.`is`(stack.item)
                }, 0, context.self.inventoryMenu.craftSlots)
            if (stack.count > count) return false
        }

        for ((stack, data) in itemList) {
            inventory.clearOrCountMatchingItems(
                {
                    it.`is`(stack.item) && data.all { data -> data.nbt.all { nbt -> nbt.checkItemStack(context, it) } }
                },
                stack.count,
                context.self.inventoryMenu.craftSlots
            )
        }
        context.self.containerMenu.broadcastChanges()
        context.self.inventoryMenu.slotsChanged(inventory)

        return true
    }
}

@Serializable
data class ItemData(val item: String, val count: Int, val nbt: List<HasNBT> = listOf()) {
    @Transient
    val itemType by lazy {
        val location = ResourceLocation(item)
        val registry = ForgeRegistries.ITEMS

        if (!registry.containsKey(location)) {
            throw Exception("Unknown has item type $location.")
        }

        registry.getValue(location)!!
    }
}