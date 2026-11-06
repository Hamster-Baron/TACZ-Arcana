package group.taczexpands.client.mixin.compat.iris;

import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public interface IrisPipelineAccessor {
    @Accessor("shaderStorageBufferHolder")
    ShaderStorageBufferHolder arcana$getShaderStorageBufferHolder();
}
