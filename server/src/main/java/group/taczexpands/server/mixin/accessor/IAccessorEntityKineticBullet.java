package group.taczexpands.server.mixin.accessor;

import com.tacz.guns.entity.EntityKineticBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EntityKineticBullet.class, remap = false)
public interface IAccessorEntityKineticBullet {
    @Accessor float getSpeed();
    @Accessor float getGravity();
    @Accessor float getFriction();
    @Accessor boolean getExplosion();
    @Accessor float getExplosionDamage();
    @Accessor float getExplosionRadius();
    @Accessor int getExplosionDelayCount();

    @Accessor void setSpeed(float speed);
    @Accessor void setGravity(float gravity);
    @Accessor void setFriction(float friction);
    @Accessor void setExplosion(boolean explosion);
    @Accessor void setExplosionDamage(float damage);
    @Accessor void setExplosionRadius(float radius);
    @Accessor void setExplosionDelayCount(int count);
}
