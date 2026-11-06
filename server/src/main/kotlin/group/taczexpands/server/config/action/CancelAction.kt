package group.taczexpands.server.config.action

import group.taczexpands.common.network.v2.s2c.S2CActionLockState
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.SelectorInstance
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.rotation.RotationSpec
import group.taczexpands.server.context.Context
import group.taczexpands.server.module.rotation.RotationManager
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.TriggerType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.level.ServerPlayer

/** A complete action lock, or a bounded rotation lock when rotation is supplied. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("CancelAction")
data class CancelAction @JvmOverloads constructor(
    val action: S2CActionLockState.Action,
    val duration: Int? = 1,
    val status: TriggerType? = null,
    val identifier: String? = null,
    val remove: Boolean = false,
    val selector: SelectorData? = null,
    override val delay: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val rotation: RotationSpec? = null
) : Action {
    init {
        require(rotation == null || action == S2CActionLockState.Action.Rotate) {
            "rotation is only valid for action: Rotate"
        }
    }

    companion object {
        val EXAMPLE = CancelAction(S2CActionLockState.Action.Sprint)

        /** Binary-compatible seven-field copy entry point for compiled Kotlin skills. */
        @JvmStatic
        @Suppress("UNUSED_PARAMETER")
        fun `copy$default`(
            value: CancelAction, action: S2CActionLockState.Action, duration: Int?, status: TriggerType?,
            identifier: String?, remove: Boolean, selector: SelectorData?, delay: Int?, mask: Int, marker: Any?
        ): CancelAction = value.copy(
            if (mask and 1 != 0) value.action else action,
            if (mask and 2 != 0) value.duration else duration,
            if (mask and 4 != 0) value.status else status,
            if (mask and 8 != 0) value.identifier else identifier,
            if (mask and 16 != 0) value.remove else remove,
            if (mask and 32 != 0) value.selector else selector,
            if (mask and 64 != 0) value.delay else delay
        )
    }

    /** Default-constructor descriptor for compiled seven-field callers. */
    @Suppress("UNUSED_PARAMETER")
    constructor(
        action: S2CActionLockState.Action, duration: Int?, status: TriggerType?, identifier: String?,
        remove: Boolean, selector: SelectorData?, delay: Int?, mask: Int,
        marker: kotlin.jvm.internal.DefaultConstructorMarker?
    ) : this(
        action,
        if (mask and 2 != 0) 1 else duration,
        if (mask and 4 != 0) null else status,
        if (mask and 8 != 0) null else identifier,
        if (mask and 16 != 0) false else remove,
        if (mask and 32 != 0) null else selector,
        if (mask and 64 != 0) null else delay,
        null
    )

    /** Copying the seven action fields retains the rotation range. */
    fun copy(
        action: S2CActionLockState.Action, duration: Int?, status: TriggerType?, identifier: String?,
        remove: Boolean, selector: SelectorData?, delay: Int?
    ): CancelAction = copy(action, duration, status, identifier, remove, selector, delay, rotation)

    override fun prepare(skill: Skill, context: Context): PrepareData {
        val preparedSelector = SelectorInstance(selector ?: SelectorData.SELF)
        preparedSelector.prepare(context)
        return SelectorPrepareData(preparedSelector)
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        if (RotationManager.handle(this, skill, context, data)) return

        val sourceIdentifier = identifier ?: skill.resolveAnonymousActionLockSourceIdentifier(this)
        for (target in data.selector.getTargets(context)) {
            if (target !is ServerPlayer) continue
            if (status != null) {
                if (remove) ActionManager.removeCancel(target, action, status, sourceIdentifier)
                else ActionManager.cancelAction(target, action, status, sourceIdentifier)
            } else if (duration != null) {
                if (remove) ActionManager.removeCancel(target, action, sourceIdentifier)
                else if (duration >= 0) ActionManager.cancelAction(target, action, duration, sourceIdentifier)
            }
        }
    }

    // Hash the seven action fields when rotation is absent; include the range when present.
    override fun hashCode(): Int {
        var hash = action.hashCode()
        hash = 31 * hash + (duration?.hashCode() ?: 0)
        hash = 31 * hash + (status?.hashCode() ?: 0)
        hash = 31 * hash + (identifier?.hashCode() ?: 0)
        hash = 31 * hash + remove.hashCode()
        hash = 31 * hash + (selector?.hashCode() ?: 0)
        hash = 31 * hash + (delay?.hashCode() ?: 0)
        return rotation?.let { 31 * hash + it.hashCode() } ?: hash
    }
}
