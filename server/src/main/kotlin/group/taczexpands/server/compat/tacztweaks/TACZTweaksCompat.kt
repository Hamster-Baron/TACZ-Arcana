package group.taczexpands.server.compat.tacztweaks

import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.util.TacHitResult
import com.tacz.guns.util.block.BlockRayTrace
import me.muksc.tacztweaks.core.BulletRayTracer
import me.muksc.tacztweaks.data.manager.BulletInteractionManager
import me.muksc.tacztweaks.data.manager.BulletParticlesManager
import me.muksc.tacztweaks.data.manager.BulletParticlesManager.EBlockParticleType
import me.muksc.tacztweaks.data.manager.BulletParticlesManager.EEntityParticleType
import me.muksc.tacztweaks.data.manager.BulletSoundsManager
import me.muksc.tacztweaks.data.manager.BulletSoundsManager.EBlockSoundType
import me.muksc.tacztweaks.data.manager.BulletSoundsManager.EEntitySoundType
import me.muksc.tacztweaks.mixin.accessor.ClipContextAccessor
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.EntityCollisionContext
import java.lang.reflect.Field

object TACZTweaksCompat {
    private var rayTracerField: Lazy<Field?> = lazy {
        try {
            val field = BlockRayTrace::class.java.getDeclaredField("tacztweaks\$rayTracer")
            if (field != null) {
                field.isAccessible = true
            }
            field
        } catch (e: NoSuchFieldException) {
            null
        }
    }

    fun initBulletRayTracer(level: Level, context: ClipContext) {
        val field = rayTracerField.value ?: return
        field.set(null, null)

        if (level !is ServerLevel) return
        val collisionContext = (context as ClipContextAccessor).collisionContext
        if (collisionContext !is EntityCollisionContext) return
        val entity = collisionContext.entity
        if (entity !is EntityKineticBullet) return

        field.set(null, BulletRayTracer(entity, level, context))
    }

    fun updatePosition(entity: EntityKineticBullet, position: Vec3) {
        (entity as EntityKineticBulletExtension).`tacztweaks$setPosition`(position)
    }

    fun handleHitBlock(entity: EntityKineticBullet, blockHitResult: BlockHitResult?, blockState: BlockState?): BlockHitResult? {
        val rayTracer = rayTracerField.value?.get(null) as? BulletRayTracer ?: return blockHitResult
        if (rayTracer.entity != entity) return blockHitResult

        if (blockHitResult == null || blockHitResult.type == HitResult.Type.MISS || blockState == null) return blockHitResult
        (entity as EntityKineticBulletExtension).`tacztweaks$setPosition`(blockHitResult.location)
        val interactionResult = BulletInteractionManager.handleBlockInteraction(entity, blockHitResult, blockState)
        BulletParticlesManager.handleBlockParticle(interactionResult.toBlockParticleType(), rayTracer.level, entity, blockHitResult, blockState)
        BulletSoundsManager.handleBlockSound(interactionResult.toBlockSoundType(), rayTracer.level, entity, blockHitResult, blockState)

        if (interactionResult.pierce) return null

        return blockHitResult
    }

    fun handleHitEntity(entity: EntityKineticBullet, result: EntityKineticBullet.EntityResult, pierce: Boolean, isDead: Boolean) {
        val rayTracer = rayTracerField.value?.get(null) as? BulletRayTracer ?: return
        if (rayTracer.entity != entity) return

        (entity as EntityKineticBulletExtension).`tacztweaks$setPosition`(result.hitPos)
        val interactionResult = BulletInteractionManager.InteractionResult(pierce, isDead)
        BulletParticlesManager.handleEntityParticle(interactionResult.toEntityParticleType(), rayTracer.level, entity, result.hitPos, result.entity)
        BulletSoundsManager.handleEntitySound(interactionResult.toEntitySoundType(), rayTracer.level, entity, result.hitPos, result.entity)
    }

    private fun BulletInteractionManager.InteractionResult.toBlockParticleType(): EBlockParticleType = when {
        condition -> EBlockParticleType.BREAK
        pierce -> EBlockParticleType.PIERCE
        else -> EBlockParticleType.HIT
    }

    private fun BulletInteractionManager.InteractionResult.toEntityParticleType(): EEntityParticleType = when {
        condition -> EEntityParticleType.KILL
        pierce -> EEntityParticleType.PIERCE
        else -> EEntityParticleType.HIT
    }

    private fun BulletInteractionManager.InteractionResult.toBlockSoundType(): EBlockSoundType = when {
        condition -> EBlockSoundType.BREAK
        pierce -> EBlockSoundType.PIERCE
        else -> EBlockSoundType.HIT
    }

    private fun BulletInteractionManager.InteractionResult.toEntitySoundType(): EEntitySoundType = when {
        condition -> EEntitySoundType.KILL
        pierce -> EEntitySoundType.PIERCE
        else -> EEntitySoundType.HIT
    }
}