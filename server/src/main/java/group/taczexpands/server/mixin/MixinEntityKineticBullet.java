package group.taczexpands.server.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.util.AmmoBlockConfigRead;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.TacHitResult;
import group.taczexpands.common.accessor.IAccessorBullet;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.data.BulletExtraHolder;
import group.taczexpands.common.data.HookType;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.util.BlockResistanceData;
import group.taczexpands.server.accessor.IAccessorServerBullet;
import group.taczexpands.server.accessor.IAccessorHitVec;
import group.taczexpands.server.bullet.BulletManager;
import group.taczexpands.server.bullet.BulletCollisionPass;
import group.taczexpands.server.bullet.MissileManager;
import group.taczexpands.server.compat.CompatHelper;
import group.taczexpands.server.compat.tacztweaks.TACZTweaksCompat;
import group.taczexpands.server.config.ServerConfig;
import group.taczexpands.server.entity.BulletExtraData;
import group.taczexpands.server.event.BulletDeflectEvent;
import group.taczexpands.server.event.BulletDiscardEvent;
import group.taczexpands.server.event.BulletPenetrateEvent;
import group.taczexpands.server.mixin.accessor.IAccessorBlockRayTrace;
import group.taczexpands.server.module.visual_break_progress.BreakProgressManager;
import group.taczexpands.server.skill.HookManager;
import group.taczexpands.server.util.ExtensionsKt;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class MixinEntityKineticBullet extends Projectile
        implements IEntityAdditionalSpawnData, IAccessorServerBullet, IAccessorBullet {
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
    @Shadow
    private boolean igniteBlock;
    @Shadow
    private ResourceLocation ammoId;
    @Shadow
    private ResourceLocation gunId;
    @Shadow
    private ResourceLocation gunDisplayId;
    @Shadow
    private float damageModifier;
    @Shadow
    private float headShot;
    @Shadow
    private float armorIgnore;
    @Shadow
    private float knockback;
    @Shadow
    private float speed;
    @Shadow
    private LinkedList<ExtraDamage.DistanceDamagePair> damageAmount;
    @Shadow
    private int pierce;
    @Shadow
    private int explosionDelayCount;
    @Shadow
    private Vec3 startPos;
    @Shadow
    private boolean igniteEntity;
    @Shadow
    private int igniteEntityTime;
    @Unique
    private BulletExtraData taczexpands$bulletExtraData = new BulletExtraData();
    @Unique
    private List<BlockPos> taczexpands$walkedBlocks = new ArrayList<BlockPos>();
    @Unique
    private int taczexpands$maxPenetration = 0;
    @Unique
    private int taczexpands$penetration = 0;
    @Unique
    private float taczexpands$damageFactor = 1.0f;
    @Unique
    private boolean taczexpands$generated = false;
    @Unique
    private int taczexpands$deflectionTimes = 0;
    @Unique
    private Vec3 taczexpands$overridePosition = null;
    @Unique
    private ItemStack taczexpands$gunItem = null;
    @Unique
    private boolean taczexpands$deflectionTick = false;
    @Unique
    private boolean taczexpands$impacted = false;
    @Unique
    private BulletCollisionPass taczexpands$collisionPass;
    @Unique
    private static final double TACZEXPANDS_MIN_VELOCITY_SQUARED = 0.01;
    @Unique
    private boolean taczexpands$decayedThisTick;
    @Unique
    private Vec3 taczexpands$rayEndpoint;

    @Shadow
    protected abstract void onHitEntity(TacHitResult hit, Vec3 start, Vec3 end);

    @Shadow
    protected abstract void onHitBlock(BlockHitResult hit, Vec3 start, Vec3 end);

    @Shadow
    protected abstract void onBulletTick();

    @Shadow
    public abstract float getDamage(Vec3 hitPosition);

    @Shadow
    protected abstract org.apache.commons.lang3.tuple.Pair<DamageSource, DamageSource> createDamageSources(EntityKineticBullet.MaybeMultipartEntity target);

    @Shadow
    protected abstract void tacAttackEntity(EntityKineticBullet.MaybeMultipartEntity target, float damage, org.apache.commons.lang3.tuple.Pair<DamageSource, DamageSource> damageSources);

    protected MixinEntityKineticBullet(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    public Vec3 taczexpands$getOverridePosition() {
        return this.taczexpands$overridePosition;
    }

    public boolean taczexpands$isGenerated() {
        return this.taczexpands$generated;
    }

    public ItemStack taczexpands$getGunItem() {
        return this.taczexpands$gunItem;
    }

    public int taczexpands$getPenetration() {
        return this.taczexpands$penetration;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At(value = "TAIL"))
    public void onPostInit(EntityType type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData, CallbackInfo ci) {
        this.taczexpands$bulletExtraData.setBulletData(bulletData);
        this.taczexpands$bulletExtraData.initializeProximityFuze((EntityKineticBullet)(Object)this);
        if (gunItem != null) {
            int overrideAmount = GunExtras.INSTANCE.getOverrideBulletAmount(gunItem);
            if (overrideAmount > 0) {
                this.damageModifier = 1.0f / (float)overrideAmount;
            }
            this.taczexpands$gunItem = gunItem.copy();
        }
        this.taczexpands$penetration = this.taczexpands$maxPenetration = IAccessorBulletData.getBulletExtraHolder((BulletData)bulletData).penetration;
        MissileManager.INSTANCE.bulletInit((EntityKineticBullet)(Object)this);
    }

    public void taczexpands$initCustomData(double x, double y, double z, float speed) {
        this.taczexpands$generated = true;
        BulletData bulletData = this.taczexpands$bulletExtraData.getBulletData();
        ExtraDamage extraDamage = bulletData.getExtraDamage();
        if (extraDamage != null) {
            this.armorIgnore = extraDamage.getArmorIgnore();
            this.headShot = extraDamage.getHeadShotMultiplier();
            this.knockback = bulletData.getKnockback();
            LinkedList damageAdjust = extraDamage.getDamageAdjust();
            if (damageAdjust != null && !damageAdjust.isEmpty()) {
                this.damageAmount = new LinkedList(damageAdjust);
            }
        } else {
            this.armorIgnore = 0.0f;
            this.headShot = 1.0f;
            this.knockback = 0.0f;
        }
        if (this.damageAmount == null || this.damageAmount.isEmpty()) {
            this.damageAmount = new LinkedList();
            float baseDamage = (float)Mth.clamp((double)((double)bulletData.getDamageAmount() * (Double)SyncConfig.DAMAGE_BASE_MULTIPLIER.get()), (double)0.0, (double)3.4028234663852886E38);
            this.damageAmount.add(new ExtraDamage.DistanceDamagePair(Float.MAX_VALUE, baseDamage));
        }
        this.speed = speed;
        this.pierce = bulletData.getPierce();
        ExplosionData explosionData = bulletData.getExplosionData();
        if (explosionData != null) {
            this.explosion = explosionData.isExplode();
            this.explosionDamage = (float)Mth.clamp((double)((double)explosionData.getDamage() * (Double)SyncConfig.DAMAGE_BASE_MULTIPLIER.get()), (double)0.0, (double)3.4028234663852886E38);
            this.explosionRadius = Mth.clamp((float)explosionData.getRadius(), (float)0.0f, (float)Float.MAX_VALUE);
            this.explosionKnockback = explosionData.isKnockback();
            int delayTickCount = (int)(explosionData.getDelay() * 20.0f);
            if (delayTickCount < 0) {
                delayTickCount = Integer.MAX_VALUE;
            }
            this.explosionDestroyBlock = explosionData.isDestroyBlock() && (Boolean)AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get() != false;
            this.explosionDelayCount = Math.max(delayTickCount, 1);
        } else {
            this.explosion = false;
        }
        this.setPos(x, y, z);
        this.startPos = this.position();
        this.taczexpands$bulletExtraData.initializeProximityFuze((EntityKineticBullet)(Object)this);
    }

    @Inject(method = "onHitBlock", at = @At(value = "HEAD"), cancellable = true)
    private void taczexpands$onHitBlock$hookHit(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        BulletExtraHolder extra = IAccessorBulletData.getBulletExtraHolder((BulletData)this.taczexpands$bulletExtraData.getBulletData());
        if (extra.hookData.isHook) {
            super.onHitBlock(result);
            this.taczexpands$overridePosition = result.getLocation();
            this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
            Entity owner = this.getOwner();
            if (owner instanceof LivingEntity) {
                LivingEntity ownerLivingEntity = (LivingEntity)owner;
                if (extra.hookData.type != HookType.PULLING) {
                    HookManager.INSTANCE.addHook((EntityKineticBullet)(Object)this, ownerLivingEntity, result, extra.hookData);
                }
            }
            this.discard();
            ci.cancel();
        }
    }

    @Redirect(method = "onHitBlock", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosion:Z", opcode = 180, ordinal = 0))
    private boolean taczexpands$onHitBlock$redirectExplosionCondition(EntityKineticBullet instance) {
        BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)this.taczexpands$bulletExtraData.getBulletData());
        return this.explosion && (!bulletExtraHolder.startExplosionDelayOnImpact || this.taczexpands$impacted);
    }

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 0, remap = true))
    private void taczexpands$onHitBlock$injectDiscardBulletPos(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        this.taczexpands$overridePosition = result.getLocation();
        this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
    }

    @Redirect(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", ordinal = 0, remap = true))
    private int taczexpands$onHitBlock$redirectSendParticles(ServerLevel instance, ParticleOptions pType, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed) {
        BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)this.taczexpands$bulletExtraData.getBulletData());
        if (bulletExtraHolder.renderNormalBulletHole) {
            return instance.sendParticles(pType, pPosX, pPosY, pPosZ, pParticleCount, pXOffset, pYOffset, pZOffset, pSpeed);
        }
        return 0;
    }

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 1, remap = true), cancellable = true)
    private void taczexpands$onHitBlock$injectFinalDiscard(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        if (!this.explosion) {
            this.taczexpands$overridePosition = result.getLocation();
            this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
        } else {
            this.taczexpands$overridePosition = null;
            ci.cancel();
        }
    }

    @Inject(method = "onHitEntity", at = @At(value = "HEAD"), cancellable = true)
    private void taczexpands$onHitEntity$hookHit(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        this.taczexpands$impacted = true;
        BulletExtraHolder extra = IAccessorBulletData.getBulletExtraHolder((BulletData)this.taczexpands$bulletExtraData.getBulletData());
        if (extra.hookData.isHook) {
            Entity owner = this.getOwner();
            Entity target = result.getEntity();
            if (owner instanceof LivingEntity) {
                LivingEntity ownerLivingEntity = (LivingEntity)owner;
                if (target instanceof LivingEntity) {
                    LivingEntity targetLivingEntity = (LivingEntity)target;
                    this.taczexpands$overridePosition = result.getLocation();
                    this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
                    HookManager.INSTANCE.addHook((EntityKineticBullet)(Object)this, ownerLivingEntity, targetLivingEntity, extra.hookData);
                    this.discard();
                    ci.cancel();
                }
            }
        }
        this.taczexpands$overridePosition = result.getLocation();
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false))
    private boolean taczexpands$onHitEntity$redirectPreEventPost(IEventBus eventBus, Event event) {
        if (event instanceof EntityHurtByGunEvent.Pre) {
            boolean cancelled;
            EntityHurtByGunEvent.Pre preEvent = (EntityHurtByGunEvent.Pre)event;
            if (((IAccessorHitVec)preEvent).taczexpands$getHitVec() == null) {
                ((IAccessorHitVec)preEvent).taczexpands$setHitVec(this.taczexpands$overridePosition);
            }
            if (cancelled = eventBus.post((Event)preEvent)) {
                this.taczexpands$overridePosition = null;
            } else {
                this.taczexpands$overridePosition = null;
                this.taczexpands$bulletExtraData.addHitEntity(preEvent, false);
            }
            return cancelled;
        }
        return eventBus.post(event);
    }

    @Redirect(method = "onHitEntity", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosion:Z", opcode = 180, ordinal = 0))
    private boolean taczexpands$onHitEntity$redirectExplosionCondition(EntityKineticBullet instance, TacHitResult result) {
        BulletData bulletData = this.taczexpands$bulletExtraData.getBulletData();
        BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)bulletData);
        if (this.explosion && (!bulletExtraHolder.startExplosionDelayOnImpact || this.taczexpands$impacted)) {
            this.taczexpands$overridePosition = result.getLocation();
            return true;
        }
        return false;
    }

    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/ExplodeUtil;createExplosion(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;FFZZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void taczexpands$onHitEntity$injectAfterExplosion(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        this.taczexpands$overridePosition = null;
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z", remap = true))
    private boolean taczexpands$onHitEntity$redirectIsDeadOrDying(LivingEntity livingCore) {
        boolean isDead = livingCore.isDeadOrDying();
        if (isDead) {
            this.taczexpands$overridePosition = null;
        }
        return isDead;
    }

    public void taczexpands$onDiscard() {
        if (this.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
            MinecraftForge.EVENT_BUS.post((Event)new BulletDiscardEvent((EntityKineticBullet)(Object)this, this.taczexpands$bulletExtraData));
        }
    }

    public BulletExtraData taczexpands$getBulletExtraData() {
        return this.taczexpands$bulletExtraData;
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = true)
    private void taczexpands$clearTickDecay(CallbackInfo ci) {
        this.taczexpands$decayedThisTick = false;
    }

    @Override
    public Vec3 taczexpands$getDecayEndpoint() {
        return this.taczexpands$decayedThisTick && !this.isRemoved() ? this.taczexpands$rayEndpoint : null;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
        target = "Lcom/tacz/guns/entity/EntityKineticBullet;setPos(DDD)V", remap = true), remap = true)
    private void taczexpands$moveToRayEndpoint(EntityKineticBullet bullet, double x, double y, double z,
            Operation<Void> original) {
        Vec3 endpoint = this.taczexpands$getDecayEndpoint();
        if (endpoint != null) {
            original.call(bullet, endpoint.x, endpoint.y, endpoint.z);
        } else {
            original.call(bullet, x, y, z);
        }
    }

    @Inject(method = "onBulletTick", at = @At(value = "HEAD"))
    private void taczexpands$onBulletTick$injectHead(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            this.taczexpands$deflectionTick = false;
            this.taczexpands$collisionPass = null;
            this.taczexpands$rayEndpoint = null;
        }
    }

    @Redirect(method = "onBulletTick", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosion:Z", opcode = 180, ordinal = 0))
    private boolean taczexpands$onBulletTick$redirectExplosionDelayCondition(EntityKineticBullet instance) {
        BulletData bulletData = this.taczexpands$bulletExtraData.getBulletData();
        BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)bulletData);
        return this.explosion && (this.taczexpands$impacted || !bulletExtraHolder.startExplosionDelayOnImpact);
    }

    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/ExplodeUtil;createExplosion(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;FFZZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void taczexpands$onBulletTick$injectAfterTickExplosion(CallbackInfo ci) {
        this.setDeltaMovement(new Vec3(0.0, 0.0, 0.0));
    }

    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;", ordinal = 0, remap = true), cancellable = true)
    private void taczexpands$onBulletTick$injectVelocityCheck(CallbackInfo ci) {
        if (this.getDeltaMovement().lengthSqr() < TACZEXPANDS_MIN_VELOCITY_SQUARED) {
            ci.cancel();
        }
    }

    @Redirect(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/block/BlockRayTrace;rayTraceBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult taczexpands$onBulletTick$redirectRayTraceBlocks(Level level, ClipContext context) {
        return this.taczexpands$redirectRayTraceBlocks(level, context);
    }

    @Inject(method = "onBulletTick", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;pierce:I", opcode = 180, ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void injectDeflectionBeforeHitEntities(CallbackInfo ci, Vec3 startVec, Vec3 endVec, HitResult result, BlockHitResult resultB, List hitEntities) {
        if (this.taczexpands$collisionPass != null && this.getRemovalReason() != null) {
            this.taczexpands$collisionPass = null;
            ci.cancel();
            return;
        }
        if (this.taczexpands$deflectionTick) {
            this.taczexpands$collisionPass = null;
            this.setPos(endVec);
            Vec3 movement = this.getDeltaMovement();
            double x = movement.x;
            double y = movement.y;
            double z = movement.z;
            double distance = movement.horizontalDistance();
            this.setYRot((float)Math.toDegrees(Mth.atan2((double)x, (double)z)));
            this.setXRot((float)Math.toDegrees(Mth.atan2((double)y, (double)distance)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            BulletManager.INSTANCE.notify((EntityKineticBullet)(Object)this);
            this.onBulletTick();
            ci.cancel();
        } else if (this.taczexpands$collisionPass != null) {
            // The ordered pass consumes the EntityUtil hit chain for block intersections.
            // Delegate unobstructed segments to TacZ's entity loop.
            this.taczexpands$collisionPass = null;
            this.onHitBlock(resultB, startVec, endVec);
            ci.cancel();
        }
    }

    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 1, remap = true), locals = LocalCapture.CAPTURE_FAILHARD)
    private void taczexpands$onBulletTick$injectPierceDiscard(CallbackInfo ci, Vec3 startVec, Vec3 endVec, HitResult result, BlockHitResult resultB, List hitEntities, EntityKineticBullet.EntityResult[] hitEntityResult, EntityKineticBullet.EntityResult[] entityResults, int resultCount, int resultIndex, EntityKineticBullet.EntityResult entityResult) {
        this.taczexpands$overridePosition = entityResult.getHitPos();
        this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
    }

    @Redirect(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onHitEntity(Lcom/tacz/guns/util/TacHitResult;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
    public void taczexpands$onBulletTick$redirectOnHitEntity(EntityKineticBullet instance, TacHitResult result, Vec3 startVec, Vec3 endVec) {
        this.onHitEntity(result, startVec, endVec);
        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.handleHitEntity((EntityKineticBullet)(Object)this, new EntityKineticBullet.EntityResult(result.getEntity(), result.getLocation(), result.isHeadshot()), this.pierce - 1 > 0, !result.getEntity().isAlive());
        }
    }

    @Unique
    private boolean taczexpands$hitOrderedEntity(EntityKineticBullet.EntityResult result, Vec3 startVec, Vec3 endVec) {
        this.taczexpands$onBulletTick$redirectOnHitEntity((EntityKineticBullet)(Object)this,
            new TacHitResult(result), startVec, endVec);
        // Consume pierce and check discard after the damage event, including cancelled events.
        --this.pierce;
        if (this.pierce < 1 || this.explosion) {
            this.taczexpands$overridePosition = result.getHitPos();
            this.setDeltaMovement(this.taczexpands$overridePosition.subtract(startVec));
            this.discard();
        }
        return this.getRemovalReason() != null;
    }

    @Unique
    public BlockHitResult taczexpands$redirectRayTraceBlocks(Level level, ClipContext context) {
        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.initBulletRayTracer(level, context);
        }
        BiFunction<ClipContext, BlockPos, BlockHitResult> taczexpandsRayTraceFunc = (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (AmmoBlockConfigRead.canPassThrough(blockState)) {
                return null;
            }
            if (IAccessorBlockRayTrace.getIGNORES().test(blockState)) {
                return null;
            }
            BlockHitResult blockHitResult = IAccessorBlockRayTrace.getBlockHitResult((Level)level, (ClipContext)rayTraceContext, (BlockPos)blockPos, (BlockState)blockState);
            if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
                if (this.taczexpands$walkedBlocks.contains(blockPos)) {
                    return null;
                }
                BulletCollisionPass hits = this.taczexpands$collisionPass;
                if (hits == null) {
                    this.taczexpands$collisionPass = hits = new BulletCollisionPass((EntityKineticBullet)(Object)this,
                        rayTraceContext.getFrom(), rayTraceContext.getTo(), () -> this.pierce <= 1 || this.explosion,
                        (result, limit) -> this.taczexpands$hitOrderedEntity(result, rayTraceContext.getFrom(), limit));
                }
                int beforeHits = hits.hitCount();
                if (hits.consumeTo(blockHitResult.getLocation())) return blockHitResult;
                if (hits.hitCount() != beforeHits) {
                    // A nearer entity's synchronous event may destroy/change this
                    // block. Re-read only when such an event actually ran.
                    blockState = level.getBlockState(blockPos);
                    if (AmmoBlockConfigRead.canPassThrough(blockState)
                            || IAccessorBlockRayTrace.getIGNORES().test(blockState)) return null;
                    blockHitResult = IAccessorBlockRayTrace.getBlockHitResult(level, rayTraceContext, blockPos, blockState);
                    if (blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS) return null;
                }
                this.taczexpands$impacted = true;
                BulletExtraData bulletExtraData = this.taczexpands$getBulletExtraData();
                BulletData bulletData = bulletExtraData.getBulletData();
                BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)bulletData);
                BlockResistanceData resistance = ExtensionsKt.getBlockResistance((BulletExtraHolder)bulletExtraHolder, (BlockState)blockState);
                if (resistance == null) {
                    return blockHitResult;
                }
                if (this.taczexpands$penetration > 0) {
                    Level patt46274$temp;
                    if (this.taczexpands$penetration >= resistance.getResistance()) {
                        BulletPenetrateEvent event = new BulletPenetrateEvent((EntityKineticBullet)(Object)this, blockHitResult);
                        this.taczexpands$overridePosition = blockHitResult.getLocation();
                        MinecraftForge.EVENT_BUS.post((Event)event);
                        this.taczexpands$overridePosition = null;
                        if (!event.isCanceled()) {
                            boolean shouldDestroyBlock;
                            this.taczexpands$penetration -= resistance.getResistance();
                            boolean stopped = false;
                            if (bulletExtraHolder.penetrationDecay && this.taczexpands$maxPenetration > 0) {
                                float factor = (float)this.taczexpands$penetration / (float)this.taczexpands$maxPenetration;
                                factor = Mth.clamp((float)factor, (float)0.0f, (float)1.0f);
                                this.taczexpands$damageFactor *= factor;
                                Vec3 deltaMovement = this.getDeltaMovement();
                                this.setDeltaMovement(deltaMovement.scale((double)factor));
                                this.taczexpands$decayedThisTick = true;
                                stopped = this.getDeltaMovement().lengthSqr() < TACZEXPANDS_MIN_VELOCITY_SQUARED;
                            }
                            BlockHitResult terminalHit = blockHitResult;
                            if (stopped) {
                                BlockHitResult backface = IAccessorBlockRayTrace.getBlockHitResult(level,
                                    new ClipContext(rayTraceContext.getTo(), rayTraceContext.getFrom(),
                                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), blockPos, blockState);
                                if (backface != null && backface.getType() != HitResult.Type.MISS) {
                                    terminalHit = backface;
                                }
                            }
                            boolean bl = shouldDestroyBlock = ((Boolean)ServerConfig.INSTANCE.getShouldDestroyBlock().get() != false || resistance.getBypassGlobalDestroyLimit()) && resistance.getShouldDestroyBlock();
                            if (shouldDestroyBlock) {
                                level.levelEvent(2001, blockPos, Block.getId((BlockState)blockState));
                                level.destroyBlock(blockPos, false, this.getOwner());
                                if (stopped) return terminalHit;
                                return this.taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                            }
                            if (resistance.getParticleOnPenetrate()) {
                                level.levelEvent(2001, blockPos, Block.getId((BlockState)blockState));
                            }
                            this.taczexpands$walkedBlocks.add(new BlockPos((Vec3i)blockPos));
                            super.onHitBlock(blockHitResult);
                            Level patt43175$temp = this.level();
                            if (patt43175$temp instanceof ServerLevel) {
                                BlockHitResult reverseBlockHitResult;
                                ServerLevel serverLevel = (ServerLevel)patt43175$temp;
                                BlockHitResult result = blockHitResult;
                                BlockPos pos = result.getBlockPos();
                                Vec3 hitVec = result.getLocation();
                                if (bulletExtraHolder.renderPenetrationBulletHole) {
                                    BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), new BlockPos((Vec3i)blockPos), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
                                    serverLevel.sendParticles((ParticleOptions)bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0.0, 0.0, 0.0, 0.0);
                                }
                                if (this.igniteBlock) {
                                    serverLevel.sendParticles((ParticleOptions)ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z, 1, 0.0, 0.0, 0.0, 0.0);
                                }
                                if (!stopped && bulletExtraHolder.renderPenetrationBulletHole && (reverseBlockHitResult = IAccessorBlockRayTrace.getBlockHitResult((Level)level, (ClipContext)new ClipContext(rayTraceContext.getTo(), rayTraceContext.getFrom(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this), (BlockPos)blockPos, (BlockState)blockState)) != null && reverseBlockHitResult.getType() != HitResult.Type.MISS) {
                                    Vec3 reverseHitVec = reverseBlockHitResult.getLocation();
                                    BulletHoleOption reverseBulletHoleOption = new BulletHoleOption(reverseBlockHitResult.getDirection(), new BlockPos((Vec3i)blockPos), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
                                    serverLevel.sendParticles((ParticleOptions)reverseBulletHoleOption, reverseHitVec.x, reverseHitVec.y, reverseHitVec.z, 1, 0.0, 0.0, 0.0, 0.0);
                                }
                                if (this.igniteBlock && ((Boolean)AmmoConfig.IGNITE_BLOCK.get()).booleanValue()) {
                                    BlockPos offsetPos = pos.relative(result.getDirection());
                                    if (BaseFireBlock.canBePlacedAt((Level)this.level(), (BlockPos)offsetPos, (Direction)result.getDirection())) {
                                        BlockState fireState = BaseFireBlock.getState((BlockGetter)this.level(), (BlockPos)offsetPos);
                                        this.level().setBlock(offsetPos, fireState, 11);
                                        ((ServerLevel)this.level()).sendParticles((ParticleOptions)ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0.0, 0.0, 0.0, 0.0);
                                    }
                                }
                            }
                            if (stopped) return terminalHit;
                            return this.taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                        }
                    }
                    if ((patt46274$temp = this.level()) instanceof ServerLevel) {
                        boolean shouldDestroyBlock;
                        ServerLevel serverLevel = (ServerLevel)patt46274$temp;
                        boolean bl = shouldDestroyBlock = ((Boolean)ServerConfig.INSTANCE.getShouldDestroyBlock().get() != false || resistance.getBypassGlobalDestroyLimit()) && resistance.getShouldDestroyBlock();
                        if (shouldDestroyBlock) {
                            float damage = (float)this.taczexpands$penetration / (float)resistance.getResistance();
                            Pair<Float, Boolean> pair = BreakProgressManager.INSTANCE.updateDamage(serverLevel, blockPos, damage);
                            Float remainingPenetration = (Float)pair.getFirst();
                            Boolean isBroken = (Boolean)pair.getSecond();
                            if (isBroken.booleanValue()) {
                                level.levelEvent(2001, blockPos, Block.getId((BlockState)blockState));
                                level.destroyBlock(blockPos, false, this.getOwner());
                                this.taczexpands$walkedBlocks.add(new BlockPos((Vec3i)blockPos));
                                this.taczexpands$penetration = (int)(remainingPenetration.floatValue() * (float)resistance.getResistance());
                                return this.taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                            }
                        }
                    } else {
                        this.taczexpands$penetration = 0;
                    }
                }
                this.taczexpands$tryDeflection(blockHitResult, resistance);
            }
            return blockHitResult;
        };
        BlockHitResult hitResult = (BlockHitResult)IAccessorBlockRayTrace.performRayTrace((ClipContext)context, (clipContext, blockPos) -> {
            BlockHitResult taczexpandsResult = (BlockHitResult)taczexpandsRayTraceFunc.apply((ClipContext)clipContext, (BlockPos)blockPos);
            if ((this.taczexpands$collisionPass == null || !this.taczexpands$collisionPass.stopped()) && taczexpandsResult != null && CompatHelper.INSTANCE.hasTACZTweaks() && taczexpandsResult.getType() != HitResult.Type.MISS) {
                BlockState blockState = level.getBlockState(taczexpandsResult.getBlockPos());
                BlockHitResult result = TACZTweaksCompat.INSTANCE.handleHitBlock((EntityKineticBullet)(Object)this, taczexpandsResult, blockState);
                return result;
            }
            return taczexpandsResult;
        }, rayTraceContext -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss((Vec3)rayTraceContext.getTo(), (Direction)Direction.getNearest((double)vec3.x, (double)vec3.y, (double)vec3.z), (BlockPos)BlockPos.containing((Position)rayTraceContext.getTo()));
        });
        BulletCollisionPass hits = this.taczexpands$collisionPass;
        if (hits != null && hitResult.getType() == HitResult.Type.MISS && !hits.stopped()) hits.consumeTo(context.getTo());
        this.taczexpands$rayEndpoint = hits != null && hits.stopped() ? hits.lastHit() : hitResult.getLocation();
        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.updatePosition((EntityKineticBullet)(Object)this,
                hits == null || hits.lastHit() == null ? hitResult.getLocation() : hits.lastHit());
        }
        return hitResult;
    }

    @Unique
    public boolean taczexpands$tryDeflection(BlockHitResult blockHitResult, BlockResistanceData blockResistanceData) {
        Level level;
        float minIncidenceAngle;
        Direction face;
        Vec3 normal;
        boolean shouldDeflection;
        if (!blockResistanceData.getDeflectable()) {
            return false;
        }
        BulletData bulletData = this.taczexpands$bulletExtraData.getBulletData();
        BulletExtraHolder bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder((BulletData)bulletData);
        boolean bl = shouldDeflection = bulletExtraHolder.deflection && this.taczexpands$deflectionTimes < bulletExtraHolder.maxDeflectionCount;
        if (!shouldDeflection) {
            return false;
        }
        Vec3 bulletDir = this.getDeltaMovement().normalize();
        double dot = bulletDir.dot(normal = Vec3.atLowerCornerOf((Vec3i)(face = blockHitResult.getDirection()).getNormal()));
        double incidenceAngle = Math.toDegrees(Math.acos(Math.abs(dot)));
        if (incidenceAngle < (double)(minIncidenceAngle = bulletExtraHolder.minIncidenceAngle)) {
            return false;
        }
        Vec3 reflectDir = bulletDir.subtract(normal.scale(2.0 * dot));
        BulletDeflectEvent event = new BulletDeflectEvent((EntityKineticBullet)(Object)this, blockHitResult);
        this.taczexpands$overridePosition = blockHitResult.getLocation();
        MinecraftForge.EVENT_BUS.post((Event)event);
        this.taczexpands$overridePosition = null;
        if (event.isCanceled()) {
            return false;
        }
        this.setDeltaMovement(reflectDir.scale(this.getDeltaMovement().length() * (double)bulletExtraHolder.deflectionSpeedFactor));
        this.taczexpands$damageFactor *= bulletExtraHolder.deflectionDamageFactor;
        this.taczexpands$walkedBlocks.add(new BlockPos((Vec3i)blockHitResult.getBlockPos()));
        ++this.taczexpands$deflectionTimes;
        this.taczexpands$deflectionTick = true;
        if (bulletExtraHolder.renderDeflectionBulletHole && (level = this.level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            BulletHoleOption bulletHoleOption = new BulletHoleOption(blockHitResult.getDirection(), new BlockPos((Vec3i)blockHitResult.getBlockPos()), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
            Vec3 hitVec = blockHitResult.getLocation();
            serverLevel.sendParticles((ParticleOptions)bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    @Inject(method = "getDamage", at = @At(value = "RETURN"), cancellable = true)
    public void hookGetDamage(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * this.taczexpands$damageFactor);
    }
}

