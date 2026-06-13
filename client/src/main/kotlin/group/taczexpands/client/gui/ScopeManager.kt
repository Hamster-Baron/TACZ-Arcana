package group.taczexpands.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.tacz.guns.api.item.IGun
import com.tacz.guns.client.model.BedrockAttachmentModel
import com.tacz.guns.client.model.bedrock.BedrockPart
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.compat.CompatHelper
import group.taczexpands.client.compat.iris.IrisCompat
import group.taczexpands.client.util.predictBullet
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

object ScopeManager {
    var lastHookPosNDC: Vector4f = Vector4f()

    var lastProjectionMatrix: Matrix4f = Matrix4f()
    var lastViewMatrix: Matrix4f = Matrix4f()
        get() = field
        set(value) {
            field = value
            lastCameraPos = Minecraft.getInstance().gameRenderer.mainCamera.position
        }
    var lastCameraPos = Vec3.ZERO
    var lastScopeModel: BedrockAttachmentModel? = null
    var lastScopeViewPath: List<BedrockPart>? = null
    var target: Int = 0
    var lockingTime: Int = 1

    var time = 0

    var lastOffset: Vector4f? = null

    fun reset() {
        lastHookPosNDC = Vector4f()
        lastProjectionMatrix = Matrix4f()
        lastViewMatrix = Matrix4f()
        lastCameraPos = Vec3.ZERO
        lastScopeModel = null
        lastScopeViewPath = null
        target = 0
        lockingTime = 1
        time = 0

        lastOffset = null
    }

    fun onClientTick() {
        time++
    }

    fun getCurrentTime(): Float {
        return max(0.0f, time.toFloat() - (1.0f - Minecraft.getInstance().partialTick))
    }

    fun worldToNdc(pos: Vec3): Vector4f {
        val worldPos = Vector4f(pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(), 1.0f)
        worldPos.mul(Matrix4f().translate(Vector3f(-lastCameraPos.x.toFloat(), -lastCameraPos.y.toFloat(), -lastCameraPos.z.toFloat())))
        worldPos.mul(lastViewMatrix)
        worldPos.mulProject(lastProjectionMatrix)
        return worldPos
    }

    fun isNdcPosVisible(ndcPos: Vector4f): Boolean {
        return ndcPos.x >= -1.0 && ndcPos.x <= 1.0 &&
                ndcPos.y >= -1.0 && ndcPos.y <= 1.0 &&
                ndcPos.z >= -1.0 && ndcPos.z <= 1.0
    }

    fun log(msg: String) {
        if ((Minecraft.getInstance().player?.tickCount ?: 1) % 20 == 0)
            TACZExpandsClient.LOGGER.info(msg)
    }

    fun logVector4f(name: String, vec: Vector4f) {
        if ((Minecraft.getInstance().player?.tickCount ?: 1) % 20 == 0)
            TACZExpandsClient.LOGGER.info("$name: ${vec.x}, ${vec.y}, ${vec.z}, ${vec.w}")
    }

    fun onRenderDivision(part: BedrockPart, poseStack: PoseStack) {

    }

    fun findIntersectionOnZPlaneVector4f(rootModelPos: Vector4f, targetModelPos: Vector4f, pivotModelPos: Vector4f): Vector4f? {
        val rootX = rootModelPos.x() / rootModelPos.w()
        val rootY = rootModelPos.y() / rootModelPos.w()
        val rootZ = rootModelPos.z() / rootModelPos.w()

        val targetX = targetModelPos.x() / targetModelPos.w()
        val targetY = targetModelPos.y() / targetModelPos.w()
        val targetZ = targetModelPos.z() / targetModelPos.w()

        val pivotZ = pivotModelPos.z() / pivotModelPos.w()

        val dx = targetX - rootX
        val dy = targetY - rootY
        val dz = targetZ - rootZ

        if (dz == 0.0f) {
            if (rootZ == pivotZ) {
                return null
            }
            return null
        }

        val t = (pivotZ - rootZ) / dz

        val intersectionX = rootX + t * dx
        val intersectionY = rootY + t * dy
        val intersectionZ = pivotZ

        return Vector4f(intersectionX, intersectionY, intersectionZ, 1.0f)
    }

