package group.taczexpands.server.config.condition

import com.mojang.brigadier.StringReader
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.commands.arguments.RangeArgument.Ints
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

@Serializable
@SerialName("HasEffect")
data class HasEffect(val effects: List<EffectData>) : Condition {
    companion object {
        val EXAMPLE = HasEffect(listOf(EffectData("minecraft:saturation", "0..")))
    }

    override fun check(context: Context): Boolean {
        for (effectData in effects) {
            if (!effectData.check(context)) return false
        }
        return true

    }
}

@Serializable
data class EffectData(val effect: String, @SerialName("timeRange") val range: String) {
    @Transient
    val timeRange = Ints().parse(StringReader(range))

    @Transient
    val effectType by lazy {
        val location = ResourceLocation(effect)
        val registry = ForgeRegistries.MOB_EFFECTS
        if (!registry.containsKey(location)) {
            throw Exception("Unknown effect type $effect.")
        }

        registry.getValue(location)!!
    }

    fun check(context: Context): Boolean {
        if (!context.self.hasEffect(effectType)) return false
        val effect = context.self.getEffect(effectType)!!
        return timeRange.matches(effect.duration)
    }
}