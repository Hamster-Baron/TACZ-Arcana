package group.taczexpands.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;

import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.surface.SurfaceRenderer;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {
    @Inject(
            method = "getMainRenderTarget",
            at = @At(value = "HEAD"),
            cancellable = true)
    private void getMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        // Complete the Surface offscreen pass before depth rendering.
        RenderTarget surface = SurfaceRenderer.target();
        if (surface != null) {
            cir.setReturnValue(surface);
        } else if (Depth.INSTANCE.getDepthRendering()) {
            cir.setReturnValue(Depth.INSTANCE.getDepthRenderTarget());
        }
    }

    @Inject(
            method = "useShaderTransparency",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookUseShaderTransparency(CallbackInfoReturnable<Boolean> cir) {
        if (SurfaceRenderer.isRendering() || Depth.INSTANCE.getDepthRendering()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "handleKeybinds",
            at = @At(value = "HEAD"))
    private void hookHandleKeybinds(CallbackInfo ci) {
        KeyInputs.INSTANCE.onHandleKeybinds();
    }
}
