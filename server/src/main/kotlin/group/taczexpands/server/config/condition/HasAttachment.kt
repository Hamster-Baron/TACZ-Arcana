package group.taczexpands.server.config.condition

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.init.ModItems
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.item.ItemStack

@Serializable
@SerialName("HasAttachment")
data class HasAttachment(val attachments: List<String>, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = HasAttachment(listOf("tacz:.*"), true)
    }

    override fun check(context: Context): Boolean {
        return checkMatchAttachment(context.self.mainHandItem)
    }

    fun checkMatchAttachment(item: ItemStack): Boolean {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (item.item != gunItem) return false

        if (attachments.isEmpty()) return true
        return AttachmentType.entries
            .filter { it != AttachmentType.NONE }
            .map { listOf(gunItem.getAttachmentId(item, it), gunItem.getBuiltInAttachmentId(item, it)) }
            .flatten()
            .filter { it != DefaultAssets.EMPTY_ATTACHMENT_ID }
            .firstOrNull { attachments.checkContains(it.toString(), useRegex) } != null

    }
}