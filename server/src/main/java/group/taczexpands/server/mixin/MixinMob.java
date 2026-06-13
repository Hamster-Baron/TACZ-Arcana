package group.taczexpands.server.mixin;

import group.taczexpands.server.accessor.IAccessorLivingEntity;
import group.taczexpands.server.skill.ParticleEmitterManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob extends LivingEntity {
    protected MixinMob(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void setTarget(LivingEntity target, CallbackInfo ci) {
        if (target == null) return;
        if (((IAccessorLivingEntity) this).taczexpands$isBlind()) {
            ci.cancel();
            return;
        }

        if (ParticleEmitterManager.INSTANCE.blockedByParticle(this.level(), new Vec3(getX(), getEyeY(), getZ()), new Vec3(target.getX(), target.getEyeY(), target.getZ()))) {
            ci.cancel();
            return;
        }
    }
}
