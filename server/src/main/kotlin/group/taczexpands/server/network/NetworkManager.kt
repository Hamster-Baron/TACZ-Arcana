package group.taczexpands.server.network

import group.taczexpands.common.network.NetworkCommon
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.PacketDistributor

object NetworkManager {
    fun init(): Boolean {
        return NetworkCommon.init()
    }

    fun <T> sendToPlayer(packet: T, player: ServerPlayer) {
        NetworkCommon.CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT)
    }

    fun <T> broadcast(packet: T, source: Entity) {
        NetworkCommon.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with { source }, packet)
    }
}
