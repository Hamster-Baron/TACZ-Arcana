package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable

/** Per-recipient evaluated overrides on the S2CRaw channel. */
@Serializable
data class S2CSurfaceCamera(
    val identifier: String? = null,
    val space: String? = null,
    val anchor: Int? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val roll: Float? = null,
    val durationMillis: Long? = null,
    val fov: Float? = null,
    val distance: Float? = null,
    val vision: String? = null,
    val shaders: Boolean? = null,
    val remove: Boolean = false,
    val bone: String? = null
) {
    companion object {
        const val NETWORK_INDEX = 8
    }

    fun create(): S2CRaw = S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
}
