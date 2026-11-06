package group.taczexpands.client.network.s2c

import group.taczexpands.client.config.RuntimeConfig
import group.taczexpands.client.gui.CustomHUDOverlay
import group.taczexpands.client.input.CameraManager
import group.taczexpands.client.input.KeyInputs
import group.taczexpands.client.nbt.PlayerExtrasClient
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.network.v2.s2c.S2CActionLockState
import group.taczexpands.common.network.v2.s2c.S2CAddHUD
import group.taczexpands.common.network.v2.s2c.S2CConfig
import group.taczexpands.common.network.v2.s2c.S2CRemoveHUD
import group.taczexpands.common.network.v2.s2c.S2CSendCameraPath
import group.taczexpands.common.network.v2.s2c.S2CSyncBullet
import group.taczexpands.common.network.v2.s2c.S2CSyncPlayerStorage
import group.taczexpands.common.network.v2.s2c.S2CSurfaceCamera
import group.taczexpands.common.network.v2.s2c.S2CParticleEffect
import group.taczexpands.client.render.particle.SnowstormParticles
import group.taczexpands.client.render.surface.SurfaceEngine
import group.taczexpands.client.render.surface.SurfaceCameraUpdate
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
                    S2CParticleEffect.NETWORK_INDEX -> SnowstormParticles.apply(
                        JSON.decodeFromString<S2CParticleEffect>(packet.data.decodeToString()))

                    S2CSurfaceCamera.NETWORK_INDEX -> {
                        val camera = JSON.decodeFromString<S2CSurfaceCamera>(packet.data.decodeToString())
                        if (camera.remove) {
                            SurfaceEngine.removeCamera(camera.identifier)
                        } else {
                            SurfaceEngine.setCameraUpdate(requireNotNull(camera.identifier), SurfaceCameraUpdate(
                                camera.space, camera.anchor, camera.x, camera.y, camera.z,
                                camera.yaw, camera.pitch, camera.roll, camera.durationMillis,
                                camera.fov, camera.distance, camera.vision, camera.shaders, camera.bone))
                        }
                    }

                    S2CSendCameraPath.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CSendCameraPath>(packet.data.decodeToString())
                        CameraManager.start(packet.cameraPathList, !packet.allowMove, !packet.allowRotate, packet.sessionId)
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
                        CustomHUDOverlay.put(packet)
                    }

                    S2CRemoveHUD.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CRemoveHUD>(packet.data.decodeToString())
                        CustomHUDOverlay.remove(packet.identifier)
                    }

                    S2CConfig.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CConfig>(packet.data.decodeToString())

                        RuntimeConfig.defaultBlockAimWhileReloading = packet.defaultBlockAimWhileReloading
                    }

                    S2CActionLockState.NETWORK_INDEX -> {
                        val packet = JSON.decodeFromString<S2CActionLockState>(packet.data.decodeToString())
                        KeyInputs.applyActionLockState(packet)
                    }
                }
            }
            it.packetHandled = true
        }
    }
}
