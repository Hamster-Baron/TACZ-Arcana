package group.taczexpands.common.data

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3


data class HookInstance(val type: Type, val fromEntityId: Int, val toEntityId: Int, val toPos: Vec3, val data: HookData) {
    companion object {
        fun deserialize(buffer: FriendlyByteBuf): HookInstance {
            val type = Type.entries.getOrElse(buffer.readInt(), { Type.ATTACH_ENTITY })
            val fromEntityId = buffer.readInt()
            val toEntityId = buffer.readInt()
            val toX = buffer.readDouble()
            val toY = buffer.readDouble()
            val toZ = buffer.readDouble()
            val hookData = HookData.deserialize(buffer)
            return HookInstance(type, fromEntityId, toEntityId, Vec3(toX, toY, toZ), hookData)
        }
    }

    enum class Type {
        ATTACH_ENTITY,
        DETACH_ENTITY,
        ATTACH_BLOCK,
        DETACH_BLOCK,
    }

    fun serialize(buffer: FriendlyByteBuf) {
        buffer.writeInt(type.ordinal)
        buffer.writeInt(fromEntityId)
        buffer.writeInt(toEntityId)
        buffer.writeDouble(toPos.x)
        buffer.writeDouble(toPos.y)
        buffer.writeDouble(toPos.z)
        data.serialize(buffer)
    }
}
