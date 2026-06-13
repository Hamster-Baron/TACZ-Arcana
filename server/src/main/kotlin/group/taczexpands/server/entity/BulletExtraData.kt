package group.taczexpands.server.entity

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.resource.pojo.data.gun.BulletData
import group.taczexpands.common.data.FlightProfileType
import group.taczexpands.server.accessor.IAccessorBullet
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

class BulletExtraData {
    var needsInit = true
    var isFirstTick = true
    var target: Entity? = null
    var targetPos: Vec3? = null
    var radarCheckTickCount: Int = 0
    var jammingCheckTickCount: Int = 20
    var jammingTarget: Entity? = null
    var bulletData: BulletData? = null
    var isClimbing: Boolean = false
    var isStriking: Boolean = false
    var enforcingSimpleLocking: Boolean = false
    var flightProfileType: FlightProfileType? = null
    val hitEntities = mutableMapOf<EntityHurtByGunEvent.Pre, Boolean>()
    companion object {
        fun get(bullet: EntityKineticBullet): BulletExtraData {
            return (bullet as IAccessorBullet).`taczexpands$getBulletExtraData`()
        }
    }

    fun addHitEntity(event: EntityHurtByGunEvent.Pre, isExplosion: Boolean) {
        hitEntities.put(event, isExplosion)
    }
}