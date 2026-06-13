package group.taczexpands.server.mixin;

import group.taczexpands.server.accessor.IAccessorEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(DamageSource.class)
public class MixinDamageSource {
    @Shadow
    @Final
    @Mutable
    @Nullable
    private Entity causingEntity;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    public void taczexpands$postInit(Holder<DamageType> pType, Entity pDirectEntity, Entity pCausingEntity, Vec3 pDamageSourcePosition, CallbackInfo ci) {
        if (pCausingEntity != null) {
            var owner = ((IAccessorEntity) pCausingEntity).taczexpands$getOwner();
            if (owner != null) {
                this.causingEntity = owner;
                return;
            }
        }

        if (pDirectEntity != null) {
            var owner = ((IAccessorEntity) pDirectEntity).taczexpands$getOwner();
            if (owner != null) {
                this.causingEntity = owner;
                return;
            }
        }
    }
}
