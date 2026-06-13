package group.taczexpands.server.skill

import group.taczexpands.common.entity.CustomDisplayEntity
import net.minecraft.world.entity.player.Player
import java.util.*

object CustomDisplayManager {
    val instances = mutableMapOf<CustomDisplayEntity, CustomDisplayInstance>()

    fun tickEntity(entity: CustomDisplayEntity) {
        if (!instances.containsKey(entity)) {
            entity.discard()
            return
        }

        instances[entity]!!.tick()
    }

    fun onRemoveEntity(entity: CustomDisplayEntity) {
        if (instances.contains(entity)) {
            instances.remove(entity)
        }
    }

    fun onDeath(entity: CustomDisplayEntity) {
        if (instances.contains(entity)) {
            instances[entity]!!.onDeath()
        }
    }

    fun isBroadcastTo(entity: CustomDisplayEntity, player: Player): Boolean? {
        if (instances.contains(entity)) {
            return instances[entity]!!.isBroadcastTo(player)
        }
        return null
    }

    fun init() {
        CustomDisplayEntity.serverTickDelegate = ::tickEntity
        CustomDisplayEntity.serverRemoveDelegate = ::onRemoveEntity
        CustomDisplayEntity.serverDeathDelegate = ::onDeath
        CustomDisplayEntity.serverBroadcastToPlayerDelegates = ::isBroadcastTo
    }
}