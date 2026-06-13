package group.taczexpands.server.config.action

import com.tacz.guns.init.ModDamageTypes
import group.taczexpands.server.accessor.IAccessorEntity
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.create
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.util.getHealth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageSource
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

@Serializable
enum class HurtModifier {
    @SerialName("value")
    VALUE,

    @SerialName("percentage")
    PERCENTAGE
}

@Serializable
@SerialName("Hurt")
data class Hurt(
    val type: String,
    val modifier: HurtModifier = HurtModifier.VALUE,
    val amount: ExpressionData = ExpressionData("1.0"),
    val cooldownGroup: String? = null,
    val cooldown: Int = 0,
    val indirectFrom: SelectorData? = null,
    val directFrom: SelectorData? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Hurt("minecraft:damagetype")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(
            listOf(
                SelectorPrepareData(selector.create(context)),
                SelectorPrepareData(directFrom.create(context)),
                SelectorPrepareData(indirectFrom.create(context)),
                amount.create(context).toData()
            )
        )
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            if (cooldownGroup != null) {
                val cooldownGroups = (target as IAccessorEntity).`taczexpands$getHurtCooldownGroups`()
                val lastHurt = cooldownGroups[cooldownGroup] ?: 0
                val currentHurt = target.tickCount
                if (currentHurt - lastHurt < cooldown) return@forEach
                cooldownGroups[cooldownGroup] = currentHurt
            }
            val damageTypes = target.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            val damageType = damageTypes.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation(type))).getOrNull()
                ?: damageTypes.getHolder(ModDamageTypes.BULLET).get()
            val damageSource = DamageSource(damageType, data.dataList[1].selector.getTarget(context), data.dataList[2].selector.getTarget(context))
            val parsedAmount = data.dataList[3].expression.get(context, target).numberValue.toFloat()
            val damageAmount = if (modifier == HurtModifier.VALUE) parsedAmount else max(0.0f, parsedAmount * target.getHealth())
            target.hurt(damageSource, damageAmount)
        }
    }
}