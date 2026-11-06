package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class S2CActionLockState(
    @SerialName("actions") val actions: Set<Action>,
    @SerialName("revision") val revision: Long
) {
    fun create(): S2CRaw = S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())

    companion object {
        const val NETWORK_INDEX = 7
    }

    @Serializable
    enum class Action {
        @SerialName("Sprint") Sprint,
        @SerialName("Aim") Aim,
        @SerialName("Walk") Walk,
        @SerialName("Jump") Jump,
        @SerialName("Fire") Fire,
        @SerialName("Reload") Reload,
        @SerialName("MissileFire") MissileFire,
        @SerialName("Zoom") Zoom,
        @SerialName("Rotate") Rotate,
        @SerialName("ShootKey") ShootKey,
        @SerialName("Inventory") Inventory,
        @SerialName("Inspect") Inspect,
        @SerialName("Drop") Drop
    }

}
