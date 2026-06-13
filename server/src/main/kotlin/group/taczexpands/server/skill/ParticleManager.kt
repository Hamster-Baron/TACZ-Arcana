package group.taczexpands.server.skill

import com.mojang.brigadier.StringReader
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries

object ParticleEmitterManager {
    private val emitters = mutableListOf<Emitter>()

    fun addEmitter(emitter: Emitter) {
        emitters.add(emitter)
    }

    fun onServerTick() {
        emitters.removeIf { it.update() }
    }

    fun blockedByParticle(level: Level, left: Vec3, right: Vec3): Boolean {
        emitters.forEach {
            if (it.blockSight && it.level == level) {
                if (it.isInsideSmoke(left, right)) return true
            }
        }
        return false
    }
}

class Emitter(
    val type: String,
    val extraParam: String = "",
    val level: ServerLevel,
    val location: Vec3,
    val mode: Mode,
    val radius: Double,
    val maxNum: Int,
    var duration: Int,
    val startDelay: Int,
    val blockSight: Boolean,
    val gravityValue: Float = 0.0f
) {
    private var tickCount = 0

    @Serializable
    enum class Mode {
        SIGNAL,
        SMOKE,
        BALL,
        RING
    }

    fun update(): Boolean {
        try {
            if (duration-- <= 0) return true
            tickCount++

            val progress = when {
                tickCount < startDelay -> 0.0
                tickCount >= duration -> 1.0
                else -> (tickCount - startDelay).toDouble() / (duration - startDelay)
            }
            val particles = generateParticles(progress)

            spawnParticles(particles)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun <T : ParticleOptions> getType(type: String): ParticleType<T>? {
        return ForgeRegistries.PARTICLE_TYPES.getValue(ResourceLocation(type)) as? ParticleType<T>
    }

    private fun spawnParticles(locations: List<Vec3>) {
        val players = level.players()

        val particle = getType<ParticleOptions>(type) ?: return
        val options = particle.deserializer.fromCommand(particle, StringReader(extraParam))

        for (player in players) {
            if (player.distanceToSqr(location) > 256 * 256) continue
            for (pos in locations) {
                val packet = ClientboundLevelParticlesPacket(
                    options,
                    false, pos.x, pos.y, pos.z, 0f, 0f, 0f, 0f, 1
                )
                player.connection.send(packet)
            }
        }
    }

    private fun generateParticles(progress: Double): List<Vec3> {
        val particles = mutableListOf<Vec3>()

        val currentRadius = radius * progress

        when (mode) {
            Mode.SIGNAL -> {
                val maxHeight = this.radius

                for (i in 0 until maxNum) {
                    val verticalMovement = Math.random() * progress * maxHeight

                    val currentSideRadius = verticalMovement * 0.1

                    val angle = Math.random() * Math.PI * 2
                    val xOffset = currentSideRadius * Math.cos(angle) * Math.random()
                    val zOffset = currentSideRadius * Math.sin(angle) * Math.random()

                    particles.add(location.add(xOffset, verticalMovement, zOffset))
                }
            }

            Mode.SMOKE -> {
                for (i in 0 until maxNum) {
                    val xOffset = (Math.random() - 0.5) * currentRadius * 2
                    val yOffset = (Math.random() - 0.5) * currentRadius * 2
                    val zOffset = (Math.random() - 0.5) * currentRadius * 2
                    particles.add(location.add(xOffset, yOffset, zOffset))
                }
            }

            Mode.BALL -> {
                for (i in 0 until maxNum) {
                    val theta = Math.random() * Math.PI * 2
                    val phi = Math.acos(2 * Math.random() - 1)
                    val r = currentRadius * Math.cbrt(Math.random())
                    val x = r * Math.sin(phi) * Math.cos(theta)
                    val y = r * Math.sin(phi) * Math.sin(theta)
                    val z = r * Math.cos(phi)
                    particles.add(location.add(x, y, z))
                }
            }

            Mode.RING -> {
                val initialRadius = this.radius

                val targetR = when {
                    progress < 0.2 -> initialRadius * (progress / 0.2)
                    else -> initialRadius + (progress - 0.2) * (initialRadius * 0.2)
                }

                val maxR = targetR * 1.05

                for (i in 0 until maxNum) {
                    val angle = Math.random() * Math.PI * 2
                    val r = Math.random() * (maxR - targetR * 0.8) + targetR * 0.8
                    val x = r * Math.cos(angle)
                    val z = r * Math.sin(angle)

                    val yOffset = progress * 0.5

                    particles.add(location.add(x, yOffset, z))
                }
            }
        }

        val finalParticles = if (gravityValue > 0.0f && mode != Mode.SIGNAL) {
            particles.map { pos ->
                val blockPos = BlockPos.containing(pos.x, pos.y, pos.z)

                val floorY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos.x, blockPos.z).toDouble()
                val targetY = floorY + 0.8

                val initialY = location.y

                val deltaY = initialY - targetY

                val gravityFactor = gravityValue.toDouble() * 0.05

                val currentTickProgress = (tickCount - startDelay).coerceAtLeast(0).toDouble()

                val fallProgress = (currentTickProgress * gravityFactor).coerceIn(0.0, 1.0)

                val newY = initialY - deltaY * fallProgress

                if (newY < targetY) {
                    Vec3(pos.x, targetY, pos.z)
                } else {
                    Vec3(pos.x, newY, pos.z)
                }
            }
        } else {
            particles
        }

        return finalParticles
    }





    fun isInsideSmoke(startPos: Vec3, endPos: Vec3): Boolean {
        val currentDuration = duration + tickCount
        val progress = when {
            tickCount < startDelay -> 0.0
            currentDuration <= 0 -> 1.0
            else -> (tickCount - startDelay).toDouble() / (currentDuration - startDelay)
        }
        val currentRadius = radius * progress

        val emitterCenterY: Double

        if (gravityValue > 0.0f && mode != Mode.SIGNAL) {
            val blockPos = BlockPos.containing(location.x, location.y, location.z)
            val floorY = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos.x, blockPos.z).toDouble()
            val targetY = floorY + 0.8
            val initialY = location.y
            val deltaY = initialY - targetY

            val gravityFactor = gravityValue.toDouble() * 0.05
            val currentTickProgress = (tickCount - startDelay).coerceAtLeast(0).toDouble()
            val fallProgress = (currentTickProgress * gravityFactor).coerceIn(0.0, 1.0)

            emitterCenterY = initialY - deltaY * fallProgress

        } else if (mode == Mode.SIGNAL) {
            val signalHeight = radius * progress
            val yBottom = location.y

            emitterCenterY = yBottom + signalHeight / 2.0

        } else {
            emitterCenterY = location.y
        }

        val emitterCenter = Vec3(location.x, emitterCenterY, location.z)
        val distanceToLine = distanceFromPointToLine(emitterCenter, startPos, endPos)
        return distanceToLine <= currentRadius
    }

    private fun distanceFromPointToLine(point: Vec3, lineStart: Vec3, lineEnd: Vec3): Double {
        val lineVec = lineEnd.subtract(lineStart)
        val pointVec = point.subtract(lineStart)
        val t = pointVec.dot(lineVec) / lineVec.lengthSqr()
        val closestPoint = if (t < 0) lineStart else if (t > 1) lineEnd else lineStart.add(lineVec.scale(t))
        return closestPoint.distanceTo(point)
    }
}
