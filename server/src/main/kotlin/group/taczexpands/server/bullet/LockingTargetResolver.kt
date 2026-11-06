package group.taczexpands.server.bullet

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.init.ModItems
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.BulletExtraHolder
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.server.listener.PlayerListener
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import kotlin.jvm.optionals.getOrNull

object LockingTargetResolver {
    enum class Mode(val parameterValue: String, val defaultRequireAiming: Boolean) {
        EFFECTIVE("effective", true),
        SIMPLE("simple", true),
        RAW("raw", false),
        SKILL("skill", false);

        companion object {
            fun fromParameter(value: String): Mode? = entries.firstOrNull { it.parameterValue == value }
        }
    }

    fun resolve(player: ServerPlayer, mode: Mode = Mode.EFFECTIVE, requireAiming: Boolean = mode.defaultRequireAiming): Entity? {
        if (requireAiming && !IGunOperator.fromLivingEntity(player).synIsAiming) return null
        val playerState = PlayerListener.getPlayerStates(player)
        val target = when (mode) {
            Mode.RAW -> playerState.lockingTarget
            Mode.SKILL -> playerState.skillLockingTarget
            Mode.SIMPLE -> {
                val bulletExtraHolder = getCurrentMissileData(player) ?: return null
                if (playerState.lockingTime < bulletExtraHolder.missileData.lockingTime) return null
                playerState.lockingTarget
            }
            Mode.EFFECTIVE -> {
                val bulletExtraHolder = getCurrentMissileData(player) ?: return null
                if (bulletExtraHolder.missileData.isSimpleLocking || GunExtras.getEnforcingSimpleLocking(player.mainHandItem)) {
                    if (playerState.lockingTime < bulletExtraHolder.missileData.lockingTime) return null
                    playerState.lockingTarget
                } else {
                    playerState.skillLockingTarget
                }
            }
        }
        return target?.takeIf { !it.isRemoved && it.level() == player.level() && (it !is LivingEntity || it.isAlive) }
    }

    private fun getCurrentMissileData(player: ServerPlayer): BulletExtraHolder? {
        val mainHand = player.mainHandItem
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (mainHand.item != gunItem) return null
        val gunIndex = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return null
        val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)
        val bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData)
        return bulletExtraHolder.takeIf { it.missileData.isMissile }
    }
}
