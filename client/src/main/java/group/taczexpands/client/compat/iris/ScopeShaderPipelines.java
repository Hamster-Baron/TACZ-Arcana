package group.taczexpands.client.compat.iris;

import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.config.ScopeShaderConfig;
import group.taczexpands.client.compat.RendererCapabilities;
import static group.taczexpands.client.compat.iris.IrisViewPipelines.*;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import group.taczexpands.client.mixin.compat.iris.IrisPipelineAccessor;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import java.util.*;

/** Scope histories and prewarming on the shared optional Oculus view adapter. */
public final class ScopeShaderPipelines {
    private static final Map<PipelineManager, Map<NamespacedId, WorldRenderingPipeline>> scopePipelines = new IdentityHashMap<>();
    private static final Map<WorldRenderingPipeline, WorldRenderingPipeline> warmTargets = new IdentityHashMap<>();
    private static final ScopeShaderConfig CONFIG = ScopeShaderConfig.current();
    private static final boolean disabled = !CONFIG.enabled;
    static { IrisViewPipelines.configureScopeViews(!disabled); }

    private ScopeShaderPipelines() {}
    public static boolean isScope() { return TACZExpandsClient.Companion.getPatchMainTargetDraw(); }

    public static boolean isLightweightScope() {
        return isScope() && TACZExpandsClient.Companion.shouldUseThermalImaging()
                && RendererCapabilities.current().terrainBindings();
    }

    /** HEAD hook: null delegates pipeline preparation to PipelineManager. */
    public static WorldRenderingPipeline prepareScope(PipelineManager manager, NamespacedId dimension) {
        boolean lightweight = isLightweightScope();
        if (!isScope() || disabled && !lightweight) return null;
        WorldRenderingPipeline pipeline = lightweight
                ? vanillaPipeline() : scopePipeline(manager, dimension);
        ((PipelineAccess) manager).arcana$setPipeline(pipeline);
        bindBuffers(pipeline);
        return pipeline;
    }

    /** RETURN hook: PipelineManager owns and prepares the main view. */
    public static void preparedMain(PipelineManager manager, NamespacedId dimension, WorldRenderingPipeline pipeline) {
        if (disabled || isScope() || pipeline == null) return;
        if (CONFIG.prewarm && !warmTargets.containsKey(pipeline)
                && pipeline instanceof IrisPipelineAccessor && renderFeatureEnabled()) {
            warmTargets.put(pipeline, scopePipeline(manager, dimension));
            ((PipelineAccess) manager).arcana$setPipeline(pipeline);
        }
        bindBuffers(pipeline);
    }

    // Mixin entry points delegate frame and chunk state to the shared view adapter.
    public static void beginDisplayFrame(long timestamp) { IrisViewPipelines.beginDisplayFrame(timestamp); }
    public static void endDisplayFrame() { IrisViewPipelines.endDisplayFrame(); }
    public static boolean advanceCounter() { return IrisViewPipelines.advanceCounter(); }
    public static boolean advanceTimer(long timestamp) { return IrisViewPipelines.advanceTimer(timestamp); }
    public static void selectChunkPrograms(IrisChunkProgramOverrides owner) { IrisViewPipelines.selectChunkPrograms(owner); }
    public static void deleteOtherChunkPrograms(IrisChunkProgramOverrides owner) { IrisViewPipelines.deleteOtherChunkPrograms(owner); }

    public static void destroyScopes(PipelineManager manager) {
        Map<NamespacedId, WorldRenderingPipeline> map = scopePipelines.remove(manager);
        releaseViewPipelines(map == null ? List.of() : map.values());
        warmTargets.clear();
        resetFrameTracking();
    }

    private static WorldRenderingPipeline scopePipeline(PipelineManager manager, NamespacedId dimension) {
        Map<NamespacedId, WorldRenderingPipeline> map = scopePipelines.computeIfAbsent(manager, k -> new HashMap<>());
        WorldRenderingPipeline pipeline = map.get(dimension);
        if (pipeline == null) {
            pipeline = ((PipelineAccess) manager).arcana$getPipelineFactory().apply(dimension);
            map.put(dimension, pipeline);
        }
        return pipeline;
    }

    private static boolean renderFeatureEnabled() { return ClientConfig.INSTANCE.getEnableRenderFunc().get(); }

    /** After the main terrain program exists, compile the corresponding scope variants once. */
    public static void prewarmChunks(IrisChunkProgramOverrides owner, ChunkVertexType vertexType) {
        if (disabled || !CONFIG.prewarm) return;
        PipelineManager manager = manager();
        Map<NamespacedId, WorldRenderingPipeline> map = scopePipelines.get(manager);
        if (map == null || isScope() || isAuxiliaryView()) return;
        WorldRenderingPipeline main = manager.getPipelineNullable();
        // Prewarm the dimension selected by the latest main pipeline preparation.
        WorldRenderingPipeline target = warmTargets.get(main);
        if (target == null) return;
        selectChunkPrograms(owner);
        if (hasChunkPrograms(owner, target)) return;
        PipelineAccess access = (PipelineAccess) manager;
        try {
            access.arcana$setPipeline(target);
            selectChunkPrograms(owner);
            ((ChunkAccess) owner).arcana$createShaders(target.getSodiumTerrainPipeline(), vertexType);
        } finally {
            access.arcana$setPipeline(main);
            selectChunkPrograms(owner);
            bindBuffers(main);
        }
    }
}
