package group.taczexpands.server.util

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlScalarFormatException
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.init.ModItems
import com.tacz.guns.util.block.BlockRayTrace
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.BulletExtraHolder
import group.taczexpands.common.data.MissileData
import group.taczexpands.common.util.BlockResistanceData
import group.taczexpands.common.util.getFixedLookAngle
import group.taczexpands.server.TACZExpandsServer
import group.taczexpands.server.bullet.BulletManager
import group.taczexpands.server.compat.CompatHelper
import group.taczexpands.server.compat.superbwarfare.SuperbWarfareCompat
import group.taczexpands.server.config.action.*
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.condition.*
import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.skill.Skill
import group.taczexpands.server.skill.SkillManager
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import kotlin.math.PI
import kotlin.math.cos


val YAML = Yaml(
    configuration = YamlConfiguration(encodeDefaults = true),
    serializersModule = SerializersModule {
        polymorphic(Condition::class) {
            subclass(TargetIsPlayer::class)
            subclass(TargetIsEntities::class)
            subclass(SelfStandsOn::class)
            subclass(HitBlockType::class)
            subclass(Weather::class)
            subclass(Biome::class)
            subclass(Time::class)
            subclass(HasItem::class)
            subclass(HasEffect::class)
            subclass(ConsumeItem::class)
            subclass(BulletFlyingTime::class)
            subclass(Variables::class)
            subclass(AmmoType::class)
            subclass(IsBraced::class)
            subclass(SignalState::class)
            subclass(WorldType::class)
            subclass(HasAttachment::class)
            subclass(EntityInRange::class)
            subclass(DamageType::class)
            subclass(HasPermission::class)
            subclass(ChainCondition::class)
            subclass(ArmorType::class)
            subclass(AmmoCount::class)
            subclass(HasMod::class)
            subclass(IsChangeAttachment::class)
            subclass(HasNBT::class)

        }
        polymorphic(Action::class) {
            subclass(RunCommand::class)
            subclass(Modify::class)
            subclass(Animation::class)
            subclass(ParticleEmitter::class)
            subclass(Summon::class)
            subclass(Effect::class)
            subclass(FlashEmitter::class)
            subclass(SudoAction::class)
            subclass(CancelAction::class)
            subclass(CancelSkill::class)
            subclass(RenderUtil::class)
            subclass(InvalidateCache::class)
            subclass(CustomDisplay::class)
            subclass(Sound::class)
            subclass(Message::class)
            subclass(Timer::class)
            subclass(TriggerSignal::class)
            subclass(SetLockingTarget::class)
            subclass(SetLockingPos::class)
            subclass(Hurt::class)
            subclass(Ignite::class)
            subclass(Frostbite::class)
            subclass(ChainAction::class)
            subclass(Branch::class)
            subclass(ForceFire::class)
            subclass(Shake::class)
            subclass(ForceRotation::class)
            subclass(SetAmmoType::class)
            subclass(GenerateBullet::class)
            subclass(MythicMobsSkill::class)
            subclass(SetAttachment::class)
            subclass(SetTexture::class)
            subclass(SetCamera::class)
            subclass(NBTEdit::class)
            subclass(Dash::class)
            subclass(SetHUD::class)
            subclass(DisableItem::class)
        }
    })

fun schedule(skill: Skill, player: ServerPlayer?, time: Int, block: (ScheduledTask) -> Unit, tick: (ScheduledTask) -> Unit) {
    if (player != null) {
        if (SkillManager.shouldCancel(player, skill)) return
    }
    TACZExpandsServer.SCHEDULED_TASKS.add(ScheduledTask(skill, player, block, time, false, tick))
}

fun schedule(skill: Skill, player: ServerPlayer?, time: Int, block: (ScheduledTask) -> Unit) {
    if (player != null) {
        if (SkillManager.shouldCancel(player, skill)) return
    }
    TACZExpandsServer.SCHEDULED_TASKS.add(ScheduledTask(skill, player, block, time))
}

fun schedule(time: Int, block: (ScheduledTask) -> Unit) {
    TACZExpandsServer.SCHEDULED_TASKS.add(ScheduledTask(null, null, block, time))
}

fun ItemStack.equalsLore(other: ItemStack): Boolean {
    val left = this.getRawLore()
    val right = other.getRawLore()

    if (left == right) return true

    return false
}

fun ItemStack.getRawLore(): List<String>? {
    val item = this

    if (!item.hasTag()) return null
    val tag = item.tag!!
    if (!tag.contains("display", 10)) return null
    val displayTag = tag.getCompound("display")
    if (!displayTag.contains("Lore", 9)) return null
    val loreList = displayTag.getList("Lore", 8)
    if (loreList.size <= 0) return null


    return IntRange(0, loreList.size - 1).map {
        loreList.getString(it)
    }
}

