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
import java.security.MessageDigest
import java.util.Base64

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

private data class SkillDefinitionSource(val group: SkillDefinitionSourceGroup, val description: String)

private data class SkillReloadCandidate(
    val skills: MutableMap<TriggerType, MutableList<Skill>>,
    val conditions: MutableMap<ScopedDefinitionName, ConditionConfig>,
    val actions: MutableMap<ScopedDefinitionName, ActionConfig>,
    val skillSources: MutableList<SkillDefinitionSource>,
    val conditionSources: MutableMap<ScopedDefinitionName, SkillDefinitionSource>,
    val actionSources: MutableMap<ScopedDefinitionName, SkillDefinitionSource>,
    val definitionDigests: MutableMap<String, SkillDefinitionSource>,
    val errors: MutableList<String>
) {
    fun copyCandidate() = SkillReloadCandidate(
        skills.mapValuesTo(linkedMapOf()) { it.value.toMutableList() },
        conditions.toMutableMap(), actions.toMutableMap(), skillSources.toMutableList(),
        conditionSources.toMutableMap(), actionSources.toMutableMap(), definitionDigests.toMutableMap(), errors.toMutableList()
    )
}

object SkillManager {
    val LOADED_SKILLS = mutableMapOf<TriggerType, MutableList<Skill>>().apply {
        TriggerType.entries.forEach { this[it] = mutableListOf() }
    }

    val LOADED_CHAIN_CONDITIONS = mutableMapOf<ScopedDefinitionName, ConditionConfig>()
    val LOADED_CHAIN_ACTIONS = mutableMapOf<ScopedDefinitionName, ActionConfig>()

    val KEEP_CHECK_LIST = mutableListOf<ContextSkillPair>()

    val KEEP_TRIGGER_MAP = mutableMapOf<ServerPlayer, MutableList<ContextSkillPair>>()

    val REVERSE_CHECK_MAP = mutableMapOf<ServerPlayer, MutableList<ReverseContext>>()

    val QUIT_TRIGGER_MAP = mutableMapOf<ServerPlayer, MutableSet<ReverseContext>>()

    val CANCEL_SKILL_MAP = mutableMapOf<ServerPlayer, MutableList<SkillCancelStatus>>()

    private var reloadCandidate: SkillReloadCandidate? = null
    private var optionalSourceCheckpoint: SkillReloadCandidate? = null
    private var appendSource = SkillDefinitionSource(SkillDefinitionSourceGroup.LOCAL, "local:api")

    @JvmStatic
    fun clear() {
        LOADED_SKILLS.values.flatten().forEach { it.deactivate() }
        TACZExpandsServer.SCHEDULED_TASKS.filter { it.skill != null }.forEach { it.cancel() }
        KEEP_CHECK_LIST.clear()
        KEEP_TRIGGER_MAP.clear()
        REVERSE_CHECK_MAP.clear()
        QUIT_TRIGGER_MAP.clear()
        CANCEL_SKILL_MAP.clear()
        GLOBAL_COOLDOWN_GROUP_MAP.clear()
        LOADED_SKILLS.values.forEach { it.clear() }
        LOADED_CHAIN_CONDITIONS.clear()
        LOADED_CHAIN_ACTIONS.clear()
        ActionManager.resetAll()
    }

    fun beginReload() {
        discardReload()
        reloadCandidate = createReloadCandidate()
    }

    fun beginOptionalSource(group: SkillDefinitionSourceGroup, description: String) {
        val candidate = checkNotNull(reloadCandidate) { "No skill reload candidate is active" }
        check(optionalSourceCheckpoint == null) { "An optional skill source is already active" }
        optionalSourceCheckpoint = candidate.copyCandidate()
        appendSource = SkillDefinitionSource(group, description)
    }

    fun commitOptionalSource() {
        check(optionalSourceCheckpoint != null) { "No optional skill source is active" }
        optionalSourceCheckpoint = null
        appendSource = SkillDefinitionSource(SkillDefinitionSourceGroup.LOCAL, "local:api")
    }

