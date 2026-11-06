package group.taczexpands.server.skill

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.init.ModItems
import group.taczexpands.common.network.v2.s2c.S2CActionLockState
import group.taczexpands.server.accessor.IAccessorServerBullet
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.SkillConfig
import group.taczexpands.server.config.action.Branch
import group.taczexpands.server.config.action.CancelAction
import group.taczexpands.server.config.action.ChainAction
import group.taczexpands.server.config.action.CustomDisplay
import group.taczexpands.server.config.action.RunCommand
import group.taczexpands.server.config.action.Timer
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.performAction
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.util.LOGGER
import group.taczexpands.server.util.schedule
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import kotlin.random.Random

val GLOBAL_RANDOM = Random(System.currentTimeMillis())

val GLOBAL_COOLDOWN_GROUP_MAP = mutableMapOf<Pair<Player, String>, Int>()

class Skill(val config: SkillConfig) {
    val coolDownMap = mutableMapOf<Player, Int>()
    var isActive = true
        private set
    private val anonymousActionLockSignatureIdentifiers = mutableMapOf<AnonymousActionLockSignature, String>()
    private val anonymousActionLockSourceIdentifiers = IdentityHashMap<CancelAction, String>()
    val trigger = TriggerType.valueOf(config.trigger)
    private val gunPatterns = compilePatterns("Guns", config.guns)
    private val attachmentPatterns = compilePatterns("Attachments", config.attachments)
    private val lorePattern = compilePattern("Lore", config.lore)
    private val namePattern = compilePattern("Name", config.name)

    init {
        registerAnonymousActionLockSourceIdentifiers(config.actions)
        registerAnonymousActionLockSourceIdentifiers(config.reverseActions ?: emptyList())
    }

    fun resolveAnonymousActionLockSourceIdentifier(action: CancelAction): String {
        return anonymousActionLockSourceIdentifiers.computeIfAbsent(action) { UUID.randomUUID().toString() }
    }

    fun deactivate() {
        isActive = false
        coolDownMap.clear()
    }

    fun reportExecutionError(exception: Exception) {
        LOGGER.error("Failed to execute skill '{}' (trigger {})", config.name, trigger, exception)
    }

    private fun registerAnonymousActionLockSourceIdentifiers(actions: List<Action>) {
        val occurrences = mutableMapOf<AnonymousActionLockSignature, Int>()
        collectCancelActions(actions).filter { it.identifier == null }.forEach { action ->
            val signature = AnonymousActionLockSignature(action.action, action.status, action.selector ?: SelectorData.SELF)
            val occurrence = occurrences.getOrDefault(signature, 0)
            occurrences[signature] = occurrence + 1
            val signatureIdentifier = anonymousActionLockSignatureIdentifiers.getOrPut(signature) { UUID.randomUUID().toString() }
            anonymousActionLockSourceIdentifiers[action] = "$signatureIdentifier:$occurrence"
        }
    }

    private fun collectCancelActions(actions: List<Action>): List<CancelAction> {
        val result = mutableListOf<CancelAction>()
        val visited = Collections.newSetFromMap(IdentityHashMap<Action, Boolean>())
        fun collect(action: Action) {
            if (!visited.add(action)) return
            when (action) {
                is CancelAction -> result.add(action)
                is ChainAction -> action.actions.orEmpty().forEach(::collect)
                is Branch -> {
                    action.succeeded?.let(::collect)
                    action.failed?.let(::collect)
                }
                is Timer -> collect(action.action)
                is RunCommand -> {
                    action.succeeded?.let(::collect)
                    action.failed?.let(::collect)
                }
                is CustomDisplay -> action.onDeath?.let(::collect)
                else -> Unit
            }
        }
        actions.forEach(::collect)
        return result
    }

    private data class AnonymousActionLockSignature(
        val action: S2CActionLockState.Action,
        val status: TriggerType?,
        val selector: SelectorData
    )

    private fun compilePatterns(field: String, values: Collection<String>?): List<Regex> {
        if (!config.flags.useRegex || values == null) return emptyList()
        return values.map { compilePattern(field, it)!! }
    }

