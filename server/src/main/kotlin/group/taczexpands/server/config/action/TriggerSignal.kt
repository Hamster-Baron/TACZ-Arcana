package group.taczexpands.server.config.action

import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.SignalManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("TriggerSignal")
data class TriggerSignal(
    val signal: String,
    val duration: Int = 20,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = TriggerSignal("name")
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        SignalManager.dispatchSignal(signal, duration)
    }
}