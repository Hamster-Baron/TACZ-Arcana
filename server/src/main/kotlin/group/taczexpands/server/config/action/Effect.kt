package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.util.schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.registries.ForgeRegistries


@Serializable
enum class EffectAction {
    @SerialName("give")
    GIVE,

    @SerialName("clear")
    CLEAR
}

@Serializable
@SerialName("Effect")
data class Effect(
    val action: EffectAction = EffectAction.GIVE,
    val type: String? = null,
    val selector: SelectorData? = null,
    val level: Int = 0,
    val time: Int = 0,
    val showParticle: Boolean = true,
    val showUI: Boolean = true,
    val showIcon: Boolean = true,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Effect()
    }

    val effectType: MobEffect? by lazy {
        if (type == null) null
        else ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation(type))
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val targets = data.selector.getTargets(context)
        targets.forEach { target ->
            if (target !is LivingEntity) return@forEach
            if (action == EffectAction.GIVE) {
                if (effectType != null) {
                    target.addEffect(
                        MobEffectInstance(
                            effectType!!,
                            time,
                            level,
                            showParticle,
                            showUI,
                            showIcon
                        )
                    )
                }
            } else if (action == EffectAction.CLEAR) {
                val invokeClear = {
                    if (effectType != null) {
                        target.removeEffect(effectType!!)
                    } else {
                        target.removeAllEffects()
                    }
                }

                if (time > 0) {
                    schedule(skill, context.self, time, {}, {
                        invokeClear()
                        it.time--
                    })
                } else {
                    invokeClear()
                }
            }
        }
    }
}