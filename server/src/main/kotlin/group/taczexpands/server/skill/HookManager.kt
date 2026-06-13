package group.taczexpands.server.skill

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.common.data.HookData
import group.taczexpands.common.data.HookInstance
import group.taczexpands.common.data.HookType
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.context.HookBlockContext
import group.taczexpands.server.context.HookEntityContext
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.util.immutable
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

private const val INFLATE_AMOUNT = 2.0
private const val DAMPING_DISTANCE = 6.0
private const val PULL_SMOOTHNESS = 0.25
private const val BOTTOM_CHECK_DELAY = 10


fun applyScaledPullForce(entity: LivingEntity, targetPosition: Vec3, maxForce: Double) {
    val currentPos = entity.position()
    val deltaVec = targetPosition.subtract(currentPos)
    val distance = deltaVec.length()

    if (distance < INFLATE_AMOUNT) {
        val newMovement = entity.deltaMovement.multiply(0.1, 1.0, 0.1)
        entity.setDeltaMovement(newMovement)
        if (entity is ServerPlayer) {
            NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = newMovement), entity)
        }
        return
    }

    val dampingFactor = Mth.clamp(distance / DAMPING_DISTANCE, 0.0, 1.0)
    val pullDirection = deltaVec.normalize()

    val calculatedMaxSpeed = maxForce

    val targetVelocity = pullDirection.scale(calculatedMaxSpeed * dampingFactor)

    val requiredChange = targetVelocity.subtract(entity.deltaMovement)
    val pullForce = requiredChange.scale(PULL_SMOOTHNESS)

    val finalForceY = Mth.clamp(pullForce.y, -0.5, 0.5)

    val newMovement = entity.deltaMovement.add(pullForce.x, finalForceY, pullForce.z)

    entity.setDeltaMovement(newMovement)
    entity.hasImpulse = true

    if (entity is ServerPlayer) {
        NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = newMovement), entity)
    }
}

fun checkEntityLineOfSight(checkingEntity: LivingEntity, targetEntity: LivingEntity, tick: Int): Boolean {
    val level = checkingEntity.level()
    val startPosEye = checkingEntity.getEyePosition()
    val startPosBottom = checkingEntity.position()
    val endPos = targetEntity.getEyePosition()

    val blockHitResultEye = level.clip(
        ClipContext(startPosEye, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, checkingEntity)
    )

    val blockHitResultBottom = if (tick > BOTTOM_CHECK_DELAY) level.clip(
        ClipContext(startPosBottom, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, checkingEntity)
    ) else null

    val hitResult = when {
        blockHitResultEye.type == HitResult.Type.BLOCK -> blockHitResultEye
        blockHitResultBottom?.type == HitResult.Type.BLOCK -> blockHitResultBottom
        else -> null
    }

    return hitResult == null
}

class HookEntity(val from: LivingEntity, val to: LivingEntity, val hookData: HookData) {
    var removed = false
    var tick = 0
    fun update(): Boolean {
        if (removed) return false
        tick++
        val type = hookData.type
        val pullForce = hookData.force

        if (!from.isAlive || !to.isAlive || from.level() != to.level()) {
            return false
        }

        val delta = to.position().subtract(from.position())
        val distance = delta.length()

        if (distance > hookData.maxLength) {
            return false
        }

        val lineOfSightCheckPassed: Boolean = when (type) {
            HookType.PULLING -> checkEntityLineOfSight(to, from, tick)
            HookType.CHASING -> checkEntityLineOfSight(from, to, tick)
            HookType.CONVERGING -> checkEntityLineOfSight(from, to, tick) && checkEntityLineOfSight(to, from, tick)
        }

        if (!lineOfSightCheckPassed) {
            return false
        }

        val inflatedTargetBox = to.boundingBox.inflate(INFLATE_AMOUNT)
        if (from.boundingBox.intersects(inflatedTargetBox)) {
            return false
        }

        when (type) {
            HookType.PULLING -> {
                val targetPos = from.position()
                applyScaledPullForce(to, targetPos, pullForce)
            }

            HookType.CHASING -> {
                val targetPos = to.position()
                applyScaledPullForce(from, targetPos, pullForce)
            }

            HookType.CONVERGING -> {
                val targetPosFrom = to.position()
                val targetPosTo = from.position()

                applyScaledPullForce(from, targetPosFrom, pullForce / 2.0)
                applyScaledPullForce(to, targetPosTo, pullForce / 2.0)
            }
        }

        return true
    }
}

