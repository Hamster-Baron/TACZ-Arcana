package group.taczexpands.client.input

import group.taczexpands.client.mixin.accessor.IAccessorCamera
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.data.CameraPath
import group.taczexpands.common.data.CoordinateType
import group.taczexpands.common.network.c2s.C2SActionKey
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.commons.math3.analysis.function.Min

object CameraManager {
    var modified = false
    var moveLock = false
    var rotateLock = false

    private var paths = mutableListOf<CameraPath>()

    private var startTimeMillis = 0L

    private var xOffset: Double = 0.0
    private var yOffset: Double = 0.0
    private var zOffset: Double = 0.0

    private var initialPos = Vec3.ZERO
    private var initialRot = FloatArray(3)

    private val resolvedPositions = mutableListOf<Vec3>()
    private val resolvedRotations = mutableListOf<FloatArray>()

    private var lastCameraType = Minecraft.getInstance().options.cameraType

    private var lastSyncTick = 0

    fun start(paths: List<CameraPath>, moveLock: Boolean, rotateLock: Boolean) {
        if (!modified) {
            lastCameraType = Minecraft.getInstance().options.cameraType
        }
        this.paths.clear()
        this.moveLock = moveLock
        this.rotateLock = rotateLock
        this.paths.addAll(paths)
        if (paths.size < 2) {
            reset()
            return
        }
        val mc = Minecraft.getInstance()
        val player = mc.player
        if (player == null) {
            reset()
            return
        }
        val camera = mc.gameRenderer.mainCamera

        initialPos = camera.position
        initialRot[0] = camera.yRot
        initialRot[1] = camera.xRot
        initialRot[2] = 0f

        resolveAllPaths(player)

        this.startTimeMillis = System.currentTimeMillis()
        this.modified = true
    }

    fun reset() {
        modified = false
        moveLock = false
        rotateLock = false
        Minecraft.getInstance().options.cameraType = lastCameraType

        NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.UNBIND_CLIENT_CAMERA))
    }

    private fun resolveAllPaths(player: LocalPlayer) {
        resolvedPositions.clear()
        resolvedRotations.clear()

        var lastX = initialPos.x
        var lastY = initialPos.y
        var lastZ = initialPos.z
        var lastYaw = initialRot[0]
        var lastPitch = initialRot[1]
        var lastRoll = initialRot[2]

        for (path in paths) {
            val currentX = path.x ?: lastX
            val currentY = path.y ?: lastY
            val currentZ = path.z ?: lastZ
            val currentYaw = path.yaw ?: lastYaw
            val currentPitch = path.pitch ?: lastPitch
            val currentRoll = path.roll ?: lastRoll

            lastX = currentX; lastY = currentY; lastZ = currentZ
            lastYaw = currentYaw; lastPitch = currentPitch; lastRoll = currentRoll

            val basePos = player.position().add(xOffset, yOffset, zOffset)
            val absPos = when (path.coordinateType) {
                CoordinateType.WORLD_SPACE -> Vec3(currentX, currentY, currentZ)
                CoordinateType.MODEL_SPACE_NO_ROTATION -> basePos.add(currentX, currentY, currentZ)
                CoordinateType.MODEL_SPACE_Y_ROTATION_ONLY -> {
                    Vec3(currentX, currentY, currentZ).yRot(Math.toRadians((-player.yRot).toDouble()).toFloat()).add(basePos)
                }

                CoordinateType.MODEL_SPACE_WITH_ROTATION -> {
                    Vec3(currentX, currentY, currentZ).xRot(Math.toRadians((-player.xRot).toDouble()).toFloat())
                        .yRot(Math.toRadians((-player.yRot).toDouble()).toFloat()).add(basePos)
                }
            }

            resolvedPositions.add(absPos)
            resolvedRotations.add(floatArrayOf(currentYaw, currentPitch, currentRoll))
        }
    }

    @SubscribeEvent
    fun onCameraSetup(event: ViewportEvent.ComputeCameraAngles) {
        if (!modified || resolvedPositions.isEmpty()) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.cameraEntity != player) return

        val elapsedMillis = System.currentTimeMillis() - startTimeMillis

        var accumulatedMillis = 0L
        var targetIdx = -1

        for (i in 1 until paths.size) {
            val segmentMs = paths[i].durationMillis
            if (elapsedMillis < accumulatedMillis + segmentMs) {
                targetIdx = i
                break
            }
            accumulatedMillis += segmentMs
        }

        if (targetIdx == -1) {
            reset()
            return
        }

        Minecraft.getInstance().options.cameraType = CameraType.THIRD_PERSON_BACK

        val alpha = (elapsedMillis - accumulatedMillis).toFloat() / paths[targetIdx].durationMillis.toFloat()
        val smoothAlpha = alpha * alpha * (3 - 2 * alpha)

        val v1 = resolvedPositions[targetIdx - 1]
        val v2 = resolvedPositions[targetIdx]
        val v0 = if (targetIdx > 1) resolvedPositions[targetIdx - 2] else v1.subtract(v2.subtract(v1))
        val v3 = if (targetIdx < resolvedPositions.size - 1) resolvedPositions[targetIdx + 1] else v2.add(v2.subtract(v1))

        val camera = event.camera
        val finalX = catmullRom(smoothAlpha, v0.x, v1.x, v2.x, v3.x)
        val finalY = catmullRom(smoothAlpha, v0.y, v1.y, v2.y, v3.y)
        val finalZ = catmullRom(smoothAlpha, v0.z, v1.z, v2.z, v3.z)

        (camera as IAccessorCamera).setPosition(Vec3(finalX, finalY, finalZ))

        val rot1 = resolvedRotations[targetIdx - 1]
        val rot2 = resolvedRotations[targetIdx]

        event.yaw = Mth.rotLerp(smoothAlpha, rot1[0], rot2[0])
        event.pitch = Mth.lerp(smoothAlpha, rot1[1], rot2[1])
        event.roll = Mth.lerp(smoothAlpha, rot1[2], rot2[2])

        val currentTick = player.tickCount
        if (currentTick != lastSyncTick) {
            lastSyncTick = currentTick
            NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.UPDATE_CLIENT_CAMERA, event.yaw, event.pitch))

        }
    }

    private fun catmullRom(t: Float, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * (
                (2 * p1) +
                        (-p0 + p2) * t +
                        (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
                        (-p0 + 3 * p1 - 3 * p2 + p3) * t3
                )
    }
}