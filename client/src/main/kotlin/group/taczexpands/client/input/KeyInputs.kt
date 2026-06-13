package group.taczexpands.client.input

import com.mojang.blaze3d.platform.InputConstants
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.api.item.IGun
import com.tacz.guns.client.input.ShootKey
import com.tacz.guns.init.ModItems
import group.taczexpands.client.gui.SelectAmmoScreen
import group.taczexpands.client.mixin.accessor.IAccessorKeyMapping
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.client.render.HookManager
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.HookData
import group.taczexpands.common.network.c2s.C2SActionKey
import group.taczexpands.common.network.v2.c2s.C2SMouseScroll
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.glfw.GLFW

object KeyInputs {
    val SwitchAmmoKey = KeyMapping(
        "key.taczexpands.switch_ammo.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.category.tacz"
    )

    val SwitchUnderBarrelKey = KeyMapping(
        "key.taczexpands.switch_under_barrel.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        "key.category.tacz"
    )

    val Action1Key = KeyMapping(
        "key.taczexpands.action_1.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_1,
        "key.category.tacz"
    )

    val Action2Key = KeyMapping(
        "key.taczexpands.action_2.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_2,
        "key.category.tacz"
    )

    val Action3Key = KeyMapping(
        "key.taczexpands.action_3.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_3,
        "key.category.tacz"
    )

    val Action4Key = KeyMapping(
        "key.taczexpands.action_4.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_4,
        "key.category.tacz"
    )

    val ToggleLaserKey = KeyMapping(
        "key.taczexpands.toggle_laser.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_5,
        "key.category.tacz"
    )

    val ToggleFlashlightKey = KeyMapping(
        "key.taczexpands.toggle_flashlight.desc",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_KP_6,
        "key.category.tacz"
    )

    fun init(event: RegisterKeyMappingsEvent) {
        event.register(SwitchAmmoKey)
        event.register(SwitchUnderBarrelKey)
        event.register(Action1Key)
        event.register(Action2Key)
        event.register(Action3Key)
        event.register(Action4Key)
        event.register(ToggleLaserKey)
        event.register(ToggleFlashlightKey)
    }

    fun matches(key: KeyMapping, event: InputEvent.Key): Boolean {
        if (key.matches(event.key, event.scanCode)) return true
        return false
    }

    fun rotate(yaw: Float, pitch: Float, relative: Boolean) {

    }


