package group.taczexpands.client.sound

import group.taczexpands.common.TACZExpandsCommon
import net.minecraft.client.Minecraft
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

object DynamicSoundManager {
    private const val CLEANUP_INTERVAL_TICKS = 5
    private const val MAX_ACTIVE_PER_GROUP = 8

    private val taczSoundLister = FileToIdConverter("tacz_sounds", ".ogg")
    private val cache = ConcurrentHashMap<String, ArrayDeque<DynamicSoundInstance>>()
    private val soundResourceExistsCache = ConcurrentHashMap<ResourceLocation, Boolean>()
    private val missingSoundWarned = ConcurrentHashMap.newKeySet<ResourceLocation>()

    private var tickCount = 0

    fun play(group: String, instance: DynamicSoundInstance) {
        val minecraft = Minecraft.getInstance()
        if (!hasSoundResource(minecraft, instance.soundName)) {
            return
        }

        val key = normalizeGroup(group)
        val sounds = cache.computeIfAbsent(key) { ArrayDeque() }
        synchronized(sounds) {
            cleanupInactive(minecraft, sounds)
            limitGroupSize(sounds)
            minecraft.soundManager.play(instance)
            sounds.addLast(instance)
        }
    }

    fun stop(group: String) {
        val key = normalizeGroup(group)
        val sounds = cache.remove(key) ?: return
        synchronized(sounds) {
            sounds.forEach { it.setStop() }
            sounds.clear()
        }
    }

    fun stopAll() {
        cache.keys.toList().forEach(::stop)
    }

    fun onClientTick() {
        tickCount++
        if (tickCount < CLEANUP_INTERVAL_TICKS) {
            return
        }
        tickCount = 0

        val minecraft = Minecraft.getInstance()
        if (minecraft.level == null) {
            stopAll()
            return
        }

        cache.forEach { (key, sounds) ->
            synchronized(sounds) {
                cleanupInactive(minecraft, sounds)
                if (sounds.isEmpty()) {
                    cache.remove(key, sounds)
                }
            }
        }
    }

    fun clearSoundResourceCache() {
        soundResourceExistsCache.clear()
        missingSoundWarned.clear()
    }

    private fun limitGroupSize(sounds: ArrayDeque<DynamicSoundInstance>) {
        while (sounds.size >= MAX_ACTIVE_PER_GROUP) {
            if (sounds.isEmpty()) {
                return
            }
            val oldest = sounds.removeFirst()
            oldest.setStop()
        }
    }

    private fun cleanupInactive(minecraft: Minecraft, sounds: ArrayDeque<DynamicSoundInstance>) {
        val iterator = sounds.iterator()
        while (iterator.hasNext()) {
            val sound = iterator.next()
            if (!minecraft.soundManager.isActive(sound) || sound.isStopped) {
                iterator.remove()
            }
        }
    }

    private fun hasSoundResource(minecraft: Minecraft, soundId: ResourceLocation): Boolean {
        val exists = soundResourceExistsCache.computeIfAbsent(soundId) {
            minecraft.resourceManager.getResource(taczSoundLister.idToFile(it)).isPresent
        }
        if (!exists && missingSoundWarned.add(soundId)) {
            TACZExpandsCommon.LOGGER.warn(
                "[TACZExpands Sound] Missing tacz sound resource, skipped. sound={}, path={}",
                soundId,
                taczSoundLister.idToFile(soundId)
            )
        }
        return exists
    }

    private fun normalizeGroup(group: String): String {
        return group.ifBlank { "default" }
    }
}
