package group.taczexpands.server.listener

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.entity.IGunOperator
import com.tacz.guns.api.entity.ReloadState
import com.tacz.guns.api.event.common.*
import com.tacz.guns.api.event.server.AmmoHitBlockEvent
import com.tacz.guns.api.item.gun.FireMode
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.init.ModItems
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.common.network.v2.s2c.S2CConfig
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.accessor.IAccessorLivingEntity
import group.taczexpands.server.bullet.CameraData
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.config.GLOBAL_VOLATILE_VARIABLES
import group.taczexpands.server.config.ReverseTriggerCondition
import group.taczexpands.server.config.ServerConfig
import group.taczexpands.server.context.*
import group.taczexpands.server.event.*
import group.taczexpands.server.module.gun_durability.GunDurabilityManager
import group.taczexpands.server.module.gun_shield.GunShieldManager
import group.taczexpands.server.module.util_v2.getGun
import group.taczexpands.server.nbt.DataStorage
import group.taczexpands.server.nbt.DataType
import group.taczexpands.server.nbt.PlayerExtrasServer
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.ActionManager
import group.taczexpands.server.skill.GLOBAL_COOLDOWN_GROUP_MAP
import group.taczexpands.server.skill.SkillManager
import group.taczexpands.server.skill.TriggerType
import group.taczexpands.server.util.GunManager
import group.taczexpands.server.util.equalsLore
import group.taczexpands.server.util.getCenterPosition
import net.minecraft.core.Holder
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.PlayerTickEvent
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDamageEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.jvm.optionals.getOrNull

object PlayerListener {
    data class ClientCamera(var yaw: Float, var pitch: Float)
    class PlayerState(
        val player: ServerPlayer,
        var lastPos: Vec3,
        var moving: Boolean,
        var lastSprinting: Boolean,
        var lastMoving: Boolean,
        var lastJumping: Boolean,
        var lastCrouching: Boolean,
        var lastAiming: Boolean,
        var lastReloading: Boolean,
        var lastReloadingItem: ItemStack,
        var lastMainItem: ItemStack,
        var lastOffItem: ItemStack,
        var lastHotbar: NonNullList<ItemStack>,
        var shootKeyDown: Boolean,
        var lastShooting: Boolean,
        var lastShootKeyDown: Boolean = false,
        var skillLockingTarget: Entity? = null,
        var skillLockingPos: Vec3? = null,
        var lockingTargetBuffer: Entity? = null,
        var clientCamera: ClientCamera? = null,
        var scriptStoredTarget: Entity? = null,
        var chargeProgress: Float = 0f,
        var lastChargeProgress: Float = 0f,

        ) {


        var forceShoot = false

        fun onLockingUpdate() {
            if (lockingTarget == null) return

            val gunItem = ModItems.MODERN_KINETIC_GUN.get()
            val mainHand = player.mainHandItem
            if (mainHand.item != gunItem) {
                return
            }
            if (!IGunOperator.fromLivingEntity(player).synIsAiming) {
                return
            }

            val gunID = gunItem.getGunId(mainHand)

            val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
            if (gunIndex == null) {
                return
            }

            val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)
            val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

            if (extraBulletData.missileData.isMissile) {
                if (lockingTime == extraBulletData.missileData.lockingTime) {
                    SkillManager.trigger(TriggerType.ON_COMPLETE_LOCKING_TARGET, Context(player))
                }
            }
        }

