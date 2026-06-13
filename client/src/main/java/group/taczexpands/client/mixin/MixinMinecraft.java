package group.taczexpands.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.client.render.Depth;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {


    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void getMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (Depth.INSTANCE.getDepthRendering()) {
            cir.setReturnValue(Depth.INSTANCE.getDepthRenderTarget());
        }
    }

    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void hookUseShaderTransparency(CallbackInfoReturnable<Boolean> cir) {
        if (Depth.INSTANCE.getDepthRendering()) cir.setReturnValue(false);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void hookHandleKeybinds(CallbackInfo ci) {
        KeyInputs.INSTANCE.onHandleKeybinds();
    }

}
