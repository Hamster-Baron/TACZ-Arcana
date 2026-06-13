package group.taczexpands.common.nbt

import com.tacz.guns.resource.CommonAssetsManager
import com.tacz.guns.resource.index.CommonAttachmentIndex
import com.tacz.guns.resource.network.CommonNetworkCache
import group.taczexpands.common.data.GunExtraAmmo
import group.taczexpands.common.mixin.accessor.IAccessorCommonAssetsManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object AttachmentExtras {
    val ROOT = "Extras"

    val IS_GENERATED = "IsGenerated"

    fun getOrCreateRootTag(attachment: ItemStack): CompoundTag {
        val nbt = attachment.orCreateTag
        if (!nbt.contains(ROOT)) {
            val root = CompoundTag()
            nbt.put(ROOT, root)
            return root
        }
        return nbt.getCompound(ROOT)!!
    }

    fun getRootTag(attachment: ItemStack): CompoundTag? {
        val nbt = attachment.orCreateTag
        if (!nbt.contains(ROOT)) {
            return null
        }
        return nbt.getCompound(ROOT)
    }

    fun getIsGenerated(attachment: ItemStack): Boolean {
        val tag = getRootTag(attachment) ?: return false
        if (!tag.contains(IS_GENERATED)) return false
        return tag.getBoolean(IS_GENERATED)
    }

    fun setIsGenerated(attachment: ItemStack, value: Boolean) {
        val tag = getOrCreateRootTag(attachment)
        tag.putBoolean(IS_GENERATED, value)
    }
}