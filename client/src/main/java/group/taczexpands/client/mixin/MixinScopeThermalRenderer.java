package group.taczexpands.client.mixin;

import com.tacz.guns.client.renderer.scope.ScopeThermalRenderer;
import group.taczexpands.client.render.scope.ScopeLensComposition;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScopeThermalRenderer.class, remap = false)
public abstract class MixinScopeThermalRenderer {
    @Inject(method = "onWorldStage", at = @At("HEAD"), cancellable = true)
    private static void taczexpands$keepLensView(RenderLevelStageEvent event, CallbackInfo ci) {
        if (SurfaceRenderer.isWorldPass() || ScopeLensComposition.preserveThermalMask()) ci.cancel();
    }
}
