package group.taczexpands.client.render.particle

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.network.v2.s2c.S2CParticleEffect
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

object SnowstormParticles {
    private data class Key(val identifier: String, val owner: Int)
    private data class Following(val emitter: ParticleEmitterInstance, val owner: Int, val offset: Vec3,
        val facing: Boolean)

    private val named = mutableMapOf<Key, ParticleEmitterInstance>()
    private val following = mutableListOf<Following>()
    private val warned = mutableSetOf<String>()
    private var level: ClientLevel? = null

    fun apply(packet: S2CParticleEffect) {
        val current = Minecraft.getInstance().level ?: return
        if (level !== current) {
            clear()
            level = current
        }
        val effect = packet.effect
        val ownerId = packet.owner
        val key = packet.identifier?.let { name -> ownerId?.let { Key(name, it) } }
        if (effect == null || packet.remove) {
            val emitter = named[key] ?: return
            if (packet.remove) {
                stop(emitter)
                named.remove(key)
                following.removeIf { it.emitter === emitter }
            } else packet.variables?.forEach(emitter::setVariable)
            return
        }
        val id = ResourceLocation.tryParse(effect)
        val definition = id?.let { ParticleDefinitionLoader.getInstance().getDefinition(it) }
        if (definition == null) {
            val missing = id?.toString() ?: effect
            if (warned.add(missing)) TACZExpandsCommon.LOGGER.warn("Unknown Snowstorm particle: {}", missing)
            return
        }
        val position = Vec3(packet.x ?: 0.0, packet.y ?: 0.0, packet.z ?: 0.0)
        val manager = WorldEmitterManager.getInstance()
        val emitter = manager.addEmitter(current, position, Vec3.ZERO, definition) ?: return
        packet.variables?.forEach(emitter::setVariable)
        if (key != null) {
            named.put(key, emitter)?.let { previous ->
                stop(previous)
                following.removeIf { it.emitter === previous }
            }
        }
        val facing = packet.yaw != null && packet.pitch != null
        if (facing) manager.updateEmitterAnchor(emitter, anchor(position, packet.yaw!!, packet.pitch!!))
        if (packet.follow == true && ownerId != null) {
            val owner = current.getEntity(ownerId)
            if (owner == null) stop(emitter)
            else following.add(Following(emitter, ownerId, position.subtract(owner.position()), facing))
        }
    }

    fun tick() {
        val minecraft = Minecraft.getInstance()
        if (level !== minecraft.level) {
            clear()
            return
        }
        if (minecraft.isPaused) return
        val manager = WorldEmitterManager.getInstance()
        following.removeIf { entry ->
            val owner = level?.getEntity(entry.owner)
            if (owner == null || owner.isRemoved) stop(entry.emitter)
            else {
                val position = owner.position().add(entry.offset)
                manager.updateEmitterPosition(entry.emitter, position)
                if (entry.facing) manager.updateEmitterAnchor(entry.emitter,
                    anchor(position, owner.getViewYRot(1f), owner.getViewXRot(1f)))
            }
            entry.emitter.isFinished
        }
        named.entries.removeIf { it.value.isFinished }
    }

    private fun anchor(position: Vec3, yaw: Float, pitch: Float): Matrix4f = Matrix4f()
        .translation(position.x.toFloat(), position.y.toFloat(), position.z.toFloat())
        .rotateY(Math.toRadians(-yaw.toDouble()).toFloat())
        .rotateX(Math.toRadians(pitch.toDouble()).toFloat())

    private fun stop(emitter: ParticleEmitterInstance) {
        emitter.setRemoved(true)
        emitter.setActive(false)
    }

    fun clear() {
        named.values.forEach(::stop)
        following.forEach { stop(it.emitter) }
        named.clear()
        following.clear()
        level = null
    }
}
