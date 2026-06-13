package group.taczexpands.server.config

import group.taczexpands.server.config.action.base.Action
import kotlinx.serialization.Serializable

@Serializable
data class ActionConfig(val name: String, val actions: List<Action>) {
}