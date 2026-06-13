package group.taczexpands.client.sound

import com.tacz.guns.init.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import kotlin.math.sqrt

class DynamicSoundInstance(
    source: SoundSource,
    private val baseVolume: Float,
    pitch: Float,
    val distance: Int,
    val soundName: ResourceLocation,
    val mono: Boolean,
    pos: Vec3,
    private val entity: Entity?
) : AbstractTickableSoundInstance(
    ModSounds.GUN.get(),
    source,
    RandomSource.create()
) {
    private var stopped = false
    private var redirectedSound: Sound? = null

    init {
        this.x = pos.x
        this.y = pos.y
        this.z = pos.z
        this.pitch = pitch
        this.attenuation = SoundInstance.Attenuation.NONE
        this.volume = computeVolume()
    }

    fun setStop() {
        stopped = true
        Minecraft.getInstance().soundManager.stop(this)
    }

    override fun isStopped(): Boolean {
        return stopped
    }

    override fun canPlaySound(): Boolean {
        return !stopped && entity?.isSilent != true
    }

    override fun tick() {
        val trackedEntity = entity ?: return
        if (trackedEntity.isRemoved || trackedEntity is LivingEntity && trackedEntity.isDeadOrDying) {
            setStop()
            return
        }
        this.x = trackedEntity.x
        this.y = trackedEntity.y
        this.z = trackedEntity.z
        this.volume = computeVolume()
    }

    override fun resolve(manager: SoundManager): WeighedSoundEvents? {
        val events = super.resolve(manager)
        if (events != null) {
            val taczSound = TaczSound(soundName, TACZ_SOUND_LISTER.idToFile(soundName), super.getSound())
            redirectedSound = taczSound
            this.sound = taczSound
        } else {
            redirectedSound = null
        }
        return events
    }

    override fun getSound(): Sound {
        return redirectedSound ?: super.getSound()
    }

    private fun computeVolume(): Float {
        val player = Minecraft.getInstance().player ?: return baseVolume
        val distanceFactor = if (distance > 0) {
            1.0f - min(1.0f, sqrt(player.distanceToSqr(x, y, z)).toFloat() / distance)
        } else {
            1.0f
        }
        val mixedVolume = baseVolume * distanceFactor
        return mixedVolume * mixedVolume
    }

    private class TaczSound(
        private val soundLocation: ResourceLocation,
        private val soundPath: ResourceLocation,
        template: Sound
    ) : Sound(
        soundLocation.toString(),
        template.getVolume(),
        template.getPitch(),
        template.getWeight(),
        Type.FILE,
        template.shouldStream(),
        false,
        template.getAttenuationDistance()
    ) {
        override fun getLocation(): ResourceLocation {
            return soundLocation
        }

        override fun getPath(): ResourceLocation {
            return soundPath
        }
    }

    companion object {
        private val TACZ_SOUND_LISTER = FileToIdConverter("tacz_sounds", ".ogg")
    }
}
