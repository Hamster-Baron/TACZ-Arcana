package group.taczexpands.client.network.s2c

import group.taczexpands.client.config.RuntimeConfig
import group.taczexpands.client.gui.CustomHUDOverlay
import group.taczexpands.client.input.CameraManager
import group.taczexpands.client.nbt.PlayerExtrasClient
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.network.v2.s2c.S2CAddHUD
import group.taczexpands.common.network.v2.s2c.S2CConfig
import group.taczexpands.common.network.v2.s2c.S2CRemoveHUD
import group.taczexpands.common.network.v2.s2c.S2CSendCameraPath
import group.taczexpands.common.network.v2.s2c.S2CSyncBullet
import group.taczexpands.common.network.v2.s2c.S2CSyncPlayerStorage
import group.taczexpands.common.util.JSON
import net.minecraft.client.Minecraft
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CRawImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CRaw, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CRaw, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.type) {
                    S2CSendCameraPath.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CSendCameraPath>(packet.data.decodeToString())
                        CameraManager.start(packet.cameraPathList, !packet.allowMove, !packet.allowRotate)
                    }

                    S2CSyncBullet.NETWORK_INDEX -> {
                        val level = Minecraft.getInstance().level ?: return@enqueueWork
                        JSON.decodeFromString<S2CSyncBullet>(packet.data.decodeToString()).apply(level)
                    }

                    S2CSyncPlayerStorage.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CSyncPlayerStorage>(packet.data.decodeToString())
                        PlayerExtrasClient.data = packet.data
                    }

                    S2CAddHUD.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CAddHUD>(packet.data.decodeToString())
                        CustomHUDOverlay.hudMap[packet.identifier] = packet
                    }

                    S2CRemoveHUD.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CRemoveHUD>(packet.data.decodeToString())
                        if (packet.identifier == null) {
                            CustomHUDOverlay.hudMap.clear()
                        } else {
                            CustomHUDOverlay.hudMap.remove(packet.identifier!!)
                        }
                    }

                    S2CConfig.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CConfig>(packet.data.decodeToString())

                        RuntimeConfig.defaultBlockAimWhileReloading = packet.defaultBlockAimWhileReloading
                    }
                }
            }
            it.packetHandled = true
        }
    }
}