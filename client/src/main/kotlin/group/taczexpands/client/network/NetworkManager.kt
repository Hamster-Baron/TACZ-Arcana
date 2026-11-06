package group.taczexpands.client.network

import group.taczexpands.common.network.NetworkCommon
import net.minecraft.client.Minecraft
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol

object NetworkManager {
    fun init(): Boolean = NetworkCommon.init()

    fun canSendPlayPacket(): Boolean {
        val listener = Minecraft.getInstance().connection ?: return false
        val connection = listener.connection
        if (!connection.isConnected) return false
        return connection.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get() == ConnectionProtocol.PLAY
    }

    fun sendToServer(packet: Any): Boolean {
        if (!canSendPlayPacket()) return false
        NetworkCommon.CHANNEL.sendToServer(packet)
        return true
    }
}
