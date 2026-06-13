package group.taczexpands.common.network.s2c

import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class S2CSendVariable(val type: Type, val variable: String, val value: String) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CSendVariable, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.type.ordinal)
            buffer.writeUtf(packet.variable)
            buffer.writeUtf(packet.value)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CSendVariable {
            val index = buffer.readInt()
            val variable = buffer.readUtf()
            val value = buffer.readUtf()
            return S2CSendVariable(Type.entries.getOrElse(index, { Type.VOLATILE }), variable, value)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CSendVariable, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CSendVariable> {
            return Function(::decode)
        }
    }

    enum class Type {
        VOLATILE,
        UTIL_API,
        VARIABLE,
        INVALIDATE_VARIABLE,
        INVALIDATE_ALL_VARIABLE
    }

}