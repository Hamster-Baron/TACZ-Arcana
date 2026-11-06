package group.taczexpands.client.input

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.client.render.surface.SurfaceRenderer
import group.taczexpands.common.network.v2.c2s.C2SViewState
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object ViewStateManager {
    private var lastSampleTick = Int.MIN_VALUE
    private var sequence = 0L

    @SubscribeEvent
    fun onRenderLevel(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        if (SurfaceRenderer.isWorldPass()) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        if (player.tickCount == lastSampleTick) return
        val camera = event.camera
        val cameraEntity = minecraft.cameraEntity
        val source = when {
            RemoteCameraSession.isActive && cameraEntity is EntityKineticBullet -> C2SViewState.Source.REMOTE_PROJECTILE
            CameraManager.modified -> C2SViewState.Source.CAMERA_PATH
            else -> return
        }
        lastSampleTick = player.tickCount
        sequence++
        NetworkManager.sendToServer(C2SViewState(
            source, 0.0, 0.0, 0.0, camera.yRot, camera.xRot,
            (cameraEntity as? EntityKineticBullet)?.id?.takeIf { source == C2SViewState.Source.REMOTE_PROJECTILE },
            CameraManager.sessionId.takeIf { source == C2SViewState.Source.CAMERA_PATH },
            sequence
        ).create())
    }

    fun reset() {
        lastSampleTick = Int.MIN_VALUE
        sequence = 0L
    }

    fun invalidateSample() {
        lastSampleTick = Int.MIN_VALUE
    }
}
