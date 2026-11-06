package group.taczexpands.server.util

import group.taczexpands.common.data.CameraPath
import group.taczexpands.common.data.CoordinateType
import group.taczexpands.common.network.v2.c2s.C2SViewState
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.listener.PlayerListener
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicLong

object CameraViewStateManager {
    private const val MAX_STATE_AGE_TICKS = 5L
    private const val MAX_PATH_START_DELAY_TICKS = 200L
    private val nextCameraPathSessionId = AtomicLong()

    fun update(player: ServerPlayer, packet: C2SViewState) {
        if (packet.source == C2SViewState.Source.PLAYER) return
        if (!packet.yaw.isFinite() || !packet.pitch.isFinite() || packet.pitch !in -90f..90f) return
        if (packet.sequence < 0L) return
        val playerState = PlayerListener.getPlayerStates(player)
        val previous = playerState.clientViewState
        if (previous != null && packet.sequence <= previous.clientSequence) return
        val serverTick = player.server.tickCount.toLong()
        if (previous?.lastServerTick == serverTick) return
        val level = player.level() as ServerLevel
        when (packet.source) {
            C2SViewState.Source.PLAYER -> return
            C2SViewState.Source.REMOTE_PROJECTILE -> {
                val cameraData = MissileManager.tvGuidancePlayers[player] ?: return
                val bullet = cameraData.bullet ?: return
                if (bullet.id != packet.cameraEntityId || bullet.owner != player || bullet.isRemoved || bullet.level() !== level) return
            }
            C2SViewState.Source.CAMERA_PATH -> {
                val session = playerState.cameraPathSession ?: return
                session.resolveOrigin(packet.cameraPathSessionId, serverTick, level) ?: return
            }
        }
        playerState.clientViewState = PlayerViewState(
            Mth.wrapDegrees(packet.yaw), packet.pitch, packet.source,
            packet.cameraEntityId, packet.cameraPathSessionId, packet.sequence, serverTick
        )
    }

    fun startCameraPath(player: ServerPlayer, paths: List<CameraPath>): CameraPathSession? {
        val playerState = PlayerListener.getPlayerStates(player)
        if (playerState.clientViewState?.source == C2SViewState.Source.CAMERA_PATH) {
            playerState.clientViewState = null
        }
        if (paths.size < 2 || paths.any {
                it.durationMillis < 0L || it.yaw?.isFinite() == false || it.pitch?.isFinite() == false || it.roll?.isFinite() == false
            }) {
            playerState.cameraPathSession = null
            return null
        }
        val points = resolveCameraPathPoints(player, paths) ?: run {
            playerState.cameraPathSession = null
            return null
        }
        var remainingDurationMillis = 3_600_000L
        val segmentDurationMillis = paths.drop(1).map { path ->
            path.durationMillis.coerceAtMost(remainingDurationMillis).also {
                remainingDurationMillis -= it
            }
        }
        if (segmentDurationMillis.none { it > 0L }) {
            playerState.cameraPathSession = null
            return null
        }
        val serverTick = player.server.tickCount.toLong()
        val session = CameraPathSession(nextCameraPathSessionId.incrementAndGet(), player.level().dimension(), points, segmentDurationMillis, serverTick + MAX_PATH_START_DELAY_TICKS)
        playerState.cameraPathSession = session
        return session
    }

    fun clearRemoteProjectile(player: ServerPlayer) {
        val playerState = PlayerListener.getPlayerStates(player)
        if (playerState.clientViewState?.source == C2SViewState.Source.REMOTE_PROJECTILE) {
            playerState.clientViewState = null
        }
    }

    fun clearCameraPath(player: ServerPlayer) {
        val playerState = PlayerListener.getPlayerStates(player)
        if (playerState.clientViewState?.source == C2SViewState.Source.CAMERA_PATH) {
            playerState.clientViewState = null
        }
        playerState.cameraPathSession = null
    }

    fun clear(player: ServerPlayer) {
        val playerState = PlayerListener.getPlayerStates(player)
        playerState.clientViewState = null
        playerState.cameraPathSession = null
    }

    internal fun resolve(player: ServerPlayer): CameraView {
        val playerState = PlayerListener.getPlayerStates(player)
        val state = playerState.clientViewState
        val serverTick = player.server.tickCount.toLong()
        if (state != null && serverTick - state.lastServerTick in 0..MAX_STATE_AGE_TICKS) {
            when (state.source) {
                C2SViewState.Source.PLAYER -> Unit
                C2SViewState.Source.REMOTE_PROJECTILE -> {
                    val bullet = MissileManager.tvGuidancePlayers[player]?.bullet
                    if (bullet != null && bullet.id == state.cameraEntityId && bullet.owner == player && !bullet.isRemoved && bullet.level() === player.level()) {
                        return CameraView(bullet.getEyePosition(1.0f), Vec3.directionFromRotation(state.pitch, state.yaw), bullet, player)
                    }
                }
                C2SViewState.Source.CAMERA_PATH -> {
                    val session = playerState.cameraPathSession
                    val level = player.level() as ServerLevel
                    val origin = session?.resolveOrigin(state.cameraPathSessionId, serverTick, level)
                    if (origin != null) {
                        return CameraView(origin, Vec3.directionFromRotation(state.pitch, state.yaw), player, player)
                    }
                }
            }
        }
        return CameraView(player.eyePosition, player.lookAngle, player, player)
    }

    private fun resolveCameraPathPoints(player: ServerPlayer, paths: List<CameraPath>): List<Vec3>? {
        val initialPosition = player.eyePosition
        var lastX = initialPosition.x
        var lastY = initialPosition.y
        var lastZ = initialPosition.z
        val points = mutableListOf<Vec3>()
        paths.forEach { path ->
            val currentX = path.x ?: lastX
            val currentY = path.y ?: lastY
            val currentZ = path.z ?: lastZ
            if (!currentX.isFinite() || !currentY.isFinite() || !currentZ.isFinite()) return null
            lastX = currentX
            lastY = currentY
            lastZ = currentZ
            val basePosition = player.position()
            val position = when (path.coordinateType) {
                CoordinateType.WORLD_SPACE -> Vec3(currentX, currentY, currentZ)
                CoordinateType.MODEL_SPACE_NO_ROTATION -> basePosition.add(currentX, currentY, currentZ)
                CoordinateType.MODEL_SPACE_Y_ROTATION_ONLY -> Vec3(currentX, currentY, currentZ).yRot(Math.toRadians(-player.yRot.toDouble()).toFloat()).add(basePosition)
                CoordinateType.MODEL_SPACE_WITH_ROTATION -> Vec3(currentX, currentY, currentZ).xRot(Math.toRadians(-player.xRot.toDouble()).toFloat()).yRot(Math.toRadians(-player.yRot.toDouble()).toFloat()).add(basePosition)
            }
            points.add(position)
        }
        return points
    }

    internal data class CameraView(val origin: Vec3, val direction: Vec3, val sourceEntity: Entity, val viewer: Entity)
}
