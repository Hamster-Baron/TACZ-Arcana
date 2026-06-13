package group.taczexpands.common.entity

import group.taczexpands.common.TACZExpandsCommon
import net.minecraft.core.Holder
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3


class CustomDisplayEntity(type: EntityType<CustomDisplayEntity>, level: Level) : LivingEntity(type, level) {
    companion object {
        val RENDER_DATA_ACCESSOR = SynchedEntityData.defineId<CompoundTag>(
            CustomDisplayEntity::class.java,
            EntityDataSerializers.COMPOUND_TAG
        )

        var clientTickDelegate: ((CustomDisplayEntity) -> Unit)? = null
        var serverTickDelegate: ((CustomDisplayEntity) -> Unit)? = null

        var clientRemoveDelegate: ((CustomDisplayEntity) -> Unit)? = null
        var serverRemoveDelegate: ((CustomDisplayEntity) -> Unit)? = null

        var clientDeathDelegate: ((CustomDisplayEntity) -> Unit)? = null
        var serverDeathDelegate: ((CustomDisplayEntity) -> Unit)? = null

        var serverBroadcastToPlayerDelegates: ((CustomDisplayEntity, Player) -> Boolean?)? = null
    }

    init {
        noPhysics = true
        isNoGravity = true
        isInvulnerable = true
    }

    var immuneTable: List<String>? = null

    var immuneExceptPlayer = false

    constructor(
        level: Level,
        position: Vec3,
        yaw: Float,
        pitch: Float,
        modelID: String,
        textureID: String,
        animationID: String,
        animationName: String,
        animationDelay: Int,
        deathAnimationName: String?,
        immuneTable: List<String>?,
        immuneExceptPlayer: Boolean
    ) : this(TACZExpandsCommon.ENTITYTYPE_CUSTOM_DISPLAY.get(), level) {
        setPos(position)
        setRot(yaw, pitch)

        this.immuneTable = immuneTable
        this.immuneExceptPlayer = immuneExceptPlayer


        renderData = CompoundTag().apply {
            putString("modelID", modelID)
            putString("textureID", textureID)
            putString("animationID", animationID)
            putString("animationName", animationName)
            putInt("animationDelay", animationDelay)

            if (deathAnimationName != null) {
                putString("deathAnimationName", deathAnimationName)
            }
        }

        refreshDimensions()
    }


    override fun tick() {
        super.tick()

        if (this.level().isClientSide) {
            clientTickDelegate?.invoke(this)
        } else {
            serverTickDelegate?.invoke(this) ?: this.discard()
        }

    }

    override fun getDimensions(pPose: Pose): EntityDimensions {
        if (renderData.contains("dimensionWidth") && renderData.contains("dimensionHeight")) {
            return EntityDimensions.scalable(renderData.getFloat("dimensionWidth"), renderData.getFloat("dimensionHeight"))
        }

        return super.getDimensions(pPose)
    }

    override fun isPickable(): Boolean {
        return renderData.contains("maxHealth")
    }

    override fun isInvulnerableTo(pSource: DamageSource): Boolean {
        if (isInvulnerable) return true
        return super.isInvulnerableTo(pSource)
    }

    override fun hurt(pSource: DamageSource, pAmount: Float): Boolean {
        if (immuneExceptPlayer && pSource.directEntity !is Player && pSource.entity !is Player) {
            return false
        }

        if (immuneTable == null) {
            return super.hurt(pSource, pAmount)
        }

        val typeHolder = pSource.typeHolder()
        if (typeHolder !is Holder.Reference<DamageType>) {
            return super.hurt(pSource, pAmount)
        }

        if (!immuneTable!!.contains(typeHolder.key().registry().toString())) {
            return super.hurt(pSource, pAmount)
        }

        return false
    }

    override fun broadcastToPlayer(pPlayer: ServerPlayer): Boolean {
        if (this.level().isClientSide) {
            return super.broadcastToPlayer(pPlayer)
        }

        return serverBroadcastToPlayerDelegates?.invoke(this, pPlayer) ?: super.broadcastToPlayer(pPlayer)
    }

    override fun handleEntityEvent(pId: Byte) {
        if (pId == 3.toByte()) {
            this.setRemoved(RemovalReason.KILLED)
            return
        }

        super.handleEntityEvent(pId)
    }


    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(RENDER_DATA_ACCESSOR, CompoundTag())
    }

    var renderData: CompoundTag
        get() {
            return this.entityData.get(RENDER_DATA_ACCESSOR)
        }
        set(value) {
            this.entityData.set(RENDER_DATA_ACCESSOR, value, true)
        }

    override fun onSyncedDataUpdated(pKey: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(pKey)

        if (RENDER_DATA_ACCESSOR == pKey) {
            refreshDimensions()
        }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        if (tag.contains("data", Tag.TAG_COMPOUND.toInt())) {
            renderData = tag.getCompound("data")
        }
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        tag.put("data", renderData)
    }

    override fun onRemovedFromWorld() {
        super.onRemovedFromWorld()

        if (this.level().isClientSide) {
            clientRemoveDelegate?.invoke(this)
        } else {
            serverRemoveDelegate?.invoke(this)
        }
    }

    override fun tickDeath() {
        super.tickDeath()

        if (this.level().isClientSide) {
            clientDeathDelegate?.invoke(this)
        } else {
            serverDeathDelegate?.invoke(this)
        }
    }

    override fun getArmorSlots(): Iterable<ItemStack> {
        return emptyList()
    }

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack {
        return ItemStack.EMPTY
    }

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {
    }

    override fun getMainArm(): HumanoidArm {
        return HumanoidArm.RIGHT
    }

    override fun isPushable(): Boolean {
        return false
    }

    override fun canBeCollidedWith(): Boolean {
        return false
    }

    override fun save(pCompound: CompoundTag): Boolean {
        return false
    }

    override fun saveAsPassenger(pCompound: CompoundTag): Boolean {
        return false
    }

    override fun displayFireAnimation(): Boolean {
        return false
    }
}