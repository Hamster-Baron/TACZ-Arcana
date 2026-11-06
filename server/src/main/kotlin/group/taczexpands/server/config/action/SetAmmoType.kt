package group.taczexpands.server.config.action

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.SelectorInstance
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.context.Context
import group.taczexpands.server.network.c2s.C2SSwitchAmmoImpl
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import kotlin.jvm.optionals.getOrNull

@Serializable
@SerialName("SetAmmoType")
data class SetAmmoType @JvmOverloads constructor(
    val index: Int? = null,
    val ammoType: String? = null,
    val selector: SelectorData? = null,
    override val delay: Int? = null,
    val preserveLoadedAmmo: Boolean = false,
    val triggerSwitchAmmoEvent: Boolean = false
) : Action {
    companion object {
        val EXAMPLE = SetAmmoType(null, "tacz:9mm")

        // Guard synchronous notifications per player while nested selections refresh state.
        // Delayed selections begin a fresh notification scope.
        private val notifyingPlayers = ThreadLocal.withInitial { HashSet<ServerPlayer>() }

        /** Binary entry point used by Kotlin callers compiled against the four-field action. */
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun `copy$default`(
            value: SetAmmoType, index: Int?, ammoType: String?, selector: SelectorData?, delay: Int?,
            mask: Int, marker: Any?
        ): SetAmmoType = value.copy(
            if (mask and 1 != 0) value.index else index,
            if (mask and 2 != 0) value.ammoType else ammoType,
            if (mask and 4 != 0) value.selector else selector,
            if (mask and 8 != 0) value.delay else delay
        )
    }

    /** Binary-compatible default-constructor entry point for compiled Kotlin callers. */
    @Suppress("UNUSED_PARAMETER")
    constructor(
        index: Int?, ammoType: String?, selector: SelectorData?, delay: Int?,
        mask: Int, marker: kotlin.jvm.internal.DefaultConstructorMarker?
    ) : this(
        if (mask and 1 != 0) null else index,
        if (mask and 2 != 0) null else ammoType,
        if (mask and 4 != 0) null else selector,
        if (mask and 8 != 0) null else delay,
        false, false
    )

    override fun prepare(skill: Skill, context: Context): PrepareData {
        val preparedSelector = SelectorInstance(selector ?: SelectorData.SELF)
        preparedSelector.prepare(context)
        return SelectorPrepareData(preparedSelector)
    }

    /** Binary-compatible four-argument Java copy entry point. */
    fun copy(index: Int?, ammoType: String?, selector: SelectorData?, delay: Int?): SetAmmoType =
        copy(index, ammoType, selector, delay, preserveLoadedAmmo, triggerSwitchAmmoEvent)

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        data.selector.getTargets(context).forEach { target ->
            target as? ServerPlayer ?: return@forEach
            if (ammoType != null) {
                select(target, ResourceLocation(ammoType))
            } else if (index != null) {
                val mainHand = target.mainHandItem
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                if (gunItem == mainHand.item) {
                    val gunIndex = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull() ?: return@forEach
                    val ammoList = IAccessorGunData.getExtraAmmoList(gunIndex.gunData, mainHand)
                    val realIndex = index - 1
                    if (realIndex == -1) {
                        select(target, gunIndex.gunData.ammoId)
                    } else if (realIndex >= 0) {
                        if (ammoList.isEmpty()) return@forEach
                        val ammoType = ammoList[realIndex % ammoList.size].ammoId
                        select(target, ammoType)
                    }
                }
            }
        }
    }

    private fun select(player: ServerPlayer, ammoId: ResourceLocation) {
        if (!preserveLoadedAmmo) {
            C2SSwitchAmmoImpl.setAmmoType(player, ammoId, true)
            return
        }

        val stack = player.mainHandItem
        val gun = ModItems.MODERN_KINETIC_GUN.get()
        if (stack.item != gun) return
        val gunData = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).getOrNull()?.gunData ?: return
        if (!IAccessorGunData.isValidAmmoId(gunData, stack, ammoId)) return

        val previousId = IAccessorGunData.getCurrentAmmoId(gunData, stack)
        GunExtras.setCurrentExtraAmmoId(stack, ammoId.takeUnless { it == gunData.ammoId })
        AttachmentPropertyManager.postChangeEvent(player, stack)
        player.inventoryMenu.broadcastChanges()
        NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.RefreshCache), player)

        // A same-ID call refreshes caches; changed IDs can emit a notification.
        if (!triggerSwitchAmmoEvent || player.mainHandItem !== stack || previousId == ammoId ||
            IAccessorGunData.getCurrentAmmoId(gunData, stack) != ammoId) return

        val active = notifyingPlayers.get()
        if (!active.add(player)) return
        try {
            SkillManager.trigger(TriggerType.ON_SWITCH_AMMO, Context(player))
        } finally {
            active.remove(player)
            if (active.isEmpty()) notifyingPlayers.remove()
        }
    }
}
