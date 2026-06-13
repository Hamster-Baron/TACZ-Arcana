package group.taczexpands.server.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.HitboxHelper;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.server.accessor.IAccessorBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EntityUtil.class, remap = false)
public class MixinEntityUtil {
    @Unique
    private static EntityKineticBullet taczexpands$prevBullet;

    @Unique
    private static float taczexpands$prevProximityFuzeRange = 0.0f;

    @Redirect(method = "findEntityOnPath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", remap = true))
    private static List<Entity> redirectFindEntityOnPathGetEntities(Level instance, Entity bulletEntity, AABB boundingBox, Predicate<? super Entity> predicate) {
        return taczexpands$getEntities(instance, bulletEntity, boundingBox, predicate);
    }

    @Redirect(method = "findEntitiesOnPath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", remap = true))
    private static List<Entity> redirectFindEntitiesOnPathGetEntities(Level instance, Entity bulletEntity, AABB boundingBox, Predicate<? super Entity> predicate) {
        return taczexpands$getEntities(instance, bulletEntity, boundingBox, predicate);
    }

    @Unique
    private static List<Entity> taczexpands$getEntities(Level instance, Entity bulletEntity, AABB boundingBox, Predicate<? super Entity> predicate) {
        taczexpands$prevBullet = null;
        if (!(bulletEntity instanceof EntityKineticBullet bullet)) return instance.getEntities(bulletEntity, boundingBox, predicate);

        var bulletExtraData = ((IAccessorBullet) bullet).taczexpands$getBulletExtraData();
        var bulletData = bulletExtraData.getBulletData();
        if (bulletData == null) return instance.getEntities(bulletEntity, boundingBox, predicate);

        var bulletExtraHolder = IAccessorBulletData.getBulletExtraHolder(bulletData);
        var explosionData = bulletData.getExplosionData();
        if (explosionData == null || !explosionData.isExplode()) return instance.getEntities(bulletEntity, boundingBox, predicate);
        if (bulletExtraHolder.proximityFuzeRange <= 0) return instance.getEntities(bulletEntity, boundingBox, predicate);

        if (bulletExtraHolder.proximityFuzeMinSpeed > 0
                && bulletEntity.getDeltaMovement().lengthSqr() <= bulletExtraHolder.proximityFuzeMinSpeed * bulletExtraHolder.proximityFuzeMinSpeed)
            return instance.getEntities(bulletEntity, boundingBox, predicate);

        taczexpands$prevBullet = bullet;
        taczexpands$prevProximityFuzeRange = bulletExtraHolder.proximityFuzeRange;
        return instance.getEntities(bulletEntity, bulletEntity.getBoundingBox().inflate(bulletExtraHolder.proximityFuzeRange).expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0));
    }

    @Inject(method = "getHitResult", at = @At("RETURN"), cancellable = true)
    private static void postGetHitResult(Projectile bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec, CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        var result = cir.getReturnValue();
        if (result != null) return;
        if (bulletEntity != taczexpands$prevBullet) return;

        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, bulletEntity.getOwner());
        var center = boundingBox.getCenter();
        if (bulletEntity.distanceToSqr(center) <= taczexpands$prevProximityFuzeRange * taczexpands$prevProximityFuzeRange) {
            cir.setReturnValue(new EntityKineticBullet.EntityResult(entity, center, false));
        }
    }
}
