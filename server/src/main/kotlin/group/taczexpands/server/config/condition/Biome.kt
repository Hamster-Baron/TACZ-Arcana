package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries
import kotlin.jvm.optionals.getOrNull

@Serializable
@SerialName("Biome")
data class Biome(val biomes: List<String>, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = Biome(listOf("minecraft:.*"), true)
    }


    override fun check(context: Context): Boolean {
        val level = context.self.level()
        return biomes.checkContains(level.getBiome(context.self.blockPosition().below()).unwrapKey().getOrNull()?.location()?.toString() ?: "", useRegex)
    }
}