package group.taczexpands.client.network.s2c

import group.taczexpands.client.sound.DynamicSoundInstance
import group.taczexpands.client.sound.DynamicSoundManager
import group.taczexpands.common.network.s2c.S2CSound
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CSoundImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CSound, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CSound, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.action) {
                    "play" -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        val soundSource = SoundSource.entries.getOrNull(packet.sourceType) ?: SoundSource.PLAYERS
                        val soundName = ResourceLocation.tryParse(packet.soundName) ?: return@enqueueWork
                        val entity = player.level().getEntity(packet.sourceEntityID)

                        val instance = DynamicSoundInstance(
                            soundSource,
                            packet.volume,
                            packet.pitch,
                            packet.distance,
                            soundName,
                            packet.mono,
                            packet.sourcePos,
                            entity
                        )

                        DynamicSoundManager.play(packet.group, instance)
                    }

                    "stop" -> {
                        DynamicSoundManager.stop(packet.group)
                    }
                }
            }
            it.packetHandled = true
        }
    }
}
