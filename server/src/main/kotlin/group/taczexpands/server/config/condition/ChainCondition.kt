package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.DefinitionScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("Chain")
data class ChainCondition(
    val conditions: List<Condition>? = null,
    val from: String? = null,
) : Condition {
    companion object {
        val EXAMPLE = ChainCondition(listOf())
    }

    @Transient
    private val definitionScope = DefinitionScope.current

    override fun check(context: Context): Boolean {
        if (conditions != null) {
            for (condition in conditions) {
                if (!condition.check(context)) return false
            }
        }
        if (from != null) {
            val fromConditions = SkillManager.resolveChainCondition(from, definitionScope)?.conditions
            if (fromConditions != null) {
                for (condition in fromConditions) {
                    if (!condition.check(context)) return false
                }
            }
        }
        return true
    }
}
