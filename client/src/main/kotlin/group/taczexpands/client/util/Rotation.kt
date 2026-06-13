package group.taczexpands.client.util

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.max

class Rotation(
    private val totalYaw: Float,
    private val totalPitch: Float,
    private val durationMillis: Long
) {
    companion object {
        private const val BASE_MILLIS_PER_DEGREE = 2.0

        fun create(
            yaw: Float,
            pitch: Float,
            relative: Boolean
        ): Rotation? {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return null

            val currentYaw = player.yRot
            val currentPitch = player.xRot

            val totalYawDelta: Float
            val totalPitchDelta: Float

            if (relative) {
                totalYawDelta = yaw
                totalPitchDelta = pitch
            } else {
                val targetPitch = pitch.coerceIn(-90f, 90f)
                totalPitchDelta = targetPitch - currentPitch

                val targetYaw = yaw
                totalYawDelta = Mth.wrapDegrees(targetYaw - currentYaw)
            }

            if (abs(totalYawDelta) < 0.001f && abs(totalPitchDelta) < 0.001f) {
                return null
            }

            val sensitivitySetting = mc.options.sensitivity().get().toFloat()
            val speedFactor = sensitivitySetting * 0.6f + 0.2f

            val durationMultiplier = 1.0f / speedFactor

            val maxAngleDelta = max(abs(totalYawDelta), abs(totalPitchDelta))

            val durationMillis = (maxAngleDelta * BASE_MILLIS_PER_DEGREE * durationMultiplier).toLong().coerceAtLeast(10)
            return Rotation(
                totalYaw = totalYawDelta,
                totalPitch = totalPitchDelta,
                durationMillis = durationMillis
            )
        }
    }

    private val startTime = System.currentTimeMillis()
    private val endTime = startTime + durationMillis
    private var lastUpdateTime = startTime

    fun update(frameCurrentTimeMillis: Long): Boolean {
        val player = Minecraft.getInstance().player ?: return false

        if (frameCurrentTimeMillis >= endTime) {
            return false
        }

        val deltaTimeMillis = frameCurrentTimeMillis - lastUpdateTime
        if (deltaTimeMillis <= 0) {
            return true
        }

        val progress = deltaTimeMillis.toFloat() / durationMillis.toFloat()

        val deltaYaw = totalYaw * progress
        val deltaPitch = totalPitch * progress

        player.yRot += deltaYaw
        player.xRot = (player.xRot + deltaPitch).coerceIn(-90f, 90f)

        lastUpdateTime = frameCurrentTimeMillis

        return true
    }
}