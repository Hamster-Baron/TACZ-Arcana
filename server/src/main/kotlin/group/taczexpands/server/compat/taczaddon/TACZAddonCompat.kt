package group.taczexpands.server.compat.taczaddon

import com.mafuyu404.taczaddon.common.LiberateAttachment
import com.mafuyu404.taczaddon.init.GunSmithingManager
import net.minecraft.world.entity.player.Inventory


object TACZAddonCompat {
    fun getInventoryRefit(inventory: Inventory): Inventory {
        return LiberateAttachment.useVirtualInventory(inventory)
    }

    fun getInventoryUnload(inventory: Inventory): Inventory {
        if (LiberateAttachment.isLiberated(inventory.player)) return LiberateAttachment.useVirtualInventory(inventory)

        val attachmentItems = GunSmithingManager.getResult(inventory.getSelected())
        if (attachmentItems.isEmpty()) {
            return inventory
        } else {
            return LiberateAttachment.useVirtualInventory(inventory)
        }
    }
}