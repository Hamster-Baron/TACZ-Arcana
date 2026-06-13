package group.taczexpands.server.expression

import com.charleskorn.kaml.YamlContentPolymorphicSerializer
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.ezylang.evalex.Expression
import com.ezylang.evalex.data.EvaluationValue
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.SelectorInstance
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.world.entity.Entity

@Serializable(with = ExpressionDataPolymorphicSerializer::class)
data class ExpressionData(
    val expression: String,
    val variables: Map<String, ExpressionData> = mapOf(),
    val instant: Boolean = false,
    val cacheOnGet: Boolean = false,
    val target: SelectorData? = null,
    val self: SelectorData? = null,
) {
}

class ExpressionInstance(
    val data: ExpressionData,
    val processDelegate: ((Expression) -> Expression)? = null,
) {
    private var target: SelectorInstance? = null
    private var self: SelectorInstance? = null
    private var cachedValue: EvaluationValue? = null
    private var variables: Map<String, ExpressionInstance>? = null
    fun prepare(context: Context) {
        target = data.target?.create(context, SelectorData.RAW_TARGET)
        self = data.self?.create(context, SelectorData.SELF)
        variables = data.variables.create(context)

        if (data.instant) {
            cachedValue = eval(context, null)
        }
    }

    private fun eval(context: Context, defaultTarget: Entity?): EvaluationValue {
        return ExpressionHelper.initExpression(
            data.expression,
            context,
            target?.getTarget(context) ?: defaultTarget,
            self?.getTarget(context),
            processDelegate
        ).also {
            variables?.forEach { (key, value) ->
                it.with(key, value)
            }
        }.evaluate()
    }

    fun get(context: Context, defaultTarget: Entity?): EvaluationValue {
        if (cachedValue != null) return cachedValue!!

        if (data.cacheOnGet) {
            cachedValue = eval(context, defaultTarget)
            return cachedValue!!
        }

        return eval(context, defaultTarget)
    }
}

object ExpressionDataPolymorphicSerializer : YamlContentPolymorphicSerializer<ExpressionData>(ExpressionData::class) {
    override fun selectDeserializer(node: YamlNode): DeserializationStrategy<ExpressionData> {
        return when (node) {
            is YamlScalar -> ExpressionDataScalarSerializer
            is YamlMap -> ExpressionDataClassSerializer
            else -> throw IllegalArgumentException("Unsupported YAML structure at line ${node.location.line}")
        }
    }

    override fun serialize(encoder: Encoder, value: ExpressionData) {
        return ExpressionDataClassSerializer.serialize(encoder, value)
    }
}

object ExpressionDataScalarSerializer : KSerializer<ExpressionData> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): ExpressionData {
        return ExpressionData(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ExpressionData) {
        throw IllegalStateException("Scalar serialization is not supported")
    }
}

object ExpressionDataClassSerializer : KSerializer<ExpressionData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ExpressionData") {
        element<String>("expression")
        element<Map<String, ExpressionData>>("variables", isOptional = true)
        element<Boolean>("instant", isOptional = true)
        element<Boolean>("cacheOnGet", isOptional = true)
        element<SelectorData?>("target", isOptional = true)
        element<SelectorData?>("self", isOptional = true)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): ExpressionData {
        val composite = decoder.beginStructure(descriptor)

        var expression = ""
        var variables: Map<String, ExpressionData> = mapOf()
        var instant = false
        var cacheOnGet = false
        var target: SelectorData? = null
        var self: SelectorData? = null

        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> expression = composite.decodeStringElement(descriptor, 0)
                1 -> variables = composite.decodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), ExpressionData.serializer()))
                2 -> instant = composite.decodeBooleanElement(descriptor, 2)
                3 -> cacheOnGet = composite.decodeBooleanElement(descriptor, 3)
                4 -> target = composite.decodeNullableSerializableElement(descriptor, 4, SelectorData.serializer())
                5 -> self = composite.decodeNullableSerializableElement(descriptor, 5, SelectorData.serializer())
                else -> throw IllegalArgumentException("Unexpected index: $index")
            }
        }
        composite.endStructure(descriptor)
        return ExpressionData(expression, variables, instant, cacheOnGet, target, self)

    }


    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: ExpressionData) {
        val composite = encoder.beginStructure(descriptor)

        composite.encodeStringElement(descriptor, 0, value.expression)
        composite.encodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), ExpressionData.serializer()), value.variables)
        composite.encodeBooleanElement(descriptor, 2, value.instant)
        composite.encodeBooleanElement(descriptor, 3, value.cacheOnGet)
        composite.encodeNullableSerializableElement(descriptor, 4, SelectorData.serializer(), value.target)
        composite.encodeNullableSerializableElement(descriptor, 5, SelectorData.serializer(), value.self)

        composite.endStructure(descriptor)
    }
}

fun ExpressionData.create(context: Context): ExpressionInstance {
    val instance = ExpressionInstance(this)
    instance.prepare(context)
    return instance
}

fun List<ExpressionData>?.create(context: Context): List<ExpressionInstance> {
    return this?.map { it.create(context) } ?: emptyList()
}

fun Map<String, ExpressionData>?.create(context: Context): Map<String, ExpressionInstance> {
    return this?.mapValues { (_, v) -> v.create(context) } ?: emptyMap()
}

