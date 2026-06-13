package group.taczexpands.client.gui

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator
import com.tacz.guns.api.item.IGun
import com.tacz.guns.client.model.bedrock.BedrockPart
import com.tacz.guns.resource.pojo.data.gun.GunData
import group.taczexpands.common.accessor.IAccessorGunData
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import kotlin.jvm.optionals.getOrNull

object GunContextManager {
    var underBarrelLockReleaseTime = 0L
    var scopeElementVolatile = false
    var animationRedirectVolatile = false
    private val scopeElementRenderMap = mutableMapOf<String, Boolean>()
    private val animationRedirectMap = mutableMapOf<String, String>()


    fun shouldRenderScopeElement(name: String): Boolean {
        return scopeElementRenderMap[name] ?: false
    }

    fun setScopeElementRenderState(name: String, state: Boolean) {
        scopeElementRenderMap[name] = state
    }

    fun resetScopeElementRender() {
        scopeElementRenderMap.clear()
    }

    fun resetAnimationRedirect() {
        animationRedirectMap.clear()
    }

    fun resetUnderBarrelLockReleaseTime() {
        underBarrelLockReleaseTime = 0L
    }

    fun reset() {
        resetScopeElementRender()
        resetAnimationRedirect()
        resetUnderBarrelLockReleaseTime()
        resetAmmoElementRender()
    }

    fun lockUnderBarrel(timeSecond: Float) {
        val player = Minecraft.getInstance().player ?: return
        val timeMillis = (timeSecond * 1000).toLong()
        underBarrelLockReleaseTime = System.currentTimeMillis() + timeMillis
        IClientPlayerGunOperator.fromLocalPlayer(player).dataHolder.lockState { true }
    }

    fun getRedirect(name: String): String? {
        return animationRedirectMap[name]
    }

    fun addRedirect(name: String, to: String) {
        animationRedirectMap[name] = to
    }

    fun removeRedirect(name: String) {
        animationRedirectMap.remove(name)
    }

    fun onChangeGun() {
        if (scopeElementVolatile) {
            resetScopeElementRender()
        }

        if (animationRedirectVolatile) {
            resetAnimationRedirect()
        }

        resetAmmoElementRender()
    }

    fun resetAmmoElementRender() {
        lastAmmoId = null
        delay = null
    }

    var lastAmmoId: String? = null
    var delay: Long? = null

    private const val DEFAULT_AMMO_MODEL_ELEMENT = "default"
    private const val API_MARKER = "_api_"
    private const val LEGACY_AMMO_API_PREFIX = "ammo_api_"
    private const val LEGACY_AMMO_MAG_API_PREFIX = "ammomag_api_"
    private const val CUSTOM_AMMO_API_PREFIX = "ammo_"

    private data class AmmoModelName(val groupPrefix: String, val element: String)

    private data class AmmoRenderContext(
        val gunData: GunData,
        val gunItem: ItemStack,
        val currentAmmoId: String,
        val availableElements: List<String>
    )

    fun isAmmoModelElementName(name: String): Boolean {
        return name.startsWith(LEGACY_AMMO_API_PREFIX)
                || name.startsWith(LEGACY_AMMO_MAG_API_PREFIX)
                || (name.startsWith(CUSTOM_AMMO_API_PREFIX) && name.contains(API_MARKER))
    }

    fun shouldRenderAmmoElement(input: BedrockPart): Boolean {
        return shouldRenderAmmoModelElement(input)
    }

    fun shouldRenderAmmoMagElement(input: BedrockPart): Boolean {
        return shouldRenderAmmoModelElement(input)
    }

    fun shouldRenderAmmoModelElement(input: BedrockPart): Boolean {
        val name = input.name ?: return false
        val context = getAmmoRenderContext() ?: return name.endsWith("_$DEFAULT_AMMO_MODEL_ELEMENT")
        val parsedName = parseAmmoModelName(name, context.availableElements) ?: return false

        updateAmmoModelState(context)

        val renderedAmmoId = lastAmmoId ?: context.currentAmmoId
        val renderedElement = encodeAmmoId(renderedAmmoId)
        val currentElement = if (hasAmmoModelElement(input, parsedName.groupPrefix, renderedElement)) {
            renderedElement
        } else {
            DEFAULT_AMMO_MODEL_ELEMENT
        }

        return parsedName.element == currentElement
    }

