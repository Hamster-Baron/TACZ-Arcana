package group.taczexpands.server.nbt

import group.taczexpands.common.nbt.PlayerExtras
import group.taczexpands.common.network.v2.s2c.S2CSyncPlayerStorage
import group.taczexpands.server.network.NetworkManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.LivingEntity

object PlayerExtrasServer {
    private fun getPlayerExtraScore(): CompoundTagScoreData {
        return DataStorage.get().getOrCreateScore(DataType.COMPOUND_TAG, "__BUILTIN_PLAYER_EXTRA__")
    }

    fun getPlayerExtraData(player: LivingEntity): CompoundTag? {
        val score = getPlayerExtraScore()
        if (!score.hasValue(player)) return null
        return score.getValue(player)
    }

    fun getOrCreatePlayerExtraData(player: LivingEntity): CompoundTag {
        val score = getPlayerExtraScore()
        return score.getValue(player)
    }

    fun setPlayerExtraData(player: LivingEntity, value: CompoundTag) {
        val score = getPlayerExtraScore()
        score.setValue(player, value)
        notify(player, value)
    }

    fun notify(player: LivingEntity, value: CompoundTag) {
        if (player is ServerPlayer) {
            NetworkManager.sendToPlayer(S2CSyncPlayerStorage(value).create(), player)
        }
    }

    fun notify(player: LivingEntity) {
        val value = getPlayerExtraData(player) ?: return
        if (player is ServerPlayer) {
            NetworkManager.sendToPlayer(S2CSyncPlayerStorage(value).create(), player)
        }
    }

    fun init() {
        PlayerExtras.serverGetPlayerExtraDataDelegate = ::getPlayerExtraData
    }
}