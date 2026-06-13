package group.taczexpands.server.config

import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.skill.TriggerType
import group.taczexpands.server.util.Debug
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkillConfig(
    @SerialName("Name") val name: String,
    @SerialName("Lore") val lore: String,
    @SerialName("Guns") val guns: List<String>? = null,
    @SerialName("Attachments") val attachments: List<String>? = null,
    @SerialName("Trigger") val trigger: String,
    @SerialName("Flags") val flags: Flags,
    @SerialName("Conditions") val conditions: List<Condition>,
    @SerialName("Actions") val actions: List<Action>,
    @SerialName("ReverseActions") val reverseActions: List<Action>? = null,
) {
    companion object {
        val EXAMPLE = SkillConfig("example", "test", listOf("tacz:ak47"), listOf("tacz:attachment"), TriggerType.ON_ACTION_1.name, Flags(), Debug.CONDITIONS, Debug.ACTIONS, null)
    }

    val hasReverseAction: Boolean
        get() = !reverseActions.isNullOrEmpty()

    fun has(condition: ReverseTriggerCondition): Boolean {
        return flags.reverseTriggerConditions.contains(condition)
    }
}

@Serializable
data class Flags(
    @SerialName("TriggerProbability") val triggerProbability: Int = 100,
    @SerialName("CooldownGroup") val cooldownGroup: String? = null,
    @SerialName("Cooldown") val coolDown: Int = 0,
    @SerialName("KeepCheckTimes") val keepCheckTimes: Int = 0,
    @SerialName("BypassFirstConditionCheck") val bypassFirstConditionCheck: Boolean = false,
    @SerialName("Frequency") val frequency: Int = 0,
    @SerialName("RepeatTimes") val repeatTimes: Int = 0,
    @SerialName("RepeatDelay") val repeatDelay: Int = 0,
    @SerialName("ConditionExpression") val conditionExpression: String = "ALL_TRUE",
    @SerialName("ReverseTriggerConditions") val reverseTriggerConditions: Set<ReverseTriggerCondition> = setOf(ReverseTriggerCondition.NEED_TRIGGER_ONCE),
    @SerialName("UseRegex") val useRegex: Boolean = false,

    @SerialName("StrictSourceMode") val strictSourceMode: Boolean = true,
) {
    init {
        if (!ReverseTriggerCondition.checkValid(reverseTriggerConditions)) {
            throw IllegalArgumentException("ReverseTriggerConditions conflicts ${reverseTriggerConditions}");
        }
    }

}

@Serializable
enum class ReverseTriggerCondition(conflictsSupplier: () -> Set<ReverseTriggerCondition>) {
    @SerialName("NEED_TRIGGER_ONCE")
    NEED_TRIGGER_ONCE({ setOf(NEED_NO_FAIL) }),

    @SerialName("NEED_NO_FAIL")
    NEED_NO_FAIL({ setOf(NEED_TRIGGER_ONCE) }),

    @SerialName("BYPASS_LEAVE")
    BYPASS_LEAVE({ setOf() });

    val conflicts: Set<ReverseTriggerCondition> by lazy(conflictsSupplier)

    companion object {
        fun checkValid(sets: Set<ReverseTriggerCondition>): Boolean {
            for (set in sets) {
                if (sets.intersect(set.conflicts).isNotEmpty()) return false
            }
            return true
        }
    }
}

