package group.taczexpands.client.mixin;

import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.CompatHelper;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = CapturedRenderingState.class, remap = false)
public class MixinCapturedRenderingState {
    @Inject(method = "getCurrentRenderedEntity", at = @At("HEAD"), cancellable = true)
    public void hookGetCurrentRenderedEntity(CallbackInfoReturnable<Integer> cir) {
        if(CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse() && TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(32765);
        }
    }
}
