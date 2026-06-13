package group.taczexpands.common.network

import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.network.c2s.C2SActionKey
import group.taczexpands.common.network.c2s.C2SGetVariable
import group.taczexpands.common.network.c2s.C2SRaw
import group.taczexpands.common.network.c2s.C2SSwitchAmmo
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CAnimation
import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.common.network.s2c.S2CFlash
import group.taczexpands.common.network.s2c.S2CRaw
import group.taczexpands.common.network.s2c.S2CSendVariable
import group.taczexpands.common.network.s2c.S2CShake
import group.taczexpands.common.network.s2c.S2CSound
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import java.lang.reflect.Modifier
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Supplier

object NetworkCommon {
    val VERSION = "2.0"
    val CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation(TACZExpandsCommon.MODID, "main"),
        { VERSION },
        { it == VERSION },
        { it == VERSION })

    var index = 0
    private var registered = false

    @JvmStatic
    @Synchronized
    fun init(): Boolean {
        if (registered) return true
        registered = true

        registerC2S(C2SActionKey::class.java, C2SActionKey.getEncoder(), C2SActionKey.getDecoder(), "group.taczexpands.server.network.c2s.C2SActionKeyImpl")
        registerC2S(C2SGetVariable::class.java, C2SGetVariable.getEncoder(), C2SGetVariable.getDecoder(), "group.taczexpands.server.network.c2s.C2SGetVariableImpl")
        registerC2S(C2SRaw::class.java, C2SRaw.getEncoder(), C2SRaw.getDecoder(), "group.taczexpands.server.network.c2s.C2SRawImpl")
        registerC2S(C2SSwitchAmmo::class.java, C2SSwitchAmmo.getEncoder(), C2SSwitchAmmo.getDecoder(), "group.taczexpands.server.network.c2s.C2SSwitchAmmoImpl")

        registerS2C(S2CAction::class.java, S2CAction.getEncoder(), S2CAction.getDecoder(), "group.taczexpands.client.network.s2c.S2CActionImpl")
        registerS2C(S2CAnimation::class.java, S2CAnimation.getEncoder(), S2CAnimation.getDecoder(), "group.taczexpands.client.network.s2c.S2CAnimationImpl")
        registerS2C(S2CCancelAction::class.java, S2CCancelAction.getEncoder(), S2CCancelAction.getDecoder(), "group.taczexpands.client.network.s2c.S2CCancelActionImpl")
        registerS2C(S2CFlash::class.java, S2CFlash.getEncoder(), S2CFlash.getDecoder(), "group.taczexpands.client.network.s2c.S2CFlashImpl")
        registerS2C(S2CRaw::class.java, S2CRaw.getEncoder(), S2CRaw.getDecoder(), "group.taczexpands.client.network.s2c.S2CRawImpl")
        registerS2C(S2CSendVariable::class.java, S2CSendVariable.getEncoder(), S2CSendVariable.getDecoder(), "group.taczexpands.client.network.s2c.S2CSendVariableImpl")
        registerS2C(S2CShake::class.java, S2CShake.getEncoder(), S2CShake.getDecoder(), "group.taczexpands.client.network.s2c.S2CShakeImpl")
        registerS2C(S2CSound::class.java, S2CSound.getEncoder(), S2CSound.getDecoder(), "group.taczexpands.client.network.s2c.S2CSoundImpl")

        return true
    }

    private fun <MSG> registerC2S(
        type: Class<MSG>,
        encoder: BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf>,
        decoder: java.util.function.Function<net.minecraft.network.FriendlyByteBuf, MSG>,
        handlerClass: String
    ) {
        CHANNEL.registerMessage(index++, type, encoder, decoder, reflectedHandler(handlerClass), Optional.of(NetworkDirection.PLAY_TO_SERVER))
    }

    private fun <MSG> registerS2C(
        type: Class<MSG>,
        encoder: BiConsumer<MSG, net.minecraft.network.FriendlyByteBuf>,
        decoder: java.util.function.Function<net.minecraft.network.FriendlyByteBuf, MSG>,
        handlerClass: String
    ) {
        CHANNEL.registerMessage(index++, type, encoder, decoder, reflectedHandler(handlerClass), Optional.of(NetworkDirection.PLAY_TO_CLIENT))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <MSG> reflectedHandler(handlerClass: String): BiConsumer<MSG, Supplier<NetworkEvent.Context>> {
        return BiConsumer { packet, context ->
            try {
                val clazz = Class.forName(handlerClass)
                val method = clazz.getMethod("getHandler")
                val receiver = if (Modifier.isStatic(method.modifiers)) null else clazz.getField("INSTANCE").get(null)
                val handler = method.invoke(receiver) as BiConsumer<MSG, Supplier<NetworkEvent.Context>>
                handler.accept(packet, context)
            } catch (e: ClassNotFoundException) {
                setHandled(packet, context)
            }
        }
    }

    @JvmStatic
    fun <MSG> setHandled(packet: MSG, context: Supplier<NetworkEvent.Context>) {
        context.get().let {
            it.packetHandled = true
        }
    }

    @JvmStatic
    fun <MSG> getHandler(): BiConsumer<MSG, Supplier<NetworkEvent.Context>> {
        return BiConsumer(::setHandled)
    }
}
