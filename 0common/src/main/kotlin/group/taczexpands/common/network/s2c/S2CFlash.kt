package group.taczexpands.common.network.s2c

import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class S2CFlash(
    val durationMillis: Long,
    val fadeInMillis: Long,
    val fadeOutMillis: Long,
    val strength: Int
) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CFlash, buffer: FriendlyByteBuf) {
            buffer.writeLong(packet.durationMillis)
            buffer.writeLong(packet.fadeInMillis)
            buffer.writeLong(packet.fadeOutMillis)
            buffer.writeInt(packet.strength)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CFlash {
            val duration = buffer.readLong()
            val fadeIn = buffer.readLong()
            val fadeOut = buffer.readLong()
            val strength = buffer.readInt()
            return S2CFlash(duration, fadeIn, fadeOut, strength)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CFlash, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CFlash> {
            return Function(::decode)
        }
    }
}