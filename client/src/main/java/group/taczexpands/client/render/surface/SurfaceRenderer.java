package group.taczexpands.client.render.surface;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import group.taczexpands.client.accessor.IAccessorLightTexture;
import group.taczexpands.client.compat.RendererPresence;
import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.Flashlight;
import group.taczexpands.client.util.RenderHelper;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.irisshaders.iris.api.v0.IrisApi;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Draws one offscreen view and owns its transient pass state. Registry and scheduling live in SurfaceEngine. */
public final class SurfaceRenderer {
    private static SurfaceFeed active;
    private static boolean worldPass, cameraView, shaderView;
    private static SurfaceCameraPose activeCamera;
    private static final SurfaceCamera camera = new SurfaceCamera();
    private static SurfaceBackend backend = SurfaceBackend.VANILLA;

    private SurfaceRenderer() {}

    static void installBackend(SurfaceBackend integration) {
        if (active != null) throw new IllegalStateException("Cannot replace backend during a surface draw");
        backend = Objects.requireNonNull(integration);
    }
    static void configureCameraViews(boolean enabled) { backend.configureCameraViews(enabled); }
    static void releaseViews() { backend.release(); }

    public static SurfaceFeed activeFeed() {
        return active;
    }

    public static boolean isRendering() {
        return active != null;
    }

    public static boolean secondaryPass() {
        return isRendering() || Depth.INSTANCE.getDepthRendering()
                || RendererPresence.OCULUS_PRESENT && IrisApi.getInstance().isRenderingShadowPass();
    }

    public static boolean isWorldPass() {
        return worldPass;
    }

    public static SurfaceCamera camera() {
        return camera;
    }

    public static RenderTarget target() {
        return active == null ? null : worldPass ? active.viewTarget() : active.target();
    }

    static void endView() {
        active = null;
        worldPass = false;
        cameraView = false;
        shaderView = false;
        activeCamera = null;
    }

    public static boolean isCameraView() { return cameraView; }
    public static boolean isShaderView() { return shaderView; }

    public static boolean cameraPass() {
        return cameraView && !SurfaceHud.isDrawing();
    }
    public static boolean shaderPass() {
        return worldPass && shaderView;
    }
    public static float cameraDistance() { return activeCamera == null ? 0 : activeCamera.distance(); }
    public static String vision() { return cameraPass() ? activeCamera.vision() : "normal"; }

    static void prepare(SurfaceFeed feed, float partialTick, long now) {
        feed.resolveCamera(partialTick, now);
        if (feed.cameraPose() != null) feed.cameraFailure(backend.cameraUnavailableReason());
    }

    static void updateLight(float partialTick) {
        var light = Minecraft.getInstance().gameRenderer.lightTexture();
        ((IAccessorLightTexture) light).taczexpands$setUpdateLightTexture();
        light.updateLightTexture(partialTick);
    }

    static void update(SurfaceFeed feed, float partialTick, long nanos, Set<String> feedIds) {
        if (active != null) throw new IllegalStateException("Nested surface render");
        long start = System.nanoTime();
        prepare(feed, partialTick, start);
        boolean renderCamera = feed.cameraPose() != null && feed.cameraFailure() == null;
        SurfaceBackend passBackend = renderCamera ? backend : SurfaceBackend.VANILLA;
        boolean renderShaders = renderCamera && backend.usesShaders(feed.cameraSpec());
        try (var state = new SurfaceRenderState(renderCamera, partialTick, passBackend)) {
            var modelView = RenderSystem.getModelViewStack();
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            feed.allocate();
            active = feed;
            activeCamera = renderCamera ? feed.cameraPose() : null;
            cameraView = renderCamera;
            shaderView = renderShaders;
            passBackend.beginView();
            feed.target().setClearColor(0, 0, 0, renderCamera ? 1 : 0);
            feed.target().clear(Minecraft.ON_OSX);
            feed.target().bindWrite(true);
            if (renderCamera) renderWorld(feed, partialTick, nanos);
            composite(feed, renderCamera, feedIds);
            feed.recordUpdate();
        } catch (RuntimeException exception) {
            // Attach the feed ID while preserving the cause and suppressed cleanup exceptions.
            throw new IllegalStateException("Rendering surface '" + feed.id() + "'", exception);
        } finally {
            feed.recordRenderTime(System.nanoTime() - start);
        }
    }

    private static void renderWorld(SurfaceFeed feed, float partialTick, long nanos) {
        var mc = Minecraft.getInstance();
        var spec = activeCamera;
        feed.allocateView();
        worldPass = true;
        feed.viewTarget().bindWrite(true);
        camera.configure(spec, partialTick, feed.boneCamera());
        var view = new PoseStack();
        view.mulPose(Axis.ZP.rotationDegrees(spec.roll()));
        view.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        view.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180));
        Matrix4f projection =
                new Matrix4f()
                        .perspective(
                                (float) Math.toRadians(spec.fov()),
                                (float) feed.definition().width() / feed.definition().height(),
                                0.05f,
                                spec.distance());
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);
        updateLight(partialTick);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setInverseViewRotationMatrix(new Matrix3f(view.last().normal()).invert());
        mc.getProfiler().push("arcana_surface_camera");
        try {
            mc.levelRenderer.prepareCullFrustum(view, camera.getPosition(), projection);
            mc.levelRenderer.renderLevel(
                    view,
                    partialTick,
                    nanos,
                    false,
                    camera,
                    mc.gameRenderer,
                    mc.gameRenderer.lightTexture(),
                    projection);
            if (Flashlight.INSTANCE.hasPreparedLights()) {
                var light = feed.flashlight();
                Flashlight.INSTANCE.composite(
                        partialTick, view, feed.viewTarget(), light.getChain(), light.getDepthBackup());
            }
        } finally {
            mc.getProfiler().pop();
        }
    }

    private static void composite(SurfaceFeed feed, boolean renderCamera, Set<String> feedIds) {
        worldPass = false;
        feed.target().bindWrite(true);
        if (renderCamera) {
            RenderHelper.INSTANCE.blitScope(
                    feed.definition().width(), feed.definition().height(), true, feed.viewTarget());
            // World sky is alpha-zero; a camera display itself is opaque.
            RenderSystem.colorMask(false, false, false, true);
            RenderSystem.clearColor(0, 0, 0, 1);
            RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.colorMask(true, true, true, true);
        }
        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0, feed.definition().width(), feed.definition().height(), 0, 1000, 21000),
                VertexSorting.ORTHOGRAPHIC_Z);
        var modelView = RenderSystem.getModelViewStack();
        modelView.setIdentity();
        modelView.translate(0, 0, -11000);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        SurfaceHud.render(feed, feedIds);
    }

}
