package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.CompoundTagSerializer
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level

@Serializable
data class S2CSyncPlayerStorage(@Serializable(with = CompoundTagSerializer::class) val data: CompoundTag) {
    companion object {
        const val NETWORK_INDEX = 3
    }

    fun create(): S2CRaw {
        return S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }
}