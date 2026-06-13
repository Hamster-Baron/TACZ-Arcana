package group.taczexpands.client.network.s2c

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator
import com.tacz.guns.client.gameplay.LocalPlayerReload
import group.taczexpands.client.input.KeyInputs
import group.taczexpands.common.network.s2c.S2CCancelAction
import net.minecraft.client.Minecraft
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CCancelActionImpl {

    @JvmStatic
    fun getHandler(): BiConsumer<S2CCancelAction, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CCancelAction, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.action) {
                    S2CCancelAction.Action.Aim -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        if (!packet.isReset) {
                            IClientPlayerGunOperator.fromLocalPlayer(player).aim(false)
                        }
                        KeyInputs.cancelAim(packet.duration)

                    }

                    S2CCancelAction.Action.Walk -> {
                        KeyInputs.cancelWalk(packet.duration)
                    }

                    S2CCancelAction.Action.Jump -> {
                        KeyInputs.cancelJump(packet.duration)
                    }

                    S2CCancelAction.Action.Fire -> {
                        KeyInputs.cancelFire(packet.duration)
                    }

                    S2CCancelAction.Action.Reload -> {
                        if (!packet.isReset) {
                            val player = Minecraft.getInstance().player ?: return@enqueueWork
                            try {
                                player::class.java.getDeclaredField("tac\$reload").let {
                                    it.isAccessible = true
                                    (it.get(player) as LocalPlayerReload).cancelReload()
                                }
                            } catch (e: Exception) {
                            }
                        }

                        KeyInputs.cancelReload(packet.duration)
                    }

                    S2CCancelAction.Action.Sprint -> {
                        KeyInputs.cancelSprint(packet.duration)
                    }

                    S2CCancelAction.Action.MissileFire -> {
                        KeyInputs.cancelMissileFire(packet.duration)
                    }

                    S2CCancelAction.Action.Zoom -> {

                    }

                    S2CCancelAction.Action.Rotate -> {
                        KeyInputs.cancelRotate(packet.duration)
                    }

                    S2CCancelAction.Action.ShootKey -> {

                    }

                    S2CCancelAction.Action.Inventory -> {
                        KeyInputs.cancelInventory(packet.duration)
                    }

                    S2CCancelAction.Action.Inspect -> {
                        KeyInputs.cancelInspect(packet.duration)
                    }
                }
            }

            it.packetHandled = true
        }
    }
}