package group.taczexpands.server.mixin;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.TacHitResult;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.data.HookType;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.util.BlockResistanceData;
import group.taczexpands.server.accessor.IAccessorBullet;
import group.taczexpands.server.accessor.IAccessorHitVec;
import group.taczexpands.server.bullet.BulletManager;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class MixinEntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData, IAccessorBullet {
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
    protected abstract void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec);

    @Shadow
    protected abstract void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec);

    @Shadow
    protected abstract void onBulletTick();

    @Shadow
    public abstract float getDamage(Vec3 hitVec);

    @Shadow
    protected abstract Pair<DamageSource, DamageSource> createDamageSources(EntityKineticBullet.MaybeMultipartEntity parts);

    @Shadow
    private boolean igniteEntity;
    @Shadow
    private int igniteEntityTime;

    @Shadow
    protected abstract void tacAttackEntity(EntityKineticBullet.MaybeMultipartEntity parts, float damage, Pair<DamageSource, DamageSource> sources);

    @Unique
    private BulletExtraData taczexpands$bulletExtraData = new BulletExtraData();

    @Unique
    private List<BlockPos> taczexpands$walkedBlocks = new ArrayList<>();

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

    protected MixinEntityKineticBullet(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    @Override
    public Vec3 taczexpands$getOverridePosition() {
        return taczexpands$overridePosition;
    }

    @Override
    public boolean taczexpands$isGenerated() {
        return taczexpands$generated;
    }

    @Override
    public ItemStack taczexpands$getGunItem() {
        return taczexpands$gunItem;
    }

    @Override
    public int taczexpands$getPenetration() {
        return taczexpands$penetration;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("TAIL"))
    public void onPostInit(EntityType type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData, CallbackInfo ci) {
        this.taczexpands$bulletExtraData.setBulletData(bulletData);
        if (gunItem != null) {
            var overrideAmount = GunExtras.INSTANCE.getOverrideBulletAmount(gunItem);
            if (overrideAmount > 0) {
                this.damageModifier = 1.0f / overrideAmount;
            }

            taczexpands$gunItem = gunItem.copy();
        }

        taczexpands$maxPenetration = IAccessorBulletData.getBulletExtraHolder(bulletData).penetration;
        taczexpands$penetration = taczexpands$maxPenetration;
        MissileManager.INSTANCE.bulletInit((EntityKineticBullet) (Object) this);
    }

    @Override
    public void taczexpands$initCustomData(double x, double y, double z, float speed) {
        taczexpands$generated = true;
        var bulletData = taczexpands$bulletExtraData.getBulletData();
        var extraDamage = bulletData.getExtraDamage();
        if (extraDamage != null) {
            armorIgnore = extraDamage.getArmorIgnore();
            headShot = extraDamage.getHeadShotMultiplier();
            knockback = bulletData.getKnockback();
            damageAmount = extraDamage.getDamageAdjust();
        } else {
            armorIgnore = 0.0f;
            headShot = 1.0f;
            knockback = 0.0f;
            damageAmount = new LinkedList<>();
        }
        this.speed = speed;
        pierce = bulletData.getPierce();
        var explosionData = bulletData.getExplosionData();
        if (explosionData != null) {
            this.explosion = explosionData.isExplode();
            this.explosionDamage = (float) Mth.clamp(explosionData.getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(explosionData.getRadius(), 0, Float.MAX_VALUE);
            this.explosionKnockback = explosionData.isKnockback();
            int delayTickCount = (int) (explosionData.getDelay() * 20);
            if (delayTickCount < 0) {
                delayTickCount = Integer.MAX_VALUE;
            }
            this.explosionDestroyBlock = explosionData.isDestroyBlock() && AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get();
            this.explosionDelayCount = Math.max(delayTickCount, 1);
        } else {
            this.explosion = false;
        }

        this.setPos(x, y, z);
        this.startPos = this.position();
    }


    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void taczexpands$onHitBlock$hookHit(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }

        var extra = IAccessorBulletData.getBulletExtraHolder(taczexpands$bulletExtraData.getBulletData());
        if (extra.hookData.isHook) {
            super.onHitBlock(result);

            taczexpands$overridePosition = result.getLocation();
            setDeltaMovement(taczexpands$overridePosition.subtract(startVec));

            var owner = this.getOwner();
            if (owner instanceof LivingEntity ownerLivingEntity) {
                if (extra.hookData.type != HookType.PULLING) {
                    HookManager.INSTANCE.addHook((EntityKineticBullet) (Object) this, ownerLivingEntity, result, extra.hookData);
                }
            }

            this.discard();
            ci.cancel();
        }
    }

    @Redirect(method = "onHitBlock", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosion:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean taczexpands$onHitBlock$redirectExplosionCondition(EntityKineticBullet instance) {
        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(taczexpands$bulletExtraData.getBulletData());
        return explosion && (!bulletExtraHolder.startExplosionDelayOnImpact || taczexpands$impacted);
    }

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 0, remap = true))
    private void taczexpands$onHitBlock$injectDiscardBulletPos(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        taczexpands$overridePosition = result.getLocation();
        setDeltaMovement(taczexpands$overridePosition.subtract(startVec));
    }

    @Redirect(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", ordinal = 0, remap = true))
    private int taczexpands$onHitBlock$redirectSendParticles(ServerLevel instance, ParticleOptions pType, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed) {
        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(taczexpands$bulletExtraData.getBulletData());
        if (bulletExtraHolder.renderNormalBulletHole) {
            return instance.sendParticles(pType, pPosX, pPosY, pPosZ, pParticleCount, pXOffset, pYOffset, pZOffset, pSpeed);
        }
        return 0;
    }

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 1, remap = true), cancellable = true)
    private void taczexpands$onHitBlock$injectFinalDiscard(BlockHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        if (!explosion) {
            taczexpands$overridePosition = result.getLocation();
            setDeltaMovement(taczexpands$overridePosition.subtract(startVec));
        } else {
            taczexpands$overridePosition = null;
            ci.cancel();
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void taczexpands$onHitEntity$hookHit(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        taczexpands$impacted = true;

        var extra = IAccessorBulletData.getBulletExtraHolder(taczexpands$bulletExtraData.getBulletData());

        if (extra.hookData.isHook) {
            var owner = this.getOwner();
            var target = result.getEntity();
            if (owner instanceof LivingEntity ownerLivingEntity && target instanceof LivingEntity targetLivingEntity) {
                taczexpands$overridePosition = result.getLocation();
                setDeltaMovement(taczexpands$overridePosition.subtract(startVec));
                HookManager.INSTANCE.addHook((EntityKineticBullet) (Object) this, ownerLivingEntity, targetLivingEntity, extra.hookData);

                this.discard();
                ci.cancel();
            }
        }

        taczexpands$overridePosition = result.getLocation();
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false))
    private boolean taczexpands$onHitEntity$redirectPreEventPost(IEventBus eventBus, Event event) {
        if (event instanceof EntityHurtByGunEvent.Pre preEvent) {
            if (((IAccessorHitVec) preEvent).taczexpands$getHitVec() == null) {
                ((IAccessorHitVec) preEvent).taczexpands$setHitVec(taczexpands$overridePosition);
            }

            boolean cancelled = eventBus.post(preEvent);

            if (cancelled) {
                taczexpands$overridePosition = null;
            } else {
                taczexpands$overridePosition = null;
                taczexpands$bulletExtraData.addHitEntity(preEvent, false);
            }
            return cancelled;
        }

        return eventBus.post(event);
    }

    @Redirect(method = "onHitEntity", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosion:Z", opcode = org.objectweb.asm.Opcodes.GETFIELD, ordinal = 0))
    private boolean taczexpands$onHitEntity$redirectExplosionCondition(EntityKineticBullet instance, TacHitResult result) {
        var bulletData = taczexpands$bulletExtraData.getBulletData();
        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData);
        if (explosion && (!bulletExtraHolder.startExplosionDelayOnImpact || taczexpands$impacted)) {
            taczexpands$overridePosition = result.getLocation();
            return true;
        }
        return false;
    }

    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/ExplodeUtil;createExplosion(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;FFZZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void taczexpands$onHitEntity$injectAfterExplosion(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        taczexpands$overridePosition = null;
    }

    @Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z", remap = true))
    private boolean taczexpands$onHitEntity$redirectIsDeadOrDying(LivingEntity livingCore) {
        boolean isDead = livingCore.isDeadOrDying();
        if (isDead) {
            taczexpands$overridePosition = null;
        }
        return isDead;
    }





    @Override
    public void taczexpands$onDiscard() {
        if (getRemovalReason() != RemovalReason.DISCARDED) {
            MinecraftForge.EVENT_BUS.post(new BulletDiscardEvent((EntityKineticBullet) (Object) this, taczexpands$bulletExtraData));
        }
    }

    @Override
    public BulletExtraData taczexpands$getBulletExtraData() {
        return taczexpands$bulletExtraData;
    }

    @Unique
    private boolean taczexpands$deflectionTick = false;

    @Unique
    private boolean taczexpands$impacted = false;


    @Inject(method = "onBulletTick", at = @At("HEAD"))
    private void taczexpands$onBulletTick$injectHead(CallbackInfo ci) {
        if (!this.level().isClientSide()) {
            taczexpands$deflectionTick = false;
        }
    }


    @Redirect(method = "onBulletTick", at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;explosionDelayCount:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int taczexpands$onBulletTick$redirectExplosionDelayCondition(EntityKineticBullet instance) {
        var bulletData = taczexpands$bulletExtraData.getBulletData();
        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData);

        if (explosionDelayCount > 0 && (taczexpands$impacted || !bulletExtraHolder.startExplosionDelayOnImpact)) {
            return explosionDelayCount;
        }
        return 0;
    }

    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/ExplodeUtil;createExplosion(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;FFZZLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void taczexpands$onBulletTick$injectAfterTickExplosion(CallbackInfo ci) {
        this.setDeltaMovement(new Vec3(0.0D, 0.0D, 0.0D));
    }


    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;", ordinal = 0, remap = true), cancellable = true)
    private void taczexpands$onBulletTick$injectVelocityCheck(CallbackInfo ci) {
        if (this.getDeltaMovement().lengthSqr() < 0.01) {
            ci.cancel();
        }
    }

    @Redirect(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/block/BlockRayTrace;rayTraceBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult taczexpands$onBulletTick$redirectRayTraceBlocks(Level level, ClipContext context) {
        return taczexpands$redirectRayTraceBlocks(level, context);
    }

    @Inject(
            method = "onBulletTick",
            at = @At(value = "FIELD",
                    target = "Lcom/tacz/guns/entity/EntityKineticBullet;pierce:I",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void injectDeflectionBeforeHitEntities(CallbackInfo ci, Vec3 startVec, Vec3 endVec, HitResult result, BlockHitResult resultB, List hitEntities) {
        if (taczexpands$deflectionTick) {
            this.setPos(endVec);
            Vec3 movement = this.getDeltaMovement();
            double x = movement.x;
            double y = movement.y;
            double z = movement.z;
            double distance = movement.horizontalDistance();
            this.setYRot((float) Math.toDegrees(Mth.atan2(x, z)));
            this.setXRot((float) Math.toDegrees(Mth.atan2(y, distance)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();

            BulletManager.INSTANCE.notify((EntityKineticBullet) (Object) this);
            onBulletTick();
            ci.cancel();
        }
    }

    @Inject(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;discard()V", ordinal = 1, remap = true), locals = LocalCapture.CAPTURE_FAILHARD)
    private void taczexpands$onBulletTick$injectPierceDiscard(CallbackInfo ci, Vec3 startVec, Vec3 endVec, HitResult result, BlockHitResult resultB, List hitEntities, EntityKineticBullet.EntityResult[] hitEntityResult, EntityKineticBullet.EntityResult[] var7, int var8, int var9, EntityKineticBullet.EntityResult entityResult) {
        taczexpands$overridePosition = entityResult.getHitPos();
        this.setDeltaMovement(taczexpands$overridePosition.subtract(startVec));
    }

    @Redirect(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onHitEntity(Lcom/tacz/guns/util/TacHitResult;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
    public void taczexpands$onBulletTick$redirectOnHitEntity(EntityKineticBullet instance, TacHitResult result, Vec3 startVec, Vec3 endVec) {
        this.onHitEntity(result, startVec, endVec);
        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.handleHitEntity((EntityKineticBullet) (Object) this, new EntityKineticBullet.EntityResult(result.getEntity(), result.getLocation(), result.isHeadshot()), pierce - 1 > 0, !result.getEntity().isAlive());
        }
    }





    @Unique
    public BlockHitResult taczexpands$redirectRayTraceBlocks(Level level, ClipContext context) {
        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.initBulletRayTracer(level, context);
        }

        BiFunction<ClipContext, BlockPos, BlockHitResult> taczexpandsRayTraceFunc = (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            List<String> ids = AmmoConfig.PASS_THROUGH_BLOCKS.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (blockId != null && ids.contains(blockId.toString())) {
                return null;
            }
            if (IAccessorBlockRayTrace.getIGNORES().test(blockState)) {
                return null;
            }

            var blockHitResult = IAccessorBlockRayTrace.getBlockHitResult(level, rayTraceContext, blockPos, blockState);

            if (blockHitResult != null && blockHitResult.getType() != BlockHitResult.Type.MISS) {
                if (taczexpands$walkedBlocks.contains(blockPos)) {
                    return null;
                }

                taczexpands$impacted = true;

                var bulletExtraData = taczexpands$getBulletExtraData();
                var bulletData = bulletExtraData.getBulletData();
                var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData);
                var resistance = ExtensionsKt.getBlockResistance(bulletExtraHolder, blockState);

                if (resistance == null) {
                    return blockHitResult;
                }

                if (taczexpands$penetration > 0) {
                    if (taczexpands$penetration >= resistance.getResistance()) {
                        var event = new BulletPenetrateEvent((EntityKineticBullet) (Object) this, blockHitResult);
                        taczexpands$overridePosition = blockHitResult.getLocation();
                        MinecraftForge.EVENT_BUS.post(event);
                        taczexpands$overridePosition = null;
                        if (!event.isCanceled()) {
                            taczexpands$penetration -= resistance.getResistance();

                            if (bulletExtraHolder.penetrationDecay && taczexpands$maxPenetration > 0) {
                                var factor = taczexpands$penetration / (float) taczexpands$maxPenetration;
                                factor = Mth.clamp(factor, 0.0f, 1.0f);
                                taczexpands$damageFactor *= factor;
                                var deltaMovement = getDeltaMovement();
                                setDeltaMovement(deltaMovement.scale(factor));
                            }


                            var shouldDestroyBlock = (ServerConfig.INSTANCE.getShouldDestroyBlock().get() || resistance.getBypassGlobalDestroyLimit()) && resistance.getShouldDestroyBlock();

                            if (shouldDestroyBlock) {
                                level.levelEvent(2001, blockPos, Block.getId(blockState));
                                level.destroyBlock(blockPos, false, getOwner());
                                return taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                            } else if (resistance.getParticleOnPenetrate()) {
                                level.levelEvent(2001, blockPos, Block.getId(blockState));
                            }

                            taczexpands$walkedBlocks.add(new BlockPos(blockPos));

                            super.onHitBlock(blockHitResult);


                            if (this.level() instanceof ServerLevel serverLevel) {
                                var result = blockHitResult;
                                BlockPos pos = result.getBlockPos();
                                Vec3 hitVec = result.getLocation();
                                if (bulletExtraHolder.renderPenetrationBulletHole) {
                                    BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), new BlockPos(blockPos), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
                                    serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
                                }
                                if (this.igniteBlock) {
                                    serverLevel.sendParticles(ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
                                }
                                if (bulletExtraHolder.renderPenetrationBulletHole) {
                                    var reverseBlockHitResult = IAccessorBlockRayTrace.getBlockHitResult(level, new ClipContext(rayTraceContext.getTo(), rayTraceContext.getFrom(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), blockPos, blockState);
                                    if (reverseBlockHitResult != null && reverseBlockHitResult.getType() != BlockHitResult.Type.MISS) {
                                        var reverseHitVec = reverseBlockHitResult.getLocation();
                                        BulletHoleOption reverseBulletHoleOption = new BulletHoleOption(reverseBlockHitResult.getDirection(), new BlockPos(blockPos), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
                                        serverLevel.sendParticles(reverseBulletHoleOption, reverseHitVec.x, reverseHitVec.y, reverseHitVec.z, 1, 0, 0, 0, 0);
                                    }
                                }


                                if (this.igniteBlock && AmmoConfig.IGNITE_BLOCK.get()) {
                                    BlockPos offsetPos = pos.relative(result.getDirection());
                                    if (BaseFireBlock.canBePlacedAt(this.level(), offsetPos, result.getDirection())) {
                                        BlockState fireState = BaseFireBlock.getState(this.level(), offsetPos);
                                        this.level().setBlock(offsetPos, fireState, Block.UPDATE_ALL_IMMEDIATE);
                                        ((ServerLevel) this.level()).sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0, 0, 0, 0);
                                    }
                                }
                            }


                            return taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                        }
                    }


                    if (this.level() instanceof ServerLevel serverLevel) {
                        var shouldDestroyBlock = (ServerConfig.INSTANCE.getShouldDestroyBlock().get() || resistance.getBypassGlobalDestroyLimit()) && resistance.getShouldDestroyBlock();
                        if (shouldDestroyBlock) {
                            var damage = taczexpands$penetration / (float) resistance.getResistance();
                            var pair = BreakProgressManager.INSTANCE.updateDamage(serverLevel, blockPos, damage);
                            var remainingPenetration = pair.getFirst();
                            var isBroken = pair.getSecond();
                            if (isBroken) {
                                level.levelEvent(2001, blockPos, Block.getId(blockState));
                                level.destroyBlock(blockPos, false, getOwner());
                                taczexpands$walkedBlocks.add(new BlockPos(blockPos));
                                taczexpands$penetration = (int) (remainingPenetration * resistance.getResistance());
                                return taczexpands$tryDeflection(blockHitResult, resistance) ? blockHitResult : null;
                            }
                        }
                    } else {
                        taczexpands$penetration = 0;
                    }

                }

                taczexpands$tryDeflection(blockHitResult, resistance);
            }

            return blockHitResult;
        };

        var hitResult = IAccessorBlockRayTrace.performRayTrace(context, (clipContext, blockPos) -> {
            var taczexpandsResult = taczexpandsRayTraceFunc.apply(clipContext, blockPos);
            if (taczexpandsResult != null && CompatHelper.INSTANCE.hasTACZTweaks()) {
                if (taczexpandsResult.getType() != BlockHitResult.Type.MISS) {
                    var blockState = level.getBlockState(taczexpandsResult.getBlockPos());
                    var result = TACZTweaksCompat.INSTANCE.handleHitBlock((EntityKineticBullet) (Object) this, taczexpandsResult, blockState);
                    return result;
                }
            }

            return taczexpandsResult;

        }, (rayTraceContext) -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(rayTraceContext.getTo()));
        });

        if (CompatHelper.INSTANCE.hasTACZTweaks()) {
            TACZTweaksCompat.INSTANCE.updatePosition((EntityKineticBullet) (Object) this, hitResult.getLocation());
        }

        return hitResult;
    }

    @Unique
    public boolean taczexpands$tryDeflection(BlockHitResult blockHitResult, BlockResistanceData blockResistanceData) {
        if (!blockResistanceData.getDeflectable()) return false;

        var bulletData = taczexpands$bulletExtraData.getBulletData();
        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData);

        var shouldDeflection = bulletExtraHolder.deflection && taczexpands$deflectionTimes < bulletExtraHolder.maxDeflectionCount;
        if (!shouldDeflection) return false;

        var bulletDir = getDeltaMovement().normalize();
        var face = blockHitResult.getDirection();
        var normal = Vec3.atLowerCornerOf(face.getNormal());

        var dot = bulletDir.dot(normal);
        var incidenceAngle = Math.toDegrees(Math.acos(Math.abs(dot)));

        var minIncidenceAngle = bulletExtraHolder.minIncidenceAngle;
        if (incidenceAngle < minIncidenceAngle) {
            return false;
        }

        var reflectDir = bulletDir.subtract(normal.scale(2 * dot));

        var event = new BulletDeflectEvent((EntityKineticBullet) (Object) this, blockHitResult);
        taczexpands$overridePosition = blockHitResult.getLocation();
        MinecraftForge.EVENT_BUS.post(event);
        taczexpands$overridePosition = null;
        if (event.isCanceled()) {
            return false;
        }

        setDeltaMovement(reflectDir.scale(getDeltaMovement().length() * bulletExtraHolder.deflectionSpeedFactor));
        taczexpands$damageFactor *= bulletExtraHolder.deflectionDamageFactor;


        taczexpands$walkedBlocks.add(new BlockPos(blockHitResult.getBlockPos()));
        taczexpands$deflectionTimes++;
        taczexpands$deflectionTick = true;
        if (bulletExtraHolder.renderDeflectionBulletHole) {
            if (level() instanceof ServerLevel serverLevel) {
                BulletHoleOption bulletHoleOption = new BulletHoleOption(blockHitResult.getDirection(), new BlockPos(blockHitResult.getBlockPos()), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
                var hitVec = blockHitResult.getLocation();
                serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }

    @Inject(method = "getDamage", at = @At("RETURN"), cancellable = true)
    public void hookGetDamage(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * taczexpands$damageFactor);
    }
}
