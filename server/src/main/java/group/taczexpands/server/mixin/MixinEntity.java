package group.taczexpands.server.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.server.accessor.IAccessorBullet;
import group.taczexpands.server.accessor.IAccessorEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public class MixinEntity implements IAccessorEntity {

    @Shadow
    private int remainingFireTicks;
    @Unique
    float taczexpands$igniteExtraDamage = 0.0f;

    @Unique
    Entity taczexpands$owner = null;

    @Unique
    Map<String, Integer> taczexpands$hurtCooldownGroups = null;

    @Inject(method = "discard", at = @At("HEAD"))
    public void onDiscard(CallbackInfo ci) {
        if ((Object) this instanceof EntityKineticBullet) {
            ((IAccessorBullet) ((EntityKineticBullet) ((Object) this))).taczexpands$onDiscard();
        }
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    public boolean redirectHurt(Entity instance, DamageSource p_19946_, float p_19947_) {
        var result = instance.hurt(p_19946_, p_19947_);
        if (result && p_19946_ == instance.level().damageSources().onFire()) {
            var igniteExtraDamage = taczexpands$igniteExtraDamage;
            if (igniteExtraDamage > 0.0f) {
                instance.hurt(p_19946_, p_19947_ + igniteExtraDamage);
                if (remainingFireTicks - 1 <= 0) {
                    taczexpands$igniteExtraDamage = 0.0f;
                }
            }
        }
        return result;
    }

    @Override
    public void taczexpands$setIgniteExtraDamage(float damage) {
        taczexpands$igniteExtraDamage = damage;
    }

    @Override
    public Map<String, Integer> taczexpands$getHurtCooldownGroups() {
        if (taczexpands$hurtCooldownGroups == null) {
            taczexpands$hurtCooldownGroups = new HashMap<>();
        }

        return taczexpands$hurtCooldownGroups;
    }


    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"), cancellable = true)
    public void taczexpands$postIsAlliedTo(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;

        if (taczexpands$owner != null && entity == taczexpands$owner) {
            cir.setReturnValue(true);
            return;
        }
    }

    @Override
    public Entity taczexpands$getOwner() {
        return taczexpands$owner;
    }

    @Override
    public void taczexpands$setOwner(Entity owner) {
        taczexpands$owner = owner;
    }
}
