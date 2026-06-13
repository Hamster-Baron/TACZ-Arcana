package group.taczexpands.server.config

import group.taczexpands.server.config.condition.base.Condition
import kotlinx.serialization.Serializable

@Serializable
data class ConditionConfig(val name: String, val conditions: List<Condition>) {
}