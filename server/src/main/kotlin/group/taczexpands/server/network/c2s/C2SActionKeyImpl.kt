package group.taczexpands.server.network.c2s

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.c2s.C2SActionKey
import group.taczexpands.common.network.c2s.C2SActionKey.Action
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.context.Context
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.skill.HookManager
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

object C2SActionKeyImpl {

    @JvmStatic
    fun getHandler(): BiConsumer<C2SActionKey, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: C2SActionKey, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                val player = it.sender ?: return@enqueueWork
                val action = packet.action

                when (action) {
                    Action.ACTION_1 -> {
                        SkillManager.trigger(TriggerType.ON_ACTION_1, Context(player))
                    }

                    Action.ACTION_2 -> {
                        SkillManager.trigger(TriggerType.ON_ACTION_2, Context(player))
                    }

                    Action.ACTION_3 -> {
                        SkillManager.trigger(TriggerType.ON_ACTION_3, Context(player))
                    }

                    Action.ACTION_4 -> {
                        SkillManager.trigger(TriggerType.ON_ACTION_4, Context(player))
                    }

                    Action.SWITCH_UNDERBARREL -> {
                        if (IGunOperator.fromLivingEntity(player).synReloadState.stateType.isReloading) return@enqueueWork
                        val mainHand = player.mainHandItem
                        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                        if (mainHand.item == gunItem) {
                            val currentUsing = GunExtras.getUsingUnderBarrel(mainHand)
                            if (currentUsing) {
                                GunExtras.setUsingUnderBarrel(mainHand, false)
                                AttachmentPropertyManager.postChangeEvent(player, mainHand)
                                player.inventoryMenu.broadcastChanges()
                                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.Draw, 2), player)
                                SkillManager.triggerReverse(TriggerType.ON_SWITCH_TO_UNDER_BARREL, Context(player))
                                SkillManager.trigger(TriggerType.ON_SWITCH_TO_GUN, Context(player))
                            } else {
                                val underBarrel = GunExtras.getUnderBarrel(mainHand) ?: return@enqueueWork
                                TimelessAPI.getCommonGunIndex(underBarrel.gunId).getOrNull()
                                    ?: return@enqueueWork
                                GunExtras.setUsingUnderBarrel(mainHand, true)
                                AttachmentPropertyManager.postChangeEvent(player, mainHand)
                                player.inventoryMenu.broadcastChanges()
                                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.Draw, 1), player)
                                SkillManager.triggerReverse(TriggerType.ON_SWITCH_TO_GUN, Context(player))
                                SkillManager.trigger(TriggerType.ON_SWITCH_TO_UNDER_BARREL, Context(player))
                            }
                        }
                    }

                    Action.LASER -> {
                        val mainHand = player.mainHandItem
                        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                        if (mainHand.item == gunItem) {
                            GunExtras.setLaser(mainHand, !GunExtras.getLaser(mainHand))
                            AttachmentPropertyManager.postChangeEvent(player, mainHand)
                            player.inventoryMenu.broadcastChanges()
                            SkillManager.trigger(TriggerType.ON_SWITCH_LASER, Context(player))
                        }
                    }

                    Action.FLASHLIGHT -> {
                        val mainHand = player.mainHandItem
                        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                        if (mainHand.item == gunItem) {
                            GunExtras.setFlashlight(mainHand, !GunExtras.getFlashlight(mainHand))
                            player.inventoryMenu.broadcastChanges()
                            SkillManager.trigger(TriggerType.ON_SWITCH_FLASHLIGHT, Context(player))
                        }
                    }

                    Action.SHOOT_DOWN -> {
                        if (!ActionManager.shouldCancel(player, S2CCancelAction.Action.ShootKey))
                            PlayerListener.getPlayerStates(player).shootKeyDown = true
                    }

                    Action.SHOOT_RELEASE -> {
                        PlayerListener.getPlayerStates(player).shootKeyDown = false
                    }

                    Action.CAMERA -> {
                        val cameraData = MissileManager.tvGuidancePlayers[player] ?: return@enqueueWork
                        if (cameraData.yaw == null || cameraData.pitch == null) return@enqueueWork
                        if (packet.yaw != null && packet.pitch != null && packet.input != null) {
                            cameraData.yaw = packet.yaw
                            cameraData.pitch = packet.pitch
                            cameraData.input = packet.input!!
                        }
                    }

                    Action.UNBIND_CAMERA -> {
                        MissileManager.tvGuidancePlayers.remove(player) ?: return@enqueueWork
                    }

                    Action.INSPECT -> {
                        SkillManager.trigger(TriggerType.ON_INSPECT, Context(player))
                    }

                    Action.RELEASE_HOOK -> {
                        HookManager.listHookEntity.forEach {
                            if (it.from == player) {
                                it.removed = true
                            }
                        }

                        HookManager.listHookBlock.forEach {
                            if (it.from == player) {
                                it.removed = true
                            }
                        }
                    }

                    Action.UPDATE_CLIENT_CAMERA -> {
                        val state = PlayerListener.getPlayerStates(player)
                        if (state.clientCamera == null) {
                            state.clientCamera = PlayerListener.ClientCamera(0.0f, 0.0f)
                        }
                        val camera = state.clientCamera!!

                        if (packet.yaw != null && packet.pitch != null) {
                            camera.yaw = packet.yaw!!
                            camera.pitch = packet.pitch!!
                        }
                    }

                    Action.UNBIND_CLIENT_CAMERA -> {
                        PlayerListener.getPlayerStates(player).clientCamera = null
                    }

                    Action.UPDATE_CHARGE -> {
                        val value = packet.yaw ?: 0.0f
                        PlayerListener.getPlayerStates(player).chargeProgress = value
                    }

                }
            }

            it.packetHandled = true
        }
    }
}
