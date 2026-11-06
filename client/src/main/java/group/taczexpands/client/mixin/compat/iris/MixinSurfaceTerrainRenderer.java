package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.compat.iris.IrisTerrainBindings;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Switches the offscreen basic terrain cache; Iris override caches use per-view isolation. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer", remap = false, priority = 900)
public abstract class MixinSurfaceTerrainRenderer implements IrisTerrainBindings.ProgramOwner {
    @Shadow @Final protected ChunkVertexType vertexType;
    @Shadow @Final @Mutable private Map<Object, Object> programs;

    @Override public Map<Object, Object> surface$getPrograms() { return programs; }
    @Override public void surface$setPrograms(Map<Object, Object> value) { programs = value; }

    @Inject(method = "begin(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", at = @At("HEAD"))
    private void taczexpands$beginSurface(CallbackInfo ci) {
        if (IrisTerrainBindings.isBasicPass()) IrisTerrainBindings.begin(this, vertexType);
        else if (!SurfaceRenderer.isRendering()) IrisTerrainBindings.observe(this, vertexType);
    }

    @Inject(method = "end(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V", at = @At("RETURN"))
    private void taczexpands$endSurface(CallbackInfo ci) { IrisTerrainBindings.end(this); }

    @Inject(method = "delete(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;)V", at = @At("HEAD"))
    private void taczexpands$deleteSurface(CallbackInfo ci) { IrisTerrainBindings.delete(this); }
}
