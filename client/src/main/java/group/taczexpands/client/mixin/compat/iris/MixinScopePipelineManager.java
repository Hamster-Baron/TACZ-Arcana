package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.compat.iris.IrisSurfacePipeline;
import group.taczexpands.client.compat.iris.IrisViewPipelines;
import group.taczexpands.client.compat.iris.IrisViewPipelines.PipelineAccess;
import group.taczexpands.client.compat.iris.ScopeShaderPipelines;
import group.taczexpands.client.render.surface.SurfaceRenderer;

import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Function;

/** One selection point for the main view, the scope and model cameras. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.PipelineManager", remap = false)
public abstract class MixinScopePipelineManager implements PipelineAccess {
    @Accessor("pipelineFactory")
    public abstract Function<NamespacedId, WorldRenderingPipeline> arcana$getPipelineFactory();
    @Accessor("pipeline") public abstract void arcana$setPipeline(WorldRenderingPipeline pipeline);
    @Invoker("resetTextureState") public abstract void arcana$resetTextureState();

    @Inject(method = "preparePipeline", at = @At("HEAD"), cancellable = true)
    private void taczexpands$prepareView(
            NamespacedId dimension, CallbackInfoReturnable<WorldRenderingPipeline> ci) {
        if (SurfaceRenderer.isWorldPass()) {
            ci.setReturnValue(IrisSurfacePipeline.INSTANCE.prepare((PipelineManager) (Object) this, dimension, SurfaceRenderer.activeFeed(), SurfaceRenderer.shaderPass()));
            return;
        }
        WorldRenderingPipeline scope = ScopeShaderPipelines.prepareScope((PipelineManager) (Object) this, dimension);
        if (scope != null) ci.setReturnValue(scope);
    }

    @Inject(method = "preparePipeline", at = @At("RETURN"))
    private void taczexpands$preparedMain(
            NamespacedId dimension, CallbackInfoReturnable<WorldRenderingPipeline> ci) {
        if (!SurfaceRenderer.isRendering())
            ScopeShaderPipelines.preparedMain((PipelineManager) (Object) this, dimension, ci.getReturnValue());
    }

    @Inject(method = "getPipelineNullable", at = @At("HEAD"), cancellable = true)
    private void taczexpands$surfacePipeline(CallbackInfoReturnable<WorldRenderingPipeline> ci) {
        if (SurfaceRenderer.isCameraView()) {
            ci.setReturnValue(IrisSurfacePipeline.INSTANCE.get(SurfaceRenderer.isWorldPass()));
        }
    }

    @Inject(method = "getPipeline", at = @At("HEAD"), cancellable = true)
    private void taczexpands$optionalSurfacePipeline(CallbackInfoReturnable<Optional<WorldRenderingPipeline>> ci) {
        if (SurfaceRenderer.isCameraView()) {
            ci.setReturnValue(Optional.of(IrisSurfacePipeline.INSTANCE.get(SurfaceRenderer.isWorldPass())));
        }
    }

    @Inject(method = "destroyPipeline", at = @At("HEAD"))
    private void taczexpands$destroyViews(CallbackInfo ci) {
        IrisSurfacePipeline.INSTANCE.release();
        ScopeShaderPipelines.destroyScopes((PipelineManager) (Object) this);
        IrisViewPipelines.releaseVanillaPipeline();
    }
}
