package group.taczexpands.server.bullet

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.pojo.data.gun.BulletData
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.BulletExtraHolder
import group.taczexpands.common.data.FlightProfileType
import group.taczexpands.common.data.GuidanceType
import group.taczexpands.common.entity.EntityKineticBulletShared
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.common.util.getFixedLookAngle
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.config.ServerConfig
import group.taczexpands.server.entity.BulletExtraData
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.mixin.accessor.IAccessorChunkMap
import group.taczexpands.server.mixin.accessor.IAccessorServerEntity
import group.taczexpands.server.mixin.accessor.IAccessorTrackedEntity
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.util.*
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import kotlin.math.*
import kotlin.random.Random

class MissileData(val dataHolder: BulletExtraData, val bulletData: BulletData, val bulletExtraData: BulletExtraHolder)
class CameraData(
    var bullet: EntityKineticBullet? = null,
    var yaw: Float? = null,
    var pitch: Float? = null,
    var input: Vec3 = Vec3.ZERO,
)

object MissileManager {
    val INTERNAL_MISSILE_CANCEL_IDENTIFIER = "internal_missile_cancel"
    val tvGuidancePlayers = mutableMapOf<ServerPlayer, CameraData>()
    val jammingEntities = mutableMapOf<Predicate<Entity>, Pair<Double, Double>>()

    fun reloadJammingEntities() {
        jammingEntities.clear()
        val list = ServerConfig.jammingEntities.get()
        for (entry in list) {
            val line = entry.replace(" ", "").split(",")
            if (line.size != 3) continue

            val entity = line[0]
            val efficiency = line[1].toDoubleOrNull() ?: continue
            val sphereRadius = line[2].toDoubleOrNull() ?: continue

            val predicate = if (entity.startsWith("#")) {
                val tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation(entity.removePrefix("#")))
                Predicate<Entity> {
                    return@Predicate it.type.`is`(tagKey)
                }
            } else {
                val type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation(entity))
                if (type == null) {
                    Predicate<Entity> {
                        return@Predicate false
                    }
                } else {
                    Predicate<Entity> {
                        it.type == type
                    }
                }
            }

