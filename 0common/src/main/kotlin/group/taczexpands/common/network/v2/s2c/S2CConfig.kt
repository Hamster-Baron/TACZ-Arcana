package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable

@Serializable
data class S2CConfig(val defaultBlockAimWhileReloading: Boolean) {
    companion object {
        const val NETWORK_INDEX = 6
    }

    fun create(): S2CRaw {
        return S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }

}