    fun rollbackOptionalSource() {
        val checkpoint = checkNotNull(optionalSourceCheckpoint) { "No optional skill source is active" }
        reloadCandidate?.skills?.values?.flatten()?.filter { skill -> checkpoint.skills.values.none { skill in it } }
            ?.forEach { it.deactivate() }
        reloadCandidate = checkpoint
        optionalSourceCheckpoint = null
        appendSource = SkillDefinitionSource(SkillDefinitionSourceGroup.LOCAL, "local:api")
    }

    fun <T> withAppendSource(group: SkillDefinitionSourceGroup, description: String, block: () -> T): T {
        val previous = appendSource
        appendSource = SkillDefinitionSource(group, description)
        return try {
            block()
        } finally {
            appendSource = previous
        }
    }

    fun appendPlainData(type: Int, data: ByteArray, group: SkillDefinitionSourceGroup, description: String) {
        val source = SkillDefinitionSource(group, description)
        try {
            appendToCandidate(checkNotNull(reloadCandidate) { "No skill reload candidate is active" }, type, data, source)
        } catch (e: Exception) {
            reportDefinitionError(source.description, e)
        }
    }

    fun hasReloadErrors(): Boolean = reloadCandidate?.errors?.isNotEmpty() == true

    fun getReloadErrors(): List<String> = reloadCandidate?.errors?.toList().orEmpty()

    fun publishReload(): SkillReloadCounts {
        val candidate = checkNotNull(reloadCandidate) { "No skill reload candidate is active" }
        check(candidate.errors.isEmpty()) { "Cannot publish a skill reload candidate with errors" }
        check(optionalSourceCheckpoint == null) { "Cannot publish while an optional skill source is active" }
        val serverSkills = candidate.skillSources.count {
            it.group == SkillDefinitionSourceGroup.LOCAL || it.group == SkillDefinitionSourceGroup.SERVER_NATIVE
        }
        val packSkills = candidate.skillSources.size - serverSkills
        clear()
        candidate.skills.forEach { (trigger, skills) -> LOADED_SKILLS[trigger]!!.addAll(skills) }
        LOADED_CHAIN_CONDITIONS.putAll(candidate.conditions)
        LOADED_CHAIN_ACTIONS.putAll(candidate.actions)
        reloadCandidate = null
        appendSource = SkillDefinitionSource(SkillDefinitionSourceGroup.LOCAL, "local:api")
        return SkillReloadCounts(serverSkills, packSkills)
    }

    fun discardReload() {
        reloadCandidate?.skills?.values?.flatten()?.forEach { it.deactivate() }
        reloadCandidate = null
        optionalSourceCheckpoint = null
        appendSource = SkillDefinitionSource(SkillDefinitionSourceGroup.LOCAL, "local:api")
    }


    @JvmStatic
    fun appendData(type: Int, data: ByteArray) {
        try {
            val candidate = reloadCandidate
            if (candidate == null) {
                appendToLoadedData(type, data)
            } else {
                appendToCandidate(candidate, type, data, appendSource)
            }
        } catch (e: Exception) {
            reportDefinitionError(appendSource.description, e)
        }
    }

    fun reportReloadError(source: String, exception: Exception) {
        reportLoadError(reloadCandidate, source, exception, true)
    }

    fun reportDefinitionError(source: String, exception: Exception) {
        reportLoadError(reloadCandidate, source, exception, false)
    }

    private fun reportLoadError(candidate: SkillReloadCandidate?, source: String, exception: Exception, fatal: Boolean) {
        var detail = exception.message
        if (detail.isNullOrBlank()) {
            detail = exception.javaClass.simpleName
        }
        val message = "Failed to load $source: $detail"
        if (candidate != null && fatal) {
            candidate.errors.add(message)
        }
        val server = TACZExpandsServer.INSTANCE
        server.reloadErrorCount++
        val listener = server.reloadMessageListener
        if (listener != null) {
            listener.sendModMessage(Component.literal(message))
        }
        TACZExpandsServer.LOGGER.error(message, exception)
    }

