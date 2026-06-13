package group.taczexpands.common.network.s2c

import group.taczexpands.common.data.HookInstance
import kotlinx.serialization.Serializable
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.Vec3
import java.util.function.BiConsumer
import java.util.function.Function

class S2CAction @JvmOverloads constructor(val action: Action, val signal: Int = 0, val yaw: Float = 0f, val pitch: Float = 0f, val hook: HookInstance? = null, val velocity: Vec3? = null) {
    companion object {
        @JvmStatic
        fun encode(packet: S2CAction, buffer: FriendlyByteBuf) {
            buffer.writeInt(packet.action.ordinal)
            buffer.writeInt(packet.signal)
            buffer.writeFloat(packet.yaw)
            buffer.writeFloat(packet.pitch)
            if (packet.action == Action.Hook) {
                packet.hook!!.serialize(buffer)
            }
            if (packet.action == Action.SetVelocity) {
                buffer.writeDouble(packet.velocity!!.x)
                buffer.writeDouble(packet.velocity!!.y)
                buffer.writeDouble(packet.velocity!!.z)
            }
        }

        @JvmStatic
        fun decode(buffer: FriendlyByteBuf): S2CAction {
            val index = buffer.readInt()
            val signal = buffer.readInt()
            val yaw = buffer.readFloat()
            val pitch = buffer.readFloat()

            val action = Action.entries.getOrElse(index, { Action.RefreshCache })
            val hook = if (action == Action.Hook) {
                HookInstance.deserialize(buffer)
            } else null
            val velocity = if (action == Action.SetVelocity) {
                Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
            } else null
            return S2CAction(action, signal, yaw, pitch, hook, velocity)
        }

        @JvmStatic
        fun getEncoder(): BiConsumer<S2CAction, FriendlyByteBuf> {
            return BiConsumer(::encode)
        }

        @JvmStatic
        fun getDecoder(): Function<FriendlyByteBuf, S2CAction> {
            return Function(::decode)
        }
    }

    @Serializable
    enum class Action {
        RefreshCache,
        Reload,
        Draw,
        StopShake,
        SaveDrawStack,
        Shoot,
        Inspect,
        BindCamera,
        BindTarget,
        BindLockingTime,
        BindCurrentLockingTime,
        Frostbite,
        Rotate,
        Hook,
        SetVelocity,
        ForceShoot,
        MeleeAttack
    }
}