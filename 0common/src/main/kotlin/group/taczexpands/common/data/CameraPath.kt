package group.taczexpands.common.data

import group.taczexpands.common.util.readOptionalDouble
import group.taczexpands.common.util.readOptionalFloat
import group.taczexpands.common.util.writeOptionalDouble
import group.taczexpands.common.util.writeOptionalFloat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.FriendlyByteBuf

@Serializable
data class CameraPath(
    val x: Double?, val y: Double?, val z: Double?,
    val yaw: Float?, val pitch: Float?, val roll: Float?,
    val durationMillis: Long,
    val coordinateType: CoordinateType,
) {
    companion object {
        fun readFrom(buffer: FriendlyByteBuf): CameraPath {
            val x = buffer.readOptionalDouble()
            val y = buffer.readOptionalDouble()
            val z = buffer.readOptionalDouble()
            val yaw = buffer.readOptionalFloat()
            val pitch = buffer.readOptionalFloat()
            val roll = buffer.readOptionalFloat()
            val durationMillis = buffer.readLong()
            val coordinateType = buffer.readInt()
            return CameraPath(x, y, z, yaw, pitch, roll, durationMillis, CoordinateType.entries[coordinateType])

        }
    }

    fun writeTo(buffer: FriendlyByteBuf) {
        buffer.writeOptionalDouble(x)
        buffer.writeOptionalDouble(y)
        buffer.writeOptionalDouble(z)
        buffer.writeOptionalFloat(yaw)
        buffer.writeOptionalFloat(pitch)
        buffer.writeOptionalFloat(roll)
        buffer.writeLong(durationMillis)
        buffer.writeInt(coordinateType.ordinal)
    }
}

@Serializable
enum class CoordinateType {
    @SerialName("world_space")
    WORLD_SPACE,

    @SerialName("model_space_no_rotation")
    MODEL_SPACE_NO_ROTATION,

    @SerialName("model_space_y_rotation_only")
    MODEL_SPACE_Y_ROTATION_ONLY,

    @SerialName("model_space_with_rotation")
    MODEL_SPACE_WITH_ROTATION,
}