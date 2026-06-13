package group.taczexpands.server.network.c2s

import group.taczexpands.common.network.c2s.C2SRaw
import group.taczexpands.common.network.v2.c2s.C2SMouseScroll
import group.taczexpands.server.event.PlayerMouseScrollEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object C2SRawImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<C2SRaw, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: C2SRaw, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.type) {
                    C2SMouseScroll.NETWORK_INDEX -> {
                        val player = it.sender ?: return@enqueueWork
                        MinecraftForge.EVENT_BUS.post(PlayerMouseScrollEvent(player, packet.type))
                    }
                }
            }

            it.packetHandled = true
        }
    }
}