    @SubscribeEvent
    fun onKeyPress(event: InputEvent.Key) {
        if (isInGame()) {
            if (event.action == GLFW.GLFW_PRESS) {
                if (matches(SwitchAmmoKey, event)) {
                    val player = Minecraft.getInstance().player ?: return
                    val mainHand = player.mainHandItem
                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    if (mainHand.item != gunItem) return
                    val gunId = gunItem.getGunId(mainHand)
                    TimelessAPI.getCommonGunIndex(gunId).ifPresent {
                        val extraList = IAccessorGunData.getExtraAmmoList(it.gunData, mainHand)
                        if (extraList != null) {
                            val ammoList = (listOf(IAccessorGunData.getCurrentBaseAmmo(it.gunData, mainHand)?.ammoId ?: it.gunData.ammoId)
                                    + extraList.filter { !it.isHide }.map { it.ammoId }).distinct()
                            if (ammoList.size > 1) {
                                Minecraft.getInstance().setScreen(SelectAmmoScreen(ammoList))
                            }
                        }
                    }
                } else if (matches(SwitchUnderBarrelKey, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.SWITCH_UNDERBARREL))

                } else if (matches(Action1Key, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.ACTION_1))


                } else if (matches(Action2Key, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.ACTION_2))
                } else if (matches(Action3Key, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.ACTION_3))
                } else if (matches(Action4Key, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.ACTION_4))
                } else if (matches(ToggleLaserKey, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.LASER))
                } else if (matches(ToggleFlashlightKey, event)) {
                    NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.FLASHLIGHT))
                }
                return
            }
        }
        if (event.action == GLFW.GLFW_RELEASE) {
            if (matches(SwitchAmmoKey, event)) {
                val currentScreen = Minecraft.getInstance().screen
                if (currentScreen is SelectAmmoScreen) {
                    currentScreen.doClose()
                }
            }
        }
    }

    @SubscribeEvent
    fun onMouseScroll(event: InputEvent.MouseScrollingEvent) {
        val mc = Minecraft.getInstance() ?: return
        val player = mc.player ?: return
        if (!IClientPlayerGunOperator.fromLocalPlayer(player).isAim) return

        val mainHand = player.mainHandItem
        val iGun = IGun.getIGunOrNull(mainHand) ?: return

        val gunExtra = IAccessorGunData.getExtraHolder(mainHand) ?: return

        if (gunExtra.disableScrollOnAim) {
            NetworkManager.sendToServer(C2SMouseScroll(event.scrollDelta.toInt()).create())
            event.isCanceled = true
        }
    }

    fun isInGame(): Boolean {
        val mc = Minecraft.getInstance()

        if (mc.overlay != null) {
            return false
        }

        if (mc.screen != null) {
            return false
        }

        if (!mc.mouseHandler.isMouseGrabbed) {
            return false
        }

        return mc.isWindowActive
    }

    var walkCancelTicks = 0
        get() {
            if (field <= 0 && (hookStatus.moveLock || CameraManager.moveLock)) {
                return 1
            }
            return field
        }

    var jumpCancelTicks = 0
        get() {
            if (field <= 0 && (hookStatus.jumpLock || CameraManager.moveLock)) {
                return 1
            }
            return field
        }

    var aimCancelTicks = 0

    var fireCancelTicks = 0

    var missileFireCancelTicks = 0

    var reloadCancelTicks = 0
        get() {
            if (field <= 0 && hookStatus.reloadLock) {
                return 1
            }
            return field
        }

    var sprintCancelTicks = 0
        get() {
            if (field <= 0 && hookStatus.runLock) {
                return 1
            }
            return field
        }

    var rotateCancelTicks = 0
        get() {
            if (field <= 0 && CameraManager.rotateLock) {
                return 1
            }
            return field
        }


    var inventoryCancelTicks = 0
        get() {
            if (field <= 0 && hookStatus.inventoryLock) {
                return 1
            }
            return field
        }

    var inspectCancelTicks = 0
        get() {
            if (field <= 0 && hookStatus.inspectLock) {
                return 1
            }
            return field
        }


    fun cancelWalk(duration: Int) {
        walkCancelTicks = duration

    }

    fun cancelJump(duration: Int) {
        jumpCancelTicks = duration
    }

    fun cancelAim(duration: Int) {
        aimCancelTicks = duration
    }


    fun cancelFire(duration: Int) {
        fireCancelTicks = duration
    }

    fun cancelReload(duration: Int) {
        reloadCancelTicks = duration
    }

    fun cancelSprint(duration: Int) {
        sprintCancelTicks = duration
    }

    fun cancelMissileFire(duration: Int) {
        missileFireCancelTicks = duration
    }

    fun cancelRotate(duration: Int) {
        rotateCancelTicks = duration
    }

    fun cancelInventory(duration: Int) {
        inventoryCancelTicks = duration
    }

    fun cancelInspect(duration: Int) {
        inspectCancelTicks = duration
    }

    var hookStatus = HookData()

    fun updateHookStatus() {
        val finalData = HookData()
        HookManager.predicateHookData { list ->
            list.forEach { (source, data) ->
                if (data.runLock) finalData.runLock = true
                if (data.jumpLock) finalData.jumpLock = true
                if (data.reloadLock) finalData.reloadLock = true
                if (data.inspectLock) finalData.inspectLock = true
                if (data.moveLock) finalData.moveLock = true
                if (source == HookManager.Source.FROM && data.inventoryLock) finalData.inventoryLock = true
            }
        }
        hookStatus = finalData
    }

    fun onPostInputTick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (walkCancelTicks > 0) {
            player.input.forwardImpulse = 0f
            player.input.leftImpulse = 0f
            player.input.up = false
            player.input.down = false
            player.input.left = false
            player.input.right = false
        }

        if (jumpCancelTicks > 0) {
            player.input.jumping = false
        }

        if (sprintCancelTicks > 0) {
            mc.options.keySprint.isDown = false
            player.isSprinting = false
        }
    }

    @SubscribeEvent
    fun onPreClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        updateHookStatus()
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {

        if (event.phase != TickEvent.Phase.END) return

        if (walkCancelTicks > 0) {
            walkCancelTicks--
        }

        if (jumpCancelTicks > 0) {
            jumpCancelTicks--
        }

        if (aimCancelTicks > 0)
            aimCancelTicks--

        if (fireCancelTicks > 0)
            fireCancelTicks--

        if (missileFireCancelTicks > 0)
            missileFireCancelTicks--

        if (reloadCancelTicks > 0)
            reloadCancelTicks--

        if (sprintCancelTicks > 0) {
            sprintCancelTicks--
        }

        if (rotateCancelTicks > 0) {
            rotateCancelTicks--
        }

        if (inventoryCancelTicks > 0) {
            inventoryCancelTicks--
        }
        if (inspectCancelTicks > 0) {
            inspectCancelTicks--
        }
    }

    private var lastShootKeyDown = false

    private var lastJumpKeyDown = false

    @SubscribeEvent
    fun onClientPostTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (!isInGame()) return


        val isShootKeyDown = ShootKey.SHOOT_KEY.isDown
        if (isShootKeyDown != lastShootKeyDown) {
            if (isShootKeyDown) {
                NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.SHOOT_DOWN))
            } else {
                NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.SHOOT_RELEASE))
            }
            lastShootKeyDown = isShootKeyDown
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val isJumpKeyDown = player.input.jumping
        if (isJumpKeyDown != lastJumpKeyDown) {
            if (isJumpKeyDown) {
                HookManager.tryRelease()
            }
            lastJumpKeyDown = isJumpKeyDown
        }
    }


    @SubscribeEvent
    fun onClientLoggedOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        walkCancelTicks = 0
        jumpCancelTicks = 0
        aimCancelTicks = 0
        fireCancelTicks = 0
        missileFireCancelTicks = 0
        reloadCancelTicks = 0
        sprintCancelTicks = 0
        rotateCancelTicks = 0
        inventoryCancelTicks = 0
        inspectCancelTicks = 0
    }

    fun onHandleKeybinds() {
        val options = Minecraft.getInstance().options
        if (inventoryCancelTicks > 0) {
            (options.keyPickItem as IAccessorKeyMapping).release()
            (options.keySwapOffhand as IAccessorKeyMapping).release()
            options.keyHotbarSlots.forEach { (it as IAccessorKeyMapping).release() }
        }
    }
}