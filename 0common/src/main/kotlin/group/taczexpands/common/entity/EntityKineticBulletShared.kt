package group.taczexpands.common.entity

import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.resource.pojo.data.gun.BulletData
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.data.BulletExtraHolder
import group.taczexpands.common.data.GuidanceType
import group.taczexpands.common.util.getFixedLookAngle
import net.minecraft.client.Minecraft
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.*

object EntityKineticBulletShared {
    val IS_MISSILE_TICK_DATA_ACCESSOR = SynchedEntityData.defineId<Boolean>(EntityKineticBullet::class.java,
        EntityDataSerializers.BOOLEAN)

    val OVERLAY_TEXTURE_DATA_ACCESSOR = SynchedEntityData.defineId<String>(EntityKineticBullet::class.java, EntityDataSerializers.STRING)

    val TV_SPEED_DATA_ACCESSOR = SynchedEntityData.defineId<Float>(EntityKineticBullet::class.java, EntityDataSerializers.FLOAT)
    val MAX_SPEED_DATA_ACCESSOR = SynchedEntityData.defineId<Float>(EntityKineticBullet::class.java, EntityDataSerializers.FLOAT)
    val ACCELERATION_LIMIT_DATA_ACCESSOR = SynchedEntityData.defineId<Float>(EntityKineticBullet::class.java, EntityDataSerializers.FLOAT)
    val TV_ROTATION_CLAMP_DATA_ACCESSOR = SynchedEntityData.defineId<Boolean>(EntityKineticBullet::class.java, EntityDataSerializers.BOOLEAN)
    val TV_ROTATION_LOCK_DATA_ACCESSOR = SynchedEntityData.defineId<Boolean>(EntityKineticBullet::class.java, EntityDataSerializers.BOOLEAN)
    val TV_ROTATION_CLAMP_MODIFIER_DATA_ACCESSOR = SynchedEntityData.defineId<Float>(EntityKineticBullet::class.java, EntityDataSerializers.FLOAT)



    var onMissileBulletTickDelegate: ((EntityKineticBullet) -> Pair<Boolean, Boolean>)? = null

    var onMissileAmmoParticleSpawnDelegate: ((EntityKineticBullet) -> Unit)? = null

    var isMissileDroneDelegate: ((EntityKineticBullet) -> Boolean)? = null




}