package group.taczexpands.server.config.action

import group.taczexpands.server.bukkit.BukkitHelper
import group.taczexpands.server.bukkit.mythicmobs.MythicMobsForge
import group.taczexpands.server.bukkit.mythicmobs.MythicMobsHelper
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("MythicMobsSkill")
data class MythicMobsSkill(
    val name: String,
    val caster: SelectorData? = null,
    val parameters: Map<String, String>? = null,
    val args: List<String>? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = MythicMobsSkill("skill")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(caster.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { caster ->
            if (BukkitHelper.hasBukkit && MythicMobsHelper.hasMythicMobs) {
                MythicMobsForge.callSkill(name,
                    caster,
                    context.self,
                    parameters?.mapNotNull {
                        val key = ExpressionHelper.parse(it.key, args, context, caster) ?: return@mapNotNull null
                        val value = ExpressionHelper.parse(it.value, args, context, caster) ?: return@mapNotNull null
                        key to value
                    }?.toMap(mutableMapOf())
                )
            }
        }
    }
}