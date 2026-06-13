package group.taczexpands.client.util

import com.tacz.guns.api.GunProperties
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.api.item.gun.AbstractGunItem
import com.tacz.guns.config.common.AmmoConfig
import com.tacz.guns.resource.pojo.data.gun.BulletData
import com.tacz.guns.util.block.BlockRayTrace
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult


fun <T> modifyProperty(owner: LivingEntity, id: String, type: Class<T>, original: T): T {
    val gun = owner.mainHandItem
    val gunItem = gun.item
    if (gunItem is AbstractGunItem) {
        val dataHolder = IGunOperator.fromLivingEntity(owner).dataHolder
        return gunItem.modifyProperty<T>(dataHolder, gun, owner, id, type, original)
    }

    return original
}

fun predictBullet(
    level: Level,
    owner: LivingEntity,
    bulletData: BulletData,
): HitResult? {
    val mc = Minecraft.getInstance() ?: return null
    val cacheProperty = IGunOperator.fromLivingEntity(owner).cacheProperty ?: return null

    var speed: Float = modifyProperty(owner, "ammo_speed", Float::class.java, cacheProperty.getCache(GunProperties.AMMO_SPEED))
    speed *= AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get().toFloat()
    val processedSpeed = Mth.clamp((speed / 20), 0f, Float.MAX_VALUE)


    val lifeSecond = modifyProperty(owner, "bullet_life", Float::class.java, bulletData.lifeSecond)
    val maxTicks = Mth.clamp((lifeSecond * 20.0f).toInt(), 1, Int.MAX_VALUE)
    val baseGravity = Mth.clamp(
        modifyProperty(owner, "bullet_gravity", Float::class.java, bulletData.gravity),
        0.0f,
        Float.MAX_VALUE
    )
    val baseFriction = Mth.clamp(
        modifyProperty(owner, "bullet_friction", Float::class.java, bulletData.friction),
        0.0f,
        Float.MAX_VALUE
    )

    val startPos = owner.getEyePosition(mc.partialTick)
    val startVelocity = owner.getViewVector(mc.partialTick).scale(processedSpeed.toDouble())

    var currentPos = startPos
    var currentVelocity = startVelocity

    for (i in 0 until maxTicks) {
        val nextPos = currentPos.add(currentVelocity)

        val blockHit = BlockRayTrace.rayTraceBlocks(
            level, ClipContext(
                currentPos,
                nextPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
        )
        currentPos = nextPos

        if (blockHit.type != HitResult.Type.MISS) {
            return blockHit
        }


        var activeFriction = baseFriction
        var activeGravity = baseGravity

        val currentBlockPos = BlockPos.containing(currentPos.x, currentPos.y, currentPos.z)
        if (level.getFluidState(currentBlockPos).`is`(FluidTags.WATER)) {
            activeFriction = 0.4f
            activeGravity *= 0.6f
        }

        currentVelocity = currentVelocity.scale((1.0f - activeFriction).toDouble())
        currentVelocity = currentVelocity.add(0.0, (-activeGravity).toDouble(), 0.0)
    }

    val finalBlockPos = BlockPos(Mth.floor(currentPos.x), Mth.floor(currentPos.y), Mth.floor(currentPos.z))
    return BlockHitResult.miss(currentPos, Direction.UP, finalBlockPos)
}