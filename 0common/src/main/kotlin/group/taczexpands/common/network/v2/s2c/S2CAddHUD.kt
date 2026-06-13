package group.taczexpands.common.network.v2.s2c

import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.util.JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class S2CAddHUD(val identifier: String, val renderSpace: RenderSpace, val alignment: Alignment, val x: Double, val y: Double, val z: Double, val scale: Float, val text: String?, val imagePath: String?, val imageWidth: Int = 256, val imageHeight: Int = 256) {
    companion object {
        const val NETWORK_INDEX = 4
    }

    fun create(): S2CRaw {
        return S2CRaw(NETWORK_INDEX, JSON.encodeToString(this).encodeToByteArray())
    }

    @Serializable
    enum class RenderSpace {
        @SerialName("world")
        WORLD,
        @SerialName("screen")
        SCREEN
    }

    @Serializable
    enum class Alignment {
        @SerialName("left")
        LEFT,
        @SerialName("center")
        CENTER,
        @SerialName("right")
        RIGHT,
    }

}