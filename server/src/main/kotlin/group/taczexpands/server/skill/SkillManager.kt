package group.taczexpands.server.skill

import group.taczexpands.common.util.sendModMessage
import group.taczexpands.server.TACZExpandsServer
import group.taczexpands.server.config.ActionConfig
import group.taczexpands.server.config.ConditionConfig
import group.taczexpands.server.config.ReverseTriggerCondition
import group.taczexpands.server.config.SkillConfig
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.YAML
import kotlinx.serialization.decodeFromString
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

data class ContextSkillPair(val context: Context, val skillMagicNumPair: SkillMagicNumPair, var time: Int) {


}

data class ReverseContext(val skillMagicNumPair: SkillMagicNumPair) {
    private var _context: Context? = null
    var context: Context
        get() {
            return _context!!
        }
        set(value) {
            _context = value
        }
    var failed: Boolean = false
}

data class SkillMagicNumPair(val skill: Skill, val magicNum: Int)
data class SkillCancelStatus(val skill: String, var duration: Int)

object SkillManager {
    val LOADED_SKILLS = mutableMapOf<TriggerType, MutableList<Skill>>().apply {
        TriggerType.entries.forEach { this[it] = mutableListOf() }
    }

    val LOADED_CHAIN_CONDITIONS = mutableMapOf<String, ConditionConfig>()
    val LOADED_CHAIN_ACTIONS = mutableMapOf<String, ActionConfig>()

    val KEEP_CHECK_LIST = mutableListOf<ContextSkillPair>()

    val KEEP_TRIGGER_MAP = mutableMapOf<ServerPlayer, MutableList<ContextSkillPair>>()

    val REVERSE_CHECK_MAP = mutableMapOf<ServerPlayer, MutableList<ReverseContext>>()

    val QUIT_TRIGGER_MAP = mutableMapOf<ServerPlayer, MutableSet<ReverseContext>>()

    val CANCEL_SKILL_MAP = mutableMapOf<ServerPlayer, MutableList<SkillCancelStatus>>()


    @JvmStatic
    fun clear() {
        LOADED_SKILLS.values.forEach { it.clear() }
        KEEP_TRIGGER_MAP.clear()
        LOADED_CHAIN_CONDITIONS.clear()
        LOADED_CHAIN_ACTIONS.clear()
    }



    @JvmStatic
    fun appendData(type: Int, data: ByteArray) {
        try {
            if (type == 0) {
                val config = YAML.decodeFromString<SkillConfig>(String(data, Charsets.UTF_8))
                registerSkill(Skill(config))
            } else if (type == 1) {
                val config = YAML.decodeFromString<ConditionConfig>(String(data, Charsets.UTF_8))
                registerCondition(config)
            } else if (type == 2) {
                val config = YAML.decodeFromString<ActionConfig>(String(data, Charsets.UTF_8))
                registerAction(config)
            }
        } catch (e: Exception) {
            val message = e.message
            if (message != null) {
                TACZExpandsServer.INSTANCE.reloadMessageListener?.sendModMessage(Component.literal(message))
            }
            TACZExpandsServer.INSTANCE.reloadErrorCount++
            e.printStackTrace()
        }
    }

    fun registerSkill(skill: Skill) {
        LOADED_SKILLS[skill.trigger]!!.add(skill)
    }

    fun registerCondition(condition: ConditionConfig) {
        LOADED_CHAIN_CONDITIONS[condition.name] = condition
    }

    fun registerAction(action: ActionConfig) {
        LOADED_CHAIN_ACTIONS[action.name] = action
    }


    fun trigger(type: TriggerType, context: Context, magicNum: Int = -1) {
        triggerSpecificType(type, context, magicNum)

        type.superTypes.forEach {
            triggerSpecificType(it, context, magicNum)
        }
    }

