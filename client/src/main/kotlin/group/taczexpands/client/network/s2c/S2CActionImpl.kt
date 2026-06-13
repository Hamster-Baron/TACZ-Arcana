package group.taczexpands.client.network.s2c

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.network.NetworkHandler
import com.tacz.guns.network.message.ClientMessagePlayerMelee
import com.tacz.guns.network.message.ClientMessagePlayerShoot
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.gui.FrostbiteOverlay
import group.taczexpands.client.gui.GunContextManager
import group.taczexpands.client.gui.ScopeManager
import group.taczexpands.client.input.InputManager
import group.taczexpands.client.input.ShakeManager
import group.taczexpands.client.mixin.accessor.IAccessorFirstPersonRenderHandler
import group.taczexpands.client.render.HookManager
import group.taczexpands.client.util.Rotation
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.util.getFixedLookAngle
import net.minecraft.client.Minecraft
import net.minecraft.client.player.Input
import net.minecraft.world.item.ItemStack
import net.minecraftforge.network.NetworkEvent
import java.util.function.BiConsumer
import java.util.function.Supplier

object S2CActionImpl {
    var lastDrawStack: ItemStack = ItemStack.EMPTY

    @JvmStatic
    fun getHandler(): BiConsumer<S2CAction, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::handle)
    }

    @JvmStatic
    fun handle(packet: S2CAction, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.enqueueWork {
                when (packet.action) {
                    S2CAction.Action.RefreshCache -> {
                       val player = Minecraft.getInstance().player ?: return@enqueueWork
                       AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                    }

                    S2CAction.Action.Reload -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        IClientPlayerGunOperator.fromLocalPlayer(player).reload()
                    }

                    S2CAction.Action.Draw -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        TACZExpandsClient.nextSwitchDraw = true
                        if (packet.signal == 0) {
                            IClientPlayerGunOperator.fromLocalPlayer(player).draw(lastDrawStack)
                        } else if (packet.signal == 3) {
                            IAccessorFirstPersonRenderHandler.`taczexpands$onItemChangedInSameSlot`(player.mainHandItem)
                        } else {
                            AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                            val item = player.mainHandItem
                            TimelessAPI.getGunDisplay(item).ifPresent {
                                val anim = it.animationStateMachine
                                if (anim != null) {
                                    anim.context?.setCurrentGunItem(item)

                                    if (GunExtras.getUsingUnderBarrel(item)) {
                                        anim.trigger("sub_transform")
                                    } else {
                                        anim.trigger("transform")
                                    }

                                    val underBarrel = GunExtras.getUnderBarrel(item)
                                    if (underBarrel != null && underBarrel.animationLockTime > 0.0f) {
                                        GunContextManager.lockUnderBarrel(underBarrel.animationLockTime)
                                    }

                                }
                            }
                        }
                        AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                        lastDrawStack = ItemStack.EMPTY
                    }

                    S2CAction.Action.StopShake -> {
                        ShakeManager.shakeList.clear()
                    }

                    S2CAction.Action.SaveDrawStack -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        lastDrawStack = player.mainHandItem.copy()
                    }

                    S2CAction.Action.Shoot -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        IClientPlayerGunOperator.fromLocalPlayer(player).shoot()
                    }

                    S2CAction.Action.Inspect -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        IClientPlayerGunOperator.fromLocalPlayer(player).inspect()
                    }

                    S2CAction.Action.BindCamera -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        val entityID = packet.signal
                        val entity = player.level().getEntity(entityID) ?: return@enqueueWork

                        InputManager.storeLocal()
                        player.input = Input()
                        Minecraft.getInstance().cameraEntity = entity

                        val velocity = entity.deltaMovement
                        if (velocity.lengthSqr() < 0.001) {
                            val (yaw, pitch) = TACZExpandsClient.INSTANCE.vecToYawPitch(entity.getFixedLookAngle())
                            InputManager.yaw = yaw
                            InputManager.pitch = pitch
                        } else {
                            val (yaw, pitch) = TACZExpandsClient.INSTANCE.vecToYawPitch(velocity)
                            InputManager.yaw = yaw
                            InputManager.pitch = pitch
                        }

                        if (entity is EntityKineticBullet) {
                            TACZExpandsClient.INSTANCE.lastCameraEntity = entity
                        }
                        InputManager.sendCameraInput = true
                    }

                    S2CAction.Action.BindTarget -> {
                        ScopeManager.target = packet.signal
                        ScopeManager.time = 0
                    }

                    S2CAction.Action.BindLockingTime -> {
                        ScopeManager.lockingTime = packet.signal
                    }

                    S2CAction.Action.BindCurrentLockingTime -> {
                        ScopeManager.time = packet.signal
                    }

                    S2CAction.Action.Frostbite -> {
                        FrostbiteOverlay.time = packet.signal
                    }

                    S2CAction.Action.Rotate -> {
                        val rotation = Rotation.create(packet.yaw, packet.pitch, packet.signal == 1) ?: return@enqueueWork
                        TACZExpandsClient.rotations.add(rotation)
                    }

                    S2CAction.Action.Hook -> {
                        val hook = packet.hook!!
                        HookManager.processHook(hook)
                    }

                    S2CAction.Action.SetVelocity -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        val velocity = packet.velocity!!

                        player.deltaMovement = velocity

                    }

                    S2CAction.Action.ForceShoot -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        if (packet.signal == 0) {
                            IClientPlayerGunOperator.fromLocalPlayer(player).shoot()
                        } else {
                            NetworkHandler.CHANNEL.sendToServer(ClientMessagePlayerShoot(System.currentTimeMillis() - IClientPlayerGunOperator.fromLocalPlayer(
                                player).dataHolder.clientBaseTimestamp))
                        }
                    }

                    S2CAction.Action.MeleeAttack -> {
                        val player = Minecraft.getInstance().player ?: return@enqueueWork
                        if (packet.signal == 0) {
                            IClientPlayerGunOperator.fromLocalPlayer(player).melee()
                        } else {
                            NetworkHandler.CHANNEL.sendToServer(ClientMessagePlayerMelee())
                        }
                    }
                }
            }
            it.packetHandled = true
        }
    }
}