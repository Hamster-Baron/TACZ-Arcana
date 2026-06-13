package group.taczexpands.server.skill

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.init.ModItems
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.config.SkillConfig
import group.taczexpands.server.context.Context
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.util.LOGGER
import group.taczexpands.server.util.checkContains
import group.taczexpands.server.util.schedule
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.random.Random

val GLOBAL_RANDOM = Random(System.currentTimeMillis())

val GLOBAL_COOLDOWN_GROUP_MAP = mutableMapOf<Pair<Player, String>, Int>()

class Skill(val config: SkillConfig) {
    val coolDownMap = mutableMapOf<Player, Int>()
    val trigger = TriggerType.valueOf(config.trigger)

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
            if (Component.Serializer.fromJson(loreList.getString(i))!!.string.checkContains(config.lore, config.flags.useRegex)) {
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

        return config.guns.checkContains(gunItem.getGunId(item).toString(), config.flags.useRegex)
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
            .firstOrNull { config.attachments.checkContains(it.toString(), config.flags.useRegex) } != null

    }

    fun checkMatchConfig(item: ItemStack): Boolean {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (item.item != gunItem) return false
        if (PackSkillsManager.getSkills(gunItem.getGunId(item)).checkContains(config.name, config.flags.useRegex)) return true
        return false
    }

    fun checkCondition(context: Context, magicNum: Int): Boolean {
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
                (context.bullet as? IAccessorBullet)?.`taczexpands$getGunItem`() ?: context.self.mainHandItem
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
            try {
                action.perform(this, context)
            } catch (e: Exception) {
                LOGGER.error("Error executing action. Skipping. ", e)
            }
        }

        if (config.flags.repeatTimes > 0) {
            if (config.flags.repeatDelay <= 0) {
                IntRange(1, config.flags.repeatTimes).forEach {
                    for (action in config.actions) {
                        try {
                            action.perform(this, context)
                        } catch (e: Exception) {
                            LOGGER.error("[BATCH] Error executing action. Skipping. ", e)
                        }
                    }
                }
            } else {
                var runTimes = 0
                schedule(this, null, config.flags.repeatDelay) {
                    for (action in config.actions) {
                        try {
                            action.perform(this, context)
                        } catch (e: Exception) {
                            LOGGER.error("[SCHEDULE] Error executing action. Skipping. ", e)
                        }
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
        if (config.hasReverseAction) {
            for (action in config.reverseActions!!) {
                try {
                    action.perform(this, context)
                } catch (e: Exception) {
                    LOGGER.error("Error executing reverse action. Skipping. ", e)
                }
            }
        }
    }
}
