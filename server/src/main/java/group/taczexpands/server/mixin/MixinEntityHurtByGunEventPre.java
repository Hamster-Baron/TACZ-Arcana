package group.taczexpands.server.mixin;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import group.taczexpands.server.accessor.IAccessorHitVec;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = EntityHurtByGunEvent.Pre.class, remap = false)
public class MixinEntityHurtByGunEventPre implements IAccessorHitVec {
    @Unique
    private Vec3 taczexpands$hitVec = null;


    @Unique
    @Override
    public void taczexpands$setHitVec(Vec3 vec) {
        taczexpands$hitVec = vec;
    }

    @Unique
    @Override
    public Vec3 taczexpands$getHitVec() {
        return taczexpands$hitVec;
    }
}
