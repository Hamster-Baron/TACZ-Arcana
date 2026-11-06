package group.taczexpands.client.render.surface;

import group.taczexpands.client.render.FlashlightComposition;

import com.mojang.blaze3d.pipeline.TextureTarget;
import group.taczexpands.client.compat.RendererCapabilities;
import group.taczexpands.client.compat.RendererPresence;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.iris.IrisSurfacePbr;
import group.taczexpands.common.network.v2.s2c.S2CAddHUD;
import group.taczexpands.client.render.ShaderTextureReferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** Owns one feed's immutable definition, GPU targets and update statistics; access on the render thread. */
public final class SurfaceFeed extends AbstractTexture {
    private final String id;
    private final SurfaceDefinition definition;
    private final List<S2CAddHUD> staticHud;
    private final ResourceLocation texture;
    private final RenderType renderType;
    private final RenderType shaderRenderType;
    private final long interval, phase;
    private TextureTarget target, viewTarget;
    private FlashlightComposition flashlight;
    private String cameraFailure;
    private SurfaceCameraOverride cameraOverride;
    private SurfaceDefinition.Camera cameraSpec;
    private SurfaceCameraPose cameraPose;
    private long seenFrame = -100, lastUpdate, updates, renderNanos;

    SurfaceFeed(String id, SurfaceConfig.Feed spec, int serial, int count) {
        this.id = id;
        definition = SurfaceDefinition.fromConfig(id, spec);
        cameraSpec = definition.camera();
        interval = 1_000_000_000L / definition.fps();
        phase = interval * serial / count;
        lastUpdate = System.nanoTime() - interval + phase;
        staticHud = SurfaceHud.compile(definition.hud());
        texture = new ResourceLocation("taczexpands", "surface/" + serial);
        renderType = SurfaceMaterial.create(texture);
        shaderRenderType = RenderType.entityCutoutNoCull(texture);
    }

    public String id() { return id; }
    public SurfaceDefinition definition() { return definition; }
    public SurfaceDefinition.Camera cameraSpec() { return cameraSpec; }
    public SurfaceCameraPose cameraPose() { return cameraPose; }
    public boolean boneCamera() {
        var spec = cameraSpec();
        return spec != null && spec.space().equals("bone");
    }
    String boneName() { return cameraOverride == null || cameraOverride.values == null ? null : cameraOverride.values.bone(); }
    boolean hasCamera() { return cameraSpec != null; }
    void cameraOverride(SurfaceCameraOverride value) {
        cameraOverride = value;
        cameraSpec = value == null ? definition.camera() : value.enabled()
                ? value.values.settings(definition.camera()) : null;
    }
    void resolveCamera(float partialTick, long now) {
        var spec = cameraSpec();
        cameraPose = spec == null ? null : boneCamera() ? SurfaceBoneAnchor.pose(id, boneName(), spec) : SurfaceCamera.resolve(spec,
                cameraOverride == null ? null : cameraOverride.values.anchor(), partialTick);
        if (cameraOverride != null) cameraPose = cameraOverride.interpolate(cameraPose, now);
        cameraFailure = spec != null && cameraPose == null ? "Camera anchor is unavailable" : null;
    }
    public String cameraFailure() { return cameraFailure; }
    void cameraFailure(String reason) { cameraFailure = reason; }
    public long updates() { return updates; }
    public long renderNanos() { return renderNanos; }

    List<S2CAddHUD> staticHud() { return staticHud; }
    RenderType renderType() {
        return RendererPresence.OCULUS_PRESENT && IrisCompat.INSTANCE.hasShaderPackInUse()
                ? shaderRenderType : renderType;
    }
    ResourceLocation texture() { return texture; }
    TextureTarget target() { return target; }
    TextureTarget viewTarget() { return viewTarget; }
    long seenFrame() { return seenFrame; }
    void markSeen(long frame) { seenFrame = frame; }
    boolean readyCamera() { return cameraPose != null && cameraFailure == null; }
    long nextUpdate() { return lastUpdate + interval; }
    boolean due(long frame, long now) {
        return frame - seenFrame <= 2 && now >= nextUpdate();
    }
    void updated(long now) { lastUpdate = now; }
    void recordUpdate() { updates++; }
    void recordRenderTime(long nanos) { renderNanos += nanos; }

    void allocate() {
        if (target != null) return;
        target = new TextureTarget(definition.width(), definition.height(), true, Minecraft.ON_OSX);
        target.setFilterMode(GL11.GL_LINEAR);
        target.setClearColor(0, 0, 0, hasCamera() ? 1 : 0);
        target.clear(Minecraft.ON_OSX);
        Minecraft.getInstance().getTextureManager().register(texture, this);
        if (RendererCapabilities.current().surfacePbr()) IrisSurfacePbr.track(this);
    }

    TextureTarget allocateView() {
        if (viewTarget == null)
            viewTarget = new TextureTarget(definition.width(), definition.height(), true, Minecraft.ON_OSX);
        return viewTarget;
    }

    FlashlightComposition flashlight() {
        if (flashlight == null) flashlight = new FlashlightComposition(viewTarget);
        return flashlight;
    }

    void release() {
        if (target != null) Minecraft.getInstance().getTextureManager().release(texture);
        else close();
        seenFrame = -100;
        lastUpdate = System.nanoTime() - interval + phase;
    }

    @Override public int getId() { return target == null ? -1 : target.getColorTextureId(); }
    @Override public void load(ResourceManager resources) {}
    @Override public void close() {
        if (flashlight != null) {
            flashlight.close();
            flashlight = null;
        }
        if (viewTarget != null) {
            destroyTarget(viewTarget);
            viewTarget = null;
        }
        if (target != null) {
            destroyTarget(target);
            target = null;
        }
    }
    private static void destroyTarget(TextureTarget target) {
        ShaderTextureReferences.clear(target.getColorTextureId());
        ShaderTextureReferences.clear(target.getDepthTextureId());
        target.destroyBuffers();
    }
    @Override public void releaseId() { close(); }
}
