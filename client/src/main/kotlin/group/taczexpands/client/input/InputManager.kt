package group.taczexpands.client.input

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator
import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.mixin.accessor.IAccessorCamera
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.entity.EntityKineticBulletShared
import group.taczexpands.common.network.c2s.C2SActionKey
import net.minecraft.client.Minecraft
import net.minecraft.client.player.Input
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin

class InputData(val xRot: Float, val yRot: Float, val yBodyRot: Float, val yHeadRot: Float, val input: Input, val camYRot: Float, val camXRot: Float) {}

object InputManager {
    var sendCameraInput = false
    private var inputData: InputData? = null

    var yaw: Float = 0f
    var pitch: Float = 0f

    var lastYaw: Float = 0f
    var lastPitch: Float = 0f

    var lastInput: Vec3 = Vec3.ZERO

    var lastYawCopy: Float = 0f
    var lastPitchCopy: Float = 0f
    var lastChargeProgress: Float = 0f

    fun storeLocal() {
        val player = Minecraft.getInstance().player ?: return


        val camera = Minecraft.getInstance().gameRenderer.mainCamera

        inputData = InputData(
            player.xRot,
            player.yRot,
            player.yBodyRot,
            player.yHeadRot,
            player.input,
            camera.yRot,
            camera.xRot
        )
    }

    fun loadLocal() {
        if (inputData != null) {
            val data = inputData!!
            inputData = null
            val player = Minecraft.getInstance().player ?: return
            player.xRot = data.xRot
            player.yRot = data.yRot
            player.yHeadRot = data.yHeadRot
            player.yBodyRot = data.yBodyRot
            player.input = data.input

            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            (camera as IAccessorCamera).setRotation(data.camYRot, data.camXRot)
        }
    }


    fun turn(yawRaw: Double, pitchRaw: Double) {
        val yaw = yawRaw.toFloat() * 0.15f
        val pitch = pitchRaw.toFloat() * 0.15f

        this.yaw += yaw
        this.pitch += pitch

        while (this.yaw > 180.0f) {
            this.yaw -= 360.0f
        }

        while (this.yaw < -180.0f) {
            this.yaw += 360.0f
        }

        this.pitch = Mth.clamp(this.pitch, -90.0f, 90.0f)
    }

    fun updateClamp() {
        val missile = Minecraft.getInstance().cameraEntity as? EntityKineticBullet
        if (missile != null) {
            val clamp = missile.entityData.get(EntityKineticBulletShared.TV_ROTATION_CLAMP_DATA_ACCESSOR)
            if (!clamp) return
            val clampModifier = missile.entityData.get(EntityKineticBulletShared.TV_ROTATION_CLAMP_MODIFIER_DATA_ACCESSOR)
            val accelerationLimit = missile.entityData.get(EntityKineticBulletShared.ACCELERATION_LIMIT_DATA_ACCESSOR)
            if (accelerationLimit > 0.0f) {
                val partialTicks = Minecraft.getInstance().partialTick
                val currentSpeed = missile.entityData.get(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR)

                val missileYaw = -(Mth.lerp(partialTicks, missile.yRotO, missile.yRot))
                val missilePitch = -(Mth.lerp(partialTicks, missile.xRotO, missile.xRot))

                val maxDeviationRad = (accelerationLimit / currentSpeed) * clampModifier

                val currentCameraVec = Vec3.directionFromRotation(this.pitch, this.yaw)
                val missileForwardVec = Vec3.directionFromRotation(missilePitch, missileYaw)

                val dot = missileForwardVec.dot(currentCameraVec).coerceIn(-1.0, 1.0)
                val theta = acos(dot)

                if (theta > maxDeviationRad && theta > 1e-7) {
                    val t = (maxDeviationRad / theta).toDouble()
                    val finalVec = slerp(missileForwardVec, currentCameraVec, t)

                    val (yaw, pitch) = TACZExpandsClient.INSTANCE.vecToYawPitch(finalVec)
                    this.yaw = yaw
                    this.pitch = pitch
                }
            }
        }
    }

    private fun slerp(start: Vec3, end: Vec3, t: Double): Vec3 {
        val dot = start.dot(end).coerceIn(-1.0, 1.0)
        val theta = acos(dot)
        val sinTheta = sin(theta)

        if (sinTheta < 1e-6) {
            return start.lerp(end, t).normalize()
        }

        val weightStart = sin((1.0 - t) * theta) / sinTheta
        val weightEnd = sin(t * theta) / sinTheta

        return start.scale(weightStart).add(end.scale(weightEnd)).normalize()
    }

    fun getYawPitch(): Pair<Float, Float> {
        return yaw to pitch
    }

    fun updateToServer() {
        val mc = Minecraft.getInstance()
        val keyLeft = mc.options.keyLeft
        val keyRight = mc.options.keyRight
        val keyUp = mc.options.keyUp
        val keyDown = mc.options.keyDown
        val keyJump = mc.options.keyJump
        val keyShift = mc.options.keyShift

        val x = (if (keyLeft.isDown) 1.0 else 0.0) - (if (keyRight.isDown) 1.0 else 0.0)
        val y = (if (keyJump.isDown) 1.0 else 0.0) - (if (keyShift.isDown) 1.0 else 0.0)
        val z = (if (keyUp.isDown) 1.0 else 0.0) - (if (keyDown.isDown) 1.0 else 0.0)

        val input = Vec3(x, y, z)

        if (yaw != lastYaw || pitch != lastPitch || input != lastInput) {
            NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.CAMERA, yaw, pitch, input))
            lastYaw = yaw
            lastPitch = pitch
            lastInput = input
        }
    }

    fun onClientTick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val gunOperator = IClientPlayerGunOperator.fromLocalPlayer(player)
        if (abs(gunOperator.chargeProgress - lastChargeProgress) > 0.05f
            || (lastChargeProgress <= 0.0f && gunOperator.chargeProgress > 0.0f)
            || (lastChargeProgress > 0.0f && gunOperator.chargeProgress <= 0.0f)
        ) {
            NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.UPDATE_CHARGE, gunOperator.chargeProgress))
            lastChargeProgress = gunOperator.chargeProgress
        }
    }

    fun clear() {
        inputData = null
        lastChargeProgress = 0.0f
    }
}