    private fun compilePattern(field: String, value: String): Regex? {
        if (!config.flags.useRegex) return null
        try {
            return Regex(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid regex in skill '${config.name}' field '$field': $value", e)
        }
    }

    fun checkMatchLore(item: ItemStack): Boolean {
        if (!item.hasTag()) return false
        val tag = item.tag!!
        if (!tag.contains("display", 10)) return false
        val displayTag = tag.getCompound("display")
        if (!displayTag.contains("Lore", 9)) return false
        val loreList = displayTag.getList("Lore", 8)
        if (loreList.size <= 0) return false

        var matchLore = false
        for (i in 0..<loreList.size) {
            val lore = Component.Serializer.fromJson(loreList.getString(i))!!.string
            if (lorePattern?.matches(lore) ?: lore.contains(config.lore)) {
                matchLore = true
                break
            }
        }
        if (!matchLore) return false
        return true
    }

    fun checkMatchGun(item: ItemStack): Boolean {
        if (config.guns.isNullOrEmpty()) return false
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (item.item != gunItem) return false

        val gunId = gunItem.getGunId(item).toString()
        return if (config.flags.useRegex) gunPatterns.any { it.matches(gunId) } else config.guns.contains(gunId)
    }

    fun checkMatchAttachment(item: ItemStack): Boolean {
        if (config.attachments.isNullOrEmpty()) return false
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (item.item != gunItem) return false


        return AttachmentType.entries
            .filter { it != AttachmentType.NONE }
            .map { listOf(gunItem.getAttachmentId(item, it), gunItem.getBuiltInAttachmentId(item, it)) }
            .flatten()
            .filter { it != DefaultAssets.EMPTY_ATTACHMENT_ID }
            .firstOrNull { attachmentId ->
                if (config.flags.useRegex) attachmentPatterns.any { it.matches(attachmentId.toString()) }
                else config.attachments.contains(attachmentId.toString())
            } != null

    }

    fun checkMatchConfig(item: ItemStack): Boolean {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (item.item != gunItem) return false
        val skills = PackSkillsManager.getSkills(gunItem.getGunId(item))
        return if (config.flags.useRegex) skills.any { namePattern!!.matches(it) } else skills.contains(config.name)
    }

    fun checkCondition(context: Context, magicNum: Int): Boolean {
        if (!isActive) return false
        if (!config.flags.cooldownGroup.isNullOrEmpty()) {
            val currentTime = ServerLifecycleHooks.getCurrentServer().tickCount
            val untilTime = GLOBAL_COOLDOWN_GROUP_MAP.getOrDefault(context.self to config.flags.cooldownGroup, 0)
            if (currentTime <= untilTime) return false
        } else if (config.flags.coolDown > 0) {
            val currentTime = ServerLifecycleHooks.getCurrentServer().tickCount
            val lastTime = coolDownMap.getOrDefault(context.self, 0)

            if (currentTime - lastTime <= config.flags.coolDown) {
                return false
            }
        }

        if (trigger == TriggerType.ON_PLAYER_TICK && config.flags.frequency != 0 && context.self.tickCount % config.flags.frequency != 0) {
            return false
        }


        val item = if (trigger == TriggerType.ON_OFF_HAND) {
            context.self.offhandItem
        } else if (trigger == TriggerType.ON_HOT_BAR || trigger == TriggerType.ON_SIGNAL) {
            context.self.inventory.getItem(magicNum)
        } else {
            if (config.flags.strictSourceMode) {
                (context.bullet as? IAccessorServerBullet)?.`taczexpands$getGunItem`() ?: context.self.mainHandItem
            } else {
                context.self.mainHandItem
            }
        }

        if (!checkMatchConfig(item) && !checkMatchGun(item) && !checkMatchAttachment(item) && !checkMatchLore(item)) {
            return false
        }

        if (config.flags.conditionExpression == "ALL_TRUE") {
            for (condition in config.conditions) {
                if (!condition.check(context)) return false
            }
            return true
        } else if (config.flags.conditionExpression == "ALL_FALSE") {
            for (condition in config.conditions) {
                if (condition.check(context)) return false
            }
            return true
        } else {

            val expression = ExpressionHelper.initExpression(config.flags.conditionExpression, context, null)
            config.conditions.map { it.check(context) }.forEachIndexed { index, it ->
                expression.with("C${index + 1}", it)
            }
            return expression.evaluate().booleanValue
        }
    }

    fun triggerAction(context: Context): Boolean {
        if (!isActive) return false
        if (config.flags.coolDown > 0) {
            if (config.flags.cooldownGroup.isNullOrEmpty()) {
                coolDownMap[context.self] = ServerLifecycleHooks.getCurrentServer().tickCount
            } else {
                GLOBAL_COOLDOWN_GROUP_MAP[context.self to config.flags.cooldownGroup] =
                    ServerLifecycleHooks.getCurrentServer().tickCount + config.flags.coolDown
            }
        }
        if (config.flags.triggerProbability <= 0) return false
        if (config.flags.triggerProbability < 100) {
            val rand = GLOBAL_RANDOM.nextInt(1, 100)
            if (rand >= config.flags.triggerProbability) return false
        }


        for (action in config.actions) {
            action.performAction(this, context)
        }

        if (config.flags.repeatTimes > 0) {
            if (config.flags.repeatDelay <= 0) {
                IntRange(1, config.flags.repeatTimes).forEach {
                    for (action in config.actions) {
                        action.performAction(this, context)
                    }
                }
            } else {
                var runTimes = 0
                schedule(this, null, config.flags.repeatDelay) {
                    for (action in config.actions) {
                        action.performAction(this, context)
                    }
                    runTimes++
                    if (runTimes < config.flags.repeatTimes) {
                        it.time = config.flags.repeatDelay
                    }
                }
            }
        }
        return true
    }

    fun triggerReverseAction(context: Context) {
        if (!isActive) return
        if (config.hasReverseAction) {
            for (action in config.reverseActions!!) {
                action.performAction(this, context)
            }
        }
    }
}
