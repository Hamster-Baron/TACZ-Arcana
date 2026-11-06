package group.taczexpands.common.network.v2.c2s

import group.taczexpands.common.network.c2s.C2SRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class C2SViewState(
    @SerialName("source") val source: Source,
    @SerialName("origin_x") val originX: Double,
    @SerialName("origin_y") val originY: Double,
    @SerialName("origin_z") val originZ: Double,
    @SerialName("yaw") val yaw: Float,
    @SerialName("pitch") val pitch: Float,
    @SerialName("camera_entity_id") val cameraEntityId: Int? = null,
    @SerialName("camera_path_session_id") val cameraPathSessionId: Long? = null,
    @SerialName("sequence") val sequence: Long
) {
    companion object {
        const val NETWORK_INDEX = 2
    }

    @Serializable
    enum class Source {
        @SerialName("player") PLAYER,
        @SerialName("remote_projectile") REMOTE_PROJECTILE,
        @SerialName("camera_path") CAMERA_PATH
    }

    fun create(): C2SRaw {
        return C2SRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }
}
