package group.taczexpands.server.config.action

import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType
import group.taczexpands.common.data.FlightProfileType
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.compat.CompatHelper
import group.taczexpands.server.compat.superbwarfare.SuperbWarfareCompat
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitByTargetContext
import group.taczexpands.server.context.HitEntityContext
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.Variables
import group.taczexpands.server.expression.create
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.mixin.accessor.IAccessorEntityKineticBullet
import group.taczexpands.server.nbt.DataStorage
import group.taczexpands.server.nbt.DataType
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.util.LOGGER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.server.ServerLifecycleHooks
import java.math.BigDecimal
import java.util.*

@Serializable
@SerialName("Modify")
data class Modify(
    @SerialName("params") val parameters: List<Parameter>,
    override val delay: Int? = null
) : Action {
    companion object {
        val EXAMPLE = Modify(listOf(Parameter("HP", expressionStr = ExpressionData("20.0"))))
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(parameters.map { it.prepare(context) })
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        parameters.forEachIndexed { index, it ->
            try {
                it.execute(context, data.dataList[index])
            } catch (e: Exception) {
                LOGGER.error("Modify error ${it.name}. Skipping. ", e)
            }
        }

    }
}

val ATTRIBUTE_MODIFIER_UUID = mapOf<Attribute, UUID>(
    Attributes.MOVEMENT_SPEED to UUID.fromString("0aa62249-9247-4d11-968a-6aa04c97fd8b"),
)
val PARAMETERS = arrayOf(

    "Damage",
    "HP",
    "MaxHP",
    "Hunger",
    "AmmoCount",

    "RPMModifier",

    "AllRecoilModifier",
    "StandRecoilModifier",
    "MoveRecoilModifier",
    "SneakRecoilModifier",
    "LieRecoilModifier",
    "AimRecoilModifier",

    "AllSpreadModifier",
    "StandSpreadModifier",
    "MoveSpreadModifier",
    "SneakSpreadModifier",
    "LieSpreadModifier",
    "AimSpreadModifier",

    "ReloadTimeModifier",
    "AimTimeModifier",
    "DrawTimeModifier",
    "BoltTimeModifier",
    "BulletLife",
    "BulletSpeed",
    "BulletDeltaSpeed",
    "BulletGravity",
    "BulletFriction",
    "MovementSpeedModifier",

    "BulletExplosion",
    "BulletExplosionDamage",
    "BulletExplosionRadius",
    "BulletExplosionDelay",

    "BulletFlightProfileType",
    "GunFlightProfileType",
    "GunEnforcingSimpleLocking",
    "GunNightVision",
    "GunThermalImaging",
    "GunMonochrome",

    "Locking",
    "LockingTime",
    "DeltaSpeed",
    "GunBulletAmount",
    "CurrentDurabilityDamage",


    "IsBlocked",
    "BlockingFactor",
    "GunShieldBlockingPower"

)

