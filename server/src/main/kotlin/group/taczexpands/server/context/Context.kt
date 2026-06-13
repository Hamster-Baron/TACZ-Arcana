package group.taczexpands.server.context

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.accessor.IAccessorHitVec
import group.taczexpands.server.util.immutable
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.server.ServerLifecycleHooks


open class Context @JvmOverloads constructor(private val _self: ServerPlayer, val from: Event? = null) {
    val serverTick = ServerLifecycleHooks.getCurrentServer().tickCount
    val self: ServerPlayer
        get() {
            return _self
        }

    val target: Entity?
        get() {
            return if (this is HitEntityContext)
                this._target
            else if (this is HookEntityContext)
                this._target
            else null
        }

    val bullet: EntityKineticBullet?
        get() {
            return if (this is BulletContext)
                this._bullet
            else if (this is HitEntityContext)
                this._bullet
            else if (this is HookEntityContext)
                this._bullet
            else if (this is HookBlockContext)
                this._bullet
            else null
        }

    val event: EntityHurtByGunEvent.Pre?
        get() {
            return if (this is HitEntityContext)
                this._event
            else null
        }

    val blockHitResult: BlockHitResult?
        get() {
            return if (this is HitBlockContext)
                this._blockHitResult
            else if (this is HookBlockContext)
                this._blockHitResult
            else null
        }

    val blockPos: BlockPos?
        get() {
            return if (this is HitBlockContext)
                this._blockHitResult.blockPos
            else if (this is HookBlockContext)
                this._blockHitResult.blockPos
            else
                (this.bullet as? IAccessorBullet)?.`taczexpands$getOverridePosition`()
                    ?.let { BlockPos(Mth.floor(it.x), Mth.floor(it.y), Mth.floor(it.z)) } ?: this.bullet?.blockPosition()

        }

    val pos: Vec3?
        get() {
            return if (this is HitBlockContext)
                this._blockHitResult.location
            else if (this is HookBlockContext)
                this._blockHitResult.location
            else
                (this.bullet as? IAccessorBullet)?.`taczexpands$getOverridePosition`() ?: this.bullet?.position()

        }

    val signal: String?
        get() {
            return if (this is SignalContext)
                this._signal
            else null
        }

    val zoom: Int?
        get() {
            return if (this is ZoomContext)
                this._zoom
            else null
        }

    val scrollDelta: Int?
        get() {
            return if (this is ScrollContext)
                this._scrollDelta
            else null
        }

    var isBlocked: Boolean? = null

    var blockingFactor: Float = 0.0f
}

class AttachmentChangeContext(self: ServerPlayer, val _prevAttachmentId: String?, val _nowAttachmentId: String?) : Context(self) {}

open class ScrollContext(self: ServerPlayer, val _scrollDelta: Int) : Context(self) {}

open class ZoomContext(self: ServerPlayer, val _zoom: Int) : Context(self) {}
open class SignalContext(self: ServerPlayer, val _signal: String) : Context(self) {}

open class BulletContext(self: ServerPlayer, val _bullet: EntityKineticBullet) : Context(self) {}

open class HitEntityContext(
    self: ServerPlayer, val _target: Entity, val _bullet: EntityKineticBullet?, val _event: EntityHurtByGunEvent.Pre
) : Context(self) {
    val oldDamage = _event.baseAmount
}


class HitByTargetContext(
    self: ServerPlayer, target: Entity, bullet: EntityKineticBullet, event: EntityHurtByGunEvent.Pre
) : HitEntityContext(self, target, bullet, event) {
}


class HitBlockContext(self: ServerPlayer, _bullet: EntityKineticBullet, blockHitResult: BlockHitResult) : BulletContext(
    self, _bullet
) {
    val _blockHitResult = blockHitResult.immutable()
}

class HookEntityContext(self: ServerPlayer, val _target: Entity, val _bullet: EntityKineticBullet?) : Context(self) {}

class HookBlockContext(self: ServerPlayer, blockHitResult: BlockHitResult, val _bullet: EntityKineticBullet?) : Context(self) {
    val _blockHitResult = blockHitResult.immutable()
}