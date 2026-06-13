package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraftforge.event.entity.living.LivingHurtEvent

@Serializable
@SerialName("DamageType")
data class DamageType(val types: List<String>, val useRegex: Boolean = false) : Condition {

    companion object {
        val EXAMPLE = DamageType(listOf("minecraft:.*"), true)
    }

    override fun check(context: Context): Boolean {
        val hurtEvent = context.from as? LivingHurtEvent ?: return false
        return types.checkContains(hurtEvent.source.typeHolder().unwrapKey().orElse(null)?.location()?.toString() ?: "", useRegex)
    }
}
