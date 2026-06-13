package group.taczexpands.client.mixin;

import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AmmoParticleSpawner.class, remap = false)
public abstract class MixinAmmoParticleSpawner {
    @Shadow
    private static void spawnParticle(EntityKineticBullet bullet, AmmoParticle particle) {
    }

    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
    private static void createParticle(EntityKineticBullet bullet, CallbackInfo ci) {
        if (bullet != Minecraft.getInstance().cameraEntity) {

        } else {
            ci.cancel();
        }
    }

    @Inject(method = "spawnParticle", at = @At("RETURN"))
    private static void taczexpands$postSpawnParticle(EntityKineticBullet bullet, AmmoParticle particle, CallbackInfo ci) {
        var next = IAccessorAmmoParticle.getNext(particle);
        if (next != null) {
            spawnParticle(bullet, next);
        }
    }
}
