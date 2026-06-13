package group.taczexpands.client.nbt

import group.taczexpands.common.nbt.PlayerExtras
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity

object PlayerExtrasClient {
    var data = CompoundTag()

    fun reset() {
        data = CompoundTag()
    }

    fun getPlayerExtraData(livingEntity: LivingEntity): CompoundTag? {
        if (livingEntity == Minecraft.getInstance().player) return data
        return null
    }

    fun init() {
        PlayerExtras.clientGetPlayerExtraDataDelegate = ::getPlayerExtraData
    }
}