    private fun getAmmoRenderContext(): AmmoRenderContext? {
        val player = Minecraft.getInstance().player ?: return null
        val mainHand = player.mainHandItem
        val iGun = IGun.getIGunOrNull(mainHand) ?: return null
        val gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(mainHand)).getOrNull() ?: return null
        val currentAmmoId = IAccessorGunData.getCurrentAmmoId(gunIndex.gunData, mainHand).toString()
        val availableElements = linkedSetOf(DEFAULT_AMMO_MODEL_ELEMENT, encodeAmmoId(gunIndex.gunData.ammoId.toString()))

        IAccessorGunData.getExtraAmmoList(gunIndex.gunData, mainHand).forEach {
            availableElements.add(encodeAmmoId(it.ammoId.toString()))
        }

        return AmmoRenderContext(gunIndex.gunData, mainHand, currentAmmoId, availableElements.toList())
    }

    private fun updateAmmoModelState(context: AmmoRenderContext) {
        if (lastAmmoId == null) {
            lastAmmoId = context.currentAmmoId
        }

        if (lastAmmoId != context.currentAmmoId) {
            if (delay == null) {
                delay = getAmmoModelSwitchTime(context, lastAmmoId!!)
            }

            val now = System.currentTimeMillis()
            if (now >= delay!!) {
                lastAmmoId = context.currentAmmoId
                delay = null
            }
        }
    }

    private fun getAmmoModelSwitchTime(context: AmmoRenderContext, previousAmmoId: String): Long {
        val currentExtraAmmo = IAccessorGunData.getCurrentBaseAmmo(context.gunData, context.gunItem)
            ?: IAccessorGunData.getCurrentExtraAmmo(context.gunData, context.gunItem)

        val delaySeconds = if (currentExtraAmmo != null && currentExtraAmmo.isAutoModelControl) {
            currentExtraAmmo.changeModelDelay
        } else {
            val previousAmmoLocation = ResourceLocation.tryParse(previousAmmoId)
            val previousExtraAmmo = previousAmmoLocation?.let {
                IAccessorGunData.getExtraAmmo(context.gunData, context.gunItem, it, true)
            }
            if (previousExtraAmmo != null && previousExtraAmmo.isAutoModelControl) {
                previousExtraAmmo.changeModelDelay
            } else {
                0.0f
            }
        }

        return System.currentTimeMillis() + (1000 * delaySeconds).toLong()
    }

    private fun parseAmmoModelName(name: String, availableElements: List<String>): AmmoModelName? {
        if (!isAmmoModelElementName(name)) return null
        return availableElements
            .sortedByDescending { it.length }
            .firstNotNullOfOrNull { element ->
                if (!name.endsWith(element)) return@firstNotNullOfOrNull null
                val groupPrefix = name.dropLast(element.length)
                if (groupPrefix.isNotEmpty() && groupPrefix.endsWith("_") && groupPrefix.contains(API_MARKER)
                    && isAmmoModelElementName(groupPrefix + element)) {
                    AmmoModelName(groupPrefix, element)
                } else {
                    null
                }
            }
    }

    private fun hasAmmoModelElement(input: BedrockPart, groupPrefix: String, element: String): Boolean {
        return getRootPart(input).walk().any {
            it.name == groupPrefix + element
        }
    }

    private fun getRootPart(input: BedrockPart): BedrockPart {
        return generateSequence(input) { it.parent }.last()
    }

    private fun BedrockPart.walk(): Sequence<BedrockPart> = sequence {
        yield(this@walk)
        this@walk.children.forEach {
            yieldAll(it.walk())
        }
    }

    private fun encodeAmmoId(ammoId: String): String {
        return ammoId.replace(":", "_")
    }
}
