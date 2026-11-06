package group.taczexpands.client.compat.iris;

import group.taczexpands.client.mixin.compat.iris.IrisPipelineAccessor;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkShaderInterface;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisTerrainPass;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/** Shares Oculus 1.8.0 view resources and frame clocks across scope and Surface rendering. */
public final class IrisViewPipelines {
    /** Implemented by the PipelineManager mixin. */
    public interface PipelineAccess {
        Function<NamespacedId, WorldRenderingPipeline> arcana$getPipelineFactory();
        void arcana$setPipeline(WorldRenderingPipeline pipeline);
        void arcana$resetTextureState();
    }

    /** Implemented by the chunk-program mixin. */
    public interface ChunkAccess {
        EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> arcana$getPrograms();
        void arcana$setPrograms(EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> programs);
        boolean arcana$getShadersCreated();
        void arcana$setShadersCreated(boolean created);
        int arcana$getShaderVersion();
        void arcana$setShaderVersion(int version);
        void arcana$createShaders(SodiumTerrainPipeline pipeline, ChunkVertexType vertexType);
    }

    private static final Map<IrisChunkProgramOverrides, ChunkState> chunks = new IdentityHashMap<>();
    private static long epoch, counterEpoch = Long.MIN_VALUE, timerEpoch = Long.MIN_VALUE;
    private static boolean inDisplayFrame, cameraViews, scopeViews, auxiliaryView;
    private static long displayFrame;
    private static int frameClockPauses;
    private static WorldRenderingPipeline vanilla;

    private IrisViewPipelines() {}
    static void configureScopeViews(boolean enabled) { scopeViews = enabled; }
    public static void configureCameraViews(boolean enabled) { cameraViews = enabled; }
    public static void beginAuxiliaryView() { auxiliaryView = true; }
    public static void endAuxiliaryView() { auxiliaryView = false; }
    static boolean isAuxiliaryView() { return auxiliaryView; }
    static long displayFrame() { return displayFrame; }
    private static boolean sharedServicesDisabled() { return !scopeViews && !cameraViews; }
    static PipelineManager manager() { return Iris.getPipelineManager(); }

    public static WorldRenderingPipeline vanillaPipeline() {
        if (vanilla == null) vanilla = preserveChunkSettings(VanillaRenderingPipeline::new);
        return vanilla;
    }

    public static void releaseVanillaPipeline() {
        if (vanilla == null) return;
        releaseViewPipelines(List.of(vanilla));
        vanilla = null;
    }

    public static void beginDisplayFrame(long timestamp) {
        epoch++;
        inDisplayFrame = true;
    }
    public static void endDisplayFrame() { inDisplayFrame = false; }
    public static void pauseFrameClock() { frameClockPauses++; }
    public static void resumeFrameClock() { frameClockPauses--; }

    // A repeated clock call in this display frame belongs to another world view.
    public static boolean advanceCounter() {
        if (frameClockPauses > 0) return false;
        if (!inDisplayFrame) return true;
        if (counterEpoch == epoch) return false;
        counterEpoch = epoch;
        return true;
    }

    public static boolean advanceTimer(long timestamp) {
        if (frameClockPauses > 0) return false;
        if (!inDisplayFrame) return true;
        if (timerEpoch == epoch) return false;
        timerEpoch = epoch;
        displayFrame++;
        return true;
    }

    private record Snapshot(EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> programs,
                            boolean created, int version) {}

    private static final class ChunkState {
        WorldRenderingPipeline active;
        final Map<WorldRenderingPipeline, Snapshot> views = new IdentityHashMap<>();
    }

    public static void selectChunkPrograms(IrisChunkProgramOverrides owner) {
        if (sharedServicesDisabled() && chunks.isEmpty()) return;
        WorldRenderingPipeline pipeline = manager().getPipelineNullable();
        ChunkState state = chunks.computeIfAbsent(owner, k -> new ChunkState());
        if (state.views.isEmpty()) {
            state.active = pipeline;
            state.views.put(pipeline, snapshot(owner));
            return;
        }
        if (state.active == pipeline) return;
        state.views.put(state.active, snapshot(owner));
        Snapshot next = state.views.get(pipeline);
        if (next == null) {
            next = new Snapshot(new EnumMap<>(IrisTerrainPass.class), false,
                    manager().getVersionCounterForSodiumShaderReload());
            state.views.put(pipeline, next);
        }
        ChunkAccess access = (ChunkAccess) owner;
        access.arcana$setPrograms(next.programs());
        access.arcana$setShadersCreated(next.created());
        access.arcana$setShaderVersion(next.version());
        state.active = pipeline;
    }

