package group.taczexpands.client.render.flashlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.BlendMode;
import group.taczexpands.client.accessor.IAccessorGameRenderer;
import group.taczexpands.client.accessor.IAccessorLevelRenderer;
import group.taczexpands.client.compat.RendererPresence;
import group.taczexpands.client.compat.iris.FlashlightShaderState;
import group.taczexpands.client.override.BeamRendererOverride;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/** Render state borrowed by the flashlight's depth view. */
public final class FlashlightDepthState {
    private static final BlendMode OPAQUE = new BlendMode();
    private static final BlendMode OUTLINE = new BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);

    /** Keep post-effect mode caching and the separate alpha factors in agreement. */
    public static void prepareOutlineBlend() {
        OPAQUE.apply();
        OUTLINE.apply();
    }

    private final Minecraft minecraft = Minecraft.getInstance();
    private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final int[] viewport = new int[4];
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    private final Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
    private final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
    private final Camera entityCamera = minecraft.getEntityRenderDispatcher().camera;
    private final Quaternionf entityOrientation = new Quaternionf(minecraft.getEntityRenderDispatcher().cameraOrientation());
    private final Camera blockEntityCamera = minecraft.getBlockEntityRenderDispatcher().camera;
    private final boolean renderHand =
            ((IAccessorGameRenderer) minecraft.gameRenderer).taczexpands$getRenderHand();
    private final Matrix4f beamProjection = new Matrix4f(BeamRendererOverride.lastProjectionMatrix);
    private final Matrix4f beamView = new Matrix4f(BeamRendererOverride.lastViewMatrix);
    private final IAccessorLevelRenderer levelRenderer = (IAccessorLevelRenderer) minecraft.levelRenderer;
    private final Frustum cullingFrustum = levelRenderer.taczexpands$getCullingFrustum();
    private final Frustum capturedFrustum = levelRenderer.taczexpands$getCapturedFrustum();
    private final boolean captureFrustum = levelRenderer.taczexpands$getCaptureFrustum();
    private final FlashlightShaderState shaderState =
            RendererPresence.OCULUS_PRESENT ? FlashlightShaderState.begin() : null;

    private FlashlightDepthState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
    }

    public static FlashlightDepthState begin() {
        FlashlightDepthState state = new FlashlightDepthState();
        // glClear obeys the write mask left disabled by Iris final composition.
        RenderSystem.depthMask(true);
        return state;
    }

    public static Matrix4f handProjection() {
        return RendererPresence.OCULUS_PRESENT
                ? FlashlightShaderState.handProjection()
                : new Matrix4f(RenderSystem.getProjectionMatrix());
    }

    public void restore() {
        try {
            if (shaderState != null) shaderState.restore();
        } finally {
            try {
                levelRenderer.taczexpands$setCaptureFrustum(captureFrustum);
                levelRenderer.taczexpands$setCapturedFrustum(capturedFrustum);
                levelRenderer.taczexpands$setCullingFrustum(cullingFrustum);
                BeamRendererOverride.lastViewMatrix = beamView;
                BeamRendererOverride.lastProjectionMatrix = beamProjection;
                minecraft.gameRenderer.setRenderHand(renderHand);
                minecraft.getEntityRenderDispatcher().camera = entityCamera;
                minecraft.getEntityRenderDispatcher().overrideCameraOrientation(entityOrientation);
                minecraft.getBlockEntityRenderDispatcher().camera = blockEntityCamera;
            } finally {
                try {
                    RenderSystem.getModelViewStack().last().pose().set(modelView);
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.setInverseViewRotationMatrix(inverseView);
                    minecraft.gameRenderer.resetProjectionMatrix(projection);
                } finally {
                    RenderSystem.depthMask(depthMask);
                    RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                }
            }
        }
    }
}