    fun reportCurrentSourceError(entry: String, exception: Exception) {
        reportDefinitionError("${appendSource.description}:$entry", exception)
    }

    private fun createReloadCandidate(): SkillReloadCandidate {
        return SkillReloadCandidate(
            TriggerType.entries.associateWith { mutableListOf<Skill>() }.toMutableMap(),
            mutableMapOf(), mutableMapOf(), mutableListOf(), mutableMapOf(), mutableMapOf(),
            mutableMapOf(), mutableListOf()
        )
    }

    private fun appendToCandidate(candidate: SkillReloadCandidate, type: Int, data: ByteArray, source: SkillDefinitionSource) {
        val digest = "$type:${Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data))}"
        val previousSource = candidate.definitionDigests[digest]
        if (previousSource != null && isNativeSource(previousSource.group) != isNativeSource(source.group)) return
        val scope = definitionScope(source.description)
        DefinitionScope.with(scope) {
            when (type) {
                0 -> {
                    val config = YAML.decodeFromString<SkillConfig>(String(data, Charsets.UTF_8))
                    val skill = Skill(config)
                    candidate.skillSources.add(source)
                    candidate.skills[skill.trigger]!!.add(skill)
                }
                1 -> {
                    val config = YAML.decodeFromString<ConditionConfig>(String(data, Charsets.UTF_8))
                    val key = ScopedDefinitionName(scope, config.name)
                    val previousSource = candidate.conditionSources[key]
                    check(previousSource == null) { "Duplicate condition name '${config.name}' in scope '$scope' from ${source.description}; already defined by ${previousSource?.description}" }
                    candidate.conditionSources[key] = source
                    candidate.conditions[key] = config
                }
                2 -> {
                    val config = YAML.decodeFromString<ActionConfig>(String(data, Charsets.UTF_8))
                    val key = ScopedDefinitionName(scope, config.name)
                    val previousSource = candidate.actionSources[key]
                    check(previousSource == null) { "Duplicate action name '${config.name}' in scope '$scope' from ${source.description}; already defined by ${previousSource?.description}" }
                    candidate.actionSources[key] = source
                    candidate.actions[key] = config
                }
                else -> throw IllegalArgumentException("Unknown skill definition type $type")
            }
        }
        candidate.definitionDigests[digest] = source
    }

    private fun appendToLoadedData(type: Int, data: ByteArray) {
        val scope = definitionScope(appendSource.description)
        DefinitionScope.with(scope) {
            when (type) {
                0 -> registerSkill(Skill(YAML.decodeFromString<SkillConfig>(String(data, Charsets.UTF_8))))
                1 -> registerCondition(YAML.decodeFromString<ConditionConfig>(String(data, Charsets.UTF_8)))
                2 -> registerAction(YAML.decodeFromString<ActionConfig>(String(data, Charsets.UTF_8)))
                else -> throw IllegalArgumentException("Unknown skill definition type $type")
            }
        }
    }

    private fun definitionScope(source: String): String {
        if (source.startsWith("resource:")) return source.removePrefix("resource:").substringBefore(':')
        if (source.startsWith("local:")) return "local"
        if (source.startsWith("native:")) return source.removePrefix("native:")
        return source
    }

    private fun isNativeSource(group: SkillDefinitionSourceGroup): Boolean =
        group == SkillDefinitionSourceGroup.SERVER_NATIVE || group == SkillDefinitionSourceGroup.HYBRID_NATIVE

    private fun resolveDefinitionName(reference: String, scope: String?): Pair<String?, String> {
        val separator = reference.indexOf(':')
        return if (separator > 0) {
            reference.substring(0, separator) to reference.substring(separator + 1)
        } else {
            scope to reference
        }
    }

    fun resolveChainAction(reference: String, scope: String?): ActionConfig? {
        val (requestedScope, name) = resolveDefinitionName(reference, scope)
        if (requestedScope != null) {
            LOADED_CHAIN_ACTIONS[ScopedDefinitionName(requestedScope, name)]?.let { return it }
        }
        val matches = LOADED_CHAIN_ACTIONS.filterKeys { it.name == name }.values
        return matches.singleOrNull()
    }

