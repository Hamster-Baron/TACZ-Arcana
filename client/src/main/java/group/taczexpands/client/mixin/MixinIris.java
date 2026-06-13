package group.taczexpands.client.mixin;

import group.taczexpands.client.render.Depth;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = Iris.class, remap = false)
public class MixinIris {
    @Unique
    private static PipelineManager taczexpands$vanillaRenderingPipeline = new PipelineManager((id) -> new VanillaRenderingPipeline());

    @Inject(method = "getPipelineManager", at = @At("HEAD"), cancellable = true)
    private static void hookGetPipelineManager(CallbackInfoReturnable<PipelineManager> cir) {
        if (Depth.INSTANCE.getDepthRendering()) cir.setReturnValue(taczexpands$vanillaRenderingPipeline);
    }
}