class HookBlock(val from: LivingEntity, val blockPos: BlockPos, val to: Vec3, val hookData: HookData, val blockHitResult: BlockHitResult) {
    var removed = false
    var tick = 0
    fun update(): Boolean {
        if (removed) return false
        tick++
        val type = hookData.type
        val pullForce = hookData.force

        if (!from.isAlive) {
            return false
        }

        val level = from.level()
        val blockState = level.getBlockState(blockPos)

        if (blockState.isAir() || blockState.liquid()) {
            return false
        }

        if (type == HookType.PULLING) {
            return false
        }

        val delta = to.subtract(from.position())
        val distance = delta.length()

        if (distance > hookData.maxLength) {
            return false
        }

        val startPosEye = from.getEyePosition()
        val startPosBottom = from.position()
        val endPos = to

        val blockHitResultEye = level.clip(
            ClipContext(startPosEye, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)
        )

        val blockHitResultBottom = if (tick > BOTTOM_CHECK_DELAY) level.clip(
            ClipContext(startPosBottom, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)
        ) else null

        val hitResult = when {
            blockHitResultEye.type == HitResult.Type.BLOCK -> blockHitResultEye
            blockHitResultBottom?.type == HitResult.Type.BLOCK -> blockHitResultBottom
            else -> null
        }

        if (hitResult != null) {
            return false
        }

        val targetAABB = AABB(to.x, to.y, to.z, to.x, to.y, to.z).inflate(INFLATE_AMOUNT)

        if (from.boundingBox.intersects(targetAABB)) {
            return false
        }

        when (type) {
            HookType.CHASING, HookType.CONVERGING -> {
                applyScaledPullForce(from, to, pullForce)
            }

            else -> {}
        }

        return true
    }
}

object HookManager {
    val listHookEntity = mutableListOf<HookEntity>()
    val listHookBlock = mutableListOf<HookBlock>()

    fun addHook(bullet: EntityKineticBullet, from: LivingEntity, to: LivingEntity, hookData: HookData) {
        listHookEntity.add(HookEntity(from, to, hookData))
        NetworkManager.broadcast(S2CAction(S2CAction.Action.Hook, hook = HookInstance(HookInstance.Type.ATTACH_ENTITY, from.id, to.id, Vec3.ZERO, hookData)),
            from)
        if (from is ServerPlayer)
            SkillManager.trigger(TriggerType.ON_HOOK_ATTACH_ENTITY, HookEntityContext(from, to, bullet))
    }

    fun addHook(bullet: EntityKineticBullet, from: LivingEntity, blockHitResult: BlockHitResult, hookData: HookData) {
        val result = blockHitResult.immutable()
        val blockPos = result.blockPos
        val to = result.location

        listHookBlock.add(HookBlock(from, blockPos, to, hookData, blockHitResult))
        NetworkManager.broadcast(S2CAction(S2CAction.Action.Hook, hook = HookInstance(HookInstance.Type.ATTACH_BLOCK, from.id, 0, to, hookData)), from)
        if (from is ServerPlayer)
            SkillManager.trigger(TriggerType.ON_HOOK_ATTACH_BLOCK, HookBlockContext(from, result, bullet))
    }

    fun onServerTick() {
        listHookEntity.removeIf {
            if (!it.update()) {
                NetworkManager.broadcast(S2CAction(S2CAction.Action.Hook,
                    hook = HookInstance(HookInstance.Type.DETACH_ENTITY, it.from.id, it.to.id, Vec3.ZERO, it.hookData)), it.from)
                if (it.from is ServerPlayer)
                    SkillManager.trigger(TriggerType.ON_HOOK_DETACH_ENTITY, HookEntityContext(it.from, it.to, null))
                true
            } else false
        }
        listHookBlock.removeIf {
            if (!it.update()) {
                NetworkManager.broadcast(S2CAction(S2CAction.Action.Hook,
                    hook = HookInstance(HookInstance.Type.DETACH_BLOCK, it.from.id, 0, it.to, it.hookData)), it.from)
                if (it.from is ServerPlayer)
                    SkillManager.trigger(TriggerType.ON_HOOK_DETACH_BLOCK, HookBlockContext(it.from, it.blockHitResult, null))
                true
            } else false
        }
    }
}

