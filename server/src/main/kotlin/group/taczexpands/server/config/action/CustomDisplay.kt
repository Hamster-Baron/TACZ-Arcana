package group.taczexpands.server.config.action

import group.taczexpands.common.entity.CustomDisplayEntity
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.CustomDisplayInstance
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

@Serializable
@SerialName("CustomDisplay")
data class CustomDisplay(
    val modelID: String = "",
    val textureID: String = "",
    val life: Int = 40,
    val animationID: String? = "",
    val animationName: String? = "",
    val animationDelay: Int = 0,
    val x: String? = null,
    val y: String? = null,
    val z: String? = null,
    val yaw: String? = null,
    val pitch: String? = null,
    val dimensionWidth: String? = null,
    val dimensionHeight: String? = null,
    val maxHealth: String? = null,
    val onDeath: ChainAction? = null,
    val deathAnimationName: String? = null,
    val entityName: String? = null,
    val immuneTable: List<String>? = null,
    val immuneExceptPlayer: Boolean = false,
    val viewers: SelectorData? = null, 
    val ambientSound: Sound? = null,
    val ambientSoundInterval: Int = 70,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = CustomDisplay()
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            val level = target.level() as ServerLevel

            val entity = CustomDisplayEntity(
                level,
                target.position(),
                target.yHeadRot,
                target.xRot,
                modelID,
                textureID,
                animationID ?: "",
                animationName ?: "",
                animationDelay,
                deathAnimationName ?: "",
                immuneTable,
                immuneExceptPlayer
            )

            if (entityName != null) {
                entity.customName = Component.literal(entityName)
            }

            CustomDisplayInstance(entity, target, context, x, y, z, yaw, pitch, life, skill, onDeath, dimensionWidth, dimensionHeight, maxHealth, viewers?.create(context), ambientSound, ambientSoundInterval)
            level.addFreshEntity(entity)
        }
    }
}