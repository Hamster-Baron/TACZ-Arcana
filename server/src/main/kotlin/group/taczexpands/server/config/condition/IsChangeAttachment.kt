package group.taczexpands.server.config.condition

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.init.ModItems
import com.tacz.guns.item.AttachmentItem
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.AttachmentChangeContext
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.item.ItemStack


@Serializable
@SerialName("IsChangeAttachment")
data class IsChangeAttachment(val prev: String? = null, val now: String? = null, val useRegex: Boolean = false) : Condition {
    companion object {
        val EXAMPLE = IsChangeAttachment("tacz:empty")
    }

    override fun check(context: Context): Boolean {
        if(context !is AttachmentChangeContext) return false
        val prevId = context._prevAttachmentId ?: DefaultAssets.EMPTY_ATTACHMENT_ID.toString()
        val nowId = context._nowAttachmentId ?: DefaultAssets.EMPTY_ATTACHMENT_ID.toString()
        return (prev == null || prevId.checkContains(prev, useRegex)) && (now == null || nowId.checkContains(now, useRegex))
    }
}