package group.taczexpands.server.config.action

import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.builder.AttachmentItemBuilder
import group.taczexpands.common.nbt.AttachmentExtras
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.util.GunManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

@Serializable
@SerialName("SetTexture")
data class SetTexture(
    val id: String? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {

    companion object {
        val EXAMPLE = SetTexture( "example:textures/gun.png")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach

            val mainHand = target.mainHandItem
            val iGun = IGun.getIGunOrNull(mainHand) ?: return@forEach

            GunExtras.setOverrideTexture(mainHand, id)
        }
    }
}