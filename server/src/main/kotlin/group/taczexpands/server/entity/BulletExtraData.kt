package group.taczexpands.server.entity

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.resource.pojo.data.gun.BulletData
import group.taczexpands.common.data.FlightProfileType
import group.taczexpands.common.data.BulletExtraHolder
import group.taczexpands.server.accessor.IAccessorServerBullet
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
    private var proximityFuzeLaunchPosition: Vec3? = null
    private var proximityFuzeLaunchTick = 0
    private var proximityFuzeArmed = false
    companion object {
        fun get(bullet: EntityKineticBullet): BulletExtraData {
            return (bullet as IAccessorServerBullet).`taczexpands$getBulletExtraData`()
        }
    }

    fun addHitEntity(event: EntityHurtByGunEvent.Pre, isExplosion: Boolean) {
        hitEntities.put(event, isExplosion)
    }

    fun initializeProximityFuze(bullet: EntityKineticBullet) {
        proximityFuzeLaunchPosition = bullet.position()
        proximityFuzeLaunchTick = bullet.tickCount
        proximityFuzeArmed = false
    }

    fun isProximityFuzeArmed(bullet: EntityKineticBullet, bulletExtraHolder: BulletExtraHolder): Boolean {
        if (proximityFuzeArmed) return true
        val launchPosition = proximityFuzeLaunchPosition ?: bullet.position().also {
            proximityFuzeLaunchPosition = it
            proximityFuzeLaunchTick = bullet.tickCount
        }
        val armingTicks = maxOf(
            bulletExtraHolder.proximityFuzeArmingTicks,
            bulletExtraHolder.missileData.proximityFuzeArmingTicks
        ).coerceAtLeast(0)
        val armingDistance = maxOf(
            bulletExtraHolder.proximityFuzeArmingDistance,
            bulletExtraHolder.missileData.proximityFuzeArmingDistance
        ).coerceAtLeast(0.0f)
        val elapsedTicks = (bullet.tickCount - proximityFuzeLaunchTick).coerceAtLeast(0)
        val traveledDistanceSqr = bullet.position().distanceToSqr(launchPosition)
        proximityFuzeArmed = elapsedTicks >= armingTicks && traveledDistanceSqr >= armingDistance.toDouble() * armingDistance.toDouble()
        return proximityFuzeArmed
    }
}
