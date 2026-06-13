package group.taczexpands.client.network.s2c

import group.taczexpands.client.gui.FlashOverlay
import group.taczexpands.common.network.s2c.S2CFlash
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CFlashImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CFlash, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CFlash, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                FlashOverlay.startEffect(packet.durationMillis, packet.fadeInMillis, packet.fadeOutMillis, packet.strength)
            }
            it.packetHandled = true
        }
    }
}