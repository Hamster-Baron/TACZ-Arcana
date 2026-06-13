package group.taczexpands.client.mixin;

import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import com.tacz.guns.api.client.animation.ObjectAnimationSoundChannel;
import group.taczexpands.client.accessor.IAccessorObjectAnimationRunner;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ObjectAnimationRunner.class, remap = false)
public abstract class MixinObjectAnimationRunner implements IAccessorObjectAnimationRunner {
    @Unique
    private float taczexpands$speed = 1.0f;


    @Override
    public void taczexpands$setSpeed(float speed) {
        this.taczexpands$speed = speed;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 2)
    private long modifyProgress(long alphaProgress) {
        return (long) (alphaProgress * taczexpands$speed);
    }

    @ModifyArg(method = "updateSoundOnly", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/ObjectAnimationRunner;updateProgress(J)V"))
    private long modifySoundOnlyProgress(long progress) {
        return (long) (progress * taczexpands$speed);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/ObjectAnimationSoundChannel;playSound(DDLnet/minecraft/world/entity/Entity;IFF)V"))
    public void invokePlaySound(ObjectAnimationSoundChannel instance, double from, double to, Entity entity, int distance, float volume, float pitch) {
        instance.playSound(from, to, entity, distance, volume, pitch * taczexpands$speed);
    }

    @Redirect(method = "updateSoundOnly", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/ObjectAnimationSoundChannel;playSound(DDLnet/minecraft/world/entity/Entity;IFF)V"))
    public void invokePlaySoundOnly(ObjectAnimationSoundChannel instance, double from, double to, Entity entity, int distance, float volume, float pitch) {
        instance.playSound(from, to, entity, distance, volume, pitch * taczexpands$speed);
    }

}
