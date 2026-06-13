package group.taczexpands.server.expression

import com.ezylang.evalex.data.EvaluationValue
import com.google.gson.annotations.SerializedName
import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType
import com.tacz.guns.util.AttachmentDataUtils
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.nbt.*
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.accessor.IAccessorHitVec
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.config.ServerConfig
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitByTargetContext
import group.taczexpands.server.context.HitEntityContext
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.mixin.accessor.IAccessorEntityKineticBullet
import group.taczexpands.server.module.gun_shield.GunShieldManager
import group.taczexpands.server.nbt.PlayerExtrasServer
import group.taczexpands.server.util.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraftforge.server.ServerLifecycleHooks
import java.lang.reflect.Field
import kotlin.jvm.optionals.getOrNull


object Variables {
    private val _builtinVars = mutableMapOf<String, Variable<*>>()

    val builtinVars: Map<String, Variable<*>> = _builtinVars

    abstract class Variable<T>() {
        init {

        }

        var name: String? = null

        constructor(name: String) : this() {
            this.name = name
            _builtinVars[name.lowercase()] = this
        }


        abstract fun get(context: Context, target: Entity?): T
        open fun unwrap(value: EvaluationValue): T? {
            LOGGER.warn("Ignored unwrap ${name}. ")
            return null
        }

        open fun set(context: Context, target: Entity?, value: T?) {
            LOGGER.warn("Ignored variable ${name} set. ")
        }
    }


    class DynamicDefVariable(val token: String) : Variable<Any>() {
        override fun get(context: Context, target: Entity?): Any {
            val stacks = token.split("__")
            var obj: Any? = null
            for (current in stacks) {
                when (current) {
                    "gundata" -> {
                        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                        target as LivingEntity
                        val mainHand = target.mainHandItem
                        if (mainHand.item != gunItem) return "empty"

                        val gunData = TimelessAPI.getCommonGunIndex(gunItem.getGunId(mainHand)).getOrNull()?.gunData ?: return "empty"

                        obj = gunData
                    }

                    else -> {
                        val tryIndex = current.toIntOrNull()
                        if (tryIndex != null && tryIndex >= 0) {
                            obj = java.lang.reflect.Array.get(obj, tryIndex)
                            continue
                        }

                        if (obj == null) {
                            throw NullPointerException("null stack. current: $current")
                        }

                        var targetField: Field? = null

                        for (field in obj.javaClass.declaredFields) {
                            if (field.isAnnotationPresent(SerializedName::class.java)) {
                                if (field.getAnnotation(SerializedName::class.java).value == current) {
                                    targetField = field
                                    break
                                }
                            }
                        }
                        if (targetField == null) {
                            for (field in obj.javaClass.declaredFields) {
                                if (field.name == current) {
                                    targetField = field
                                }
                            }
                        }
                        if (targetField == null) {
                            throw NullPointerException("null target field. current: $current")
                        }
                        targetField.isAccessible = true
                        obj = targetField.get(obj)
                    }
                }
            }
            if (obj == null)
                throw NullPointerException("null obj after dynamic def variable. ")
            return obj
        }
    }

    fun getDynamicDefVariable(token: String): DynamicDefVariable {
        return DynamicDefVariable(token)
    }

    fun <T> getBuiltinVariable(name: String): Variable<T>? {
        return builtinVars[name.lowercase()] as? Variable<T>
    }

