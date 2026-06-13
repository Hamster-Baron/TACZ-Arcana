package group.taczexpands.server.config

import com.mojang.brigadier.StringReader
import group.taczexpands.server.context.BulletContext
import group.taczexpands.server.context.Context
import group.taczexpands.server.context.HitBlockContext
import group.taczexpands.server.context.HitByTargetContext
import group.taczexpands.server.entity.BulletExtraData
import group.taczexpands.server.expression.ExpressionData
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.expression.ExpressionInstance
import group.taczexpands.server.expression.create
import group.taczexpands.server.util.findTargetInLineOfSight
import group.taczexpands.server.util.getCenterPosition
import group.taczexpands.server.util.getEntitiesInConeFromEyes
import group.taczexpands.server.util.hasLineOfSight
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.commands.arguments.selector.EntitySelectorParser
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.server.ServerLifecycleHooks


class SelectorInstance(val data: SelectorData) {
    val source: SelectorInstance? = if (data.source != null) {
        SelectorInstance(data.source)
    } else {
        null
    }

    var cachedList: List<Entity>? = null

    var args: List<ExpressionInstance>? = null

    fun prepare(context: Context) {
        args = data.args.create(context)

        if (cachedList != null) return
        source?.prepare(context)
        if (data.instant)
            cachedList = getTargets(context)
    }

    fun parseArg(context: Context, name: String): String? {
        return ExpressionHelper.parseNew(data.parameters[name], args, context, null)
    }

    fun getTarget(context: Context): Entity? {
        return getTargets(context).firstOrNull()
    }

    fun getTargets(context: Context): List<Entity> {
        if (cachedList != null) return cachedList!!

        if (data.cacheOnGet) {
            cachedList = getTargets(context)
            return cachedList!!
        }

        return internalGetTargets(context)
    }

    private fun internalGetTargets(context: Context): List<Entity> {

        val sourceEntity = source?.getTarget(context) ?: context.self
        when (data.name) {
            SelectorType.NULL -> {
                return listOf()
            }

            SelectorType.SELF -> {
                return listOf(sourceEntity)
            }

            SelectorType.RAW_TARGET -> {
                val target = context.target ?: return emptyList()
                return listOf(target)
            }

            SelectorType.ENTITY -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"
                val radius = parseArg(context, "radius")?.toDoubleOrNull() ?: 5.0

                val result = if (radius < 0 && type != "all") {
                    ServerLifecycleHooks.getCurrentServer().playerList.players.filter { it.level() == sourceEntity.level() }
                } else {
                    sourceEntity.level().getEntities(
                        sourceEntity,
                        AABB(
                            sourceEntity.x - radius,
                            sourceEntity.y - radius,
                            sourceEntity.z - radius,
                            sourceEntity.x + radius,
                            sourceEntity.y + radius,
                            sourceEntity.z + radius
                        )
                    )
                }
                    .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer }
                    .let { if (limit != null) it.take(limit) else it }

                return result
            }

            SelectorType.TARGET -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"
                val explosionOnly = data.parameters["explosionOnly"] == "true"

