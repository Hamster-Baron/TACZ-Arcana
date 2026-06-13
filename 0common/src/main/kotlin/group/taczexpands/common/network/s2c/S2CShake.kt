package group.taczexpands.common.network.s2c

import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

class S2CShake(
    val amplitudeYRot: Float,
    val amplitudeXRot: Float,
    val durationMillis: Long,
    val frequencyHz: Float = 0.5f,
    val randomness: Float = 0.1f,
    val dynamicPhaseOffset: Boolean = true,
    val phaseOffsetYRot: Float? = null,
    val phaseOffsetXRot: Float? = null,
) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CShake, buffer: FriendlyByteBuf) {
            buffer.writeFloat(packet.amplitudeYRot)
            buffer.writeFloat(packet.amplitudeXRot)
            buffer.writeLong(packet.durationMillis)
            buffer.writeFloat(packet.frequencyHz)
            buffer.writeFloat(packet.randomness)
            buffer.writeBoolean(packet.dynamicPhaseOffset)
            buffer.writeBoolean(packet.phaseOffsetYRot != null)
            if (packet.phaseOffsetYRot != null) {
                buffer.writeFloat(packet.phaseOffsetYRot)
            }
            buffer.writeBoolean(packet.phaseOffsetXRot != null)
            if (packet.phaseOffsetXRot != null) {
                buffer.writeFloat(packet.phaseOffsetXRot)
            }
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CShake {
            val amplitudeYRot = buffer.readFloat()
            val amplitudeXRot = buffer.readFloat()
            val durationMillis = buffer.readLong()
            val frequencyHz = buffer.readFloat()
            val randomness = buffer.readFloat()
            val dynamicPhaseOffset = buffer.readBoolean()
            val phaseOffsetYRot = if (buffer.readBoolean()) {
                buffer.readFloat()
            } else null

            val phaseOffsetXRot = if (buffer.readBoolean()) {
                buffer.readFloat()
            } else null

            return S2CShake(amplitudeYRot, amplitudeXRot, durationMillis, frequencyHz, randomness, dynamicPhaseOffset, phaseOffsetYRot, phaseOffsetXRot)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CShake, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CShake> {
            return Function(::decode)
        }
    }
}