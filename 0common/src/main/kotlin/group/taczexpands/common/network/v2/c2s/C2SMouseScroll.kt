package group.taczexpands.common.network.v2.c2s

import group.taczexpands.common.network.c2s.C2SRaw
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.Serializable

@Serializable
data class C2SMouseScroll(val delta: Int) {
    companion object {
        const val NETWORK_INDEX = 1
    }

    fun create(): C2SRaw {
        return C2SRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }

}
