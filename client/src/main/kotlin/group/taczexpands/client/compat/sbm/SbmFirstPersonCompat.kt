package group.taczexpands.client.compat.sbm

import group.taczexpands.client.coremod.TACZExpandsClientMixinPlugin
import group.taczexpands.client.mixin.accessor.sbm.LegacyFirstPersonRenderHandlerInvoker
import group.taczexpands.client.mixin.accessor.sbm.ModernFirstPersonRenderHandlerInvoker
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.TickEvent

object SbmFirstPersonCompat {
    fun forceRefresh(itemStack: ItemStack) {
        when (TACZExpandsClientMixinPlugin.getSbmLayout()) {
            TACZExpandsClientMixinPlugin.SbmLayout.LEGACY ->
                LegacyFirstPersonRenderHandlerInvoker.`taczexpands$refreshLegacy`(itemStack)
            TACZExpandsClientMixinPlugin.SbmLayout.MODERN -> {
                ModernFirstPersonRenderHandlerInvoker.`taczexpands$resetHandState`()
                ModernFirstPersonRenderHandlerInvoker.`taczexpands$runHandUpdate`(TickEvent.ClientTickEvent(TickEvent.Phase.START))
            }
            else -> Unit
        }
    }
}
