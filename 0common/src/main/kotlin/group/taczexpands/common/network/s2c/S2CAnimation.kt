package group.taczexpands.common.network.s2c

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class S2CAnimation(var action: Action, var track: Int, var name: String, var to: String, var blending: Boolean, var playMode: Int, var transitionTime: Float) {
    @Serializable
    enum class Action {
        @SerialName("play")
        PLAY,

        @SerialName("stop")
        STOP,

        @SerialName("add_redirect")
        ADD_REDIRECT,

        @SerialName("remove_redirect")
        REMOVE_REDIRECT,
    }

    companion object {
        @JvmStatic
        fun encode(packet: S2CAnimation, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.action.ordinal)
            buffer.writeInt(packet.track)
            buffer.writeUtf(packet.name)
            buffer.writeUtf(packet.to)
            buffer.writeBoolean(packet.blending)
            buffer.writeInt(packet.playMode)
            buffer.writeFloat(packet.transitionTime)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CAnimation {
            val action = buffer.readInt()
            val track = buffer.readInt()
            val name = buffer.readUtf()
            val to = buffer.readUtf()
            val blending = buffer.readBoolean()
            val playMode = buffer.readInt()
            val transitionTime = buffer.readFloat()
            return S2CAnimation(Action.entries.getOrElse(action, { Action.PLAY }), track, name, to, blending, playMode, transitionTime)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CAnimation, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CAnimation> {
            return Function(::decode)
        }

    }
}