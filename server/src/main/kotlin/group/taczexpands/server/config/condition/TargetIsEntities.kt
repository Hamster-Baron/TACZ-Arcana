package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitEntityContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraftforge.registries.ForgeRegistries

@Serializable
@SerialName("TargetIsEntities")
data class TargetIsEntities(val entities: List<String>) : Condition {
    companion object {
        val EXAMPLE = TargetIsEntities(listOf("minecraft:zombie"))
    }

    @Transient
    val entityTagsList = entities.filter { it.startsWith("#") }

    val entityTypeList: List<EntityType<*>> by lazy {
        entities.filter { !it.startsWith("#") }.map { key ->
            val location = ResourceLocation(key)
            val registry = ForgeRegistries.ENTITY_TYPES

            if (!registry.containsKey(location)) {
                throw Exception("Unknown target entity type $location. ")
            }
            registry.getValue(location)!!
        }.toList()
    }

    override fun check(context: Context): Boolean {
        if (context !is HitEntityContext) {
            throw Exception("Trigger type has no target param. ")
        }

        entityTagsList.forEach {
            val tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation(it.removePrefix("#")))
            if (context.target!!.type.`is`(tagKey)) return true
        }

        return entityTypeList.contains(context.target!!.type)
    }
}