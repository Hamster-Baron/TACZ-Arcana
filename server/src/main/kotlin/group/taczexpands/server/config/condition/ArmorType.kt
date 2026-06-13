package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraftforge.registries.ForgeRegistries


@Serializable
@SerialName("ArmorType")
data class ArmorType(val slot: Int, val types: List<String>, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = ArmorType(0, listOf("minecraft:.*"), true)
    }

    override fun check(context: Context): Boolean {

        val armors = context.self.inventory.armor
        if (slot !in armors.indices) return false
        val key = ForgeRegistries.ITEMS.getKey(armors[slot].item) ?: return false
        if (!types.checkContains(key.toString(), useRegex)) return false
        return true
    }
}