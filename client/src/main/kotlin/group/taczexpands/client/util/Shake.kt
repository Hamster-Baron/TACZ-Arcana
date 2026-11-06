package group.taczexpands.client.util

import net.minecraft.client.Minecraft
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


class Shake(
    var amplitudeYRot: Float,
    var amplitudeXRot: Float,
    durationMillis: Long,
    var frequencyHz: Float = 0.5f,
    var randomness: Float = 0.1f,
    var dynamicPhaseOffset: Boolean = true,
    var phaseOffsetYRot: Float? = null,
    var phaseOffsetXRot: Float? = null,

    delayMillis: Long = 0,

    ) {

    init {
        if (phaseOffsetXRot == null)
            phaseOffsetXRot = Random.nextFloat() * 2f * PI.toFloat()
        if (phaseOffsetYRot == null)
            phaseOffsetYRot = Random.nextFloat() * 2f * PI.toFloat()
    }

    private val startTime = System.currentTimeMillis() + delayMillis
    private val endTime = startTime + durationMillis
    private var periodCount: Int = 0
    private var lastFrameTime = startTime
    private var phase = 0.0

    fun update(frameCurrentTimeMillis: Long): Boolean {
        if (frameCurrentTimeMillis > endTime) {
            return false
        }

        if (frameCurrentTimeMillis < startTime) {
            return true
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return true

        phase += 2 * PI * frequencyHz * (frameCurrentTimeMillis - lastFrameTime) / 1000.0
        lastFrameTime = frameCurrentTimeMillis
        val currentPeriodCount = (phase / (2 * PI)).toInt()

        if (currentPeriodCount > periodCount) {
            if (dynamicPhaseOffset) {
                phaseOffsetXRot = Random.nextFloat() * 2f * PI.toFloat()
                phaseOffsetYRot = Random.nextFloat() * 2f * PI.toFloat()
            }
            periodCount = currentPeriodCount
        }


        val sineX = sin(phase + phaseOffsetXRot!!).toFloat() * amplitudeXRot
        val sineY = cos(phase + phaseOffsetYRot!!).toFloat() * amplitudeYRot

        val randomX = (Random.nextFloat() - 0.5f) * 2f * amplitudeXRot * randomness
        val randomY = (Random.nextFloat() - 0.5f) * 2f * amplitudeYRot * randomness

        val finalX = sineX + randomX
        val finalY = sineY + randomY

        player.xRot = (player.xRot + finalX).coerceIn(-90f, 90f)
        player.yRot += finalY

        return true
    }
}