    init {
        object : Variable<Float>("Damage") {
            override fun get(context: Context, target: Entity?): Float {
                if (context is HitEntityContext)
                    return context.event!!.baseAmount
                else throw Exception("context type wrong. ")
            }
        }


        object : Variable<Array<Double>>("HitPos") {
            override fun get(context: Context, target: Entity?): Array<Double> {
                if (context.pos != null)
                    return context.pos!!.let { arrayOf(it.x, it.y, it.z) }
                else throw Exception("context type wrong. ")
            }
        }

        object : Variable<Array<Int>>("HitBlockPos") {
            override fun get(context: Context, target: Entity?): Array<Int> {
                if (context.blockPos != null)
                    return context.blockPos!!.let { arrayOf(it.x, it.y, it.z) }
                else throw Exception("context type wrong. ")
            }
        }

        object : Variable<String>("HitBlockFace") {
            override fun get(context: Context, target: Entity?): String {
                return context.blockHitResult?.direction?.getName() ?: "none"
            }
        }

        object : Variable<String>("HitBlockID") {
            override fun get(context: Context, target: Entity?): String {
                val bullet = context.bullet ?: throw Exception("context type wrong. ")
                val blockPos = context.blockPos ?: throw Exception("context type wrong. ")

                return bullet.level().getBlockState(blockPos).blockHolder.unwrapKey().getOrNull()?.toString() ?: "unknown"
            }
        }

        object : Variable<Int>("HitPart") {
            override fun get(context: Context, target: Entity?): Int {
                if (context !is HitEntityContext) throw Exception("context type wrong. ")
                if (context.event!!.isHeadShot) return 0

                val hitVec = (context.event as IAccessorHitVec).`taczexpands$getHitVec`()

                if (hitVec == null) {
                    return 1
                }
                val target =
                    if (context is HitByTargetContext) {
                        context.self
                    } else {
                        context.target
                    }

                if (hitVec.y > target!!.position().y + 0.7) return 1
                else return 2

            }
        }




        object : Variable<Int>("TicksElapsed") {
            override fun get(context: Context, target: Entity?): Int {
                return ServerLifecycleHooks.getCurrentServer().tickCount - context.serverTick
            }
        }

        object : Variable<String>("Name") {
            override fun get(context: Context, target: Entity?): String {
                return target!!.name.string
            }
        }

        object : Variable<String>("UUID") {
            override fun get(context: Context, target: Entity?): String {
                return target!!.uuid.toString()
            }
        }

        object : Variable<Int>("ID") {
            override fun get(context: Context, target: Entity?): Int {
                return target!!.id
            }
        }

        object : Variable<Float>("HP") {
            override fun get(context: Context, target: Entity?): Float {
                return target!!.getHealth()
            }
        }

        object : Variable<Float>("MaxHP") {
            override fun get(context: Context, target: Entity?): Float {
                return target!!.getMaxHealth()
            }
        }

        object : Variable<Int>("Hunger") {
            override fun get(context: Context, target: Entity?): Int {
                return (target as? ServerPlayer)!!.foodData.foodLevel
            }
        }

        object : Variable<String>("Language") {
            override fun get(context: Context, target: Entity?): String {
                return (target as? ServerPlayer)!!.language
            }
        }

        object : Variable<Array<Double>>("Pos") {
            override fun get(context: Context, target: Entity?): Array<Double> {
                val pos = target!!.position()
                return arrayOf(pos.x, pos.y, pos.z, target.yRot.toDouble(), target.xRot.toDouble())
            }
        }

        object : Variable<Float>("DeltaSpeed") {
            override fun get(context: Context, target: Entity?): Float {
                return target!!.deltaMovement.length().toFloat()
            }
        }

        object : Variable<Boolean>("Crouching") {
            override fun get(context: Context, target: Entity?): Boolean {
                return target!!.isCrouching
            }
        }

        object : Variable<Boolean>("OnGround") {
            override fun get(context: Context, target: Entity?): Boolean {
                return target!!.onGround()
            }
        }

        object : Variable<Boolean>("Moving") {
            override fun get(context: Context, target: Entity?): Boolean {
                return PlayerListener.getPlayerStates((target as? ServerPlayer)!!).moving
            }
        }

        object : Variable<Boolean>("Sprinting") {
            override fun get(context: Context, target: Entity?): Boolean {
                return target!!.isSprinting
            }
        }

        object : Variable<Int>("MaxAmmoCount") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                val gunId = gunItem.getGunId(mainHand)
                val index = TimelessAPI.getCommonGunIndex(gunId).orElse(null) ?: return -1
                return AttachmentDataUtils.getAmmoCountWithAttachment(mainHand, index.gunData)
            }
        }

        object : Variable<Int>("AmmoCount") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                return gunItem.getCurrentAmmoCount(mainHand)
            }
        }

        object : Variable<String>("AmmoID") {
            override fun get(context: Context, target: Entity?): String {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return "unknown"
                val gunId = gunItem.getGunId(mainHand)
                val index = TimelessAPI.getCommonGunIndex(gunId).orElse(null) ?: return "unknown"
                return IAccessorGunData.getCurrentAmmoId(index.gunData, mainHand).toString()
            }
        }

        object : Variable<Int>("FireMode") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                return gunItem.getFireMode(mainHand).ordinal
            }
        }

        object : Variable<Int>("CurrentDurabilityDamage") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return 0
                return GunExtras.getDurabilityDamage(mainHand)
            }
        }

        object : Variable<Int>("Durability") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return 0
                var gunExtra = IAccessorGunData.getExtraHolder(mainHand) ?: return 0
                return gunExtra.durability
            }
        }

        object : Variable<Int>("WheelScrollDelta") {
            override fun get(context: Context, target: Entity?): Int {
                return context.scrollDelta ?: 0
            }
        }

        object : Variable<Boolean>("Aiming") {
            override fun get(context: Context, target: Entity?): Boolean {
                target as LivingEntity
                return IGunOperator.fromLivingEntity(target).synIsAiming
            }
        }

        object : Variable<Float>("AimingProgress") {
            override fun get(context: Context, target: Entity?): Float {
                target as LivingEntity
                return IGunOperator.fromLivingEntity(target).synAimingProgress
            }
        }

        object : Variable<Float>("ChargeProgress") {
            override fun get(context: Context, target: Entity?): Float {
                target as? ServerPlayer ?: return 0.0f
                return PlayerListener.getPlayerStates(target).chargeProgress
            }
        }

        object : Variable<Int>("EXP") {
            override fun get(context: Context, target: Entity?): Int {
                val player = target as? ServerPlayer ?: throw Exception("No target. ")
                return player.calcTotalExp()
            }
        }

        object : Variable<Boolean>("UsingUnderBarrel") {
            override fun get(context: Context, target: Entity?): Boolean {
                target as LivingEntity
                return GunExtras.getUsingUnderBarrel(target.mainHandItem)
            }
        }

        object : Variable<Boolean>("Locking") {
            override fun get(context: Context, target: Entity?): Boolean {
                val player = target as? ServerPlayer ?: throw Exception("No target. ")
                return PlayerListener.getPlayerStates(player).lockingTarget != null
            }
        }

        object : Variable<Int>("LockingTime") {
            override fun get(context: Context, target: Entity?): Int {
                val player = target as? ServerPlayer ?: throw Exception("No target. ")
                return PlayerListener.getPlayerStates(player).lockingTime
            }
        }

        object : Variable<Int>("ZoomLevel") {
            override fun get(context: Context, target: Entity?): Int {
                return context.zoom ?: run {
                    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                    target as LivingEntity
                    val mainHand = target.mainHandItem
                    if (mainHand.item != gunItem) return -1

                    var scopeItemId = gunItem.getAttachmentId(mainHand, AttachmentType.SCOPE)
                    if (scopeItemId == DefaultAssets.EMPTY_ATTACHMENT_ID) {
                        scopeItemId = gunItem.getBuiltInAttachmentId(mainHand, AttachmentType.SCOPE)
                    }
                    val scopeTag: CompoundTag? = gunItem.getAttachmentTag(mainHand, AttachmentType.SCOPE)
                    val zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag)
                    return zoomNumber
                }
            }
        }

        object : Variable<Boolean>("IsBlocked") {
            override fun get(context: Context, target: Entity?): Boolean {
                return context.isBlocked == true
            }
        }

        object : Variable<Float>("BlockingFactor") {
            override fun get(context: Context, target: Entity?): Float {
                return context.blockingFactor
            }
        }

        object : Variable<Float>("RPMModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRPMModifier(target, mainHand)
            }
        }

        object : Variable<Float>("AllRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, null)
            }
        }

        object : Variable<Float>("StandRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, InaccuracyType.STAND)
            }
        }

        object : Variable<Float>("MoveRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, InaccuracyType.MOVE)
            }
        }

        object : Variable<Float>("SneakRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, InaccuracyType.SNEAK)
            }
        }

        object : Variable<Float>("LieRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, InaccuracyType.LIE)
            }
        }

        object : Variable<Float>("AimRecoilModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getRecoilModifier(target, mainHand, InaccuracyType.AIM)
            }
        }

        object : Variable<Float>("AllSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, null)
            }
        }

        object : Variable<Float>("StandSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, InaccuracyType.STAND)
            }
        }

        object : Variable<Float>("MoveSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, InaccuracyType.MOVE)
            }
        }

        object : Variable<Float>("SneakSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, InaccuracyType.SNEAK)
            }
        }

        object : Variable<Float>("LieSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, InaccuracyType.LIE)
            }
        }

        object : Variable<Float>("AimSpreadModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getSpreadModifier(target, mainHand, InaccuracyType.AIM)
            }
        }

        object : Variable<Float>("ReloadTimeModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getReloadTimeModifier(target, mainHand)
            }
        }

        object : Variable<Float>("AimTimeModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getAimTimeModifier(target, mainHand)
            }
        }

        object : Variable<Float>("DrawTimeModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getDrawTimeModifier(target, mainHand)
            }
        }

        object : Variable<Float>("BoltTimeModifier") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1.0f
                return GunExtras.getDrawTimeModifier(target, mainHand)
            }
        }

        fun generateItemGunModifier(nbtKey: NBTKey<Float>): Variable<Float> {
            return object : Variable<Float>("Gun${nbtKey.key}") {
                override fun get(context: Context, target: Entity?): Float {
                    target as LivingEntity
                    val (iGun, gunItemStack) = target.getMainHandGun()
                    if (iGun == null) return -1.0f
                    return GunKeys.ROOT.getOrDefault(gunItemStack).getOrDefault(nbtKey)
                }

                override fun set(context: Context, target: Entity?, value: Float?) {
                    target as LivingEntity
                    val (iGun, gunItemStack) = target.getMainHandGun()
                    if (iGun == null) return

                    GunKeys.ROOT.getOrSet(gunItemStack) { CompoundTag() }!!.let {
                        if (value == null) it.unset(nbtKey)
                        else it.set(nbtKey, value)
                    }
                }

                override fun unwrap(value: EvaluationValue): Float? {
                    if (!value.isNumberValue) return null
                    return value.numberValue.toFloat()
                }
            }
        }

        fun generatePlayerGunModifier(nbtKey: NBTKey<Float>): Variable<Float> {
            return object : Variable<Float>("Player${nbtKey.key}") {
                override fun get(context: Context, target: Entity?): Float {
                    target as LivingEntity
                    return PlayerExtrasServer.getPlayerExtraData(target).getOrDefault(nbtKey)
                }

                override fun set(context: Context, target: Entity?, value: Float?) {
                    target as LivingEntity
                    val scoreValue = PlayerExtrasServer.getOrCreatePlayerExtraData(target)
                    if (value == null) scoreValue.unset(nbtKey)
                    else scoreValue.set(nbtKey, value)
                    PlayerExtrasServer.setPlayerExtraData(target, scoreValue)
                }

                override fun unwrap(value: EvaluationValue): Float? {
                    if (!value.isNumberValue) return null
                    return value.numberValue.toFloat()
                }
            }
        }

        val gunModifiers = GunKeys.allModifierKeys.map { generateItemGunModifier(it) }
        val playerGunModifiers = GunKeys.allModifierKeys.map { generatePlayerGunModifier(it) }


        object : Variable<Array<Double>>("BulletPos") {
            override fun get(context: Context, target: Entity?): Array<Double> {
                val bullet = context.bullet ?: throw Exception("no bullet")
                val pos = context.pos!!
                return arrayOf(pos.x, pos.y, pos.z, bullet.yRot.toDouble(), bullet.xRot.toDouble())
            }
        }

        object : Variable<Float>("AimDistance") {
            override fun get(context: Context, target: Entity?): Float {
                return target?.lookAtDistance(ServerConfig.rayTraceDistance.get().toDouble())?.toFloat() ?: -1.0f
            }
        }

        object : Variable<Array<Int>>("AimBlockPos") {
            override fun get(context: Context, target: Entity?): Array<Int> {
                val result = target!!.taczPick(ServerConfig.rayTraceDistance.get().toDouble(), 1.0f, false)
                if (result.type == HitResult.Type.BLOCK && result is BlockHitResult) {
                    return arrayOf(result.blockPos.x, result.blockPos.y, result.blockPos.z)
                }
                return arrayOf()
            }
        }

        object : Variable<String>("AimBlockID") {
            override fun get(context: Context, target: Entity?): String {
                val result = target!!.taczPick(ServerConfig.rayTraceDistance.get().toDouble(), 1.0f, false)
                return if (result.type == HitResult.Type.BLOCK && result is BlockHitResult) {
                    target.level().getBlockState(result.blockPos).blockHolder.unwrapKey().orElse(null)?.location()?.toString() ?: "none"
                } else "none"
            }
        }

        object : Variable<Boolean>("BulletIsGenerated") {
            override fun get(context: Context, target: Entity?): Boolean {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as IAccessorBullet).`taczexpands$isGenerated`()
            }
        }

        object : Variable<Float>("BulletDistance") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")
                val pos = context.pos!!
                return target?.distanceToSqr(pos)?.let { Mth.sqrt(it.toFloat()) } ?: -1.0f
            }
        }

        object : Variable<Int>("BulletLife") {
            override fun get(context: Context, target: Entity?): Int {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return bullet.tickCount
            }
        }

        object : Variable<Boolean>("BulletAlive") {
            override fun get(context: Context, target: Entity?): Boolean {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return !bullet.isRemoved
            }
        }

        object : Variable<Boolean>("IsMissileGun") {
            override fun get(context: Context, target: Entity?): Boolean {
                target as LivingEntity
                return target.mainHandIsMissileGun()
            }
        }

        object : Variable<Boolean>("BulletIsMissile") {
            override fun get(context: Context, target: Entity?): Boolean {
                val bullet = context.bullet ?: throw Exception("no bullet")

                val bulletData = (bullet as? IAccessorBullet)?.`taczexpands$getBulletExtraData`()?.bulletData ?: return false
                if (IAccessorBulletData.getBulletExtraHolder(bulletData).missileData.isMissile) return true
                return false
            }
        }

        object : Variable<Int>("GunMonochrome") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                val isMonochrome = GunExtras.getOverrideMonochrome(mainHand) ?: return -1
                return if (isMonochrome) 1 else 0
            }
        }

        object : Variable<Int>("GunThermalImaging") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                val isMonochrome = GunExtras.getOverrideThermalImaging(mainHand) ?: return -1
                return if (isMonochrome) 1 else 0
            }
        }

        object : Variable<Int>("GunNightVision") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                val isMonochrome = GunExtras.getOverrideNightVision(mainHand) ?: return -1
                return if (isMonochrome) 1 else 0
            }
        }

        object : Variable<Float>("GunShieldBlockingPower") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return 0.0f
                val override = GunExtras.getOverrideGunShieldBlockingPower(mainHand)
                if (override != null) return override
                return GunShieldManager.getShield(target)?.blockingPower ?: 0.0f
            }
        }

        object : Variable<Int>("GunBulletAmount") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                return GunExtras.getOverrideBulletAmount(mainHand)
            }
        }

        object : Variable<Boolean>("GunIsSimpleLocking") {
            override fun get(context: Context, target: Entity?): Boolean {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return true
                if (GunExtras.getEnforcingSimpleLocking(mainHand)) return true

                val gunID = gunItem.getGunId(mainHand)

                val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                if (gunIndex == null) {
                    return true
                }

                val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

                val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)
                return extraBulletData.missileData.isSimpleLocking
            }
        }

        object : Variable<Boolean>("GunEnforcingSimpleLocking") {
            override fun get(context: Context, target: Entity?): Boolean {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return false
                return GunExtras.getEnforcingSimpleLocking(mainHand)
            }
        }

        object : Variable<Int>("GunFlightProfileType") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1
                return GunExtras.getMissileFlightProfileType(mainHand) ?: -1
            }
        }

        object : Variable<Float>("GunBaseDamage") {
            override fun get(context: Context, target: Entity?): Float {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1f

                val gunID = gunItem.getGunId(mainHand)

                val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                if (gunIndex == null) {
                    return -1f
                }
                val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)
                return bulletData.damageAmount
            }
        }

        object : Variable<Int>("GunBaseBPM") {
            override fun get(context: Context, target: Entity?): Int {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return -1

                val gunID = gunItem.getGunId(mainHand)

                return TimelessAPI.getCommonGunIndex(gunID).getOrNull()?.gunData?.burstData?.bpm ?: -1
            }
        }

        object : Variable<Array<Float>>("CameraRotation") {
            override fun get(context: Context, target: Entity?): Array<Float> {
                target ?: throw Exception("no target")
                if (target is ServerPlayer) {
                    val cameraData = MissileManager.tvGuidancePlayers[target]
                    if (cameraData != null && cameraData.yaw != null && cameraData.pitch != null) {
                        return arrayOf(cameraData.yaw!!, cameraData.pitch!!)
                    }

                    val clientCamera = PlayerListener.getPlayerStates(target).clientCamera
                    if (clientCamera != null) {
                        return arrayOf(clientCamera.yaw, clientCamera.pitch)
                    }
                }
                return arrayOf(target.yRot, target.xRot)
            }
        }

        object : Variable<Map<String, Any>>("GunBaseRecoil") {
            override fun get(context: Context, target: Entity?): Map<String, Any> {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return mapOf()
                if (GunExtras.getEnforcingSimpleLocking(mainHand)) return mapOf()

                val gunID = gunItem.getGunId(mainHand)

                val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                if (gunIndex == null) {
                    return mapOf()
                }

                val map = mutableMapOf<String, Any>()
                val recoil = gunIndex.gunData.recoil
                if (recoil.yaw != null) {
                    val yawList = recoil.yaw.map {
                        mapOf("time" to it.time, "value" to it.value)
                    }
                    map["yaw"] = yawList
                }
                if (recoil.pitch != null) {
                    val pitchList = recoil.pitch.map {
                        mapOf("time" to it.time, "value" to it.value)
                    }
                    map["pitch"] = pitchList
                }
                return map
            }
        }

        object : Variable<Map<String, Any>>("GunBaseInaccuracy") {
            override fun get(context: Context, target: Entity?): Map<String, Any> {
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                target as LivingEntity
                val mainHand = target.mainHandItem
                if (mainHand.item != gunItem) return mapOf()
                if (GunExtras.getEnforcingSimpleLocking(mainHand)) return mapOf()

                val gunID = gunItem.getGunId(mainHand)

                val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                if (gunIndex == null) {
                    return mapOf()
                }

                val inaccuracy = gunIndex.gunData.inaccuracy ?: return mapOf()

                return inaccuracy.map { it.key.name to it.value }.toMap()
            }
        }

        object : Variable<Int>("BulletFlightProfileType") {
            override fun get(context: Context, target: Entity?): Int {
                val bullet = context.bullet ?: throw Exception("no bullet")

                val extraData = (bullet as? IAccessorBullet)?.`taczexpands$getBulletExtraData`()

                val bulletData = extraData?.bulletData ?: return -1
                val bulletExtra = IAccessorBulletData.getBulletExtraHolder(bulletData)
                if (!bulletExtra.missileData.isMissile) return -1

                return extraData.flightProfileType?.ordinal ?: bulletExtra.missileData.flightProfileType.ordinal
            }
        }

        object : Variable<Float>("BulletSpeed") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.speed ?: -1.0f
            }
        }

        object : Variable<Float>("BulletDeltaSpeed") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return bullet.deltaMovement.length().toFloat()
            }
        }

        object : Variable<Float>("BulletGravity") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.gravity ?: -1.0f
            }
        }

        object : Variable<Float>("BulletFriction") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.friction ?: -1.0f
            }
        }

        object : Variable<Boolean>("BulletExplosion") {
            override fun get(context: Context, target: Entity?): Boolean {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.explosion ?: false
            }
        }

        object : Variable<Float>("BulletExplosionDamage") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.explosionDamage ?: -1.0f
            }
        }

        object : Variable<Float>("BulletExplosionRadius") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return (bullet as? IAccessorEntityKineticBullet)?.explosionRadius ?: -1.0f
            }
        }

        object : Variable<Float>("BulletExplosionDelay") {
            override fun get(context: Context, target: Entity?): Float {
                val bullet = context.bullet ?: throw Exception("no bullet")

                return ((bullet as? IAccessorEntityKineticBullet)?.explosionDelayCount ?: 0) / 20.0f
            }
        }

        object : Variable<String>("CurrentSignalName") {
            override fun get(context: Context, target: Entity?): String {
                return context.signal ?: "null"
            }
        }
    }
}