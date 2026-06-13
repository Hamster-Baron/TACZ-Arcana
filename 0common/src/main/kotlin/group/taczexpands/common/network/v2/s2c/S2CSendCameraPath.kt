package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.data.CameraPath
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable

@Serializable
data class S2CSendCameraPath(val allowMove: Boolean, val allowRotate: Boolean, val cameraPathList: List<CameraPath> = listOf()) {
    companion object {
        const val NETWORK_INDEX = 1
    }

    fun create(): S2CRaw {
        return S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }
}