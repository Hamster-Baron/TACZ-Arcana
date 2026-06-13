package group.taczexpands.client.mixin;

import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoEntityDisplay;
import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.client.accessor.IAccessorAmmoEntityDisplay;
import group.taczexpands.client.accessor.IAccessorEntityKineticBullet;
import group.taczexpands.common.accessor.IAccessorBullet;
import group.taczexpands.common.accessor.IAccessorBulletData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = EntityKineticBullet.class, remap = true)
public abstract class MixinEntityKineticBullet extends Projectile implements IAccessorEntityKineticBullet {
    @Unique
    private BedrockAmmoModel taczexpands$animatedModel;

    @Unique
    private AnimationController taczexpands$animationController;

    @Unique
    private boolean taczexpands$animationInited = false;

    @Unique
    private boolean taczexpands$animationPlayed = false;

    protected MixinEntityKineticBullet(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Redirect(method = "lambda$tick$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/particle/AmmoParticleSpawner;addParticle(Lcom/tacz/guns/entity/EntityKineticBullet;)V"))
    public void onParticle(EntityKineticBullet bullet) {
        var bulletData = ((IAccessorBullet) bullet).taczexpands$getBulletData();
        if (bulletData != null && IAccessorBulletData.getBulletExtraHolder(bulletData).missileData.isMissile) {
        } else {
            AmmoParticleSpawner.addParticle(bullet);
        }
    }

    @Nullable
    @Override
    public Map.Entry<BedrockAmmoModel, AnimationController> taczexpands$getAnimatedModel(@NotNull AmmoEntityDisplay ammoEntityDisplay) {
        if (taczexpands$animationInited) {
            if (taczexpands$animatedModel == null || taczexpands$animationController == null) {
                return null;
            }

            taczexpands$updateAnimation(ammoEntityDisplay);
            return Map.entry(taczexpands$animatedModel, taczexpands$animationController);
        }


        taczexpands$animationInited = true;
        var model = IAccessorAmmoEntityDisplay.createAmmoModel(ammoEntityDisplay);
        if (model == null) return null;
        var controller = IAccessorAmmoEntityDisplay.createAnimationController(ammoEntityDisplay, model);
        if (controller == null) return null;

        taczexpands$animatedModel = model;
        taczexpands$animationController = controller;

        taczexpands$updateAnimation(ammoEntityDisplay);

        return Map.entry(taczexpands$animatedModel, taczexpands$animationController);
    }

    @Unique
    private void taczexpands$updateAnimation(@NotNull AmmoEntityDisplay ammoEntityDisplay) {
        if (taczexpands$animationPlayed) return;

        if (this.tickCount >= IAccessorAmmoEntityDisplay.getAnimationDelay(ammoEntityDisplay)) {
            taczexpands$animationPlayed = true;
            var animationName = IAccessorAmmoEntityDisplay.getAnimationName(ammoEntityDisplay);
            if (animationName != null) {
                taczexpands$animationController.runAnimation(0, animationName, ObjectAnimation.PlayType.LOOP, 0.0f);
            }
        }
    }
}