    fun triggerSpecificType(type: TriggerType, context: Context, magicNum: Int = -1) {
        if (type.reversible) {
            ActionManager.notifyStatusEntry(context.self, type)
        }
        LOADED_SKILLS[type]!!.forEach {
            try {
                if (it.config.flags.bypassFirstConditionCheck || it.checkCondition(context, magicNum)) {
                    if (it.config.flags.keepCheckTimes > 0) {

                        if (KEEP_CHECK_LIST.firstOrNull { entry -> entry.context.self == context.self && entry.skillMagicNumPair.skill == it && entry.skillMagicNumPair.magicNum == magicNum } == null) {
                            KEEP_CHECK_LIST.add(
                                ContextSkillPair(
                                    context,
                                    SkillMagicNumPair(it, magicNum),
                                    it.config.flags.keepCheckTimes
                                )
                            )
                        }
                    } else {
                        if (it.config.flags.frequency > 0 && it.trigger.reversible) {
                            triggerFrequentAction(context, it, magicNum)
                        } else {
                            triggerActionDirectly(context, it, magicNum)
                        }
                    }
                } else {
                    triggerActionFailed(context, it, magicNum)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerFrequentAction(context: Context, skill: Skill, magicNum: Int) {
        KEEP_TRIGGER_MAP.getOrPut(context.self) { mutableListOf() }.let {
            val pair = ContextSkillPair(context, SkillMagicNumPair(skill, magicNum), 0)
            if (it.firstOrNull { it.skillMagicNumPair == pair.skillMagicNumPair } == null) {
                it.add(pair)
            }
        }
    }

    fun triggerActionDirectly(context: Context, skill: Skill, magicNum: Int) {
        if (skill.triggerAction(context)) {
            addReverse(context, skill, magicNum)
        } else {
            triggerActionFailed(context, skill, magicNum)
        }
    }

    fun triggerActionFailed(context: Context, skill: Skill, magicNum: Int) {
        if (!skill.config.has(ReverseTriggerCondition.NEED_TRIGGER_ONCE) && !skill.config.has(ReverseTriggerCondition.NEED_NO_FAIL)) {
            addReverse(context, skill, magicNum)
        }
        if (skill.config.has(ReverseTriggerCondition.NEED_NO_FAIL)) {
            markReverseFailed(context, skill, magicNum)
        }
    }

    private fun addReverse(context: Context, skill: Skill, magicNum: Int) {
        if (skill.config.hasReverseAction && skill.trigger.reversible) { 
            REVERSE_CHECK_MAP.getOrPut(context.self) { mutableListOf() }
                .let {
                    val pair = ReverseContext(SkillMagicNumPair(skill, magicNum)).also { it.context = context }
                    if (!it.contains(pair)) {
                        it.add(pair)
                    }
                }
        }

        if (skill.config.hasReverseAction && !skill.trigger.reversible) {
            QUIT_TRIGGER_MAP.getOrPut(context.self) { mutableSetOf() }
                .let {
                    val pair = ReverseContext(SkillMagicNumPair(skill, magicNum)).also { it.context = context }
                    if (!it.contains(pair)) {
                        it.add(pair)
                    }
                }

        }
    }

    private fun markReverseFailed(context: Context, skill: Skill, magicNum: Int) {
        if (skill.config.hasReverseAction && skill.trigger.reversible) { 
            REVERSE_CHECK_MAP.getOrPut(context.self) { mutableListOf() }
                .let {
                    var pair = ReverseContext(SkillMagicNumPair(skill, magicNum)).also { it.context = context }
                    if (!it.contains(pair)) {
                        it.add(pair)
                    } else {
                        pair = it.firstOrNull { it.skillMagicNumPair == pair.skillMagicNumPair } ?: return
                    }
                    pair.failed = true
                }
        }

        if (skill.config.hasReverseAction && !skill.trigger.reversible) {
            QUIT_TRIGGER_MAP.getOrPut(context.self) { mutableSetOf() }
                .let {
                    var pair = ReverseContext(SkillMagicNumPair(skill, magicNum)).also { it.context = context }
                    if (!it.contains(pair)) {
                        it.add(pair)
                    } else {
                        pair = it.firstOrNull { it.skillMagicNumPair == pair.skillMagicNumPair } ?: return
                    }
                    pair.failed = true
                }

        }
    }

    fun triggerReverse(type: TriggerType, context: Context, magicNum: Int = -1) {
        if (type.reversible) {
            ActionManager.notifyStatusLeave(context.self, type)
        }

        if (KEEP_TRIGGER_MAP.containsKey(context.self)) {
            val keepTriggerList = KEEP_TRIGGER_MAP[context.self]!!
            keepTriggerList.indices.reversed().forEach {
                try {
                    val pair = keepTriggerList[it]
                    if (pair.skillMagicNumPair.skill.trigger == type && pair.skillMagicNumPair.magicNum == magicNum) {
                        keepTriggerList.removeAt(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (REVERSE_CHECK_MAP.containsKey(context.self)) {
            val reverseList = REVERSE_CHECK_MAP[context.self]!!
            reverseList.indices.reversed().forEach {
                try {
                    val pair = reverseList[it]
                    if (pair.skillMagicNumPair.skill.trigger == type && pair.skillMagicNumPair.magicNum == magicNum) {
                        val pair = reverseList.removeAt(it)
                        if (!pair.failed)
                            pair.skillMagicNumPair.skill.triggerReverseAction(pair.context)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    fun cancelSkill(player: ServerPlayer, skill: String, duration: Int, runRemainingTasks: Boolean) {
        if (duration > 0) {
            val playerCancelList = CANCEL_SKILL_MAP.getOrPut(player) { mutableListOf() }
            val existsStatus = playerCancelList.firstOrNull { it.skill == skill }
            if (existsStatus == null) {
                val newCancelStatus = SkillCancelStatus(skill, duration)
                playerCancelList.add(newCancelStatus)
            } else {
                existsStatus.duration = duration
            }
        }
        TACZExpandsServer.SCHEDULED_TASKS.filter { it.player != null && it.player == player && it.skill?.config?.name == skill }
            .forEach {
                it.cancel()
                if (runRemainingTasks) {
                    it.task(it)
                }
            }
    }

    fun shouldCancel(player: ServerPlayer, skill: Skill): Boolean {
        return CANCEL_SKILL_MAP[player]?.firstOrNull { it.skill == skill.config.name } != null
    }

    fun onServerTick() {

        KEEP_CHECK_LIST.indices.reversed().forEach {
            try {
                val pair = KEEP_CHECK_LIST[it]

                val context = pair.context
                val skill = pair.skillMagicNumPair.skill
                val magicNum = pair.skillMagicNumPair.magicNum

                if (pair.time == -1) {
                    pair.time = skill.config.flags.keepCheckTimes
                    return@forEach
                }
                if (skill.checkCondition(context, magicNum)) {
                    pair.time--
                    if (pair.time <= 0) {
                        triggerActionDirectly(context, skill, magicNum)
                        KEEP_CHECK_LIST.removeAt(it)
                    }
                } else {
                    triggerActionFailed(context, skill, magicNum)
                    KEEP_CHECK_LIST.removeAt(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        KEEP_TRIGGER_MAP.forEach { (player, list) ->
            list.forEach { pair ->
                val context = pair.context
                val skill = pair.skillMagicNumPair.skill
                val magicNum = pair.skillMagicNumPair.magicNum

                pair.time--
                if (pair.time <= 0) {
                    try {
                        if (skill.checkCondition(context, magicNum)) {
                            triggerActionDirectly(context, skill, magicNum)
                        } else {
                            triggerActionFailed(context, skill, magicNum)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    pair.time = skill.config.flags.frequency
                }
            }
        }

        CANCEL_SKILL_MAP.forEach { (player, list) ->
            list.indices.reversed().forEach {
                if (list[it].duration-- <= 0)
                    list.removeAt(it)
            }
        }
    }
}