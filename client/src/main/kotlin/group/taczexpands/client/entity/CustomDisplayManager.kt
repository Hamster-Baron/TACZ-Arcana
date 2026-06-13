package group.taczexpands.client.entity

import group.taczexpands.common.entity.CustomDisplayEntity
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import java.util.*

object CustomDisplayManager {
    val cachedInstances = mutableMapOf<UUID, CustomDisplayInstance>()

    fun onReload() {
        cachedInstances.clear()
    }

    fun getInstance(entity: CustomDisplayEntity): CustomDisplayInstance? {
        if (!cachedInstances.containsKey(entity.uuid)) {
            try {
                val tag = entity.renderData
                if (tag.contains("modelID")
                    && tag.contains("textureID")
                    && tag.contains("animationID")
                    && tag.contains("animationName")
                    && tag.contains("animationDelay")) {
                    val modelID = ResourceLocation(tag.getString("modelID"))
                    val textureID = ResourceLocation(tag.getString("textureID"))
                    val animationID = tag.getString("animationID").let { if (it == "") null else ResourceLocation(it) }
                    val animationName = tag.getString("animationName").let { if (it == "") null else it }
                    val animationDelay = tag.getInt("animationDelay")
                    cachedInstances[entity.uuid] = CustomDisplayInstance(entity,
                        modelID,
                        textureID,
                        animationID,
                        animationName,
                        animationDelay)
                } else {
                    return null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return cachedInstances[entity.uuid]
    }

    fun tryGetTextureID(entity: CustomDisplayEntity): ResourceLocation? {
        val tag = entity.renderData
        if (tag.contains("textureID", Tag.TAG_STRING.toInt())) {
            val textureID = ResourceLocation(tag.getString("textureID"))
            return textureID
        }
        return null
    }

    fun tickEntity(entity: CustomDisplayEntity) {
        if (cachedInstances.contains(entity.uuid)) {
            cachedInstances[entity.uuid]!!.tick()
        }
    }

    fun onRemoveEntity(entity: CustomDisplayEntity) {
        if (cachedInstances.contains(entity.uuid)) {
            cachedInstances.remove(entity.uuid)
        }
    }

    fun onDeath(entity: CustomDisplayEntity) {
        if (cachedInstances.contains(entity.uuid)) {
            cachedInstances[entity.uuid]!!.onDeath()
        }
    }

    fun init() {
        CustomDisplayEntity.clientTickDelegate = ::tickEntity
        CustomDisplayEntity.clientRemoveDelegate = ::onRemoveEntity
        CustomDisplayEntity.clientDeathDelegate = ::onDeath
    }

}