fun Player.hasSolidGroundInFront(): Boolean {
    val player = this
    val level = player.level()

    val facingDirection = player.direction

    val playerFeetPos: BlockPos = player.blockPosition()
    val blockPosInFront = playerFeetPos.relative(facingDirection)

    val blockStateInFront: BlockState = level.getBlockState(blockPosInFront)
    return blockStateInFront.isFaceSturdy(level, blockPosInFront, Direction.UP)
}

fun Entity.lookAtDistance(maxDistance: Double): Double {
    val player = this

    val hitEntity = findTargetInLineOfSight(player, maxDistance)
    val entityDistance = hitEntity?.distanceTo(player)

    val hitResult = player.taczPick(maxDistance, 1.0f, false)

    val blockDistance = if (hitResult.type != HitResult.Type.MISS) {
        val playerEyePos = player.getEyePosition(1.0f)
        val hitLocation = hitResult.getLocation()
        playerEyePos.distanceTo(hitLocation)
    } else null

    if (entityDistance != null && blockDistance != null) {
        if (entityDistance < blockDistance)
            return entityDistance.toDouble()
        else return blockDistance
    }

    if (entityDistance != null) return entityDistance.toDouble()
    if (blockDistance != null) return blockDistance

    return -1.0
}

fun Entity.taczPick(maxDistance: Double, partialTicks: Float, containsFluid: Boolean): HitResult {
    val vec3 = this.getEyePosition(partialTicks)
    val vec31 = this.getViewVector(partialTicks)
    val vec32 = vec3.add(vec31.x * maxDistance, vec31.y * maxDistance, vec31.z * maxDistance)
    return BlockRayTrace.rayTraceBlocks(
        this.level(),
        ClipContext(vec3, vec32, ClipContext.Block.COLLIDER, if (containsFluid) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE, this)
    )
}


fun Player.calcTotalExp(): Int {
    fun getXpNeededForNextLevel(experienceLevel: Int): Int {
        if (experienceLevel >= 30) {
            return 112 + (experienceLevel - 30) * 9
        } else {
            return if (experienceLevel >= 15) 37 + (experienceLevel - 15) * 5 else 7 + experienceLevel * 2
        }
    }

    var totalExp = 0
    for (level in 0 until this.experienceLevel) {
        totalExp += getXpNeededForNextLevel(level)
    }

    val progressPoints = (this.experienceProgress * getXpNeededForNextLevel(this.experienceLevel)).toInt()
    totalExp += progressPoints

    return totalExp
}

fun Entity.canSee(entity: Entity, maxDistance: Double): Boolean {
    if (entity.level() !== this.level()) {
        return false
    } else {
        val vec3 = if (this is EntityKineticBullet) this.getCenterPosition() else Vec3(this.getX(), this.getEyeY(), this.getZ())
        val vec31 = entity.getCenterPosition()
        if (vec31.distanceToSqr(vec3) > maxDistance * maxDistance) {
            return false
        } else {
            return BlockRayTrace.rayTraceBlocks(
                this.level(),
                ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
            ).type == HitResult.Type.MISS

        }
    }
}

fun Entity.canSee(pos: Vec3, maxDistance: Double): Boolean {
    val vec3 = if (this is EntityKineticBullet) this.getCenterPosition() else Vec3(this.getX(), this.getEyeY(), this.getZ())
    val vec31 = pos
    if (vec31.distanceToSqr(vec3) > maxDistance * maxDistance) {
        return false
    } else {
        return BlockRayTrace.rayTraceBlocks(
            this.level(),
            ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        ).type == HitResult.Type.MISS

    }
}

fun findTargetInLineOfSight(entity: Entity, maxDistance: Double, predicate: Predicate<Entity>? = null): Entity? {
    val startVec = entity.eyePosition

    val lookVec = entity.getFixedLookAngle()
    val endVec = startVec.add(lookVec.scale(maxDistance))

    val searchAABB = entity.boundingBox.expandTowards(lookVec.scale(maxDistance)).inflate(1.0)

    val hitResult = ProjectileUtil.getEntityHitResult(
        entity,
        startVec,
        endVec,
        searchAABB,
        Predicate<Entity> { it != entity && !it.isSpectator && it.isPickable && (predicate?.test(it) ?: true) && entity.canSee(it, maxDistance) },
        maxDistance * maxDistance
    )

    return hitResult?.entity
}

