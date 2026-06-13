package group.taczexpands.server.module.gun_shield

import com.tacz.guns.api.entity.IGunOperator
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.Shield
import group.taczexpands.common.data.ShieldBlockCondition
import group.taczexpands.server.context.Context
import group.taczexpands.server.module.gun_durability.GunDurabilityManager
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import group.taczexpands.server.util.getCenterPosition
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.cos

object GunShieldManager {
    fun getShield(entity: LivingEntity): Shield? {
        val gunExtras = IAccessorGunData.getExtraHolder(entity.mainHandItem)
        return gunExtras?.shield
    }

    fun matchCondition(entity: LivingEntity, condition: ShieldBlockCondition): Boolean {
        when (condition) {
            ShieldBlockCondition.WHEN_NOT_AIMING -> {
                val gunOperator = IGunOperator.fromLivingEntity(entity)
                return !gunOperator.synIsAiming && gunOperator.synAimingProgress <= 0.0f
            }

            ShieldBlockCondition.WHEN_AIMING -> {
                val gunOperator = IGunOperator.fromLivingEntity(entity)
                return gunOperator.synIsAiming && gunOperator.synAimingProgress >= 1.0f
            }

            ShieldBlockCondition.ALWAYS -> {
                return true
            }
        }
    }


    fun isUsingGunShield(entity: LivingEntity): Boolean {
        val shield = getShield(entity)
        if (shield != null) {
            if (matchCondition(entity, shield.blockingCondition)) {
                return true
            }
        }

        return false
    }

    fun shouldBlock(entity: LivingEntity, damagePos: Vec3?, penetration: Int? = null): Boolean {
        damagePos ?: return false

        val shield = getShield(entity) ?: return false

        if (!isUsingGunShield(entity)) return false

        if (!GunDurabilityManager.isNotBroken(entity.mainHandItem)) return false

        if (entity is ServerPlayer) {
            if (entity.cooldowns.isOnCooldown(entity.mainHandItem.item)) return false
        }

        val lookVec = entity.getViewVector(1.0f)

        val attackDir = damagePos.vectorTo(entity.getCenterPosition()).normalize()

        val dotProduct = attackDir.dot(lookVec)

        val targetLimitRad = Math.toRadians((180.0f - shield.blockingAngle).toDouble())
        val cosThreshold = -cos(targetLimitRad)

        if (!(dotProduct < cosThreshold)) return false

        if (penetration == null) return true

        if (penetration >= shield.blockingPower) return false

        return true
    }

    fun getBlockingFactor(entity: LivingEntity, penetration: Int? = null): Float {
        penetration ?: return 1.0f

        val shield = getShield(entity) ?: return 1.0f

        if (shield.blockingPower <= 0.0f) return 1.0f

        if (penetration >= shield.blockingPower) return 0.0f

        if (penetration < shield.blockingPower * 0.5f) return 1.0f

        return Mth.clamp((penetration - shield.blockingPower) / (shield.blockingPower * 0.5f), 0.0f, 1.0f)
    }

    fun dispatchBlock(entity: LivingEntity, damage: Float, blockingFactor: Float) {
        val shield = getShield(entity) ?: return
        val durabilityDamage = shield.baseDurabilityDamage + if (blockingFactor > 0.0f) shield.extraDurabilityDamageOnBlocked else 0
        GunDurabilityManager.damage(entity.mainHandItem, durabilityDamage)

    }

    fun attackedByAxe(entity: LivingEntity) {
        val player = entity as? ServerPlayer ?: return
        val shield = getShield(player) ?: return
        if (shield.canBeDisabledByAxes) {
            player.cooldowns.addCooldown(player.mainHandItem.item, shield.cooldown)
            SkillManager.trigger(TriggerType.ON_SHIELD_DISABLED, Context(player))
        }
    }
}