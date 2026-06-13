package group.taczexpands.common.network.s2c

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.function.BiConsumer
import java.util.function.Function

class S2CRaw(
    val type: Int,
    val data: ByteArray
) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CRaw, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.type)
            buffer.writeByteArray(packet.data)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CRaw {
            val type = buffer.readInt()
            val data = buffer.readByteArray()
            return S2CRaw(type, data)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CRaw, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CRaw> {
            return Function(::decode)
        }
    }
}