package group.taczexpands.server.config.action

import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.accessor.IAccessorEntity
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.util.LOGGER
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraftforge.registries.ForgeRegistries

@Serializable
@SerialName("Summon")
data class Summon(
    val type: String,
    val selector: SelectorData? = null,
    val x: String,
    val y: String,
    val z: String,
    val nbt: String? = null,
    val args: List<String>? = null,
    val setOwner: Boolean = true,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Summon("minecraft:zombie", x = "", y = "", z = "", nbt = "{}")
    }


    val entityType by lazy {
        ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation(type)) ?: throw Exception("Unknown entity type $type")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val targets = data.selector.getTargets(context)
        targets.forEach { target ->
            val level = target.level() as? ServerLevel ?: return@forEach
            val entity = entityType.create(level) ?: return@forEach
            val x = ExpressionHelper.parse(x, args, context, target)?.toDoubleOrNull() ?: 0.0
            val y = ExpressionHelper.parse(y, args, context, target)?.toDoubleOrNull() ?: 0.0
            val z = ExpressionHelper.parse(z, args, context, target)?.toDoubleOrNull() ?: 0.0

            if (nbt != null) {
                val tag = CompoundTag()
                entity.saveWithoutId(tag)

                val newTag = try {
                    TagParser.parseTag(nbt)
                } catch (e: Exception) {
                    LOGGER.warn("Tag parsing error", e)
                    null
                }
                if (newTag != null) {
                    tag.merge(newTag)

                }

                entity.load(tag)
            }
            entity.addTag("TACZOwner: ${context.self.uuid.toString()}")

            if (setOwner) {
                (entity as IAccessorEntity).`taczexpands$setOwner`(target)
            }
            entity.moveTo(x, y, z)
            level.addFreshEntity(entity)
        }
    }
}