                if (context is HitByTargetContext) {
                    val bullet = context.bullet ?: return emptyList()
                    return (listOf(sourceEntity) + BulletExtraData.get(bullet).hitEntities.filter { if (explosionOnly) it.value else true }.keys
                        .mapNotNull { it.hurtEntity }
                        .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer })
                        .let { if (limit != null) it.take(limit) else it }
                } else

                    if (context.target != null) {
                        val bullet = context.bullet ?: return emptyList()
                        return (listOf(context.target!!) + BulletExtraData.get(bullet).hitEntities.filter { if (explosionOnly) it.value else true }.keys
                            .mapNotNull { it.hurtEntity }
                            .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer })
                            .let { if (limit != null) it.take(limit) else it }
                    } else if (context is HitBlockContext || context is BulletContext) {
                        return BulletExtraData.get(context.bullet!!).hitEntities.filter { if (explosionOnly) it.value else true }.keys
                            .mapNotNull { it.hurtEntity }
                            .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer }
                            .let { if (limit != null) it.take(limit) else it }
                    } else return emptyList()
            }

            SelectorType.BULLET -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"
                val radius = parseArg(context, "radius")?.toDoubleOrNull() ?: 5.0
                val at = parseArg(context, "at")?.toIntOrNull()
                val explosionOnly = data.parameters["explosionOnly"] == "true"

                val bullet = context.bullet ?: return emptyList()
                val bulletPos = context.pos!!

                val entities =
                    (if (at == null) {
                        val minX = bulletPos.x - radius
                        val maxX = bulletPos.x + radius
                        val minY = bulletPos.y - radius
                        val maxY = bulletPos.y + radius
                        val minZ = bulletPos.z - radius
                        val maxZ = bulletPos.z + radius
                        bullet.level().getEntities(bullet, AABB(minX, minY, minZ, maxX, maxY, maxZ))
                            .mapNotNull { it }.filter {
                                it.hasLineOfSight(bulletPos)
                            }.filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer }.let {
                                if (limit != null) it.take(limit) else it
                            }
                    } else {
                        val extraData = BulletExtraData.get(bullet)
                        val tempList =
                            extraData.hitEntities.filter { if (explosionOnly) it.value else true }.keys
                                .mapNotNull { it.hurtEntity }
                                .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer }
                                .filter { it.distanceToSqr(bulletPos) <= radius * radius }

                        if (tempList.size >= at) {
                            tempList.let {
                                if (limit != null) it.take(limit) else it
                            }
                        } else emptyList()

                    })
                return entities
            }

            SelectorType.BULLET_PREDICATE -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"
                val radius = parseArg(context, "radius")?.toDoubleOrNull() ?: 5.0

                val bullet = context.bullet ?: return emptyList()
                val bulletPos = bullet.position()

                val scanBox = bullet.boundingBox
                    .inflate(radius)
                    .expandTowards(bullet.deltaMovement.scale(-1.0))
                    .inflate(1.0)

                val entities = bullet.level().getEntities(bullet, scanBox)
                    .mapNotNull { it }.filter {
                        it.hasLineOfSight(bulletPos)
                    }.filter {
                        type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer
                    }.let {
                        if (limit != null) it.take(limit) else it
                    }

                return entities
            }

            SelectorType.RAYCAST -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"
                val fov = parseArg(context, "fov")?.toDoubleOrNull() ?: 45.0
                val maxDistance = parseArg(context, "maxDistance")?.toDoubleOrNull() ?: ServerConfig.rayTraceDistance.get().toDouble()

                val ray = data.parameters["ray"] ?: "line"

                if (ray == "line") {
                    val target = findTargetInLineOfSight(sourceEntity, maxDistance) ?: return emptyList()
                    return listOf(target)
                } else if (ray == "cone") {
                    val entities = getEntitiesInConeFromEyes(sourceEntity, maxDistance, fov)
                        .filter { sourceEntity.hasLineOfSight(it) }

                    return entities.filter { type == "all" || it is ServerPlayer }.let {
                        if (limit != null) it.take(limit) else it
                    }
                } else return emptyList()
            }

            SelectorType.FIXED_POS -> {
                val limit = parseArg(context, "limit")?.toIntOrNull()
                val type = data.parameters["type"] ?: "all"

                val x = parseArg(context, "x")!!.toDouble()
                val y = parseArg(context, "y")!!.toDouble()
                val z = parseArg(context, "z")!!.toDouble()
                val yaw = parseArg(context, "yaw")!!.toFloat()
                val pitch = parseArg(context, "pitch")!!.toFloat()
                val fov = parseArg(context, "fov")!!.toDouble()
                val aspect = parseArg(context, "aspect")?.toFloatOrNull() ?: 1.0f
                val range = parseArg(context, "radius")!!.toDouble()

                val pos = Vec3(x, y, z)
                val frustum = Frustum(pos, yaw, pitch, fov, aspect, range)

                val searchBox = AABB(pos, pos).inflate(range)

                return context.self.level().getEntities(null, searchBox)
                    .mapNotNull { it }
                    .filter { type == "all" || type == "living" && it is LivingEntity || type == "player" && it is ServerPlayer }
                    .filter { frustum.isInFrustum(it.getCenterPosition()) && it.hasLineOfSight(pos) }
                    .let {
                        if (limit != null) it.take(limit) else it
                    }
            }

            SelectorType.VANILLA -> {
                val selectorArgs = parseArg(context, "selector") ?: return emptyList()

                val vanillaSelector = EntitySelectorParser(StringReader(selectorArgs), true).parse()

                return vanillaSelector.findEntities(
                    sourceEntity.createCommandSourceStack().withPermission(4).withSource(ServerLifecycleHooks.getCurrentServer())
                )
            }
        }
    }
}

@Serializable
enum class SelectorType {
    @SerialName("self")
    SELF,

    @SerialName("raw_target")
    RAW_TARGET,

    @SerialName("target")
    TARGET,

    @SerialName("bullet")
    BULLET,

    @SerialName("bullet_predicate")
    BULLET_PREDICATE,

    @SerialName("raycast")
    RAYCAST,

    @SerialName("entity")
    ENTITY,

    @SerialName("fixed_pos")
    FIXED_POS,

    @SerialName("vanilla")
    VANILLA,

    @SerialName("null")
    NULL

}

@Serializable
data class SelectorData(
    val name: SelectorType,
    val source: SelectorData? = null,
    @SerialName("params") val parameters: Map<String, String> = mapOf(),
    val args: List<ExpressionData>? = null,
    val instant: Boolean = true,
    val cacheOnGet: Boolean = false
) {
    companion object {
        val NULL = SelectorData(SelectorType.NULL)
        val SELF = SelectorData(SelectorType.SELF)
        val RAW_TARGET = SelectorData(SelectorType.RAW_TARGET)
    }
}

fun SelectorData?.create(context: Context): SelectorInstance {
    val instance = if (this == null)
        SelectorInstance(SelectorData.SELF)
    else SelectorInstance(this)
    instance.prepare(context)
    return instance
}

fun SelectorData?.create(context: Context, default: SelectorData): SelectorInstance {
    val instance = if (this == null)
        SelectorInstance(default)
    else SelectorInstance(this)
    instance.prepare(context)
    return instance
}

fun SelectorData?.createEmptyAsNull(context: Context): SelectorInstance {
    val instance = if (this == null)
        SelectorInstance(SelectorData.NULL)
    else SelectorInstance(this)
    instance.prepare(context)
    return instance
}