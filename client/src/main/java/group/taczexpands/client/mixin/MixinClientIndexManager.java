package group.taczexpands.client.mixin;

import com.tacz.guns.client.resource.ClientIndexManager;
import group.taczexpands.client.entity.CustomDisplayManager;
import group.taczexpands.client.render.paint.PaintResources;
import group.taczexpands.client.render.surface.ModelTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientIndexManager.class, remap = false)
public class MixinClientIndexManager {
    @Inject(method = "clear", at = @At("HEAD"))
    private static void taczexpands$clearPaint(CallbackInfo ci) {
        PaintResources.clear();
        ModelTargets.clear();
    }

    @Inject(method = "reload", at = @At("TAIL"))
     private static void onReload(CallbackInfo ci) {
        CustomDisplayManager.INSTANCE.onReload();
        PaintResources.reload();
        ModelTargets.refresh();
    }
}
