package group.taczexpands.client.render.surface;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import group.taczexpands.client.mixin.accessor.IAccessorBlendState;
import group.taczexpands.client.mixin.accessor.IAccessorBooleanState;
import group.taczexpands.client.mixin.accessor.IAccessorGlStateManager;
import net.minecraft.client.Minecraft;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Render-thread scope. Mandatory state is restored before optional backend work can fail. */
final class SurfaceRenderState implements AutoCloseable {
    private final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    private final VertexSorting sorting = RenderSystem.getVertexSorting();
    private final Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
    private final int[] viewport = {GlStateManager.Viewport.x(), GlStateManager.Viewport.y(),
            GlStateManager.Viewport.width(), GlStateManager.Viewport.height()};
    private final boolean blendEnabled;
    private final int[] blendFunction;
    private final PoseStack modelView = RenderSystem.getModelViewStack();
    private final boolean camera;
    private final SurfaceBackend backend;
    private final float partialTick;

    SurfaceRenderState(boolean camera, float partialTick, SurfaceBackend backend) {
        this.camera = camera;
        this.backend = backend;
        this.partialTick = partialTick;
        var blend = (IAccessorBlendState) IAccessorGlStateManager.taczexpands$getBlend();
        blendEnabled = ((IAccessorBooleanState) blend.taczexpands$getMode()).taczexpands$isEnabled();
        blendFunction = new int[]{blend.taczexpands$getSrcRgb(), blend.taczexpands$getDstRgb(),
                blend.taczexpands$getSrcAlpha(), blend.taczexpands$getDstAlpha()};
        modelView.pushPose();
    }

    @Override
    public void close() {
        SurfaceRenderer.endView();
        target.bindWrite(false);
        RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        RenderSystem.setProjectionMatrix(projection, sorting);
        RenderSystem.setInverseViewRotationMatrix(inverseView);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        try {
            if (camera) {
                Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
                SurfaceRenderer.updateLight(partialTick);
            }
        } finally {
            try {
                backend.endView();
                backend.restoreMain();
            } finally {
                RenderSystem.blendFuncSeparate(blendFunction[0], blendFunction[1], blendFunction[2], blendFunction[3]);
                if (blendEnabled) RenderSystem.enableBlend();
                else RenderSystem.disableBlend();
            }
        }
    }
}