    fun resolveChainCondition(reference: String, scope: String?): ConditionConfig? {
        val (requestedScope, name) = resolveDefinitionName(reference, scope)
        if (requestedScope != null) {
            LOADED_CHAIN_CONDITIONS[ScopedDefinitionName(requestedScope, name)]?.let { return it }
        }
        val matches = LOADED_CHAIN_CONDITIONS.filterKeys { it.name == name }.values
        return matches.singleOrNull()
    }

    fun registerSkill(skill: Skill) {
        LOADED_SKILLS[skill.trigger]!!.add(skill)
    }

    fun registerCondition(condition: ConditionConfig) {
        LOADED_CHAIN_CONDITIONS[ScopedDefinitionName(DefinitionScope.current ?: "local", condition.name)] = condition
    }

    fun registerAction(action: ActionConfig) {
        LOADED_CHAIN_ACTIONS[ScopedDefinitionName(DefinitionScope.current ?: "local", action.name)] = action
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
        LOADED_SKILLS[type]!!.toList().forEach {
            try {
                if (!it.isActive) return@forEach
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
                it.reportExecutionError(e)
            }
        }
    }

    fun triggerFrequentAction(context: Context, skill: Skill, magicNum: Int) {
        if (!skill.isActive) return
        KEEP_TRIGGER_MAP.getOrPut(context.self) { mutableListOf() }.let {
            val pair = ContextSkillPair(context, SkillMagicNumPair(skill, magicNum), 0)
            if (it.firstOrNull { it.skillMagicNumPair == pair.skillMagicNumPair } == null) {
                it.add(pair)
            }
        }
    }

    fun triggerActionDirectly(context: Context, skill: Skill, magicNum: Int) {
        val triggered = skill.triggerAction(context)
        if (!skill.isActive) return
        if (triggered) {
            addReverse(context, skill, magicNum)
        } else {
            triggerActionFailed(context, skill, magicNum)
        }
    }

    fun triggerActionFailed(context: Context, skill: Skill, magicNum: Int) {
        if (!skill.isActive) return
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
                val pair = keepTriggerList[it]
                if (pair.skillMagicNumPair.skill.trigger == type && pair.skillMagicNumPair.magicNum == magicNum) {
                    keepTriggerList.removeAt(it)
                }
            }
        }

        if (REVERSE_CHECK_MAP.containsKey(context.self)) {
            val reverseList = REVERSE_CHECK_MAP[context.self]!!
            reverseList.indices.reversed().forEach {
                val pair = reverseList[it]
                if (pair.skillMagicNumPair.skill.trigger == type && pair.skillMagicNumPair.magicNum == magicNum) {
                    val pair = reverseList.removeAt(it)
                    if (!pair.failed)
                        pair.skillMagicNumPair.skill.triggerReverseAction(pair.context)
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
        TACZExpandsServer.SCHEDULED_TASKS.filter { !it.cancelled && it.player != null && it.player == player && it.skill?.config?.name == skill }
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
            if (it !in KEEP_CHECK_LIST.indices) return@forEach
            val pair = KEEP_CHECK_LIST[it]
            val skill = pair.skillMagicNumPair.skill
            try {
                val context = pair.context
                val magicNum = pair.skillMagicNumPair.magicNum

                if (pair.time == -1) {
                    pair.time = skill.config.flags.keepCheckTimes
                    return@forEach
                }
                if (skill.checkCondition(context, magicNum)) {
                    pair.time--
                    if (pair.time <= 0) {
                        KEEP_CHECK_LIST.removeAt(it)
                        triggerActionDirectly(context, skill, magicNum)
                    }
                } else {
                    triggerActionFailed(context, skill, magicNum)
                    KEEP_CHECK_LIST.removeAt(it)
                }
            } catch (e: Exception) {
                skill.reportExecutionError(e)
            }
        }

        KEEP_TRIGGER_MAP.toMap().forEach { (player, list) ->
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
                        skill.reportExecutionError(e)
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
