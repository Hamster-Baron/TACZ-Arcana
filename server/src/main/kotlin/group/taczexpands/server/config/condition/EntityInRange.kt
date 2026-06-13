package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.AABB
import net.minecraftforge.registries.ForgeRegistries

@Serializable
@SerialName("EntityInRange")
data class EntityInRange(val types: List<String>? = null, val range: Float, val tags: List<String>? = null, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = EntityInRange(listOf("minecraft:.*"), 16.0f, null, true)
    }

    override fun check(context: Context): Boolean {
        val x = context.self.x
        val y = context.self.y
        val z = context.self.z
        val aabb = AABB(x - range, y - range, z - range, x + range, y + range, z + range)
        val entities = context.self.level().getEntities(context.self, aabb) {
            context.self.distanceToSqr(it) <= range * range
        }
        return entities.any {
            (types == null || types.checkContains(ForgeRegistries.ENTITY_TYPES.getKey(it.type)?.toString() ?: "",
                useRegex)) && (tags == null || it.tags.any { it in tags })
        }
    }
}
