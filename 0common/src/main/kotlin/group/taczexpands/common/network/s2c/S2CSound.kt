package group.taczexpands.common.network.s2c

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.function.BiConsumer
import java.util.function.Function

class S2CSound(
    val action: String,
    val group: String,
    val soundName: String,
    val sourceType: Int,
    val distance: Int,
    val volume: Float,
    val pitch: Float,
    val mono: Boolean,
    val sourcePos: Vec3,
    val sourceEntityID: Int
) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CSound, buffer: FriendlyByteBuf) {
            buffer.writeUtf(packet.action)
            buffer.writeUtf(packet.group)
            buffer.writeUtf(packet.soundName)
            buffer.writeInt(packet.sourceType)
            buffer.writeInt(packet.distance)
            buffer.writeFloat(packet.volume)
            buffer.writeFloat(packet.pitch)
            buffer.writeBoolean(packet.mono)
            buffer.writeDouble(packet.sourcePos.x)
            buffer.writeDouble(packet.sourcePos.y)
            buffer.writeDouble(packet.sourcePos.z)
            buffer.writeInt(packet.sourceEntityID)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CSound {
            val action = buffer.readUtf()
            val group = buffer.readUtf()
            val soundName = buffer.readUtf()
            val sourceType = buffer.readInt()
            val distance = buffer.readInt()
            val volume = buffer.readFloat()
            val pitch = buffer.readFloat()
            val mono = buffer.readBoolean()
            val sourcePosX = buffer.readDouble()
            val sourcePosY = buffer.readDouble()
            val sourcePosZ = buffer.readDouble()
            val sourceEntityID = buffer.readInt()
            return S2CSound(action, group, soundName, sourceType, distance, volume, pitch, mono, Vec3(sourcePosX, sourcePosY, sourcePosZ), sourceEntityID)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CSound, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CSound> {
            return Function(::decode)
        }
    }

}