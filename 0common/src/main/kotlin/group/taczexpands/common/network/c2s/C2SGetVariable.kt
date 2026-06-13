package group.taczexpands.common.network.c2s

import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class C2SGetVariable(val variable: String) {
    companion object {
        @JvmStatic
        fun encode(packet: C2SGetVariable, buffer: FriendlyByteBuf) {
            buffer.writeUtf(packet.variable)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): C2SGetVariable {
            val variable = buffer.readUtf()
            return C2SGetVariable(variable)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<C2SGetVariable, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, C2SGetVariable> {
            return Function(::decode)
        }
    }
}