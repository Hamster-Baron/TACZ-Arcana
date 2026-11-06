package group.taczexpands.client.compat.iris;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4f;

/** Oculus state borrowed by the flashlight's depth view. */
public final class FlashlightShaderState {
    private final Matrix4f modelView = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView());
    private final Matrix4f projection = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection());

    private FlashlightShaderState() {}

    public static VanillaRenderingPipeline createVanilla() {
        return IrisViewPipelines.preserveChunkSettings(VanillaRenderingPipeline::new);
    }

    public static FlashlightShaderState begin() {
        FlashlightShaderState state = new FlashlightShaderState();
        // Pause the depth pass clock; enclosing views retain their configured cadence.
        IrisViewPipelines.pauseFrameClock();
        return state;
    }

    public void restore() {
        try {
            CapturedRenderingState.INSTANCE.setGbufferProjection(projection);
            CapturedRenderingState.INSTANCE.setGbufferModelView(modelView);
        } finally {
            IrisViewPipelines.resumeFrameClock();
        }
    }

    public static Matrix4f handProjection() {
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        // Undo Oculus hand-depth compression to recover the physical flashlight ray.
        if (HandRenderer.INSTANCE.isActive()) {
            projection.m02(projection.m02() * 8);
            projection.m12(projection.m12() * 8);
            projection.m22(projection.m22() * 8);
            projection.m32(projection.m32() * 8);
        }
        return projection;
    }
}
