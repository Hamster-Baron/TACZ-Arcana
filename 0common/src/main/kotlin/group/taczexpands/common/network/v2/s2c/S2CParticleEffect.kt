package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable

@Serializable
data class S2CParticleEffect(
    val effect: String? = null,
    val identifier: String? = null,
    val owner: Int? = null,
    val x: Double? = null,
    val y: Double? = null,
    val z: Double? = null,
    val variables: Map<String, Float>? = null,
    val follow: Boolean? = null,
    val yaw: Float? = null,
    val pitch: Float? = null,
    val remove: Boolean = false
) {
    companion object {
        const val NETWORK_INDEX = 9
    }

    fun create(): S2CRaw = S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
}
