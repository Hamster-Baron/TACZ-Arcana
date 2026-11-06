package group.taczexpands.server.util

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class CameraPathSession(
    val id: Long,
    val dimension: ResourceKey<Level>,
    private val positions: List<Vec3>,
    private val segmentDurationMillis: List<Long>,
    private val latestStartTick: Long
) {
    private var startedAtTick: Long? = null

    fun resolveOrigin(sessionId: Long?, serverTick: Long, level: ServerLevel): Vec3? {
        if (sessionId != id || level.dimension() != dimension) return null
        val startTick = startedAtTick ?: run {
            if (serverTick > latestStartTick) return null
            startedAtTick = serverTick
            serverTick
        }
        val elapsedTicks = serverTick - startTick
        if (elapsedTicks < 0L) return null
        val elapsedMillis = elapsedTicks * 50L
        var accumulatedMillis = 0L
        var targetIndex = -1
        segmentDurationMillis.forEachIndexed { index, durationMillis ->
            if (targetIndex < 0 && elapsedMillis < accumulatedMillis + durationMillis) {
                targetIndex = index + 1
            } else if (targetIndex < 0) {
                accumulatedMillis += durationMillis
            }
        }
        if (targetIndex < 0) return null
        val durationMillis = segmentDurationMillis[targetIndex - 1]
        val alpha = (elapsedMillis - accumulatedMillis).toFloat() / durationMillis.toFloat()
        val smoothAlpha = alpha * alpha * (3.0f - 2.0f * alpha)
        val current = positions[targetIndex]
        val previous = positions[targetIndex - 1]
        val beforePrevious = if (targetIndex > 1) positions[targetIndex - 2] else previous.subtract(current.subtract(previous))
        val next = if (targetIndex < positions.lastIndex) positions[targetIndex + 1] else current.add(current.subtract(previous))
        val origin = Vec3(
            catmullRom(smoothAlpha, beforePrevious.x, previous.x, current.x, next.x),
            catmullRom(smoothAlpha, beforePrevious.y, previous.y, current.y, next.y),
            catmullRom(smoothAlpha, beforePrevious.z, previous.z, current.z, next.z)
        )
        if (!level.hasChunkAt(BlockPos.containing(origin.x, origin.y, origin.z))) return null
        return origin
    }

    private fun catmullRom(t: Float, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * (2.0 * p1 + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3)
    }
}
