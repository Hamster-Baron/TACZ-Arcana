package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.render.surface.SurfaceRenderer;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public class MixinHandRenderer {
    @Inject(
            method = {"renderSolid", "renderTranslucent"},
            at = @At("HEAD"),
            cancellable = true)
    private void taczexpands$surfaceskipHand(CallbackInfo ci) {
        if (SurfaceRenderer.isWorldPass()) ci.cancel();
    }
}
