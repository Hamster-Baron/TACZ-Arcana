package group.taczexpands.client.network.s2c

import group.taczexpands.client.gui.GunContextManager
import group.taczexpands.client.gui.VariableManager
import group.taczexpands.common.network.s2c.S2CSendVariable
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CSendVariableImpl {
    @JvmStatic
    fun getHandler(): BiConsumer<S2CSendVariable, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CSendVariable, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.type) {
                    S2CSendVariable.Type.VARIABLE -> {
                        VariableManager.add(packet.variable, packet.value)
                    }

                    S2CSendVariable.Type.INVALIDATE_VARIABLE -> {
                        VariableManager.invalidate(packet.variable)
                    }

                    S2CSendVariable.Type.INVALIDATE_ALL_VARIABLE -> {
                        VariableManager.invalidateAll()
                    }

                    S2CSendVariable.Type.UTIL_API -> {
                        GunContextManager.setScopeElementRenderState(packet.variable, packet.value == "true")
                    }

                    S2CSendVariable.Type.VOLATILE -> {
                        when (packet.variable) {
                            "render_util" -> {
                                val value = packet.value == "true"
                                GunContextManager.scopeElementVolatile = value
                            }

                            "animation_redirect" -> {
                                val value = packet.value == "true"
                                GunContextManager.animationRedirectVolatile = value
                            }
                        }
                    }
                }
            }

            it.packetHandled = true
        }
    }
}