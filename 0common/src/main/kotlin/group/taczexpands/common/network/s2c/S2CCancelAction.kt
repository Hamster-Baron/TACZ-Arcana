package group.taczexpands.common.network.s2c

import kotlinx.serialization.Serializable
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class S2CCancelAction(val action: Action, val duration: Int = 0, val isReset: Boolean = false) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CCancelAction, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.action.ordinal)
            buffer.writeInt(packet.duration)
            buffer.writeBoolean(packet.isReset)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CCancelAction {
            val index = buffer.readInt()
            val duration = buffer.readInt()
            val isReset = buffer.readBoolean()
            return S2CCancelAction(Action.entries.getOrElse(index, { Action.Sprint }), duration, isReset)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CCancelAction, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CCancelAction> {
            return Function(::decode)
        }
    }

    @Serializable
    enum class Action {
        Sprint,
        Aim,
        Walk,
        Jump,
        Fire,
        Reload,
        MissileFire,
        Zoom,
        Rotate,
        ShootKey,
        Inventory,
        Inspect
    }
}