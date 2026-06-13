package group.taczexpands.common.nbt

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity

object PlayerExtras {
    var serverGetPlayerExtraDataDelegate: ((LivingEntity) -> CompoundTag?)? = null
    var clientGetPlayerExtraDataDelegate: ((LivingEntity) -> CompoundTag?)? = null

    fun getPlayerExtraData(player: LivingEntity): CompoundTag? {
        if (!player.level().isClientSide) {
            return serverGetPlayerExtraDataDelegate?.invoke(player)
        } else {
            return clientGetPlayerExtraDataDelegate?.invoke(player)
        }
    }
}