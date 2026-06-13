package group.taczexpands.server.mixin;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.block.ProjectileExplosion;
import group.taczexpands.server.entity.BulletExtraData;
import group.taczexpands.server.event.BulletExplosionHurtEvent;
import group.taczexpands.server.event.BulletExplosionKillEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ProjectileExplosion.class, remap = false, priority = 500)
public class MixinProjectileExplosion {
    @Shadow
    @Final
    private Entity exploder;

    @Shadow
    @Final
    private float power;

    @Redirect(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap = true), remap = true)
    public boolean onHurtEntity(Entity instance, DamageSource damageSource, float v) {
        var isBulletExplosion = false;
        EntityHurtByGunEvent.Pre innerEvent = null;
        if (this.exploder instanceof EntityKineticBullet bullet) {
            isBulletExplosion = true;
            var extraData = BulletExtraData.Companion.get(bullet);
            LivingEntity owner = null;
            if (bullet.getOwner() instanceof LivingEntity livingEntity) {
                owner = livingEntity;
            }
            innerEvent = new EntityHurtByGunEvent.Pre(bullet, instance, owner, bullet.getGunId(), bullet.getGunDisplayId(), v, null, false, 1.0f, LogicalSide.SERVER);
            var eventResult = MinecraftForge.EVENT_BUS.post(new BulletExplosionHurtEvent(innerEvent));
            if (eventResult) {
                return false;
            }
            extraData.addHitEntity(innerEvent, true);
            v = innerEvent.getBaseAmount();
        }
        boolean hurtResult = instance.hurt(damageSource, v);

        if (isBulletExplosion && hurtResult && !instance.isAlive()) {
            MinecraftForge.EVENT_BUS.post(new BulletExplosionKillEvent(innerEvent));
        }

        return hurtResult;
    }
}
