package group.taczexpands.server.module.dash

import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.config.action.Dash
import group.taczexpands.server.network.NetworkManager
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

class DashInstance(
    val player: ServerPlayer,
    val mode: Dash.Mode,
    val direction: Vec3,
    var speed: Double,
    val drag: Double,
    val applyEnvironment: Boolean,
    val damageType: ResourceLocation,
    val impactDamage: Float,
    val selfImpactDamage: Float
) {
    private val damageTypeKey = ResourceKey.create(Registries.DAMAGE_TYPE, damageType)

    fun tick(): Boolean {
        if (speed < 0.1 || !player.isAlive) {
            onFinish()
            return false
        }

        applyMovement()
        handleEntityCollision()

        if (checkBlockCollision()) {
            onFinish()
            return false
        }

        speed *= drag

        return true
    }

    private fun applyMovement() {
        val currentVel = direction.scale(speed)

        when (mode) {
            Dash.Mode.BLINK -> {
                player.noPhysics = true
                player.deltaMovement = currentVel

                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = currentVel), player)

            }

            Dash.Mode.GROUND_FOLLOW -> {
                val frontPos = player.blockPosition().relative(player.direction)
                if (player.level().getBlockState(frontPos).isRedstoneConductor(player.level(), frontPos)) {
                    if (!player.level().getBlockState(frontPos.above()).isSolidRender(player.level(), frontPos.above())) {
                        player.deltaMovement = Vec3(currentVel.x, 0.42, currentVel.z)
                        NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = Vec3(currentVel.x, 0.42, currentVel.z)), player)
                    }
                } else {
                    player.deltaMovement = currentVel
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = currentVel), player)
                }
            }

            else -> {
                player.deltaMovement = currentVel
                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = currentVel), player)
            }
        }

        if (!applyEnvironment) {
            player.hurtMarked = true
        }
    }

    private fun checkBlockCollision(): Boolean {
        if (mode == Dash.Mode.BLINK) return false

        if (player.horizontalCollision || player.minorHorizontalCollision) {
            applyRecoil()
            return true
        }
        return false
    }

    private fun handleEntityCollision() {
        val targets = player.level().getEntities(player, player.boundingBox.inflate(0.5)) { it is LivingEntity }
        if (targets.isEmpty()) return

        val damageTypes = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
        val holder = damageTypes.getHolder(damageTypeKey).get()
        val damageSource = DamageSource(holder, player)

        targets.forEach { target ->
            target.hurt(damageSource, impactDamage)
        }
    }

    private fun applyRecoil() {
        val damageTypes = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
        val holder = damageTypes.getHolder(damageTypeKey).get()
        val damageSource = DamageSource(holder, player)

        player.hurt(damageSource, selfImpactDamage)
    }

    private fun onFinish() {
        if (mode == Dash.Mode.BLINK) {
            player.noPhysics = false
            player.deltaMovement = Vec3.ZERO
            NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = Vec3.ZERO), player)
        }
    }
}