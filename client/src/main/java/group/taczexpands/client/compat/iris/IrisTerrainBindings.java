package group.taczexpands.client.compat.iris;

import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import net.irisshaders.iris.compat.sodium.impl.IrisChunkShaderBindingPoints;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Iris extended layout and offscreen basic terrain programs; call on the render thread. */
public final class IrisTerrainBindings {
    public interface ProgramOwner {
        Map<Object, Object> surface$getPrograms();
        void surface$setPrograms(Map<Object, Object> programs);
    }
    private static final class State {
        final ChunkVertexType vertexType;
        final Map<Object, Object> cameraPrograms = new HashMap<>();
        Map<Object, Object> mainPrograms;
        State(ChunkVertexType vertexType) { this.vertexType = vertexType; }
    }
    private static final Map<ProgramOwner, State> states = new IdentityHashMap<>();
    private static final Map<String, Integer> locations = Map.of(
            "a_PosId", IrisChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
            "a_Color", IrisChunkShaderBindingPoints.ATTRIBUTE_COLOR,
            "a_TexCoord", IrisChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
            "a_LightCoord", IrisChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE);
    private static boolean extendedCamera;

    private IrisTerrainBindings() {}

    /** Main terrain draws establish the actual shared mesh layout before offscreen passes can use it. */
    public static void observe(ProgramOwner owner, ChunkVertexType vertexType) {
        states.computeIfAbsent(owner, ignored -> new State(vertexType));
    }

    public static String cameraUnavailableReason() {
        if (!WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) return null;
        if (states.isEmpty()) return "Waiting for the main terrain vertex layout";
        for (var state : states.values())
            if (state.vertexType != IrisModelVertexFormats.MODEL_VERTEX_XHFP)
                return "Unsupported extended terrain vertex layout: " + state.vertexType.getClass().getName();
        return null;
    }

    public static boolean isBasicPass() {
        return Depth.INSTANCE.getDepthRendering() || SurfaceRenderer.isWorldPass() && !SurfaceRenderer.shaderPass()
                || ScopeShaderPipelines.isLightweightScope();
    }

    public static void begin(ProgramOwner owner, ChunkVertexType vertexType) {
        if (!WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) return;
        observe(owner, vertexType);
        State state = states.get(owner);
        extendedCamera = true;
        if (state.mainPrograms == null) {
            state.mainPrograms = owner.surface$getPrograms();
            owner.surface$setPrograms(state.cameraPrograms);
        }
    }

    public static int location(ResourceLocation program, String attribute, int original) {
        if (!extendedCamera || !program.getNamespace().equals("sodium") || !program.getPath().equals("chunk_shader"))
            return original;
        return locations.getOrDefault(attribute, original);
    }

    public static void end(ProgramOwner owner) {
        State state = states.get(owner);
        if (state != null && state.mainPrograms != null) {
            owner.surface$setPrograms(state.mainPrograms);
            state.mainPrograms = null;
        }
        extendedCamera = false;
    }

    public static void restoreMain() {
        for (var owner : states.keySet()) end(owner);
    }

    public static void delete(ProgramOwner owner) {
        end(owner);
        State state = states.remove(owner);
        if (state != null) for (var program : state.cameraPrograms.values()) ((GlProgram<?>) program).delete();
    }

    public static void release() {
        restoreMain();
        for (var state : states.values()) {
            for (var program : state.cameraPrograms.values()) ((GlProgram<?>) program).delete();
            state.cameraPrograms.clear();
        }
    }
}
