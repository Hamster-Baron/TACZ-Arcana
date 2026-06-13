package group.taczexpands.server.bukkit.mythicmobs

import group.taczexpands.server.bukkit.toBukkit
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.skills.SkillMetadataImpl
import io.lumine.mythic.core.skills.SkillTriggers
import net.minecraft.world.entity.Entity
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

object MythicMobsForge {
    private val plugin: Plugin = Bukkit.getPluginManager().getPlugin("MythicMobs") ?: throw NullPointerException("MythicMobs plugin not found.")
    private val classLoader: ClassLoader = plugin::class.java.classLoader

    private val instMethod: Method
    private val getSkillManagerMethod: Method
    private val getSkillMethod: Method
    private val getCasterMethod: Method
    private val adaptMethod: Method
    private val executeMethod: Method

    private val skillMetadataImplConstructor: Constructor<*>
    private val apiTriggerField: Field
    private val parametersField: Field

    init {
        val mythicBukkitCls = Class.forName("io.lumine.mythic.bukkit.MythicBukkit", true, classLoader)
        val bukkitAdapterCls = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter", true, classLoader)
        val skillManagerCls = Class.forName("io.lumine.mythic.api.skills.SkillManager", true, classLoader)
        val skillCls = Class.forName("io.lumine.mythic.api.skills.Skill", true, classLoader)
        val abstractEntityCls = Class.forName("io.lumine.mythic.api.adapters.AbstractEntity", true, classLoader)
        val skillMetadataImplCls = Class.forName("io.lumine.mythic.core.skills.SkillMetadataImpl", true, classLoader)
        val skillTriggersCls = Class.forName("io.lumine.mythic.core.skills.SkillTriggers", true, classLoader)
        val skillTriggerCls = Class.forName("io.lumine.mythic.api.skills.SkillTrigger", true, classLoader)
        val skillCasterCls = Class.forName("io.lumine.mythic.api.skills.SkillCaster", true, classLoader)
        val skillMetadataCls = Class.forName("io.lumine.mythic.api.skills.SkillMetadata", true, classLoader)

        instMethod = mythicBukkitCls.getMethod("inst")
        getSkillManagerMethod = mythicBukkitCls.getMethod("getSkillManager")
        getSkillMethod = skillManagerCls.getMethod("getSkill", String::class.java)
        getCasterMethod = skillManagerCls.getMethod("getCaster", abstractEntityCls)
        adaptMethod = bukkitAdapterCls.getMethod("adapt", org.bukkit.entity.Entity::class.java)
        executeMethod = skillCls.getMethod("execute", skillMetadataCls)

        apiTriggerField = skillTriggersCls.getField("API")
        skillMetadataImplConstructor = skillMetadataImplCls.getConstructor(
            skillTriggerCls, skillCasterCls, abstractEntityCls
        )
        parametersField = skillMetadataImplCls.getDeclaredField("parameters")
        parametersField.isAccessible = true
    }

    fun callSkill(skillName: String, caster: Entity, trigger: Entity, parameters: MutableMap<String, String>? = null) {
        try {
            val mythicInst = instMethod.invoke(null)
            val skillManager = getSkillManagerMethod.invoke(mythicInst)

            val skillOpt = getSkillMethod.invoke(skillManager, skillName) as Optional<*>
            val skill = skillOpt.orElse(null) ?: return

            val bukkitCaster = caster.toBukkit()
            val bukkitTrigger = trigger.toBukkit()
            val abstractCasterEntity = adaptMethod.invoke(null, bukkitCaster)
            val abstractTriggerEntity = adaptMethod.invoke(null, bukkitTrigger)

            val mCaster = getCasterMethod.invoke(skillManager, abstractCasterEntity)

            val triggerType = apiTriggerField.get(null)
            val data = skillMetadataImplConstructor.newInstance(triggerType, mCaster, abstractTriggerEntity)

            if (parameters != null) {
                parametersField.set(data, parameters)
            }

            executeMethod.invoke(skill, data)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun callSkill1(skill: String, caster: Entity, trigger: Entity, parameters: MutableMap<String, String>? = null) {
        val skillManager = MythicBukkit.inst().skillManager
        val skill = skillManager.getSkill(skill).getOrNull() ?: return
        val data = SkillMetadataImpl(SkillTriggers.API, skillManager.getCaster(BukkitAdapter.adapt(caster.toBukkit())), BukkitAdapter.adapt(trigger.toBukkit()))
        if (parameters != null) {
            data.parameters = parameters
        }
        skill.execute(data)
    }

}