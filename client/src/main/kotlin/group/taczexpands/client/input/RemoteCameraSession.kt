package group.taczexpands.client.input

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.network.c2s.C2SActionKey
import group.taczexpands.common.util.getFixedLookAngle
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.player.Input

object RemoteCameraSession {
    private var boundEntity: EntityKineticBullet? = null
    private var originalCameraType: CameraType? = null

    val isActive: Boolean
        get() = boundEntity != null

    fun bind(entity: EntityKineticBullet) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        if (!isActive) {
            originalCameraType = minecraft.options.cameraType
            InputManager.storeLocal()
            player.input = Input()
        }
        boundEntity = entity
        minecraft.cameraEntity = entity
        minecraft.options.cameraType = CameraType.FIRST_PERSON
        val direction = if (entity.deltaMovement.lengthSqr() < 0.001) entity.getFixedLookAngle() else entity.deltaMovement
        val (yaw, pitch) = TACZExpandsClient.INSTANCE.vecToYawPitch(direction)
        InputManager.yaw = yaw
        InputManager.pitch = pitch
        InputManager.sendCameraInput = true
    }

    fun tick() {
        val entity = boundEntity ?: return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player == null || entity.isRemoved || !entity.isAlive || entity.level() != player.level()) {
            close(player != null)
            return
        }
        if (minecraft.cameraEntity != entity) {
            close()
            return
        }
        minecraft.options.cameraType = CameraType.FIRST_PERSON
        InputManager.updateToServer()
        InputManager.sendCameraInput = true
    }

    fun close(notifyServer: Boolean = true) {
        if (!isActive && originalCameraType == null) return
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        boundEntity = null
        InputManager.sendCameraInput = false
        minecraft.cameraEntity = player
        InputManager.loadLocal()
        originalCameraType?.let {
            minecraft.options.cameraType = it
        }
        originalCameraType = null
        if (notifyServer && player != null) {
            NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.UNBIND_CAMERA))
        }
    }
}
