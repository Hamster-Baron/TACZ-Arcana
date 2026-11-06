package group.taczexpands.server.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.HitboxHelper;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.data.BulletExtraHolder;
import group.taczexpands.server.accessor.IAccessorServerBullet;
import group.taczexpands.server.entity.BulletExtraData;
import java.util.List;
import java.util.function.Predicate;
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

/** Retained Arcana proximity gates with a swept, block-clipped tick segment. */
@Mixin(value = EntityUtil.class, remap = false)
public class MixinEntityUtil {
    @Unique private static EntityKineticBullet taczexpands$prevBullet;
    @Unique private static float taczexpands$prevProximityFuzeRange;

    @Redirect(method = "findEntityOnPath", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", remap = true))
    private static List<Entity> redirectFindEntityOnPathGetEntities(Level level, Entity projectile, AABB box, Predicate<? super Entity> predicate) {
        return taczexpands$getEntities(level, projectile, box, predicate);
    }

    @Redirect(method = "findEntitiesOnPath", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", remap = true))
    private static List<Entity> redirectFindEntitiesOnPathGetEntities(Level level, Entity projectile, AABB box, Predicate<? super Entity> predicate) {
        return taczexpands$getEntities(level, projectile, box, predicate);
    }

    @Unique
    private static List<Entity> taczexpands$getEntities(Level level, Entity projectile, AABB box, Predicate<? super Entity> predicate) {
        taczexpands$prevBullet = null;
        if (!(projectile instanceof EntityKineticBullet bullet)) return level.getEntities(projectile, box, predicate);
        BulletExtraData state = ((IAccessorServerBullet) bullet).taczexpands$getBulletExtraData();
        BulletData data = state.getBulletData();
        if (data == null) return level.getEntities(projectile, box, predicate);
        BulletExtraHolder extra = IAccessorBulletData.getBulletExtraHolder(data);
        ExplosionData explosion = data.getExplosionData();
        if (explosion == null || !explosion.isExplode() || extra.proximityFuzeRange <= 0
            || !state.isProximityFuzeArmed(bullet, extra)
            || (extra.proximityFuzeMinSpeed > 0 && projectile.getDeltaMovement().lengthSqr()
                <= extra.proximityFuzeMinSpeed * extra.proximityFuzeMinSpeed)) {
            return level.getEntities(projectile, box, predicate);
        }
        taczexpands$prevBullet = bullet;
        taczexpands$prevProximityFuzeRange = extra.proximityFuzeRange;
        // Apply arming gates before selecting candidate targets.
        return level.getEntities(projectile, projectile.getBoundingBox().inflate(extra.proximityFuzeRange)
            .expandTowards(projectile.getDeltaMovement()).inflate(1.0));
    }

    // The third return is the collision-box miss, after damage and friendly-fire exclusions.
    @Inject(method = "getHitResult", at = @At(value = "RETURN", ordinal = 2), cancellable = true)
    private static void postGetHitResult(Projectile projectile, Entity entity, Vec3 start, Vec3 end,
                                        CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        if (cir.getReturnValue() != null || projectile != taczexpands$prevBullet) return;
        Vec3 center = HitboxHelper.getFixedBoundingBox(entity, projectile.getOwner()).getCenter();
        double radius = taczexpands$prevProximityFuzeRange;
        if (taczexpands$distanceToSegmentSqr(center, start, end) <= radius * radius) {
            // Process the proximity target through Arcana's non-headshot hit chain.
            cir.setReturnValue(new EntityKineticBullet.EntityResult(entity, center, false));
        }
    }

    @Unique
    private static double taczexpands$distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        double dx = end.x - start.x, dy = end.y - start.y, dz = end.z - start.z;
        double px = point.x - start.x, py = point.y - start.y, pz = point.z - start.z;
        double lengthSqr = dx * dx + dy * dy + dz * dz;
        double t = lengthSqr > 0 ? Math.max(0, Math.min(1, (px * dx + py * dy + pz * dz) / lengthSqr)) : 0;
        double x = px - t * dx, y = py - t * dy, z = pz - t * dz;
        return x * x + y * y + z * z;
    }
}
