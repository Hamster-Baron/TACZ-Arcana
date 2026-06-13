package group.taczexpands.server.mixin;

import group.taczexpands.server.accessor.IAccessorLivingEntity;
import group.taczexpands.server.skill.ParticleEmitterManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = LivingEntity.class, remap = true)
public abstract class MixinLivingEntity extends Entity implements IAccessorLivingEntity {
    @Unique
    protected int taczexpands$blindUntil = -1;

    @Shadow
    protected boolean jumping;

    public MixinLivingEntity(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Inject(method = "hasLineOfSight", at = @At(value = "RETURN", ordinal = 2), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void hookHasLineOfSight(Entity p_147185_, CallbackInfoReturnable<Boolean> cir, Vec3 left, Vec3 right) {
        if (taczexpands$isBlind()) {
            cir.setReturnValue(false);
            return;
        }
        if (cir.getReturnValue()) {
            if (ParticleEmitterManager.INSTANCE.blockedByParticle(this.level(), left, right)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Override
    public boolean taczexpands$isJumping() {
        return this.jumping;
    }

    @Override
    public void taczexpands$setBlindTime(int time) {
        taczexpands$blindUntil = this.tickCount + time;
    }

    @Override
    public boolean taczexpands$isBlind() {
        return taczexpands$blindUntil > this.tickCount;
    }
}
