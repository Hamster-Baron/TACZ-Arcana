package group.taczexpands.client.compat.iris;

import group.taczexpands.client.compat.RendererCapabilities;
import group.taczexpands.client.render.surface.SurfaceBackend;
import group.taczexpands.client.render.surface.SurfaceDefinition;
import group.taczexpands.client.render.surface.SurfaceFeed;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.vertices.ImmediateState;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Surface-specific histories. Shared Oculus state belongs to IrisViewPipelines. */
public final class IrisSurfacePipeline implements SurfaceBackend {
    public static final IrisSurfacePipeline INSTANCE = new IrisSurfacePipeline();
    private final Map<SurfaceFeed, WorldRenderingPipeline> cameras = new IdentityHashMap<>();
    private WorldRenderingPipeline selected;
    private boolean layoutSaved, extendedLayout;
    private boolean thermalHookSaved, shaderHook;

    private IrisSurfacePipeline() {}

    public WorldRenderingPipeline get(boolean worldPass) {
        return worldPass && selected != null ? selected : IrisViewPipelines.vanillaPipeline();
    }

    public WorldRenderingPipeline prepare(PipelineManager manager, NamespacedId dimension, SurfaceFeed feed, boolean shaders) {
        selected = shaders
                ? cameras.computeIfAbsent(feed, key -> IrisViewPipelines.createViewPipeline(manager, dimension))
                : IrisViewPipelines.vanillaPipeline();
        IrisViewPipelines.bindViewBuffers(selected);
        return selected;
    }

    @Override public boolean usesShaders(SurfaceDefinition.Camera camera) {
        return !camera.vision().equals("thermal") && camera.shaders() && IrisApi.getInstance().isShaderPackInUse();
    }
    @Override public String cameraUnavailableReason() {
        String missing = RendererCapabilities.current().cameraFailure();
        return missing != null ? missing : IrisTerrainBindings.cameraUnavailableReason();
    }
    @Override public void configureCameraViews(boolean enabled) {
        IrisViewPipelines.configureCameraViews(enabled && RendererCapabilities.current().terrainBindings());
    }
    @Override public void beginView() {
        IrisViewPipelines.beginAuxiliaryView();
        if (SurfaceRenderer.isCameraView() && !SurfaceRenderer.isShaderView()) {
            extendedLayout = ImmediateState.renderWithExtendedVertexFormat;
            layoutSaved = true;
            ImmediateState.renderWithExtendedVertexFormat = false;
            if (SurfaceRenderer.vision().equals("thermal")) {
                shaderHook = IrisCompat.INSTANCE.setHookUsingShaderPack();
                thermalHookSaved = true;
            }
        }
    }
    @Override public void endView() {
        try {
            selected = null;
            if (RendererCapabilities.current().terrainBindings()) IrisTerrainBindings.restoreMain();
            IrisViewPipelines.endAuxiliaryView();
        } finally {
            if (thermalHookSaved) {
                IrisCompat.INSTANCE.restoreHookUsingShaderPack(shaderHook);
                thermalHookSaved = false;
            }
            if (layoutSaved) {
                ImmediateState.renderWithExtendedVertexFormat = extendedLayout;
                layoutSaved = false;
            }
        }
    }
    @Override public void restoreMain() { IrisViewPipelines.restoreMainBuffers(); }
    @Override public void release() {
        selected = null;
        if (RendererCapabilities.current().terrainBindings()) IrisTerrainBindings.release();
        if (cameras.isEmpty()) return;
        var pipelines = new ArrayList<>(cameras.values());
        IrisViewPipelines.releaseViewPipelines(pipelines);
        cameras.clear();
    }
}
