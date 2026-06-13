package group.taczexpands.common.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser

@Serializer(forClass = CompoundTag::class)
object CompoundTagSerializer : KSerializer<CompoundTag> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("net.minecraft.nbt.CompoundTag", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CompoundTag) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): CompoundTag {
        return TagParser.parseTag(decoder.decodeString())
    }
}