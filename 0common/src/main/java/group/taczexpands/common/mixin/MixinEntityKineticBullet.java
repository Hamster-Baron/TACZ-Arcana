package group.taczexpands.common.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.ExplodeUtil;
import group.taczexpands.common.accessor.IAccessorBullet;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.data.HookData;
import group.taczexpands.common.entity.EntityKineticBulletShared;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityKineticBullet.class)
public abstract class MixinEntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData, IAccessorBullet {

    @Unique
    private boolean taczexpands$isFirstTick = true;

    @Unique
    private boolean taczexpands$isSecondTick = true;

    @Unique
    private BulletData taczexpands$bulletDataForPredicate = null;

    @Shadow
    private int life;

    @Shadow
    private boolean explosion;

    @Shadow
    private float explosionDamage;

    @Shadow
    private float explosionRadius;

    @Shadow
    private boolean explosionKnockback;

    @Shadow
    private boolean explosionDestroyBlock;

    @Unique
    private Double taczexpands$xoCache = null;

    @Unique
    private Double taczexpands$yoCache = null;

    @Unique
    private Double taczexpands$zoCache = null;

    @Unique
    private Double taczexpands$xOldCache = null;

    @Unique
    private Double taczexpands$yOldCache = null;

    @Unique
    private Double taczexpands$zOldCache = null;

    @Unique
    private boolean taczexpands$isHook = false;

    @Unique
    private HookData taczexpands$hookData = null;

    @Shadow
    protected abstract void onBulletTick();

    protected MixinEntityKineticBullet(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    @Inject(method = "writeSpawnData", at = @At("TAIL"), remap = false)
    public void postWriteSpawnData(FriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeBoolean(taczexpands$isHook);
        if (taczexpands$isHook) {
            IAccessorBulletData.getBulletExtraHolder(taczexpands$bulletDataForPredicate).hookData.serialize(buffer);
        }
    }

    @Inject(method = "readSpawnData", at = @At("TAIL"), remap = false)
    public void postReadSpawnData(FriendlyByteBuf buffer, CallbackInfo ci) {
        taczexpands$isHook = buffer.readBoolean();
        if (taczexpands$isHook) {
            taczexpands$hookData = HookData.deserialize(buffer);
        }
    }

    @Override
    public HookData taczexpands$getHookData() {
        return taczexpands$hookData;
    }

    @Override
    public boolean taczexpands$isHook() {
        return taczexpands$isHook;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getIS_MISSILE_TICK_DATA_ACCESSOR(), false);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getOVERLAY_TEXTURE_DATA_ACCESSOR(), "");
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getTV_SPEED_DATA_ACCESSOR(), 0.0f);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getMAX_SPEED_DATA_ACCESSOR(), 10.0f);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getACCELERATION_LIMIT_DATA_ACCESSOR(), 0.0f);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getTV_ROTATION_CLAMP_DATA_ACCESSOR(), false);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getTV_ROTATION_LOCK_DATA_ACCESSOR(), false);
        this.entityData.define(EntityKineticBulletShared.INSTANCE.getTV_ROTATION_CLAMP_MODIFIER_DATA_ACCESSOR(), 1.0f);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("TAIL"))
    public void onPostInit(EntityType type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData, CallbackInfo ci) {
        this.taczexpands$bulletDataForPredicate = bulletData;
        this.taczexpands$hookData = IAccessorBulletData.getBulletExtraHolder(bulletData).hookData;
        this.taczexpands$isHook = taczexpands$hookData.isHook;
    }

    @Override
    public BulletData taczexpands$getBulletData() {
        return taczexpands$bulletDataForPredicate;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V", shift = At.Shift.AFTER), cancellable = true)
    public void onPreTick(CallbackInfo ci) {
        if (this.level().isClientSide) {
            var missileTick = this.entityData.get(EntityKineticBulletShared.INSTANCE.getIS_MISSILE_TICK_DATA_ACCESSOR());
            if (!missileTick) return;

            ci.cancel();

            var particleDelegate = EntityKineticBulletShared.INSTANCE.getOnMissileAmmoParticleSpawnDelegate();
            if (particleDelegate != null) {
                particleDelegate.invoke((EntityKineticBullet) (Object) this);
            }
        }

        if (!this.level().isClientSide) {
            if (this.getRemovalReason() == RemovalReason.DISCARDED) return;

            var tickDelegate = EntityKineticBulletShared.INSTANCE.getOnMissileBulletTickDelegate();
            if (tickDelegate == null) {
                this.entityData.set(EntityKineticBulletShared.INSTANCE.getIS_MISSILE_TICK_DATA_ACCESSOR(), false);
                return;
            }

            var result = tickDelegate.invoke((EntityKineticBullet) (Object) this);
            var isMissileTick = result.component1();
            this.entityData.set(EntityKineticBulletShared.INSTANCE.getIS_MISSILE_TICK_DATA_ACCESSOR(), isMissileTick);
            if (isMissileTick) {
                ci.cancel();
            } else {
                return;
            }

            var shouldExplode = result.component2();
            if (shouldExplode) {
                if (this.explosion) {
                    ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, this.position());
                }
                this.discard();
                return;
            }
        }

        if (!this.level().isClientSide) {
            float newYaw = this.getYRot();
            float newPitch = this.getXRot();

            Vec3 movement = this.getDeltaMovement();
            if (movement.lengthSqr() > 0.001) {
                newYaw = (float) Math.toDegrees(Mth.atan2(movement.x, movement.z));
                newPitch = (float) Math.toDegrees(Mth.atan2(movement.y, movement.horizontalDistance()));
            }

            var isMissileDrone = EntityKineticBulletShared.INSTANCE.isMissileDroneDelegate();
            if (isMissileDrone != null) {
                var isDrone = isMissileDrone.invoke((EntityKineticBullet) (Object) this);
                if (isDrone) {
                    newPitch = 0.0f;
                }
            }

            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();

            double nextX = this.getX() + movement.x;
            double nextY = this.getY() + movement.y;
            double nextZ = this.getZ() + movement.z;

            this.moveTo(nextX, nextY, nextZ, newYaw, newPitch);

            if (taczexpands$isSecondTick) {
                this.yRotO = newYaw;
                this.xRotO = newPitch;
                taczexpands$isSecondTick = false;
            }
        }

        if (this.level().isClientSide) {
            if (this.lerpSteps > 0) {
                double nextX = this.getX() + (this.lerpX - this.getX()) / (double) this.lerpSteps;
                double nextY = this.getY() + (this.lerpY - this.getY()) / (double) this.lerpSteps;
                double nextZ = this.getZ() + (this.lerpZ - this.getZ()) / (double) this.lerpSteps;

                float nextYaw = (float) (this.getYRot() + Mth.wrapDegrees(this.lerpYRot - (double) this.getYRot()) / (double) this.lerpSteps);
                float nextPitch = (float) (this.getXRot() + (this.lerpXRot - (double) this.getXRot()) / (double) this.lerpSteps);

                this.lerpSteps--;

                this.setPos(nextX, nextY, nextZ);
                this.setRot(nextYaw, nextPitch);
            }
        }

        if (this.tickCount >= this.life - 1) {
            this.discard();
        }
    }

    @Unique
    private int lerpSteps = 0;
    @Unique
    private double lerpX = 0.0;
    @Unique
    private double lerpY = 0.0;
    @Unique
    private double lerpZ = 0.0;
    @Unique
    private float lerpYRot = 0.0f;
    @Unique
    private float lerpXRot = 0.0f;

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int lerpSteps, boolean pTeleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = lerpSteps;
    }
}
