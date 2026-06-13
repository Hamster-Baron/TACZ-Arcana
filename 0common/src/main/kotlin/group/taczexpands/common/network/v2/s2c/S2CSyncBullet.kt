package group.taczexpands.common.network.v2.s2c

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.common.network.NetworkCommon
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


@Serializable
data class S2CSyncBullet(val entityId: Int, val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float, val velocityX: Double, val velocityY: Double, val velocityZ: Double) {
    companion object {
        const val NETWORK_INDEX = 2
    }

    fun create(): S2CRaw {
        return S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }

    fun apply(level: Level) {
        val entity = level.getEntity(entityId) ?:return
        entity.setPos(x, y, z)
        entity.xOld = entity.x
        entity.yOld = entity.y
        entity.zOld = entity.z
        entity.xo = entity.x
        entity.yo = entity.y
        entity.zo = entity.z

        entity.yRot = yaw
        entity.xRot = pitch
        entity.yRotO = entity.yRot
        entity.xRotO = entity.xRot
        entity.setDeltaMovement(velocityX, velocityY, velocityZ)
    }
}