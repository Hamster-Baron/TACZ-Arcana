package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.compat.iris.ScopeShaderPipelines;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinScopeDisplayFrame {
    @Inject(method = "render", at = @At("HEAD"))
    private void arcana$displayStart(float partial, long timestamp, boolean renderLevel, CallbackInfo ci) {
        ScopeShaderPipelines.beginDisplayFrame(timestamp);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void arcana$displayEnd(float partial, long timestamp, boolean renderLevel, CallbackInfo ci) {
        ScopeShaderPipelines.endDisplayFrame();
    }
}
