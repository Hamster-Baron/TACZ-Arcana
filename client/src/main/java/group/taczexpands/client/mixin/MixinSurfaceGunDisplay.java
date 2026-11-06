package group.taczexpands.client.mixin;

import com.tacz.guns.client.renderer.block.DisplayGunMeshGroup;
import com.tacz.guns.block.entity.GunDisplayBlockEntity;
import group.taczexpands.client.render.surface.ModelTargets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DisplayGunMeshGroup.class, remap = false)
public class MixinSurfaceGunDisplay {
    @Inject(method = "tryRender(Lcom/tacz/guns/block/entity/GunDisplayBlockEntity;II)Z", at = @At("HEAD"), cancellable = true)
    private static void taczexpands$surfacedynamic(
            GunDisplayBlockEntity owner, int index, int overlay, CallbackInfoReturnable<Boolean> ci) {
        if (ModelTargets.dynamic(owner.getItem(index))) ci.setReturnValue(false);
    }
}
