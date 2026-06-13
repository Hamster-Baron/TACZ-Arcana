package group.taczexpands.client.network.s2c

import com.tacz.guns.api.TimelessAPI
import group.taczexpands.client.accessor.IAccessorAnimationStateContext
import group.taczexpands.client.gui.GunContextManager
import group.taczexpands.common.network.s2c.S2CAnimation
import net.minecraft.client.Minecraft
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CAnimationImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CAnimation, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CAnimation, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                if (packet.track < 0) return@enqueueWork
                val player = Minecraft.getInstance().player ?: return@enqueueWork
                if (packet.action == S2CAnimation.Action.ADD_REDIRECT) {
                    GunContextManager.addRedirect(packet.name, packet.to)
                    return@enqueueWork
                } else if (packet.action == S2CAnimation.Action.REMOVE_REDIRECT) {
                    GunContextManager.removeRedirect(packet.name)
                    return@enqueueWork
                }

                TimelessAPI.getGunDisplay(player.mainHandItem).ifPresent {
                    val stateContext = it.animationStateMachine?.context ?: return@ifPresent
                    val controller = it.animationStateMachine?.animationController
                    if (packet.action == S2CAnimation.Action.PLAY) {
                        if (controller?.containPrototype(packet.name) ?: false) {
                            val extraTracks = (stateContext as IAccessorAnimationStateContext).`taczexpands$getExtraTracks`()
                            for (i in extraTracks.size until packet.track + 1) {
                                extraTracks.add(stateContext.assignNewTrack(0))
                            }

                            val track = extraTracks[packet.track]

                            try {
                                stateContext.runAnimation(
                                    packet.name,
                                    track,
                                    packet.blending,
                                    packet.playMode,
                                    packet.transitionTime
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else if (packet.action == S2CAnimation.Action.STOP) {
                        val extraTracks = (stateContext as IAccessorAnimationStateContext).`taczexpands$getExtraTracks`()
                        if (packet.track in 0 until extraTracks.size) {
                            val track = extraTracks[packet.track]
                            try {
                                stateContext.stopAnimation(track)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            it.packetHandled = true
        }

    }
}