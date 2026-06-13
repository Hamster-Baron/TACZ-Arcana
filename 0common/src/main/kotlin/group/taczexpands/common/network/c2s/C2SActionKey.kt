package group.taczexpands.common.network.c2s

import group.taczexpands.common.util.readOptionalFloat
import group.taczexpands.common.util.readOptionalVec3
import group.taczexpands.common.util.writeOptionalDouble
import group.taczexpands.common.util.writeOptionalFloat
import group.taczexpands.common.util.writeOptionalVec3
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.function.BiConsumer
import java.util.function.Function

class C2SActionKey(val action: Action, val yaw: Float? = null, val pitch: Float? = null, val input: Vec3? = null) {
    companion object {
        @JvmStatic
        fun encode(packet: C2SActionKey, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.action.ordinal)
            buffer.writeOptionalFloat(packet.yaw)
            buffer.writeOptionalFloat(packet.pitch)
            buffer.writeOptionalVec3(packet.input)
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): C2SActionKey {
            val index = buffer.readInt()
            val yaw = buffer.readOptionalFloat()
            val pitch = buffer.readOptionalFloat()
            val input = buffer.readOptionalVec3()
            return C2SActionKey(Action.entries.getOrElse(index, { Action.ACTION_1 }), yaw, pitch, input)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<C2SActionKey, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, C2SActionKey> {
            return Function(::decode)
        }
    }

    enum class Action {
        SWITCH_UNDERBARREL,
        ACTION_1,
        ACTION_2,
        ACTION_3,
        ACTION_4,
        SHOOT_DOWN,
        SHOOT_RELEASE,
        CAMERA,
        UNBIND_CAMERA,
        INSPECT,
        LASER,
        FLASHLIGHT,
        RELEASE_HOOK,

        UPDATE_CLIENT_CAMERA,
        UNBIND_CLIENT_CAMERA,

        UPDATE_CHARGE
    }

}