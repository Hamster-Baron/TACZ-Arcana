package group.taczexpands.server.config.action

import group.taczexpands.common.network.v2.s2c.S2CSurfaceCamera
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.create
import group.taczexpands.server.config.createEmptyAsNull
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** A scalar literal or expression in JSON/YAML, evaluated for each recipient at execution. */
@Serializable(with = SurfaceCameraValueSerializer::class)
data class SurfaceCameraValue(val expression: String)

object SurfaceCameraValueSerializer : KSerializer<SurfaceCameraValue> {
    override val descriptor = PrimitiveSerialDescriptor("SurfaceCameraValue", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): SurfaceCameraValue {
        val value = if (decoder is JsonDecoder) {
            val scalar = decoder.decodeJsonElement()
            require(scalar is JsonPrimitive && scalar !is JsonNull) { "Surface camera expressions must be scalar values" }
            scalar.content
        } else decoder.decodeString()
        return SurfaceCameraValue(value)
    }
    override fun serialize(encoder: Encoder, value: SurfaceCameraValue) = encoder.encodeString(value.expression)
}

@Serializable
@SerialName("SetSurfaceCamera")
data class SetSurfaceCamera(
    val identifier: String? = null,
    val space: String? = null,
    val anchor: SelectorData? = null,
    val bone: String? = null,
    val x: SurfaceCameraValue? = null,
    val y: SurfaceCameraValue? = null,
    val z: SurfaceCameraValue? = null,
    val yaw: SurfaceCameraValue? = null,
    val pitch: SurfaceCameraValue? = null,
    val roll: SurfaceCameraValue? = null,
    val durationMillis: SurfaceCameraValue? = null,
    val fov: SurfaceCameraValue? = null,
    val distance: SurfaceCameraValue? = null,
    val vision: SurfaceCameraValue? = null,
    val shaders: SurfaceCameraValue? = null,
    val remove: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    init {
        require(remove || identifier != null) { "SetSurfaceCamera requires an identifier" }
        require(identifier == null || ResourceLocation.tryParse(identifier) != null) { "Invalid surface identifier" }
        require(space == null || space in setOf("world", "player", "entity", "bone")) { "Invalid surface camera space" }
        require(anchor == null || space == null || space == "entity") { "anchor belongs to entity space" }
        require(bone == null || bone.isNotBlank() && (space == null || space == "bone")) { "bone belongs to bone space" }
    }

    companion object {
        val EXAMPLE = SetSurfaceCamera(identifier = "my_pack:monitor", space = "world",
            x = SurfaceCameraValue("0"), y = SurfaceCameraValue("70"), z = SurfaceCameraValue("0"))
    }

    override fun prepare(skill: Skill, context: Context): PrepareData = ListPrepareData(
        SelectorPrepareData(selector.create(context)), SelectorPrepareData(anchor.createEmptyAsNull(context)))

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { recipient ->
            if (recipient !is ServerPlayer) return@forEach
            if (remove) {
                NetworkManager.sendToPlayer(S2CSurfaceCamera(identifier = identifier, remove = true).create(), recipient)
                return@forEach
            }
            fun evaluate(value: SurfaceCameraValue) = ExpressionHelper.initExpression(value.expression, context, recipient).evaluate()
            fun number(value: SurfaceCameraValue?): Double? = value?.let { evaluate(it).numberValue.toDouble().also { n ->
                require(n.isFinite()) { "Surface camera values must be finite" }
            } }
            fun angle(value: SurfaceCameraValue?): Float? = number(value)?.toFloat()?.also {
                require(it.isFinite()) { "Surface camera angles must be finite" }
            }
            val cameraVision = vision?.let {
                if (it.expression in setOf("normal", "night", "thermal")) it.expression else evaluate(it).stringValue
            }?.also { require(it in setOf("normal", "night", "thermal")) { "Invalid surface camera vision" } }
            val cameraFov = angle(fov)?.also { require(it > 1 && it < 179) { "Camera FOV must be between 1 and 179" } }
            val cameraDistance = angle(distance)?.also { require(it in 16f..512f) { "Camera distance must be between 16 and 512" } }
            val duration = number(durationMillis)?.also { require(it >= 0 && it < Long.MAX_VALUE.toDouble()) }?.toLong()
            // -1 distinguishes an explicitly empty selection from an omitted anchor update.
            val cameraAnchor = if (anchor == null) null else data.dataList[1].selector.getTarget(context)?.id ?: -1
            NetworkManager.sendToPlayer(S2CSurfaceCamera(identifier, space, cameraAnchor,
                number(x), number(y), number(z), angle(yaw), angle(pitch), angle(roll), duration,
                cameraFov, cameraDistance, cameraVision, shaders?.let { evaluate(it).booleanValue }, bone = bone).create(), recipient)
        }
    }
}