        fun sendCurrentLockingTime() {
            NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.BindCurrentLockingTime, lockingTime), player)
        }

        var currentClientBindingTarget: Int = 0
            get() = field
            set(value) {
                if (field != value) {
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.BindTarget, value), player)
                }
                field = value
            }

        var currentClientBindingLockingTime: Int = 0
            get() = field
            set(value) {
                if (field != value) {
                    NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.BindLockingTime, value), player)
                }
                field = value
            }

        var lockingTime: Int = 0
        var lockingTarget: Entity? = null
            get() = field
            set(value) {
                if (field != value) {
                    lockingTime = 0
                } else {
                    lockingTime++
                }
                field = value
                onLockingUpdate()
            }
        var lockingRemainingTime: Int = 0
    }

    val playerStates = mutableMapOf<Player, PlayerState>()

    fun getPlayerStates(player: ServerPlayer): PlayerState {
        if (!playerStates.containsKey(player)) {
            playerStates[player] = PlayerState(
                player,
                player.position(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                ItemStack.EMPTY,
                NonNullList.withSize(9, ItemStack.EMPTY),
                false,
                false
            )
        }
        return playerStates[player]!!
    }

    @SubscribeEvent
    fun onPlayerLogin(event: PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return

        NetworkManager.sendToPlayer(S2CConfig(ServerConfig.defaultBlockAimWhileReloading.get()).create(), player)

        try {
            val scoreboard = ServerLifecycleHooks.getCurrentServer().scoreboard
            GLOBAL_VOLATILE_VARIABLES.variables.forEach {
                val objective = scoreboard.getObjective("tacz_$it") ?: return@forEach
                scoreboard.resetPlayerScore(event.entity.scoreboardName, objective)

                val storage = DataStorage.get()
                storage.getScore(DataType.FLOAT, it)?.removeValue(player)
                storage.getScore(DataType.STRING, it)?.removeValue(player)
            }

            PlayerExtrasServer.notify(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val gunContext = getGun(player)
            if (gunContext != null) {
                GunExtras.checkUUID(gunContext.gunItemStack)
            }

            SkillManager.trigger(TriggerType.ON_MAIN_HAND, Context(player))
            GunDurabilityManager.updateRemove(player)
            SkillManager.trigger(TriggerType.ON_OFF_HAND, Context(player))

            val state = getPlayerStates(player)
            state.lastMainItem = player.mainHandItem.copy()
            state.lastOffItem = player.offhandItem.copy()
            (0..8).forEach {
                state.lastHotbar[it] = player.inventory.getItem(it)
                SkillManager.trigger(TriggerType.ON_HOT_BAR, Context(player), it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onPlayerLogout(event: PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return

        try {
            SkillManager.REVERSE_CHECK_MAP.remove(player)?.let {
                it.forEach {
                    if (!it.skillMagicNumPair.skill.config.has(ReverseTriggerCondition.BYPASS_LEAVE) && !it.failed) {
                        it.skillMagicNumPair.skill.triggerReverseAction(it.context)
                    }
                }
            }

            SkillManager.QUIT_TRIGGER_MAP.remove(player)?.let {
                it.forEach {
                    if (!it.skillMagicNumPair.skill.config.has(ReverseTriggerCondition.BYPASS_LEAVE) && !it.failed) {
                        it.skillMagicNumPair.skill.triggerReverseAction(it.context)
                    }
                }
            }
            SkillManager.KEEP_TRIGGER_MAP.remove(player)
            SkillManager.CANCEL_SKILL_MAP.remove(player)
            ActionManager.STATUS_MAP.remove(player)
            playerStates.remove(player)
            SkillManager.LOADED_SKILLS.flatMap { it.value }.forEach {
                it.coolDownMap.remove(player)
            }
            GLOBAL_COOLDOWN_GROUP_MAP.entries.removeIf { it.key.first == player }
            ActionManager.reset(player)
            MissileManager.tvGuidancePlayers.remove(player)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val scoreboard = ServerLifecycleHooks.getCurrentServer().scoreboard
            GLOBAL_VOLATILE_VARIABLES.variables.forEach {
                val objective = scoreboard.getObjective("tacz_$it") ?: return@forEach
                scoreboard.resetPlayerScore(event.entity.scoreboardName, objective)

                val storage = DataStorage.get()
                storage.getScore(DataType.FLOAT, it)?.removeValue(player)
                storage.getScore(DataType.STRING, it)?.removeValue(player)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @SubscribeEvent
    fun onPlayerHurt(event: LivingAttackEvent) {
        val entity = event.entity
        val sourceHolder = event.source.typeHolder()
        val isTACZ = if (sourceHolder is Holder.Reference<*>) {
            sourceHolder.key().location().namespace == "tacz"
        } else false

        val isBlocked = !isTACZ && GunShieldManager.shouldBlock(entity, event.source.sourcePosition)

        val initialBlockingFactor = if (isBlocked) 1.0f else 0.0f

        var shouldBlock = isBlocked

        var blockingFactor = initialBlockingFactor

        if (shouldBlock) {
            if (entity is ServerPlayer) {
                val context = Context(entity, event)
                context.isBlocked = shouldBlock
                context.blockingFactor = blockingFactor
                SkillManager.trigger(TriggerType.ON_SHIELD_BLOCK_VANILLA, context)
                shouldBlock = context.isBlocked == true
                blockingFactor = context.blockingFactor!!
            }
        }

        if (entity is ServerPlayer) {
            val context = Context(entity, event)
            context.isBlocked = shouldBlock
            context.blockingFactor = blockingFactor
            SkillManager.trigger(TriggerType.ON_HURT, context)
            shouldBlock = context.isBlocked == true
            blockingFactor = context.blockingFactor!!
        }

        if (shouldBlock) {
            if (blockingFactor >= 1.0f) {
                event.isCanceled = true
            }


            GunShieldManager.dispatchBlock(entity, event.amount, blockingFactor)
        }

        if (isBlocked && !event.source.`is`(DamageTypeTags.IS_PROJECTILE)) {
            val attacker = event.source.directEntity as? LivingEntity ?: return
            val isAxe = attacker.mainHandItem.item is AxeItem
            if (isAxe) {
                GunShieldManager.attackedByAxe(entity)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerDeath(event: LivingDeathEvent) {
        try {
            val player = event.entity as? ServerPlayer ?: return
            SkillManager.trigger(TriggerType.ON_DEATH, Context(player, event))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent) {
        try {
            if (event.phase != TickEvent.Phase.END) return

            val player = event.player
            if (player !is ServerPlayer) return

            val playerState = getPlayerStates(player)

            val chargeProgress = playerState.chargeProgress
            val lastChargeProgress = playerState.lastChargeProgress

            if (chargeProgress > 0.0f && lastChargeProgress <= 0.0f) {
                SkillManager.trigger(TriggerType.ON_CHARGE, Context(player))
            } else if (chargeProgress <= 0.0f && lastChargeProgress > 0.0f) {
                SkillManager.triggerReverse(TriggerType.ON_CHARGE, Context(player))
            }
            playerState.lastChargeProgress = chargeProgress


            val crouching = player.isCrouching
            val lastCrouching = playerState.lastCrouching
            if (crouching && !lastCrouching) {
                SkillManager.triggerReverse(TriggerType.ON_STAND, Context(player))
                SkillManager.trigger(TriggerType.ON_CROUCH, Context(player))
                playerState.lastCrouching = true
            } else if (!crouching && lastCrouching) {
                SkillManager.triggerReverse(TriggerType.ON_CROUCH, Context(player))
                SkillManager.trigger(TriggerType.ON_STAND, Context(player))
                playerState.lastCrouching = false
            }


            val lastSprinting = playerState.lastSprinting
            val lastMoving = playerState.lastMoving
            val lastPos = playerState.lastPos
            val lastJumping = playerState.lastJumping

            var sprinting = player.isSprinting
            val pos = player.position()
            val moving = pos.distanceToSqr(lastPos) > 0.01
            val jumping = (player as IAccessorLivingEntity).`taczexpands$isJumping`()

            if (jumping && !lastJumping) {
                SkillManager.trigger(TriggerType.ON_JUMP, Context(player))
            }
            playerState.lastJumping = jumping

            playerState.moving = moving

            if (sprinting && ActionManager.shouldCancel(player, S2CCancelAction.Action.Sprint)) {
                sprinting = false
                player.isSprinting = false
            }

            if (sprinting && !lastSprinting) {
                SkillManager.triggerReverse(TriggerType.ON_STOP_SPRINT, Context(player))
                SkillManager.trigger(TriggerType.ON_SPRINT, Context(player))
            } else if (!sprinting && lastSprinting) {
                SkillManager.triggerReverse(TriggerType.ON_SPRINT, Context(player))
                SkillManager.trigger(TriggerType.ON_STOP_SPRINT, Context(player))
            }

            if (moving && !lastMoving) {
                SkillManager.triggerReverse(TriggerType.ON_STOP_MOVE, Context(player))
                SkillManager.trigger(TriggerType.ON_MOVE, Context(player))
            } else if (!moving && lastMoving) {
                SkillManager.triggerReverse(TriggerType.ON_MOVE, Context(player))
                SkillManager.trigger(TriggerType.ON_STOP_MOVE, Context(player))
            }

            playerState.lastSprinting = sprinting
            playerState.lastMoving = moving
            playerState.lastPos = Vec3(pos.x, pos.y, pos.z)

            val gunOperator = IGunOperator.fromLivingEntity(player)
            val aiming = gunOperator.synIsAiming
            val lastAiming = playerState.lastAiming
            if (aiming && !lastAiming) {
                SkillManager.trigger(TriggerType.ON_AIM, Context(player))
                playerState.lastAiming = true
            } else if (!aiming && lastAiming) {
                SkillManager.triggerReverse(TriggerType.ON_AIM, Context(player))
                playerState.lastAiming = false
            }

            val lastLocking = playerState.lockingTarget

            val gunItem = ModItems.MODERN_KINETIC_GUN.get()

            if (aiming) {
                MissileManager.updatePlayerTarget(player, playerState)
            } else {
                run {
                    val mainHand = player.mainHandItem
                    if (mainHand.item != gunItem) {
                        playerState.lockingTarget = null
                        return@run
                    }

                    val gunID = gunItem.getGunId(mainHand)

                    val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull()
                    if (gunIndex == null) {
                        playerState.lockingTarget = null
                        return@run
                    }

                    val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)

                    val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)

                    if (extraBulletData.missileData.isMissile) {
                        if (playerState.lockingRemainingTime >= extraBulletData.missileData.lockingRemainingTime) {
                            playerState.lockingTarget = null
                        } else {
                            playerState.lockingTarget = playerState.lockingTarget
                        }
                        playerState.lockingRemainingTime++
                    }
                }
            }

            if (lastLocking != playerState.lockingTarget) {
                SkillManager.triggerReverse(TriggerType.ON_LOCKING_TARGET, Context(player))
                if (playerState.lockingTarget != null) {
                    SkillManager.trigger(TriggerType.ON_LOCKING_TARGET, Context(player))
                }
            }

            MissileManager.updatePlayerFireStatus(player, playerState)
            MissileManager.updateClientLockingStatus(player, playerState)


            val currentMainItem = player.mainHandItem

            val gunContext = getGun(currentMainItem)
            if (gunContext != null) {
                GunExtras.checkUUID(currentMainItem)
            }

            val lastReloadingItem = playerState.lastReloadingItem

            val reloading = gunOperator.synReloadState.stateType != ReloadState.StateType.NOT_RELOADING

            val lastReloading = playerState.lastReloading
            if (!reloading && lastReloading) {
                if (currentMainItem.item == gunItem && lastReloadingItem.item == gunItem
                    && gunItem.getGunId(currentMainItem) == gunItem.getGunId(lastReloadingItem)
                    && currentMainItem.equalsLore(lastReloadingItem)
                ) {
                    SkillManager.trigger(TriggerType.ON_POST_RELOAD, Context(player))
                }
                playerState.lastReloading = false
            } else if (reloading && !lastReloading) {
                playerState.lastReloadingItem = currentMainItem.copy()
                playerState.lastReloading = true
            }

            val lastMainItem = playerState.lastMainItem

            if (currentMainItem.item == gunItem) {
                if (lastMainItem.item != gunItem
                    || gunItem.getGunId(lastMainItem) != gunItem.getGunId(currentMainItem)
                    || !lastMainItem.equalsLore(currentMainItem)
                    || GunExtras.getUUID(lastMainItem) != GunExtras.getUUID(currentMainItem)
                ) {
                    SkillManager.triggerReverse(TriggerType.ON_MAIN_HAND, Context(player))
                    SkillManager.trigger(TriggerType.ON_MAIN_HAND, Context(player))

                    GunDurabilityManager.updateRemove(player)

                    playerState.lastMainItem = currentMainItem.copy()

                    if (playerState.lastShooting) {
                        SkillManager.triggerReverse(TriggerType.ON_AUTO_SHOOT, Context(player))
                        playerState.lastShooting = false
                    }
                }
            } else if (lastMainItem.item == gunItem) {
                SkillManager.triggerReverse(TriggerType.ON_MAIN_HAND, Context(player))
                playerState.lastMainItem = currentMainItem.copy()

                if (playerState.lastShooting) {
                    SkillManager.triggerReverse(TriggerType.ON_AUTO_SHOOT, Context(player))
                    playerState.lastShooting = false
                }
            }

            val currentOffItem = player.offhandItem

            val lastOffItem = playerState.lastOffItem

            if (currentOffItem.item == gunItem) {
                if (lastOffItem.item != gunItem
                    || gunItem.getGunId(lastOffItem) != gunItem.getGunId(currentOffItem)
                    || !lastOffItem.equalsLore(currentOffItem)
                ) {
                    SkillManager.triggerReverse(TriggerType.ON_OFF_HAND, Context(player))
                    SkillManager.trigger(TriggerType.ON_OFF_HAND, Context(player))
                    playerState.lastOffItem = currentOffItem.copy()
                }
            } else if (lastOffItem.item == gunItem) {
                SkillManager.triggerReverse(TriggerType.ON_OFF_HAND, Context(player))
                playerState.lastOffItem = currentOffItem.copy()
            }

            (0..8).forEach {
                val currentItem = player.inventory.getItem(it)
                val lastItem = playerState.lastHotbar[it]

                if (currentItem.item == gunItem) {
                    if (lastItem.item != gunItem
                        || gunItem.getGunId(lastItem) != gunItem.getGunId(currentItem)
                        || !lastItem.equalsLore(currentItem)
                    ) {
                        SkillManager.triggerReverse(TriggerType.ON_HOT_BAR, Context(player), it)
                        SkillManager.trigger(TriggerType.ON_HOT_BAR, Context(player), it)
                        playerState.lastHotbar[it] = currentItem.copy()
                    }
                } else if (lastItem.item == gunItem) {
                    SkillManager.triggerReverse(TriggerType.ON_HOT_BAR, Context(player), it)
                    playerState.lastHotbar[it] = currentItem.copy()
                }
            }

            if (currentMainItem.item == gunItem && gunItem.getFireMode(currentMainItem) == FireMode.AUTO) {
                if (playerState.shootKeyDown && !reloading && !playerState.lastShooting) {
                    SkillManager.trigger(TriggerType.ON_AUTO_SHOOT, Context(player))
                    playerState.lastShooting = true
                }

                if (playerState.lastShooting && (reloading || !playerState.shootKeyDown)) {
                    SkillManager.triggerReverse(TriggerType.ON_AUTO_SHOOT, Context(player))
                    playerState.lastShooting = false
                }

            } else if (playerState.lastShooting) {
                SkillManager.triggerReverse(TriggerType.ON_AUTO_SHOOT, Context(player))
                playerState.lastShooting = false
            }

            if (playerState.shootKeyDown && !playerState.lastShootKeyDown) {
                SkillManager.trigger(TriggerType.ON_CLICK, Context(player))
            } else if (!playerState.shootKeyDown && playerState.lastShootKeyDown) {
                SkillManager.triggerReverse(TriggerType.ON_CLICK, Context(player))
            }

            playerState.lastShootKeyDown = playerState.shootKeyDown


            SkillManager.trigger(TriggerType.ON_PLAYER_TICK, Context(player))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onCrawlChange(event: LivingEntityCrawlSetEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.isCrawling) {
            SkillManager.trigger(TriggerType.ON_CRAWL, Context(player))
        } else {
            SkillManager.triggerReverse(TriggerType.ON_CRAWL, Context(player))
        }
    }

    @SubscribeEvent
    fun onZoomChange(event: LivingEntityZoomSetEvent) {
        val player = event.entity as? ServerPlayer ?: return

        val shouldCancel = ActionManager.shouldCancel(player, S2CCancelAction.Action.Zoom)
        if (shouldCancel) {
            event.isCanceled = true
            return
        }
        SkillManager.trigger(TriggerType.ON_ZOOM_CHANGE, ZoomContext(player, event.zoomNumber))
        if (ActionManager.shouldCancel(player, S2CCancelAction.Action.Zoom)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPlayerScroll(event: PlayerMouseScrollEvent) {
        val player = event.player
        SkillManager.trigger(TriggerType.ON_WHEEL_SCROLL, ScrollContext(player, event.delta))
    }

    @SubscribeEvent
    fun onGunShoot(event: GunShootEvent) {
        try {
            val player = event.shooter
            if (player !is ServerPlayer) return
            val playerState = getPlayerStates(player)
            if (!playerState.forceShoot && ActionManager.shouldCancel(player, S2CCancelAction.Action.Fire)) {
                event.isCanceled = true
                return
            }
            SkillManager.trigger(TriggerType.ON_SHOOT, Context(player))
            if (!playerState.forceShoot && ActionManager.shouldCancel(player, S2CCancelAction.Action.Fire)) {
                event.isCanceled = true
                return
            }
            playerState.forceShoot = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletHitBlock(event: AmmoHitBlockEvent) {
        try {
            val bullet = event.ammo
            val player = bullet.owner
            if (player !is ServerPlayer) return
            SkillManager.trigger(TriggerType.ON_HIT_BLOCK, HitBlockContext(player, bullet, event.hitResult))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletHitEntity(event: EntityHurtByGunEvent.Pre) {
        try {
            val attacker = event.attacker ?: return
            val hurtEntity = event.hurtEntity ?: return
            val bullet = event.bullet as EntityKineticBullet

            val isBlocked = if (hurtEntity is LivingEntity) {
                GunShieldManager.shouldBlock(hurtEntity, event.bullet.getCenterPosition(), IAccessorBullet.getPenetration(bullet))
            } else false

            val initialBlockingFactor = if (isBlocked && hurtEntity is LivingEntity) GunShieldManager.getBlockingFactor(hurtEntity) else 0.0f

            var shouldBlock = isBlocked

            var blockingFactor = initialBlockingFactor

            if (attacker is ServerPlayer) {
                val context =
                    HitEntityContext(
                        attacker,
                        hurtEntity,
                        event.bullet as EntityKineticBullet,
                        event
                    )

                context.isBlocked = shouldBlock
                context.blockingFactor = blockingFactor
                SkillManager.trigger(TriggerType.ON_HIT_ENTITY, context)
                shouldBlock = context.isBlocked == true
                blockingFactor = context.blockingFactor

            }

            if (hurtEntity is ServerPlayer) {
                val context =
                    HitByTargetContext(
                        hurtEntity,
                        attacker,
                        event.bullet as EntityKineticBullet,
                        event
                    )
                context.isBlocked = shouldBlock
                context.blockingFactor = blockingFactor
                SkillManager.trigger(TriggerType.ON_HIT_BY_TARGET, context)
                shouldBlock = context.isBlocked == true
                blockingFactor = context.blockingFactor
            }

            if (isBlocked && hurtEntity is LivingEntity) {
                if (hurtEntity is ServerPlayer) {
                    val context = HitByTargetContext(hurtEntity, attacker, event.bullet as EntityKineticBullet, event)
                    context.isBlocked = shouldBlock
                    SkillManager.trigger(TriggerType.ON_SHIELD_BLOCK_BULLET, context)
                    shouldBlock = context.isBlocked == true
                }

                if (shouldBlock) {
                    if (blockingFactor >= 1.0f) {
                        event.isCanceled = true
                    } else {
                        event.baseAmount *= (1.0f - blockingFactor)
                    }

                    GunShieldManager.dispatchBlock(hurtEntity, event.baseAmount, blockingFactor)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletExplosionHitEntity(baseEvent: BulletExplosionHurtEvent) {
        try {
            val event = baseEvent.event
            val attacker = event.attacker ?: return
            val hurtEntity = event.hurtEntity ?: return

            if (attacker is ServerPlayer) {
                val context =
                    HitEntityContext(
                        attacker,
                        hurtEntity,
                        event.bullet as EntityKineticBullet,
                        event
                    )
                SkillManager.trigger(TriggerType.ON_BULLET_EXPLOSION_HIT_ENTITY, context)
            }

            if (event.isCanceled)
                baseEvent.isCanceled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletExplosionKillEntity(baseEvent: BulletExplosionKillEvent) {
        try {
            val event = baseEvent.event
            val attacker = event.attacker ?: return
            val hurtEntity = event.hurtEntity ?: return

            if (attacker is ServerPlayer) {
                val context =
                    HitEntityContext(
                        attacker,
                        hurtEntity,
                        event.bullet as EntityKineticBullet,
                        event
                    )
                SkillManager.trigger(TriggerType.ON_BULLET_EXPLOSION_KILL_ENTITY, context)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletKillEntity(event: EntityKillByGunEvent) {
        try {
            val attacker = event.attacker ?: return
            val hurtEntity = event.killedEntity ?: return

            if (attacker is ServerPlayer) {
                val context =
                    HitEntityContext(
                        attacker,
                        hurtEntity,
                        event.bullet as EntityKineticBullet,
                        EntityHurtByGunEvent.Pre(
                            event.bullet,
                            hurtEntity,
                            attacker,
                            event.gunId,
                            event.gunDisplayId,
                            event.baseDamage,
                            null,
                            event.isHeadShot,
                            event.headshotMultiplier,
                            event.logicalSide
                        )
                    )
                SkillManager.trigger(TriggerType.ON_KILL_ENTITY, context)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onReload(event: GunReloadEvent) {
        try {
            val player = event.entity as? ServerPlayer ?: return
            SkillManager.trigger(TriggerType.ON_PRE_RELOAD, Context(player))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onMelee(event: GunMeleeEvent) {
        try {
            val player = event.shooter as? ServerPlayer ?: return
            SkillManager.trigger(TriggerType.ON_MELEE_ATTACK, Context(player))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onMeleeHit(event: MeleeHitEvent) {
        try {
            val player = event.user as? ServerPlayer ?: return
            val hurtEvent = EntityHurtByGunEvent.Pre(
                null,
                event.target,
                player,
                null,
                null,
                event.damage,
                null,
                false,
                1.0f,
                LogicalSide.SERVER
            )
            SkillManager.trigger(TriggerType.ON_MELEE_HIT, HitEntityContext(player, event.target, null, hurtEvent))
            event.damage = hurtEvent.baseAmount
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onMeleeKill(event: MeleeKillEvent) {
        try {
            val player = event.user as? ServerPlayer ?: return
            val hurtEvent = EntityHurtByGunEvent.Pre(
                null,
                event.target,
                player,
                null,
                null,
                event.damage,
                null,
                false,
                1.0f,
                LogicalSide.SERVER
            )
            SkillManager.trigger(TriggerType.ON_MELEE_KILL, HitEntityContext(player, event.target, null, hurtEvent))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletDiscard(event: BulletDiscardEvent) {
        try {

            val player = event.bullet.owner as? ServerPlayer ?: return

            SkillManager.trigger(
                TriggerType.ON_BULLET_DISCARD,
                BulletContext(player, event.bullet)
            )

            val bulletData = (event.bullet as IAccessorBullet).`taczexpands$getBulletExtraData`().bulletData ?: return
            if (IAccessorBulletData.getBulletExtraHolder(bulletData).missileData.guidanceType.isRemoteCamera) {
                MissileManager.tvGuidancePlayers.remove(player)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletPenetrate(event: BulletPenetrateEvent) {
        try {
            val player = event.bullet.owner as? ServerPlayer ?: return

            SkillManager.trigger(
                TriggerType.ON_BULLET_PENETRATE,
                BulletContext(player, event.bullet)
            )
        } catch (e: Exception) {
        }
    }

    @SubscribeEvent
    fun onSwitchFireMode(event: GunFireSelectEvent) {
        try {
            val player = event.shooter as? ServerPlayer ?: return
            SkillManager.trigger(TriggerType.ON_SWITCH_FIREMODE, Context(player))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onFire(event: GunFireEvent) {
        try {
            val player = event.shooter as? ServerPlayer ?: return
            val mainHand = event.gunItemStack
            val gunItem = ModItems.MODERN_KINETIC_GUN.get()
            if (mainHand.item != gunItem) return
            val gunID = gunItem.getGunId(mainHand)
            val gunIndex = TimelessAPI.getCommonGunIndex(gunID).getOrNull() ?: return
            val bulletData = IAccessorGunData.getCurrentBulletData(gunIndex.gunData, mainHand)
            val extraBulletData = IAccessorBulletData.getBulletExtraHolder(bulletData)
            if (!extraBulletData.missileData.isMissile) return

            if (extraBulletData.missileData.guidanceType.isRemoteCamera) {
                val cameraData = MissileManager.tvGuidancePlayers[player]
                if (cameraData != null) {
                    if (cameraData.bullet == null || !cameraData.bullet!!.isRemoved) {
                        event.isCanceled = true
                        return
                    } else {
                        MissileManager.tvGuidancePlayers.remove(player)
                    }
                }
            }

            val playerState = getPlayerStates(player)
            if (!extraBulletData.missileData.isSimpleLocking && !GunExtras.getEnforcingSimpleLocking(mainHand)) {
                playerState.let { it.lockingTargetBuffer = it.skillLockingTarget }
            } else {
                playerState.lockingTargetBuffer = MissileManager.getPlayerLockingTarget(player, true)
            }

            if (playerState.lockingTargetBuffer == null) {
                playerState.lockingTargetBuffer = playerState.scriptStoredTarget
            }

            if (extraBulletData.missileData.mustLocking && playerState.lockingTargetBuffer == null) {
                event.isCanceled = true
            }

            if (!event.isCanceled && extraBulletData.missileData.guidanceType.isRemoteCamera) {
                MissileManager.tvGuidancePlayers[player] = CameraData()
            }

            playerState.scriptStoredTarget = null

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onBulletSpawn(event: BulletSpawnEvent) {
        try {
            val player = event.bullet.owner as? ServerPlayer ?: return
            SkillManager.trigger(TriggerType.ON_BULLET_SPAWN, BulletContext(player, event.bullet))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onEntityRemoveTracking(event: PlayerEvent.StopTracking) {
        try {
            val player = event.entity as? ServerPlayer ?: return
            val bullet = event.target as? EntityKineticBullet ?: return
            if (bullet.owner != player) return
            val bulletData = (bullet as IAccessorBullet).`taczexpands$getBulletExtraData`().bulletData ?: return
            if (IAccessorBulletData.getBulletExtraHolder(bulletData).missileData.guidanceType.isRemoteCamera) {
                MissileManager.tvGuidancePlayers.remove(player)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}