fun getEntitiesInConeFromCenter(sourceEntity: Entity, coneRadius: Double, coneAngleDeg: Double): List<Entity> {
    val entitiesInCone = mutableListOf<Entity>()

    val lookVec = sourceEntity.getFixedLookAngle()

    val coneAngleCos = cos(coneAngleDeg * PI / 180.0)

    val sourcePos = sourceEntity.getCenterPosition()
    val searchBox = AABB(
        sourcePos.x - coneRadius, sourcePos.y - coneRadius, sourcePos.z - coneRadius,
        sourcePos.x + coneRadius, sourcePos.y + coneRadius, sourcePos.z + coneRadius
    )

    val world = sourceEntity.level()
    for (targetEntity in world.getEntitiesOfClass(Entity::class.java, searchBox)) {
        if (targetEntity == sourceEntity) {
            continue
        }

        val distanceSqr = sourcePos.distanceToSqr(targetEntity.getCenterPosition())
        if (distanceSqr > coneRadius * coneRadius) {
            continue
        }

        val vecToTarget = targetEntity.getCenterPosition().subtract(sourcePos).normalize()
        val dotProduct = lookVec.dot(vecToTarget)

        if (dotProduct > coneAngleCos) {
            entitiesInCone.add(targetEntity)
        }
    }

    return entitiesInCone.filter { it != sourceEntity && !it.isSpectator && it.isPickable }
}

fun getEntitiesInConeFromEyes(sourceEntity: Entity, coneRadius: Double, coneAngleDeg: Double): List<Entity> {
    val entitiesInCone = mutableListOf<Entity>()

    val eyePos = sourceEntity.eyePosition

    val lookVec = sourceEntity.getFixedLookAngle()

    val coneAngleCos = cos(coneAngleDeg * PI / 180.0)

    val searchBox = AABB(
        eyePos.x - coneRadius, eyePos.y - coneRadius, eyePos.z - coneRadius,
        eyePos.x + coneRadius, eyePos.y + coneRadius, eyePos.z + coneRadius
    )

    val world = sourceEntity.level()
    for (targetEntity in world.getEntitiesOfClass(Entity::class.java, searchBox)) {
        if (targetEntity == sourceEntity) {
            continue
        }

        val distanceSqr = eyePos.distanceToSqr(targetEntity.getCenterPosition())
        if (distanceSqr > coneRadius * coneRadius) {
            continue
        }

        val vecToTarget = targetEntity.getCenterPosition().subtract(eyePos).normalize()
        val dotProduct = lookVec.dot(vecToTarget)

        if (dotProduct > coneAngleCos) {
            entitiesInCone.add(targetEntity)
        }
    }

    return entitiesInCone.filter { it != sourceEntity && !it.isSpectator && it.isPickable }
}

fun LivingEntity.mainHandIsMissileGun(): Boolean {
    val gunItem = ModItems.MODERN_KINETIC_GUN.get()
    val mainHand = this.mainHandItem
    if (mainHand.item != gunItem) {
        return false
    }

    val gunID = gunItem.getGunId(mainHand)

    val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
    if (gunIndex == null) {
        return false
    }

    val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

    val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

    if (!extraBulletData.missileData.isMissile) return false
    return true
}

fun Entity.hasLineOfSight(entity: Entity): Boolean {
    if (this is LivingEntity)
        return this.hasLineOfSight(entity)
    else {
        if (entity.level() !== this.level()) {
            return false
        } else {
            val vec3 = Vec3(this.getX(), this.getEyeY(), this.getZ())
            val vec31 = Vec3(entity.getX(), entity.getEyeY(), entity.getZ())
            if (vec31.distanceTo(vec3) > 128.0) {
                return false
            } else {
                return this.level()
                    .clip(ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                    .getType() == HitResult.Type.MISS
            }
        }
    }
}

fun Entity.hasLineOfSight(pos: Vec3): Boolean {
    val vec3 = Vec3(this.getX(), this.getEyeY(), this.getZ())
    val vec31 = Vec3(pos.x, pos.y, pos.z)
    if (vec31.distanceTo(vec3) > 128.0) {
        return false
    } else {
        return this.level()
            .clip(ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
            .getType() == HitResult.Type.MISS
    }
}

fun Entity.getHealth(): Float {
    if (this is LivingEntity)
        return this.health
    else {
        if (CompatHelper.hasSuperbWarfare()) {
            val health = SuperbWarfareCompat.getHealth(this)
            if (health != null) return health
        }
        return 0.0f
    }
}

fun Entity.getMaxHealth(): Float {
    if (this is LivingEntity)
        return this.maxHealth
    else {
        if (CompatHelper.hasSuperbWarfare()) {
            val maxHealth = SuperbWarfareCompat.getMaxHealth(this)
            if (maxHealth != null) return maxHealth
        }
        return 0.0f
    }
}

fun Entity.getCenterPosition(): Vec3 {
    if (this is EntityKineticBullet)
        return this.position()
    return this.boundingBox.center
}

fun LivingEntity.getMainHandGun(): Pair<IGun?, ItemStack> {
    return IGun.getIGunOrNull(mainHandItem) to mainHandItem
}

fun MissileData.getTargetableEntityPredicator(): BiFunction<ServerPlayer, Entity, Boolean> {
    if (this.targetableEntityPredicatorCache == null) {
        val list = targetableEntityTypes
        val predicates = list.map { line ->
            if (line == "any") {
                BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                    return@BiFunction true
                }
            } else if (line == "otherteamplayers") {
                BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                    if (entity is Player && entity.team != player.team) {
                        return@BiFunction true
                    } else {
                        return@BiFunction false
                    }
                }
            } else if (line == "offtheground") {
                BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                    val level = entity.level()
                    val entityPos = entity.blockPosition()
                    for (i in 1..10) {
                        val checkPos: BlockPos = entityPos.below(i)

                        if (!level.getBlockState(checkPos).isAir) {

                            return@BiFunction false
                        }
                    }
                    return@BiFunction true
                }
            } else if (line == "riding") {
                BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                    return@BiFunction entity.vehicle != null
                }
            } else if (line == "isnotdragonparts") {
                BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                    return@BiFunction entity !is EnderDragonPart
                }
            } else {
                if (line.startsWith("#")) {
                    val tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation(line.removePrefix("#")))
                    BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                        return@BiFunction entity.type.`is`(tagKey)
                    }
                } else {
                    val type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation(line))
                    if (type == null) {
                        BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                            return@BiFunction false
                        }
                    } else {
                        BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
                            entity.type == type
                        }
                    }
                }
            }
        }

        this.targetableEntityPredicatorCache = BiFunction<ServerPlayer, Entity, Boolean> { player, entity ->
            for (predicate in predicates) {
                if (!predicate.apply(player, entity)) return@BiFunction false
            }
            return@BiFunction true
        }
    }
    return this.targetableEntityPredicatorCache!!
}

