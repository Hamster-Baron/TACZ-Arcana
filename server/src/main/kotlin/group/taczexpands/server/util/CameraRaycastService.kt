package group.taczexpands.server.util

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.init.ModItems
import com.tacz.guns.util.block.BlockRayTrace
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.util.getFixedLookAngle
import group.taczexpands.server.config.ServerConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import kotlin.jvm.optionals.getOrNull

object CameraRaycastService {
    fun trace(viewer: Entity): CameraTraceResult {
        val view = if (viewer is ServerPlayer) CameraViewStateManager.resolve(viewer)
        else CameraViewStateManager.CameraView(viewer.eyePosition, viewer.getFixedLookAngle(), viewer, viewer)
        val maxDistance = resolveMaxDistance(viewer)
        if (maxDistance <= 0.0) return CameraTraceResult(HitResult.Type.MISS, null, null, null, null)
        val direction = view.direction.normalize()
        if (direction.lengthSqr() < 1.0E-12) return CameraTraceResult(HitResult.Type.MISS, null, null, null, null)
        val end = view.origin.add(direction.scale(maxDistance))
        val blockHitResult = BlockRayTrace.rayTraceBlocks(viewer.level(), ClipContext(view.origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, view.sourceEntity))
        val entityEnd = if (blockHitResult.type == HitResult.Type.BLOCK) blockHitResult.location else end
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            view.sourceEntity, view.origin, entityEnd, AABB(view.origin, entityEnd).inflate(1.0),
            { entity -> entity !== view.viewer && entity !== view.sourceEntity && !entity.isSpectator && entity.isPickable },
            view.origin.distanceToSqr(entityEnd)
        )
        if (entityHitResult != null) {
            return CameraTraceResult(HitResult.Type.ENTITY, entityHitResult.location, null, entityHitResult.entity, view.origin.distanceTo(entityHitResult.location))
        }
        if (blockHitResult.type == HitResult.Type.BLOCK) {
            return CameraTraceResult(HitResult.Type.BLOCK, blockHitResult.location, blockHitResult.blockPos, null, view.origin.distanceTo(blockHitResult.location))
        }
        return CameraTraceResult(HitResult.Type.MISS, null, null, null, null)
    }

    private fun resolveMaxDistance(viewer: Entity): Double {
        val configuredDistance = ServerConfig.rayTraceDistance.get().toDouble().takeIf { it.isFinite() && it > 0.0 } ?: return 0.0
        if (viewer !is LivingEntity) return configuredDistance
        val missileDistance = runCatching {
            val gunItem = ModItems.MODERN_KINETIC_GUN.get()
            val mainHand = viewer.mainHandItem
            if (mainHand.item != gunItem) return@runCatching null
            val gunIndex = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return@runCatching null
            val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)
            val missileData = IAccessorBulletData.getBulletExtraHolder(bulletData).missileData
            missileData.maxLockingDistance.toDouble().takeIf { missileData.isMissile && it.isFinite() && it > 0.0 }
        }.getOrNull()
        return missileDistance?.coerceAtMost(configuredDistance) ?: configuredDistance
    }
}
