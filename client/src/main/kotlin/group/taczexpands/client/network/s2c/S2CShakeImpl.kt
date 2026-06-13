package group.taczexpands.client.network.s2c

import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.input.ShakeManager
import group.taczexpands.client.util.Shake
import group.taczexpands.common.network.s2c.S2CShake
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CShakeImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CShake, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CShake, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                ShakeManager.add(
                    Shake(packet.amplitudeYRot, packet.amplitudeXRot, packet.durationMillis, packet.frequencyHz, packet.randomness, packet.dynamicPhaseOffset, packet.phaseOffsetYRot, packet.phaseOffsetXRot)
                )
            }
            it.packetHandled = true
        }
    }
}