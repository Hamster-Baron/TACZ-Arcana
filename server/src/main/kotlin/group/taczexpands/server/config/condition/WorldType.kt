package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WorldType")
data class WorldType(val names: List<String> = listOf(), val types: List<String> = listOf(), val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = WorldType()
    }

    override fun check(context: Context): Boolean {
        return names.checkContains(context.self.level().dimension().location().toString(), useRegex) ||
                types.checkContains(context.self.level().dimensionTypeId().location().toString(), useRegex)
    }

}