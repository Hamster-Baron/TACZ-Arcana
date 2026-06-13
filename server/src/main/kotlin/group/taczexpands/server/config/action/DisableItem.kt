package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.create
import group.taczexpands.server.module.gun_shield.GunShieldManager
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer


@Serializable
@SerialName("DisableItem")
data class DisableItem(
    val time: ExpressionData = ExpressionData("100"),
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = DisableItem()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), time.create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            if (target is ServerPlayer) {
                target.cooldowns.addCooldown(target.mainHandItem.item, data.dataList[1].expression.get(context, target).numberValue.toInt())

                if (GunShieldManager.getShield(target) != null) {
                    SkillManager.trigger(TriggerType.ON_SHIELD_DISABLED, Context(target))
                }
            }
        }
    }
}