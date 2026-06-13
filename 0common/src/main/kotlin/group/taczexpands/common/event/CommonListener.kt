package group.taczexpands.common.event

import com.tacz.guns.api.GunProperties
import com.tacz.guns.api.event.common.AttachmentPropertyEvent
import group.taczexpands.common.manager.PackAllowAttachmentsModifyManager
import group.taczexpands.common.nbt.GunExtras
import it.unimi.dsi.fastutil.Pair
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent


object CommonListener {
    @SubscribeEvent
    fun onEvalAttachmentCache(event: AttachmentPropertyEvent) {
        try {
            val gunItem = event.gunItem
            if (GunExtras.getUsingUnderBarrel(gunItem)) {
                val underBarrel = GunExtras.getUnderBarrel(gunItem) ?: return
                if (underBarrel.ignoreSilencer) {
                    val pair = event.cacheProperty.getCache(GunProperties.SILENCE)
                    event.cacheProperty.setCache(GunProperties.SILENCE, Pair.of(pair.first(), false))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(PackAllowAttachmentsModifyManager)
    }
}