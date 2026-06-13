package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitBlockContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraftforge.registries.ForgeRegistries

@Serializable
@SerialName("HitBlockType")
data class HitBlockType(val blocks: List<String>) : Condition {
    companion object {
        val EXAMPLE = HitBlockType(listOf("minecraft:dirt", "#minecraft:door"))
    }

    @Transient
    val blockTagsList = blocks.filter { it.startsWith("#") }

    val blockList by lazy {
        blocks.filter { !it.startsWith("#") }.map { key ->
            val location = ResourceLocation(key)
            val registry = ForgeRegistries.BLOCKS


            if (!registry.containsKey(location)) {
                throw Exception("Unknown self stands on block type $location. ")
            }
            registry.getValue(location)!!
        }
    }

    override fun check(context: Context): Boolean {
        if (context is HitBlockContext) {
            val blockState = context.self.level().getBlockState(context.blockPos!!)
            val blockType = blockState.block
            if (blockList.contains(blockType)) return true
            blockTagsList.forEach {
                val tagKey = TagKey.create(Registries.BLOCK, ResourceLocation(it.removePrefix("#")))
                if (blockState.`is`(tagKey)) return true
            }
        }
        return false
    }

}