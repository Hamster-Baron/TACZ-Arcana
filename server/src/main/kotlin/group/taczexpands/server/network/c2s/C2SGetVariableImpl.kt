package group.taczexpands.server.network.c2s

import group.taczexpands.common.network.c2s.C2SGetVariable
import group.taczexpands.common.network.s2c.S2CSendVariable
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.network.NetworkManager
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object C2SGetVariableImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<C2SGetVariable, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: C2SGetVariable, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                val player = it.sender ?: return@enqueueWork
                val expression = packet.variable
                val result = if (expression.all { it.isLetterOrDigit() || it == '_' }) {
                    try {
                        val expression =
                            ExpressionHelper.initExpression("str($expression)", Context(player), player)
                        expression.evaluate().stringValue
                    } catch (e: Exception) {
                        "未知"
                    }
                } else "未知"

                NetworkManager.sendToPlayer(S2CSendVariable(S2CSendVariable.Type.VARIABLE, packet.variable, result),
                    player)
            }

            it.packetHandled = true
        }
    }
}