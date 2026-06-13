package group.taczexpands.common.util

import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.entity.EntityKineticBullet
import kotlinx.serialization.json.Json
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin

data class BlockResistanceData(
    val resistance: Int,
    val shouldDestroyBlock: Boolean,
    val particleOnPenetrate: Boolean,
    val accumulateBlockDamage: Boolean,
    val deflectable: Boolean,
    val bypassGlobalDestroyLimit: Boolean
)


fun Entity.getFixedLookAngle(): Vec3 {
    if (this is EntityKineticBullet) {
        val yRotRad = Math.toRadians(this.yRot.toDouble())
        val xRotRad = Math.toRadians(this.xRot.toDouble())

        val y = sin(xRotRad)
        val horizontalDistance = cos(xRotRad)

        val x = sin(yRotRad) * horizontalDistance
        val z = cos(yRotRad) * horizontalDistance

        return Vec3(x, y, z)
    } else {
        return this.lookAngle
    }
}

fun AttachmentType.isLaser(): Boolean {
    return this == AttachmentType.LASER || this.name == "LASER_2"
}

fun CommandSourceStack.sendModMessage(component: Component) {
    this.sendSystemMessage(Component.translatable("message.taczexpands.prefix").append(component))
}

fun Player.sendModMessage(component: Component) {
    this.sendSystemMessage(Component.translatable("message.taczexpands.prefix").append(component))
}

fun Vector4f.perspectiveDiv(): Vector4f {
    if (this.w != 0.0f)
        return this.div(this.w)
    return this
}

val JSON = Json

fun FriendlyByteBuf.writeOptionalVec3(value: Vec3?) {
    this.writeBoolean(value != null)
    if (value != null) {
        this.writeDouble(value.x)
        this.writeDouble(value.y)
        this.writeDouble(value.z)
    }
}

fun FriendlyByteBuf.readOptionalVec3(): Vec3? {
    return if (this.readBoolean()) {
        val x = this.readDouble()
        val y = this.readDouble()
        val z = this.readDouble()
        Vec3(x, y, z)
    } else null
}

fun FriendlyByteBuf.writeOptionalDouble(value: Double?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeDouble(value)
}

fun FriendlyByteBuf.readOptionalDouble(): Double? {
    return if (this.readBoolean()) this.readDouble() else null
}

fun FriendlyByteBuf.writeOptionalFloat(value: Float?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeFloat(value)
}

fun FriendlyByteBuf.readOptionalFloat(): Float? {
    return if (this.readBoolean()) this.readFloat() else null
}

fun FriendlyByteBuf.writeOptionalInt(value: Int?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeInt(value)
}

fun FriendlyByteBuf.readOptionalInt(): Int? {
    return if (this.readBoolean()) this.readInt() else null
}

fun FriendlyByteBuf.writeOptionalVarInt(value: Int?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeVarInt(value)
}

fun FriendlyByteBuf.readOptionalVarInt(): Int? {
    return if (this.readBoolean()) this.readVarInt() else null
}

fun FriendlyByteBuf.writeOptionalLong(value: Long?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeLong(value)
}

fun FriendlyByteBuf.readOptionalLong(): Long? {
    return if (this.readBoolean()) this.readLong() else null
}

fun FriendlyByteBuf.writeOptionalUtf(value: String?, maxLength: Int = 32767) {
    this.writeBoolean(value != null)
    if (value != null) this.writeUtf(value, maxLength)
}

fun FriendlyByteBuf.readOptionalUtf(maxLength: Int = 32767): String? {
    return if (this.readBoolean()) this.readUtf(maxLength) else null
}

fun FriendlyByteBuf.writeOptionalUUID(value: java.util.UUID?) {
    this.writeBoolean(value != null)
    if (value != null) this.writeUUID(value)
}

fun FriendlyByteBuf.readOptionalUUID(): java.util.UUID? {
    return if (this.readBoolean()) this.readUUID() else null
}