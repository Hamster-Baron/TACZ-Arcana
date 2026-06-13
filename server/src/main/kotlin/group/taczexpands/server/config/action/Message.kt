package group.taczexpands.server.config.action

import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.level.ServerPlayer

@Serializable
enum class MessageType {
    @SerialName("chat")
    CHAT,

    @SerialName("actionbar")
    ACTIONBAR,

    @SerialName("title")
    TITLE,

    @SerialName("subtitle")
    SUBTITLE
}

@Serializable
@SerialName("Message")
data class Message(
    val type: MessageType,
    val message: String = "",
    val fadeIn: Int = 0,
    val stay: Int = 20,
    val fadeOut: Int = 0,
    val isRawJsonText: Boolean = false,
    val isExpression: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Message(MessageType.CHAT)
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return SelectorPrepareData(selector.create(context))
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            when (type) {
                MessageType.CHAT -> {
                    val messageString = if (!isExpression) message else ExpressionHelper.initExpression(message, context, target).evaluate().stringValue
                    val components = if (!isRawJsonText) {
                        Component.literal(messageString)
                    } else {
                        Component.Serializer.fromJsonLenient(messageString) ?: throw IllegalArgumentException("json syntax error. $messageString")
                    }

                    target.sendSystemMessage(components)
                }

                MessageType.ACTIONBAR -> {
                    val targetPlayer = target as? ServerPlayer ?: return@forEach

                    val messageString = if (!isExpression) message else ExpressionHelper.initExpression(message, context, target).evaluate().stringValue
                    val components = if (!isRawJsonText) {
                        Component.literal(messageString)
                    } else {
                        Component.Serializer.fromJsonLenient(messageString) ?: throw IllegalArgumentException("json syntax error. $messageString")
                    }

                    targetPlayer.sendSystemMessage(components, true)
                }

                MessageType.TITLE -> {
                    val targetPlayer = target as? ServerPlayer ?: return@forEach

                    val messageString = if (!isExpression) message else ExpressionHelper.initExpression(message, context, target).evaluate().stringValue
                    val components = if (!isRawJsonText) {
                        Component.literal(messageString)
                    } else {
                        Component.Serializer.fromJsonLenient(messageString) ?: throw IllegalArgumentException("json syntax error. $messageString")
                    }

                    targetPlayer.connection.send(ClientboundSetTitlesAnimationPacket(0, 20, 0))
                    targetPlayer.connection.send(ClientboundSetTitleTextPacket(components))
                }

                MessageType.SUBTITLE -> {
                    val targetPlayer = target as? ServerPlayer ?: return@forEach

                    val messageString = if (!isExpression) message else ExpressionHelper.initExpression(message, context, target).evaluate().stringValue
                    val components = if (!isRawJsonText) {
                        Component.literal(messageString)
                    } else {
                        Component.Serializer.fromJsonLenient(messageString) ?: throw IllegalArgumentException("json syntax error. $messageString")
                    }

                    targetPlayer.connection.send(ClientboundSetSubtitleTextPacket(components))
                }
            }
        }
    }
}