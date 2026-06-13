package group.taczexpands.common.network.c2s

import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class C2SRaw(
    val type: Int,
    val data: ByteArray
) {
    companion object {
        @JvmStatic
        fun encode(packet: C2SRaw, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.type)
            buffer.writeByteArray(packet.data)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): C2SRaw {
            val type = buffer.readInt()
            val data = buffer.readByteArray()
            return C2SRaw(type, data)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<C2SRaw, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): java.util.function.Function<FriendlyByteBuf, C2SRaw> {
            return Function(::decode)
        }
    }
}