    private static Snapshot snapshot(IrisChunkProgramOverrides owner) {
        ChunkAccess access = (ChunkAccess) owner;
        return new Snapshot(access.arcana$getPrograms(), access.arcana$getShadersCreated(),
                access.arcana$getShaderVersion());
    }

    public static void deleteOtherChunkPrograms(IrisChunkProgramOverrides owner) {
        ChunkState state = chunks.remove(owner);
        if (state == null) return;
        var current = ((ChunkAccess) owner).arcana$getPrograms();
        Set<EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>>> deleted =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (Snapshot snapshot : state.views.values()) {
            var programs = snapshot.programs();
            if (programs == current || !deleted.add(programs)) continue;
            for (var program : programs.values()) if (program != null) program.delete();
            programs.clear();
        }
    }

    static void bindBuffers(WorldRenderingPipeline pipeline) {
        if (pipeline instanceof IrisPipelineAccessor accessor) {
            var buffers = accessor.arcana$getShaderStorageBufferHolder();
            if (buffers != null) buffers.setupBuffers();
        }
    }

    /** Camera views own history while borrowing the main view's baked chunk layout. */
    public static WorldRenderingPipeline createViewPipeline(PipelineManager manager, NamespacedId dimension) {
        return preserveChunkSettings(() -> ((PipelineAccess) manager).arcana$getPipelineFactory().apply(dimension));
    }

    public static <T> T preserveChunkSettings(Supplier<T> factory) {
        WorldRenderingSettings settings = WorldRenderingSettings.INSTANCE;
        boolean reloadRequired = settings.isReloadRequired();
        var blockStateIds = settings.getBlockStateIds();
        var blockTypeIds = settings.getBlockTypeIds();
        var entityIds = settings.getEntityIds();
        var itemIds = settings.getItemIds();
        float ambientOcclusionLevel = settings.getAmbientOcclusionLevel();
        boolean disableDirectionalShading = settings.shouldDisableDirectionalShading();
        boolean useSeparateAo = settings.shouldUseSeparateAo();
        boolean useExtendedVertexFormat = settings.shouldUseExtendedVertexFormat();
        boolean separateEntityDraws = settings.shouldSeparateEntityDraws();
        boolean voxelizeLightBlocks = settings.shouldVoxelizeLightBlocks();
        try {
            return factory.get();
        } finally {
            if (settings.getBlockStateIds() != blockStateIds) settings.setBlockStateIds(blockStateIds);
            if (settings.getBlockTypeIds() != blockTypeIds) settings.setBlockTypeIds(blockTypeIds);
            // setEntityIds recomputes the villager conversion flag and does not accept null.
            if (settings.getEntityIds() != entityIds) settings.setEntityIds(entityIds);
            settings.setItemIds(itemIds);
            settings.setAmbientOcclusionLevel(ambientOcclusionLevel);
            settings.setDisableDirectionalShading(disableDirectionalShading);
            settings.setUseSeparateAo(useSeparateAo);
            settings.setUseExtendedVertexFormat(useExtendedVertexFormat);
            settings.setSeparateEntityDraws(separateEntityDraws);
            settings.setVoxelizeLightBlocks(voxelizeLightBlocks);
            if (!reloadRequired) settings.clearReloadRequired();
        }
    }

    public static void bindViewBuffers(WorldRenderingPipeline pipeline) { bindBuffers(pipeline); }
    public static void restoreMainBuffers() { bindBuffers(manager().getPipelineNullable()); }

    public static void releaseViewPipelines(Collection<WorldRenderingPipeline> pipelines) {
        // Cached terrain programs hold suppliers into view targets. Release them first.
        for (IrisChunkProgramOverrides owner : new ArrayList<>(chunks.keySet())) owner.deleteShaders();
        PipelineAccess access = (PipelineAccess) manager();
        for (WorldRenderingPipeline pipeline : pipelines) {
            access.arcana$resetTextureState();
            pipeline.destroy();
        }
    }

    static boolean hasChunkPrograms(IrisChunkProgramOverrides owner, WorldRenderingPipeline pipeline) {
        ChunkState state = chunks.get(owner);
        Snapshot snapshot = state == null ? null : state.views.get(pipeline);
        return snapshot != null && snapshot.created();
    }
    static void resetFrameTracking() { counterEpoch = timerEpoch = Long.MIN_VALUE; }
}
