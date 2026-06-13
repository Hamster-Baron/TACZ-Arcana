package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CSendVariable
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("RenderUtil")
data class RenderUtil(
    val name: String? = null,
    val visible: Boolean = true,
    val volatile: Boolean? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = RenderUtil("name")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer) {
                if (name != null) {
                    NetworkManager.sendToPlayer(S2CSendVariable(S2CSendVariable.Type.UTIL_API, name, if (visible) "true" else "false"), target)
                }

                if (volatile != null) {
                    NetworkManager.sendToPlayer(S2CSendVariable(S2CSendVariable.Type.VOLATILE, "render_util", if (volatile) "true" else "false"), target)
                }
            }
        }
    }
}