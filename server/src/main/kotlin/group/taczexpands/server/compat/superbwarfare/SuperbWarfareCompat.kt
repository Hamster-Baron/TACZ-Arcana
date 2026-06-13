package group.taczexpands.server.compat.superbwarfare

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.Entity

object SuperbWarfareCompat {
    fun getHealth(entity: Entity): Float? {
        if(entity is VehicleEntity)
            return entity.health
        return null
    }

    fun getMaxHealth(entity: Entity): Float? {
        if(entity is VehicleEntity)
            return entity.maxHealth
        return null
    }

    fun setHealth(entity: Entity, newHealth: Float) {
        if(entity is VehicleEntity)
            entity.health = newHealth
    }
}