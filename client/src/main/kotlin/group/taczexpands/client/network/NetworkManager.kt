package group.taczexpands.client.network

import group.taczexpands.common.network.NetworkCommon
import net.minecraft.client.Minecraft

object NetworkManager {
    fun init(): Boolean {
        return NetworkCommon.init()
    }

    fun sendToServer(packet: Any) {
        Minecraft.getInstance().connection ?: return
        NetworkCommon.CHANNEL.sendToServer(packet)
    }
}
