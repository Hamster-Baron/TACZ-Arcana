package group.taczexpands.client.mixin.compat.iris;

import group.taczexpands.client.compat.iris.IrisViewPipelines.ChunkAccess;
import group.taczexpands.client.compat.iris.ScopeShaderPipelines;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkShaderInterface;
import net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisTerrainPass;
import net.irisshaders.iris.pipeline.SodiumTerrainPipeline;
import java.util.EnumMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides", remap = false)
public abstract class MixinScopeChunkPrograms implements ChunkAccess {
    @Accessor("programs")
    public abstract EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> arcana$getPrograms();
    @Mutable @Accessor("programs")
    public abstract void arcana$setPrograms(EnumMap<IrisTerrainPass, GlProgram<IrisChunkShaderInterface>> programs);
    @Accessor("shadersCreated") public abstract boolean arcana$getShadersCreated();
    @Accessor("shadersCreated") public abstract void arcana$setShadersCreated(boolean created);
    @Accessor("versionCounterForSodiumShaderReload") public abstract int arcana$getShaderVersion();
    @Accessor("versionCounterForSodiumShaderReload") public abstract void arcana$setShaderVersion(int version);
    @Invoker("createShaders")
    public abstract void arcana$createShaders(SodiumTerrainPipeline pipeline, ChunkVertexType vertexType);

    @Inject(method = "getProgramOverride", at = @At("HEAD"), remap = false)
    private void arcana$selectViewPrograms(TerrainRenderPass pass, ChunkVertexType vertexType,
            CallbackInfoReturnable<GlProgram<IrisChunkShaderInterface>> cir) {
        ScopeShaderPipelines.selectChunkPrograms((IrisChunkProgramOverrides) (Object) this);
    }
    @Inject(method = "getProgramOverride", at = @At("RETURN"), remap = false)
    private void arcana$prewarmPrograms(TerrainRenderPass pass, ChunkVertexType vertexType,
            CallbackInfoReturnable<GlProgram<IrisChunkShaderInterface>> cir) {
        ScopeShaderPipelines.prewarmChunks((IrisChunkProgramOverrides) (Object) this, vertexType);
    }
    @Inject(method = "deleteShaders", at = @At("HEAD"), remap = false)
    private void arcana$deleteOtherViews(CallbackInfo ci) {
        ScopeShaderPipelines.deleteOtherChunkPrograms((IrisChunkProgramOverrides) (Object) this);
    }
}
