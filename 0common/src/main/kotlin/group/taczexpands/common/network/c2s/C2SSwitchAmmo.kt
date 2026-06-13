package group.taczexpands.common.network.c2s

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import java.util.function.BiConsumer
import java.util.function.Function

class C2SSwitchAmmo(val ammoType: ResourceLocation) {
    companion object {
        @JvmStatic
        fun encode(packet: C2SSwitchAmmo, buffer: FriendlyByteBuf) {
            buffer.writeResourceLocation(packet.ammoType)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): C2SSwitchAmmo {
            val ammoType = buffer.readResourceLocation()
            return C2SSwitchAmmo(ammoType)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<C2SSwitchAmmo, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, C2SSwitchAmmo> {
            return Function(::decode)
        }
    }
}