@Serializable
data class Parameter(
    val name: String,
    val action: String = "set",
    val selector: SelectorData? = null,
    @SerialName("expression") val expressionStr: ExpressionData,
    val entities: List<String>? = null
) {
    val entityTypeList: List<EntityType<*>>? by lazy {
        entities?.map { key ->
            val location = ResourceLocation(key)
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(location)) {
                throw Exception("Unknown target entity type $location. ")
            }
            ForgeRegistries.ENTITY_TYPES.getValue(location)!!
        }?.toList()
    }

    fun prepare(context: Context): PrepareData {
        return ListPrepareData(selector.create(context).toData(), expressionStr.create(context).toData())
    }


    @Transient
    val actionType = action.let {
        if (action == "set" || action == "reset" && name == "Damage") {
        } else throw Exception("Unknown action $action, only Damage can be reset. ")
    }


    fun getNumberValue(context: Context, target: Entity?, data: PrepareData): BigDecimal {
        return data.dataList[1].expression.get(context, target).numberValue
    }

    fun execute(context: Context, data: PrepareData) {
        data.dataList[0].selector.getTargets(context).forEach { target ->
            when (name) {
                "Damage" -> {
                    if (!entityTypeList.isNullOrEmpty()) {
                        if (context is HitByTargetContext) {
                            if (!entityTypeList!!.contains(context.self.type))
                                return@forEach
                        } else
                            if (context is HitEntityContext) {
                                if (!entityTypeList!!.contains(context.target!!.type))
                                    return@forEach
                            } else return@forEach
                    }

                    if (context is HitEntityContext) {
                        if (action == "set") {
                            val newDamage = getNumberValue(context, target, data).toFloat()
                            context.event!!.baseAmount = newDamage
                        } else {
                            context.event!!.baseAmount = context.oldDamage
                        }
                    }
                }

                "HP" -> {
                    val newHP = getNumberValue(context, target, data).toFloat()
                    if (target is LivingEntity) {
                        target.health = newHP
                    } else if (CompatHelper.hasSuperbWarfare()) {
                        SuperbWarfareCompat.setHealth(target, newHP)
                    }
                }

                "MaxHP" -> {
                    val newMaxHP = getNumberValue(context, target, data).toFloat()
                    target as? LivingEntity ?: return@forEach
                    target.getAttribute(Attributes.MAX_HEALTH)?.baseValue = newMaxHP.toDouble()
                }

                "Hunger" -> {
                    val newHunger = getNumberValue(context, target, data).toInt()

                    (target as? ServerPlayer)?.foodData?.foodLevel = newHunger
                }

                "DeltaSpeed" -> {
                    val value = getNumberValue(context, target, data).toDouble()
                    target.hasImpulse = true
                    target.deltaMovement = target.deltaMovement.normalize().scale(value)
                    if (target is ServerPlayer) {
                        NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.SetVelocity, velocity = target.deltaMovement), target)
                    }
                }

                "AmmoCount" -> {
                    val newAmmoCount = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        gunItem.setCurrentAmmoCount(mainHand, newAmmoCount)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()

                }

                "RPMModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRPMModifier(mainHand, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "AllRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, null, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }


                "StandRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, InaccuracyType.STAND, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "MoveRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, InaccuracyType.MOVE, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "SneakRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, InaccuracyType.SNEAK, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "LieRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, InaccuracyType.LIE, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "AimRecoilModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setRecoilModifier(mainHand, InaccuracyType.AIM, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "AllSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, null, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }


                "StandSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, InaccuracyType.STAND, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "MoveSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, InaccuracyType.MOVE, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "SneakSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, InaccuracyType.SNEAK, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "LieSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, InaccuracyType.LIE, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "AimSpreadModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setSpreadModifier(mainHand, InaccuracyType.AIM, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "ReloadTimeModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setReloadTimeModifier(mainHand, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "AimTimeModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setAimTimeModifier(mainHand, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "DrawTimeModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setDrawTimeModifier(mainHand, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "BoltTimeModifier" -> {
                    val newModifier = getNumberValue(context, target, data).toFloat()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setBoltTimeModifier(mainHand, newModifier)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunNightVision" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setOverrideNightVision(mainHand, if (newValue == 0) false else if (newValue > 0) true else null)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunThermalImaging" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setOverrideThermalImaging(mainHand, if (newValue == 0) false else if (newValue > 0) true else null)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunMonochrome" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setOverrideMonochrome(mainHand, if (newValue == 0) false else if (newValue > 0) true else null)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunFlightProfileType" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setMissileFlightProfileType(mainHand, newValue)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunEnforcingSimpleLocking" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem) {
                        GunExtras.setEnforcingSimpleLocking(mainHand, newValue != 0)
                    }
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunBulletAmount" -> {
                    var newValue: Int? = getNumberValue(context, target, data).toInt()
                    if (newValue != null && newValue < 0) newValue = null

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem) {
                        GunExtras.setOverrideBulletAmount(mainHand, newValue)
                    }
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "GunShieldBlockingPower" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()

                    val finalValue = if (newValue < 0) null else newValue
                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem) {
                        GunExtras.setOverrideGunShieldBlockingPower(mainHand, finalValue)
                    }
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "BulletLife" -> {
                    val newValue = getNumberValue(context, target, data).toInt()
                    context.bullet?.tickCount = newValue

                }

                "BulletSpeed" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.speed = newValue
                }

                "BulletDeltaSpeed" -> {
                    context.bullet ?: return@forEach
                    val newValue = getNumberValue(context, target, data).toDouble()
                    context.bullet!!.deltaMovement = context.bullet!!.deltaMovement.normalize().scale(newValue)

                }

                "BulletGravity" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.gravity = newValue
                }

                "BulletFriction" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.friction = newValue
                }

                "BulletExplosion" -> {
                    val newValue = getNumberValue(context, target, data).toInt()
                    (context.bullet as? IAccessorEntityKineticBullet)?.explosion = (newValue != 0)
                }

                "BulletExplosionDamage" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.explosionDamage = newValue
                }

                "BulletExplosionRadius" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.explosionRadius = newValue
                }

                "BulletExplosionDelay" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    (context.bullet as? IAccessorEntityKineticBullet)?.explosionDelayCount = (newValue * 20).toInt()
                }

                "BulletFlightProfileType" -> {
                    val newValue = getNumberValue(context, target, data).toInt()
                    val newProfile = FlightProfileType.entries.getOrNull(newValue) ?: return@forEach
                    val extraData = (context.bullet as? IAccessorBullet)?.`taczexpands$getBulletExtraData`() ?: return@forEach
                    extraData.flightProfileType = newProfile
                }

                "MovementSpeed" -> {
                    target as? LivingEntity ?: return@forEach
                    val attribute = target.getAttribute(Attributes.MOVEMENT_SPEED) ?: return@forEach
                    val newModifier = getNumberValue(context, target, data).toDouble()
                    val modifierUUID = ATTRIBUTE_MODIFIER_UUID.get(Attributes.MOVEMENT_SPEED)!!
                    attribute.removeModifier(modifierUUID)
                    attribute.addTransientModifier(
                        AttributeModifier(
                            modifierUUID,
                            "TACZ Expands Modifier",
                            newModifier,
                            AttributeModifier.Operation.MULTIPLY_BASE
                        )
                    )

                }

                "Locking" -> {
                    val player = target as? ServerPlayer ?: return@forEach
                    val newValue = getNumberValue(context, target, data).toInt()
                    val newEntity = player.level().getEntity(newValue)
                    val state = PlayerListener.getPlayerStates(player)
                    state.lockingTarget = newEntity
                }

                "LockingTime" -> {
                    val player = target as? ServerPlayer ?: return@forEach
                    val newValue = getNumberValue(context, target, data).toInt()
                    val state = PlayerListener.getPlayerStates(player)
                    state.lockingTime = newValue
                    state.sendCurrentLockingTime()
                }

                "CurrentDurabilityDamage" -> {
                    val newValue = getNumberValue(context, target, data).toInt()

                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as? LivingEntity ?: return@forEach
                    val mainHand = target.mainHandItem
                    if (mainHand.item == gunItem)
                        GunExtras.setDurabilityDamage(mainHand, newValue)
                    (target as? ServerPlayer)?.inventoryMenu?.broadcastChanges()
                }

                "IsBlocked" -> {
                    val newValue = getNumberValue(context, target, data).toInt()
                    context.isBlocked = newValue == 1
                }

                "BlockingFactor" -> {
                    val newValue = getNumberValue(context, target, data).toFloat()
                    context.blockingFactor = newValue
                }

                else -> {
                    val variable = Variables.getBuiltinVariable<Any?>(name)
                    if (variable != null) {
                        val evaluationValue = data.dataList[1].expression.get(context, target)
                        variable.set(context, target, variable.unwrap(evaluationValue))
                    } else {
                        val variableName = name
                        if (variableName.startsWith("VarFloat_")) {
                            val objectiveName = variableName.removePrefix("VarFloat_")
                            val scoreData = DataStorage.get().getOrCreateScore(DataType.FLOAT, objectiveName)
                            val expression = data.dataList[1].expression
                            scoreData.setValue(target, expression.get(context, target).numberValue.toFloat())

                        } else if (variableName.startsWith("VarString_")) {
                            val objectiveName = variableName.removePrefix("VarString_")
                            val scoreData = DataStorage.get().getOrCreateScore(DataType.STRING, objectiveName)
                            val expression = data.dataList[1].expression
                            scoreData.setValue(target, expression.get(context, target).stringValue)
                        } else if (variableName.startsWith("Var_")) {
                            val scoreboard = ServerLifecycleHooks.getCurrentServer().scoreboard
                            val objectiveName = "tacz_${variableName.removePrefix("Var_")}"
                            if (!scoreboard.hasObjective(objectiveName)) {
                                scoreboard.addObjective(
                                    objectiveName,
                                    ObjectiveCriteria.DUMMY,
                                    Component.literal(objectiveName),
                                    ObjectiveCriteria.DUMMY.defaultRenderType
                                )
                            }
                            val objective = scoreboard.getOrCreateObjective(objectiveName)
                            val score = scoreboard.getOrCreatePlayerScore(target.scoreboardName, objective)
                            val expression = data.dataList[1].expression
                            val newScore = expression.get(context, target).numberValue.toInt()

                            score.score = newScore
                        } else {
                            LOGGER.warn("Modify unknown variable $variableName")
                        }
                    }
                }
            }
        }
    }
}