            jammingEntities[predicate] = efficiency to sphereRadius
        }
    }

    fun updatePlayerTarget(player: ServerPlayer, playerState: PlayerListener.PlayerState) {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = player.mainHandItem
        if (mainHand.item != gunItem) {
            playerState.lockingTarget = null
            return
        }
        if (!IGunOperator.fromLivingEntity(player).synIsAiming) {
            playerState.lockingTarget = null
            return
        }

        val gunID = gunItem.getGunId(mainHand)

        val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
        if (gunIndex == null) {
            playerState.lockingTarget = null
            return
        }

        val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

        val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

        if (extraBulletData.missileData.isMissile) {
            if (playerState.lockingTarget != null) {
                if (!isInLockingAngle(player, playerState.lockingTarget!!, extraBulletData)) {
                    if (playerState.lockingRemainingTime >= extraBulletData.missileData.lockingRemainingTime) {
                        playerState.lockingTarget = null
                    } else {
                        playerState.lockingTarget = playerState.lockingTarget
                    }
                    playerState.lockingRemainingTime++
                } else {
                    playerState.lockingTarget = playerState.lockingTarget
                    playerState.lockingRemainingTime = 0
                }
            } else {
                var target = findTargetInLineOfSight(player, extraBulletData.missileData.maxLockingDistance.toDouble()) {
                    extraBulletData.missileData.getTargetableEntityPredicator().apply(player, it)
                }
                if (target == null) {
                    playerState.lockingTarget = null
                    return
                }

                if (target is EnderDragonPart && target.parentMob != null) {
                    target = target.parentMob!!
                }

                if (!extraBulletData.missileData.getTargetableEntityPredicator().apply(player, target)) {
                    playerState.lockingTarget = null
                    return
                }

                playerState.lockingTarget = target
                playerState.lockingRemainingTime = 0
            }
        } else {
            playerState.lockingTarget = null
        }
    }

    fun updatePlayerFireStatus(player: ServerPlayer, playerState: PlayerListener.PlayerState) {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = player.mainHandItem
        if (mainHand.item != gunItem) {
            ActionManager.removeCancel(player, S2CCancelAction.Action.MissileFire, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
            return
        }

        val gunID = gunItem.getGunId(mainHand)

        val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
        if (gunIndex == null) {
            ActionManager.removeCancel(player, S2CCancelAction.Action.MissileFire, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
            return
        }

        val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

        val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)
        if (extraBulletData.missileData.isMissile) {
            if (extraBulletData.missileData.mustLocking) {
                if ((extraBulletData.missileData.isSimpleLocking || GunExtras.getEnforcingSimpleLocking(mainHand))
                    && (playerState.lockingTarget == null || playerState.lockingTime < extraBulletData.missileData.lockingTime)
                ) {
                    ActionManager.cancelAction(player, S2CCancelAction.Action.MissileFire, 999999, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
                    return
                }

                if (!extraBulletData.missileData.isSimpleLocking && !GunExtras.getEnforcingSimpleLocking(mainHand) && playerState.skillLockingTarget == null) {
                    ActionManager.cancelAction(player, S2CCancelAction.Action.MissileFire, 999999, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
                    return
                }
            }

            if (extraBulletData.missileData.guidanceType.isRemoteCamera) {
                val cameraData = tvGuidancePlayers[player]
                if (cameraData != null) {
                    if (cameraData.bullet == null || !cameraData.bullet!!.isRemoved) {
                        ActionManager.cancelAction(player, S2CCancelAction.Action.MissileFire, 999999, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
                        return
                    }
                }
            }
        }

        ActionManager.removeCancel(player, S2CCancelAction.Action.MissileFire, INTERNAL_MISSILE_CANCEL_IDENTIFIER)
    }

    fun updateClientLockingStatus(player: ServerPlayer, playerState: PlayerListener.PlayerState) {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = player.mainHandItem
        if (mainHand.item != gunItem) {
            playerState.currentClientBindingTarget = 0
            playerState.currentClientBindingLockingTime = 1
            return
        }

        val gunID = gunItem.getGunId(mainHand)

        val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
        if (gunIndex == null) {
            playerState.currentClientBindingTarget = 0
            playerState.currentClientBindingLockingTime = 1
            return
        }

        val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

        val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)
        if (!extraBulletData.missileData.isMissile) {
            playerState.currentClientBindingTarget = 0
            playerState.currentClientBindingLockingTime = 1
            return
        }

        if ((extraBulletData.missileData.isSimpleLocking || GunExtras.getEnforcingSimpleLocking(mainHand))) {
            if (playerState.lockingTarget == null) {
                playerState.currentClientBindingTarget = 0
                playerState.currentClientBindingLockingTime = 1
            } else {
                playerState.currentClientBindingTarget = playerState.lockingTarget!!.id
                playerState.currentClientBindingLockingTime = extraBulletData.missileData.lockingTime
            }
        } else {
            if (playerState.skillLockingTarget == null) {
                playerState.currentClientBindingTarget = 0
                playerState.currentClientBindingLockingTime = 1
            } else {
                playerState.currentClientBindingTarget = playerState.skillLockingTarget!!.id
                playerState.currentClientBindingLockingTime = 1
            }
        }
    }

    fun getPlayerLockingTarget(player: ServerPlayer, bypassAiming: Boolean = false): Entity? {
        val playerState = PlayerListener.getPlayerStates(player)
        if (playerState.lockingTarget == null) {
            return null
        }

        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        val mainHand = player.mainHandItem
        if (mainHand.item != gunItem) {
            return null
        }

        if (!bypassAiming && !IGunOperator.fromLivingEntity(player).synIsAiming) {
            return null
        }

        val gunID = gunItem.getGunId(mainHand)

        val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
        if (gunIndex == null) {
            return null
        }

        val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

        val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

        if (extraBulletData.missileData.isMissile && playerState.lockingTime >= extraBulletData.missileData.lockingTime) {
            return playerState.lockingTarget
        }

        return null
    }


    fun isRayIntersectingSphere(rayOrigin: Vec3, rayEnd: Vec3, sphereCenter: Vec3, sphereRadius: Double): Boolean {
        val rayDirection = rayEnd.subtract(rayOrigin).normalize()
        val centerToOrigin = rayOrigin.subtract(sphereCenter)

        val a = rayDirection.dot(rayDirection)
        val b = 2.0 * centerToOrigin.dot(rayDirection)
        val c = centerToOrigin.dot(centerToOrigin) - sphereRadius * sphereRadius

        val discriminant = b * b - 4.0 * a * c

        return discriminant >= 0.0
    }

    fun isBoundingBoxOccludedBySphere(sourcePos: Vec3, targetEntity: Entity, sphereCenter: Vec3, sphereRadius: Double): Boolean {
        val targetAABB = targetEntity.boundingBox

        val corners = arrayOf(
            Vec3(targetAABB.minX, targetAABB.minY, targetAABB.minZ),
            Vec3(targetAABB.maxX, targetAABB.minY, targetAABB.minZ),
            Vec3(targetAABB.minX, targetAABB.minY, targetAABB.maxZ),
            Vec3(targetAABB.maxX, targetAABB.minY, targetAABB.maxZ),
            Vec3(targetAABB.minX, targetAABB.maxY, targetAABB.minZ),
            Vec3(targetAABB.maxX, targetAABB.maxY, targetAABB.minZ),
            Vec3(targetAABB.minX, targetAABB.maxY, targetAABB.maxZ),
            Vec3(targetAABB.maxX, targetAABB.maxY, targetAABB.maxZ)
        )

        for (corner in corners) {
            if (!isRayIntersectingSphere(sourcePos, corner, sphereCenter, sphereRadius)) {
                return false
            }
        }

        return true
    }

    fun getAngleBetweenVectorsRad(v1: Vec3, v2: Vec3): Double {
        if (v1.lengthSqr() < 0.001 || v2.lengthSqr() < 0.001) {
            return 0.0
        }

        val normalizedV1 = v1.normalize()
        val normalizedV2 = v2.normalize()

        val dotProduct = normalizedV1.dot(normalizedV2)

        return acos(dotProduct.coerceIn(-1.0, 1.0))
    }

    fun degToRad(degrees: Double): Double {
        return degrees * PI / 180.0
    }

    fun isInJammableAngle(missile: EntityKineticBullet, target: Entity, data: MissileData): Boolean {
        if (target.level() != missile.level()) return false
        if (target.isRemoved) return false
        val missileVelocity = missile.deltaMovement
        val missilePos = missile.getCenterPosition()

        val vecToTarget = target.getCenterPosition().subtract(missilePos)

        val angleRad = getAngleBetweenVectorsRad(missileVelocity, vecToTarget)

        val jammableAngleRad = degToRad(data.bulletExtraData.missileData.jammableAngle.toDouble())

        if (angleRad <= jammableAngleRad) return true
        return false
    }

    fun isOutOfMaxGuidanceAngle(missile: EntityKineticBullet, target: Entity, data: MissileData): Boolean {
        if (target.level() != missile.level()) return true
        if (target.isRemoved) return true
        val missileVelocity = missile.deltaMovement
        val missilePos = missile.getCenterPosition()

        val vecToTarget = target.getCenterPosition().subtract(missilePos)

        val angleRad = getAngleBetweenVectorsRad(missileVelocity, vecToTarget)

        val maxGuidanceAngleRad = degToRad(data.bulletExtraData.missileData.maxOffAxisAngleOnGuidance.toDouble())

        if (angleRad > maxGuidanceAngleRad) return true
        return false
    }

    fun isOutOfMaxGuidanceAngle(missile: EntityKineticBullet, targetPos: Vec3, data: MissileData): Boolean {
        val missileVelocity = missile.deltaMovement
        val missilePos = missile.getCenterPosition()

        val vecToTarget = targetPos.subtract(missilePos)

        val angleRad = getAngleBetweenVectorsRad(missileVelocity, vecToTarget)

        val maxGuidanceAngleRad = degToRad(data.bulletExtraData.missileData.maxOffAxisAngleOnGuidance.toDouble())

        if (angleRad > maxGuidanceAngleRad) return true

        if (!missile.canSee(targetPos, data.bulletExtraData.missileData.maxLockingDistance.toDouble())) return true
        return false
    }

    fun isInLockingAngle(player: ServerPlayer, target: Entity, data: BulletExtraHolder): Boolean {
        if (target.level() != player.level()) return false
        if (target.isRemoved) return false
        val playerRotation = player.lookAngle
        val playerPos = player.eyePosition

        val vecToTarget = target.getCenterPosition().subtract(playerPos)

        val angleRad = getAngleBetweenVectorsRad(playerRotation, vecToTarget)

        val maxLockingAngleRad = degToRad(data.missileData.maxOffAxisAngleOnLocking.toDouble())


        if (angleRad > maxLockingAngleRad) return false
        if (!player.canSee(target, data.missileData.maxLockingDistance.toDouble())) return false
        return true
    }

    fun vecToYawPitch(direction: Vec3): Pair<Float, Float> {
        val x = direction.x
        val y = direction.y
        val z = direction.z

        val horizontalLength = sqrt(x * x + z * z)

        if (horizontalLength < 0.001) {
            val pitchDeg = if (y > 0) -90.0f else 90.0f
            return Pair(0.0f, pitchDeg)
        }

        val yawRad = atan2(-x, z)

        val pitchRad = asin(y / sqrt(x * x + y * y + z * z))

        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        var yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()

        if (yawDeg > 180.0f) {
            yawDeg -= 360.0f
        } else if (yawDeg < -180.0f) {
            yawDeg += 360.0f
        }

        return Pair(yawDeg, -pitchDeg)
    }

    fun calculateViewVector(xRot: Float, yRot: Float): Vec3 {
        val f = xRot * (Math.PI.toFloat() / 180f)
        val f1 = -yRot * (Math.PI.toFloat() / 180f)
        val f2 = Mth.cos(f1)
        val f3 = Mth.sin(f1)
        val f4 = Mth.cos(f)
        val f5 = Mth.sin(f)
        return Vec3((f3 * f4).toDouble(), (-f5).toDouble(), (f2 * f4).toDouble())
    }

    fun updateIsMissileTick(missile: EntityKineticBullet) {
        if (missile.level().isClientSide) return

        val extraData = (missile as IAccessorBullet).`taczexpands$getBulletExtraData`()
        val bulletData = extraData.bulletData ?: return
        val extraHolder = (bulletData as IAccessorBulletData).`taczexpands$getExtraHolder`()
        if (!extraHolder.missileData.isMissile) return
        val owner = missile.owner
        if (owner !is ServerPlayer) return

    }

    fun bulletInit(missile: EntityKineticBullet): Boolean {
        if (missile.level().isClientSide) return false

        val extraData = (missile as IAccessorBullet).`taczexpands$getBulletExtraData`()
        val bulletData = extraData.bulletData ?: return false
        val extraHolder = (bulletData as IAccessorBulletData).`taczexpands$getExtraHolder`()
        if (!extraHolder.missileData.isMissile) return false
        val owner = missile.owner
        if (owner !is ServerPlayer) return false

        val data = MissileData(extraData, bulletData, extraHolder)

        if (data.bulletExtraData.missileData.guidanceDelay <= 0)
            missile.entityData.set(EntityKineticBulletShared.IS_MISSILE_TICK_DATA_ACCESSOR, true)

        if (data.dataHolder.needsInit) {
            data.dataHolder.needsInit = false


            if (!data.bulletExtraData.missileData.tvOverlayTexture.isNullOrEmpty()) {
                missile.entityData.set(EntityKineticBulletShared.OVERLAY_TEXTURE_DATA_ACCESSOR, data.bulletExtraData.missileData.tvOverlayTexture)
            }

            missile.entityData.set(EntityKineticBulletShared.MAX_SPEED_DATA_ACCESSOR, data.bulletExtraData.missileData.maxSpeedPerTick)

            missile.entityData.set(EntityKineticBulletShared.TV_ROTATION_CLAMP_DATA_ACCESSOR, data.bulletExtraData.missileData.tvRotationClamp)

            missile.entityData.set(EntityKineticBulletShared.ACCELERATION_LIMIT_DATA_ACCESSOR, data.bulletExtraData.missileData.accelerationLimitPerTick)

            missile.entityData.set(EntityKineticBulletShared.TV_ROTATION_LOCK_DATA_ACCESSOR, data.bulletExtraData.missileData.tvRotationLock)

            missile.entityData.set(EntityKineticBulletShared.TV_ROTATION_CLAMP_MODIFIER_DATA_ACCESSOR, data.bulletExtraData.missileData.tvRotationClampModifier)

            val gunItem = ModItems.MODERN_KINETIC_GUN.get()
            val mainHand = owner.mainHandItem
            if (mainHand.item == gunItem) {
                val typeIndex = GunExtras.getMissileFlightProfileType(mainHand)
                if (typeIndex != null && typeIndex >= 0) {
                    data.dataHolder.flightProfileType = FlightProfileType.entries.getOrNull(typeIndex)
                }

                data.dataHolder.enforcingSimpleLocking = GunExtras.getEnforcingSimpleLocking(mainHand)
            }

            val playerState = PlayerListener.getPlayerStates(owner)
            data.dataHolder.target = playerState.lockingTargetBuffer
            playerState.lockingTargetBuffer = null


        }

        return true
    }

    fun transformLocalInputToWorld(localInput: Vec3, yaw: Float, pitch: Float): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())

        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        val forward = Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch)
        val right = Vec3(cosYaw, 0.0, sinYaw)
        val up = right.cross(forward).normalize()

        return forward.scale(localInput.z)
            .add(right.scale(localInput.x))
            .add(up.scale(localInput.y))
    }

    fun isMissileDrone(missile: EntityKineticBullet): Boolean {
        val extraData = (missile as IAccessorBullet).`taczexpands$getBulletExtraData`()
        val bulletData = extraData.bulletData ?: return false
        val extraHolder = (bulletData as IAccessorBulletData).`taczexpands$getExtraHolder`()
        if (!extraHolder.missileData.isMissile) return false
        return extraHolder.missileData.guidanceType == GuidanceType.DRONE
    }

    fun onMissileBulletTick(missile: EntityKineticBullet): Pair<Boolean, Boolean> {
        if (missile.level().isClientSide) return false to false

        val extraData = (missile as IAccessorBullet).`taczexpands$getBulletExtraData`()
        val bulletData = extraData.bulletData ?: return false to false
        val extraHolder = (bulletData as IAccessorBulletData).`taczexpands$getExtraHolder`()
        if (!extraHolder.missileData.isMissile) return false to false
        val owner = missile.owner
        if (owner !is ServerPlayer) return false to false

        val data = MissileData(extraData, bulletData, extraHolder)

        if (data.dataHolder.isFirstTick) {
            data.dataHolder.isFirstTick = false

            val serverLevel = missile.level() as? ServerLevel ?: return false to false
            try {
                (((serverLevel.chunkSource.chunkMap as IAccessorChunkMap).entityMap.get(missile.id) as IAccessorTrackedEntity).serverEntity as IAccessorServerEntity).setUpdateInterval(
                    1
                )
            } catch (e: Exception) {
                LOGGER.error("SEVERE!!!!!!")
                e.printStackTrace()
            }
        }


        if (missile.tickCount < data.bulletExtraData.missileData.guidanceDelay) return false to false
        if (missile.tickCount > data.bulletExtraData.missileData.guidanceDelay + data.bulletExtraData.missileData.fuel) {
            if (data.bulletExtraData.missileData.guidanceType.isRemoteCamera) {
                missile.entityData.set(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR, missile.deltaMovement.length().toFloat())
            }
            return false to false
        }

        if (data.bulletExtraData.missileData.guidanceType == GuidanceType.NONE) {
            applyPositionGuidance(missile, null, data)
            return true to false
        }

        if (data.bulletExtraData.missileData.guidanceType.isRemoteCamera) {
            val cameraData = tvGuidancePlayers[owner]
            if (cameraData == null) {
                applyPositionGuidance(missile, null, data)
                missile.entityData.set(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR, missile.deltaMovement.length().toFloat())
                return true to false
            }

            if (cameraData.yaw == null || cameraData.pitch == null) {
                cameraData.bullet = missile
                NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.BindCamera, missile.id), owner)

                val velocity = missile.deltaMovement
                if (velocity.lengthSqr() < 0.001) {
                    val lookAngle = missile.getFixedLookAngle()
                    val (yaw, pitch) = vecToYawPitch(lookAngle)
                    cameraData.yaw = yaw
                    cameraData.pitch = pitch
                } else {
                    val (yaw, pitch) = vecToYawPitch(velocity)
                    cameraData.yaw = yaw
                    cameraData.pitch = pitch
                }

                val serverLevel = missile.level() as? ServerLevel ?: return true to false
                try {
                    ((serverLevel.chunkSource.chunkMap as IAccessorChunkMap).entityMap.get(missile.id) as IAccessorTrackedEntity).setRange(32 * 16)
                } catch (e: Exception) {
                    LOGGER.error("SEVERE!!!!!!")
                    e.printStackTrace()
                }
                missile.entityData.set(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR, missile.deltaMovement.length().toFloat())
                return true to false
            }
        }

        if (data.bulletExtraData.missileData.guidanceType == GuidanceType.TV) {

            val cameraData = tvGuidancePlayers[owner] ?: return true to false

            val yaw = cameraData.yaw!!
            val pitch = cameraData.pitch!!




            applyPositionGuidance(missile, null, data, calculateViewVector(pitch, yaw))
            missile.entityData.set(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR, missile.deltaMovement.length().toFloat())


            return true to false
        }

        if (data.bulletExtraData.missileData.guidanceType == GuidanceType.DRONE) {
            val cameraData = tvGuidancePlayers[owner] ?: return true to false

            val yaw = cameraData.yaw!!

            val forwardHorizontal = calculateViewVector(0f, yaw)
            val rightHorizontal = calculateViewVector(0f, yaw - 90f)
            var rawInput = cameraData.input
            if (rawInput.length() < data.bulletExtraData.missileData.minInput) {
                rawInput = Vec3(0.0, 0.0, data.bulletExtraData.missileData.minInput.toDouble())
            }
            val worldInput = forwardHorizontal.scale(rawInput.z)
                .add(rightHorizontal.scale(rawInput.x))
                .add(Vec3(0.0, rawInput.y, 0.0))


            val targetPos = missile.getCenterPosition().add(worldInput.normalize())
            applyPositionGuidance(missile, targetPos, data, isDrone = true)
            missile.entityData.set(EntityKineticBulletShared.TV_SPEED_DATA_ACCESSOR, missile.deltaMovement.length().toFloat())

            return true to false
        }

        if (!data.bulletExtraData.missileData.isSimpleLocking && !data.dataHolder.enforcingSimpleLocking) {
            val playerState = PlayerListener.getPlayerStates(owner)
            data.dataHolder.target = playerState.skillLockingTarget
            data.dataHolder.targetPos = playerState.skillLockingPos
        } else {
            if (data.bulletExtraData.missileData.guidanceType == GuidanceType.SEMI_ACTIVE_LASER) {
                data.dataHolder.target = getPlayerLockingTarget(owner)
                if (data.dataHolder.target == null && owner.mainHandIsMissileGun() && IGunOperator.fromLivingEntity(owner).synIsAiming) {
                    val result = owner.taczPick(data.bulletExtraData.missileData.maxLockingDistance.toDouble(), 1.0f, false)
                    if (result.type != HitResult.Type.MISS) {
                        data.dataHolder.targetPos = result.location
                    } else {
                        data.dataHolder.targetPos = null
                    }
                } else {
                    data.dataHolder.targetPos = null
                }
            } else if (data.bulletExtraData.missileData.guidanceType == GuidanceType.SEMI_ACTIVE_LASER_2) {
                data.dataHolder.target = getPlayerLockingTarget(owner)
                if (data.dataHolder.target == null && owner.mainHandIsMissileGun() && IGunOperator.fromLivingEntity(owner).synIsAiming) {
                    val maxLockingDistance = data.bulletExtraData.missileData.maxLockingDistance.toDouble()
                    val result = owner.taczPick(maxLockingDistance, 1.0f, false)
                    if (result.type != HitResult.Type.MISS) {
                        data.dataHolder.targetPos = result.location
                    } else {
                        val lookVec = owner.lookAngle
                        val eyePos = owner.eyePosition
                        data.dataHolder.targetPos = eyePos.add(
                            lookVec.x * maxLockingDistance,
                            lookVec.y * maxLockingDistance,
                            lookVec.z * maxLockingDistance
                        )
                    }
                } else {
                    data.dataHolder.targetPos = null
                }
            }

            if (data.bulletExtraData.missileData.guidanceType == GuidanceType.ACTIVE_RADAR && data.dataHolder.target == null) {
                if (data.dataHolder.radarCheckTickCount-- <= 0) {
                    data.dataHolder.radarCheckTickCount = ServerConfig.radarCheckDelay.get()
                    val missilePos = missile.getCenterPosition()
                    for (entity in getEntitiesInConeFromCenter(
                        missile,
                        data.bulletExtraData.missileData.maxLockingDistance.toDouble(),
                        data.bulletExtraData.missileData.maxOffAxisAngleOnLocking.toDouble()
                    ).sortedBy { it.getCenterPosition().distanceToSqr(missilePos) }) {
                        val newEntity = if (entity is EnderDragonPart && entity.parentMob != null) entity.parentMob!! else entity
                        if (data.bulletExtraData.missileData.getTargetableEntityPredicator().apply(owner, newEntity)) {
                            data.dataHolder.target = newEntity
                        }
                    }
                }
            }
        }

        var target: Entity? = data.dataHolder.target

        var jammingTarget = data.dataHolder.jammingTarget

        if (jammingTarget == null) {
            if (data.dataHolder.jammingCheckTickCount-- <= 0) {
                data.dataHolder.jammingCheckTickCount = ServerConfig.jammingCheckDelay.get()
                val missilePos = missile.getCenterPosition()
                for (tempEntity in getEntitiesInConeFromCenter(
                    missile,
                    data.bulletExtraData.missileData.maxJammableDistance.toDouble(),
                    data.bulletExtraData.missileData.jammableAngle.toDouble()
                ).sortedBy { it.getCenterPosition().distanceToSqr(missilePos) }) {
                    val entity = if (tempEntity is EnderDragonPart && tempEntity.parentMob != null) tempEntity.parentMob!! else tempEntity

                    for ((predicate, efficiency) in jammingEntities) {
                        if (!predicate.test(entity)) continue
                        if (missile.canSee(entity, data.bulletExtraData.missileData.maxJammableDistance.toDouble())) continue
                        val distanceToTarget = entity.getCenterPosition().subtract(missile.getCenterPosition()).length()
                        val distanceProbability = 1.0 - (distanceToTarget / data.bulletExtraData.missileData.maxJammableDistance).coerceIn(0.0, 1.0)
                        val missileJamProbability = 1.0f - data.bulletExtraData.missileData.guidanceStability.coerceIn(0.0f, 1.0f)
                        val isFullOccluded = if (target != null) isBoundingBoxOccludedBySphere(
                            missilePos,
                            target,
                            entity.getCenterPosition(),
                            efficiency.second
                        )
                        else true

                        val probability = (efficiency.first * (if (isFullOccluded) 2.0 else 1.0) * distanceProbability * missileJamProbability).coerceIn(
                            0.0,
                            1.0
                        )
                        if (Random.nextFloat() < probability) {
                            jammingTarget = entity
                            data.dataHolder.jammingTarget = entity
                            break
                        }

                    }
                }
            }
        }

        if (jammingTarget != null) {
            if (isOutOfMaxGuidanceAngle(missile, jammingTarget, data)) {
                jammingTarget = null
                data.dataHolder.jammingTarget = null
            } else {
                target = jammingTarget
            }
        }

        if (jammingTarget == null && target != null) {
            if (isOutOfMaxGuidanceAngle(missile, target, data)) {
                target = null
            }
        }

        var targetPos: Vec3? = data.dataHolder.targetPos
        if (targetPos != null) {
            if (isOutOfMaxGuidanceAngle(missile, targetPos, data)) {
                targetPos = null
            }
        }

        if (target != null) {
            if (data.bulletExtraData.missileData.proximityFuzeRange > 0) {
                val distanceToTarget = target.getCenterPosition().subtract(missile.getCenterPosition()).length()
                if (distanceToTarget <= data.bulletExtraData.missileData.proximityFuzeRange) {
                    return true to true
                }
            }

            if (data.dataHolder.flightProfileType == null) {
                data.dataHolder.flightProfileType = data.bulletExtraData.missileData.flightProfileType
            }

            when (data.dataHolder.flightProfileType!!) {
                FlightProfileType.PURE_PURSUIT -> {
                    targetPos = target.getCenterPosition()
                }

                FlightProfileType.HEIGHT_OFFSET_PURE_PURSUIT -> {
                    targetPos = target.getCenterPosition().add(0.0, data.bulletExtraData.missileData.heightOffset.toDouble(), 0.0)
                }

                FlightProfileType.PROPORTIONAL_NAVIGATION -> {
                    val targetVelocity = target.deltaMovement
                    val distanceToTarget = target.getCenterPosition().subtract(missile.getCenterPosition()).length()

                    val timeToIntercept = distanceToTarget / missile.deltaMovement.length()

                    val predictedTargetPos = target.getCenterPosition()
                        .add(targetVelocity.scale(timeToIntercept * data.bulletExtraData.missileData.navigationGain))

                    targetPos = predictedTargetPos
                }

                FlightProfileType.TOP_ATTACK -> {
                    val distanceToTarget = target.getCenterPosition().subtract(missile.getCenterPosition()).length()

                    if (!data.dataHolder.isClimbing) {
                        if (distanceToTarget > data.bulletExtraData.missileData.climbDistance) {
                            targetPos = target.getCenterPosition()
                        } else {
                            data.dataHolder.isClimbing = true
                        }
                    }

                    if (data.dataHolder.isClimbing) {
                        if (!data.dataHolder.isStriking) {

                            val missilePos = missile.getCenterPosition()
                            val targetPosOffset = target.getCenterPosition().subtract(missilePos)

                            val angle = getAngleBetweenVectorsRad(targetPosOffset, Vec3(0.0, -1.0, 0.0))
                            if (angle <= degToRad(data.bulletExtraData.missileData.strikeAngle.toDouble())) {
                                data.dataHolder.isStriking = true
                            } else {
                                val ascentAngleRad = degToRad(data.bulletExtraData.missileData.ascentAngle.toDouble())
                                val horizontalVec = Vec3(targetPosOffset.x, 0.0, targetPosOffset.z)
                                val newY = horizontalVec.length() * tan(ascentAngleRad)

                                targetPos = Vec3(targetPosOffset.x, newY, targetPosOffset.z).add(missilePos)
                            }


                        }
                    }

                    if (data.dataHolder.isStriking) {
                        targetPos = target.getCenterPosition()
                    }
                }
            }
        }

        applyPositionGuidance(missile, targetPos, data)
        return true to false
    }

    fun applyPositionGuidance(missile: EntityKineticBullet, target: Vec3?, data: MissileData, direction: Vec3? = null, isDrone: Boolean = false) {
        val missilePos = missile.getCenterPosition()
        val currentVelocity = missile.deltaMovement
        val currentSpeed = currentVelocity.length()

        val directionToTarget = direction ?: if (target != null) {
            target.subtract(missilePos).normalize()
        } else if (currentSpeed > 0.001) {
            currentVelocity.normalize()
        } else {
            missile.getFixedLookAngle()
        }

        var targetSpeed = min(
            currentSpeed + data.bulletExtraData.missileData.thrustPerTick.toDouble(),
            data.bulletExtraData.missileData.maxSpeedPerTick.toDouble()
        )

        if (isDrone && target != null) {
            val distanceToTarget = missilePos.distanceTo(target)
            val slowdownRadius = 1.0
            if (distanceToTarget < slowdownRadius) {
                val factor = distanceToTarget / slowdownRadius
                targetSpeed *= factor
            }
        }

        val desiredVelocity = directionToTarget.scale(targetSpeed)

        if (currentSpeed <= 0.001) {
            missile.deltaMovement = desiredVelocity
            return
        }

        val maxTurnRateRadPerTick = data.bulletExtraData.missileData.accelerationLimitPerTick / currentSpeed

        val turnAngleRad = acos(currentVelocity.normalize().dot(desiredVelocity.normalize()))

        val finalVelocity = if (turnAngleRad > maxTurnRateRadPerTick) {
            val axis = currentVelocity.cross(desiredVelocity).normalize()
            val cosAngle = cos(maxTurnRateRadPerTick)
            val sinAngle = sin(maxTurnRateRadPerTick)

            val limitedNewVelocityX = currentVelocity.x * cosAngle + (axis.y * currentVelocity.z - axis.z * currentVelocity.y) * sinAngle
            val limitedNewVelocityY = currentVelocity.y * cosAngle + (axis.z * currentVelocity.x - axis.x * currentVelocity.z) * sinAngle
            val limitedNewVelocityZ = currentVelocity.z * cosAngle + (axis.x * currentVelocity.y - axis.y * currentVelocity.x) * sinAngle

            Vec3(limitedNewVelocityX, limitedNewVelocityY, limitedNewVelocityZ)
        } else {
            desiredVelocity
        }

        val direction = finalVelocity.normalize()
        if (finalVelocity.lengthSqr() > data.bulletExtraData.missileData.maxSpeedPerTick * data.bulletExtraData.missileData.maxSpeedPerTick) {
            missile.deltaMovement = direction.scale(data.bulletExtraData.missileData.maxSpeedPerTick.toDouble())
        } else {
            missile.deltaMovement = finalVelocity
        }
    }


}