data class TypeHolder<T>(val resourceKey: ResourceKey<Registry<T>>, val rawString: String) {
    var tagKey: TagKey<T>? = null
    var location: ResourceLocation? = null

    init {
        if (rawString.startsWith("#")) {
            tagKey = TagKey.create(resourceKey, ResourceLocation(rawString.removePrefix("#")))
        } else {
            location = ResourceLocation(rawString)
        }
    }

    fun `is`(holder: Holder<T>): Boolean {
        if (tagKey != null) {
            return holder.`is`(tagKey!!)
        } else if (location != null) {
            return holder.`is`(location!!)
        } else throw IllegalArgumentException("Invalid TypeHolder $rawString")
    }
}


fun BulletExtraHolder.getBlockResistance(blockState: BlockState): BlockResistanceData? {
    if (this.resistanceTable.blockResistanceCache == null) {
        val resolvedTable = this.resistanceTable.resistanceTable.mapNotNull { entry ->

            TypeHolder(Registries.BLOCK, entry.key) to BlockResistanceData(
                entry.resistance,
                entry.destroyable,
                entry.showParticle,
                entry.accumulateDamage,
                entry.deflectable,
                entry.bypassGlobalDestroyLimit
            )
        }.toMap()

        this.resistanceTable.blockResistanceCache = Function { blockState ->
            resolvedTable.forEach { (holder, resistance) ->
                if (holder.`is`(blockState.blockHolder)) {
                    return@Function resistance
                }
            }

            BulletManager.blockResistanceTable.forEach { (holder, resistance) ->
                if (holder.`is`(blockState.blockHolder)) {
                    return@Function resistance
                }
            }

            return@Function null
        }
    }
    return this.resistanceTable.blockResistanceCache!!.apply(blockState)
}

fun String.checkContains(subStr: String, useRegex: Boolean): Boolean {
    if (!useRegex) {
        return this.contains(subStr)
    } else {
        return subStr.toRegex().matches(this)
    }
}

fun List<String>.checkContains(subStr: String, useRegex: Boolean): Boolean {
    if (!useRegex) {
        return this.contains(subStr)
    } else {
        for (str in this) {
            if (str.checkContains(subStr, true))
                return true
        }
        return false
    }
}

fun Set<String>.checkContains(subStr: String, useRegex: Boolean): Boolean {
    if (!useRegex) {
        return this.contains(subStr)
    } else {
        for (str in this) {
            if (str.checkContains(subStr, true))
                return true
        }
        return false
    }
}

fun BlockHitResult.immutable(): BlockHitResult {
    return if (this.blockPos is BlockPos.MutableBlockPos)
        BlockHitResult(this.location, this.direction, this.blockPos.immutable(), this.isInside)
    else this
}

fun Vec3.copy(): Vec3 {
    return Vec3(this.x, this.y, this.z)
}

fun YamlScalar.infer(): Any {
    try {
        return this.toLong()
    } catch (e: Exception) {
    }

    try {
        return this.toDouble()
    } catch (e: Exception) {
    }

    try {
        return this.toBoolean()
    } catch (e: Exception) {
    }

    return this.content
}

val LOGGER = TACZExpandsServer.LOGGER