    fun onRenderDropPoint(part: BedrockPart, poseStack: PoseStack): Boolean {
        if (CompatHelper.hasIris() && IrisCompat.isHandRendererActive()) return false
        lastScopeModel ?: return false
        lastScopeViewPath ?: return false

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        val level = player.level() ?: return false
        val mainHand = player.mainHandItem
        val iGun = IGun.getIGunOrNull(mainHand) ?: return false
        val gunData = IAccessorGunData.getGunData(mainHand) ?: return false
        val bulletData = IAccessorGunData.getCurrentBulletData(gunData, mainHand) ?: return false
        if (IAccessorBulletData.getBulletExtraHolder(bulletData).missileData.isMissile) return false

        val result = predictBullet(level, player, bulletData) ?: return false

        if (result.type == HitResult.Type.MISS) return false

        val x = result.location.x
        val y = result.location.y
        val z = result.location.z


        val targetNdcPos = worldToNdc(Vec3(x, y, z))
        if (!isNdcPosVisible(targetNdcPos)) return false

        val invertedProjectionMatrix = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
        val invertedViewMatrix = Matrix4f(poseStack.last().pose()).invert()

        targetNdcPos.mulProject(invertedProjectionMatrix)
        val targetPartPos = targetNdcPos.mul(invertedViewMatrix)

        val paths = generateSequence(part.parent) { it.parent }.toList().reversed()

        val tempPoseStack = PoseStack()
        paths.forEach { it.translateAndRotateAndScale(tempPoseStack) }
        val partViewToModelView = tempPoseStack.last().pose()

        val targetModelPos = targetPartPos.mul(partViewToModelView)
        val pivotModelPos = Vector4f(getCenter(PoseStack(), paths.toMutableList().apply { add(part) }), 1.0f)
        val rootModelPos = Vector4f(getCenter(PoseStack(), lastScopeViewPath!!), 1.0f)

        val intersectionPos = findIntersectionOnZPlaneVector4f(rootModelPos, targetModelPos, pivotModelPos) ?: return onProgressRollback(part, poseStack)

        val offsetPos = intersectionPos.sub(pivotModelPos, Vector4f())

        offsetPos.mul(partViewToModelView.invert())
        part.offsetX += offsetPos.x
        part.offsetY += offsetPos.y
        part.offsetZ += offsetPos.z

        return true
    }

    fun onRenderTrack(part: BedrockPart, poseStack: PoseStack): Boolean {
        if (CompatHelper.hasIris() && IrisCompat.isHandRendererActive()) return false
        lastScopeModel ?: return onProgressRollback(part, poseStack)
        lastScopeViewPath ?: return onProgressRollback(part, poseStack)

        if (target == 0) return onProgressRollback(part, poseStack)
        val player = Minecraft.getInstance().player ?: return onProgressRollback(part, poseStack)
        val targetEntity = player.level().getEntity(target) ?: return onProgressRollback(part, poseStack)

        val lerpPercent = 1.0f - Minecraft.getInstance().partialTick.toDouble()
        val deltaX = Mth.lerp(lerpPercent, targetEntity.x, targetEntity.xo) - targetEntity.x
        val deltaY = Mth.lerp(lerpPercent, targetEntity.y, targetEntity.yo) - targetEntity.y
        val deltaZ = Mth.lerp(lerpPercent, targetEntity.z, targetEntity.zo) - targetEntity.z

        val targetNdcPos = worldToNdc(targetEntity.boundingBox.center.add(deltaX, deltaY, deltaZ))

        if (!isNdcPosVisible(targetNdcPos)) return onProgressRollback(part, poseStack)

        val invertedProjectionMatrix = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
        val invertedViewMatrix = Matrix4f(poseStack.last().pose()).invert()

        targetNdcPos.mulProject(invertedProjectionMatrix)
        val targetPartPos = targetNdcPos.mul(invertedViewMatrix)

        val paths = generateSequence(part.parent) { it.parent }.toList().reversed()

        val tempPoseStack = PoseStack()
        paths.forEach { it.translateAndRotateAndScale(tempPoseStack) }
        val partViewToModelView = tempPoseStack.last().pose()

        val targetModelPos = targetPartPos.mul(partViewToModelView)
        val pivotModelPos = Vector4f(getCenter(PoseStack(), paths.toMutableList().apply { add(part) }), 1.0f)
        val rootModelPos = Vector4f(getCenter(PoseStack(), lastScopeViewPath!!), 1.0f)

        val intersectionPos = findIntersectionOnZPlaneVector4f(rootModelPos, targetModelPos, pivotModelPos) ?: return onProgressRollback(part, poseStack)

        val offsetPos = intersectionPos.sub(pivotModelPos, Vector4f())
        val progress = if (lockingTime != 0) (getCurrentTime() / lockingTime.toFloat()).coerceIn(0.0f, 1.0f) else 1.0f
        offsetPos.mul(partViewToModelView.invert())
        lastOffset = Vector4f(offsetPos)
        offsetPos.mul(progress)
        part.offsetX += offsetPos.x
        part.offsetY += offsetPos.y
        part.offsetZ += offsetPos.z

        return progress >= 1.0f
    }

    fun onProgressRollback(part: BedrockPart, poseStack: PoseStack): Boolean {
        if (lastOffset != null) {
            val progress = getCurrentTime() / 20.toFloat()
            val rollbackProgress = 1.0f - progress
            part.offsetX += lastOffset!!.x * rollbackProgress
            part.offsetY += lastOffset!!.y * rollbackProgress
            part.offsetZ += lastOffset!!.z * rollbackProgress
            if (progress >= 1.0f) {
                lastOffset = null
            }
        }
        return false
    }

    private fun getCenter(poseStack: PoseStack, path: List<BedrockPart>): Vector3f {
        poseStack.pushPose()
        for (index in path.indices) {
            path[index].translateAndRotateAndScale(poseStack)
        }
        val result = Vector3f(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32())
        poseStack.popPose()
        return result
    }
}