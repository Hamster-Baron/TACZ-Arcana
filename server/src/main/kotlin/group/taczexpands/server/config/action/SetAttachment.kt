package group.taczexpands.server.config.action

import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.builder.AttachmentItemBuilder
import group.taczexpands.common.nbt.AttachmentExtras
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
@SerialName("SetAttachment")
data class SetAttachment(
    val slot: Slot,
    val id: String? = null,
    val remove: Boolean = false,
    val enforce: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {

    @Serializable
    enum class Slot(val type: AttachmentType) {
        @SerialName("scope")
        SCOPE(AttachmentType.SCOPE),

        @SerialName("muzzle")
        MUZZLE(AttachmentType.MUZZLE),

        @SerialName("stock")
        STOCK(AttachmentType.STOCK),

        @SerialName("grip")
        GRIP(AttachmentType.GRIP),

        @SerialName("laser")
        LASER(AttachmentType.LASER),

        @SerialName("extended_mag")
        EXTENDED_MAG(AttachmentType.EXTENDED_MAG),

        @SerialName("module")
        MODULE(AttachmentType.valueOf("MODULE"))
    }

    companion object {
        val EXAMPLE = SetAttachment(Slot.GRIP, "example:grip_id")
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            if (target !is ServerPlayer) return@forEach

            val mainHand = target.mainHandItem
            val iGun = IGun.getIGunOrNull(mainHand) ?: return@forEach
            if (!remove && id != null) {
                val attachmentItem = AttachmentItemBuilder.create().setId(ResourceLocation(id)).setCount(1).build()
                AttachmentExtras.setIsGenerated(attachmentItem, true)

                GunManager.refitAttachment(slot.type, attachmentItem, null, mainHand, target, enforce, target.inventory)
            } else {
                GunManager.unloadAttachment(slot.type, mainHand, target, target.inventory)
            }

        }
    }
}