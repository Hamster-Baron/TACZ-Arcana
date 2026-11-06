package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.compat.iris.ScopeShaderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.SystemTimeUniforms$Timer", remap = false)
public abstract class MixinScopeTimer {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private void arcana$oncePerDisplay(long timestamp, CallbackInfo ci) {
        if (!ScopeShaderPipelines.advanceTimer(timestamp)) {
            ci.cancel();
        }
    }
}
