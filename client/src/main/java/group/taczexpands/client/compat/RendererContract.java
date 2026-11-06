package group.taczexpands.client.compat;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.Opcodes;

/** Member contracts for optional adapters. Inspects bytecode without loading renderer classes. */
public final class RendererContract {
    @FunctionalInterface public interface Classes { ClassNode read(String name) throws Exception; }
    private static final String IRIS = "net.irisshaders.iris.";
    private static final String SODIUM = "me.jellysquid.mods.sodium.client.";
    private final Classes classes;

    public RendererContract(Classes classes) { this.classes = classes; }

    private void field(String owner, String name, String descriptor) throws Exception {
        if (classes.read(owner).fields.stream().noneMatch(f -> f.name.equals(name) && f.desc.equals(descriptor)))
            throw new IllegalStateException(owner + "." + name + ": expected " + descriptor);
    }
    private void method(String owner, String name, String descriptor) throws Exception {
        if (classes.read(owner).methods.stream().noneMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)))
            throw new IllegalStateException(owner + "." + name + descriptor + " unavailable");
    }

    /** Returns null when all declared view-adapter members match the contract. */
    public String views() {
        try {
            String manager = IRIS + "pipeline.PipelineManager";
            String pipeline = "Lnet/irisshaders/iris/pipeline/WorldRenderingPipeline;";
            field(manager, "pipelineFactory", "Ljava/util/function/Function;");
            field(manager, "pipeline", pipeline);
            method(manager, "preparePipeline", "(Lnet/irisshaders/iris/shaderpack/materialmap/NamespacedId;)" + pipeline);
            method(manager, "getPipelineNullable", "()" + pipeline);
            method(manager, "getPipeline", "()Ljava/util/Optional;");
            method(manager, "destroyPipeline", "()V");
            method(manager, "resetTextureState", "()V");
            method(manager, "getVersionCounterForSodiumShaderReload", "()I");
            method(IRIS + "pipeline.VanillaRenderingPipeline", "<init>", "()V");
            field(IRIS + "pipeline.IrisRenderingPipeline", "shaderStorageBufferHolder",
                    "Lnet/irisshaders/iris/gl/buffer/ShaderStorageBufferHolder;");
            method(IRIS + "gl.buffer.ShaderStorageBufferHolder", "setupBuffers", "()V");
            method(IRIS + "api.v0.IrisApi", "isShaderPackInUse", "()Z");
            method(IRIS + "uniforms.SystemTimeUniforms$FrameCounter", "beginFrame", "()V");
            method(IRIS + "uniforms.SystemTimeUniforms$Timer", "beginFrame", "(J)V");
            String hand = "(Lcom/mojang/blaze3d/vertex/PoseStack;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;" + pipeline + ")V";
            method(IRIS + "pathways.HandRenderer", "renderSolid", hand);
            method(IRIS + "pathways.HandRenderer", "renderTranslucent", hand);
            String chunks = IRIS + "compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides";
            field(chunks, "programs", "Ljava/util/EnumMap;");
            field(chunks, "shadersCreated", "Z");
            field(chunks, "versionCounterForSodiumShaderReload", "I");
            method(chunks, "deleteShaders", "()V");
            method(chunks, "getProgramOverride", "(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;)Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram;");
            method(chunks, "createShaders", "(Lnet/irisshaders/iris/pipeline/SodiumTerrainPipeline;Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;)V");
            String settings = IRIS + "shaderpack.materialmap.WorldRenderingSettings";
            publicInstanceMethod(IRIS + "pipeline.WorldRenderingPipeline", "destroy", "()V");
            publicInstanceMethod(IRIS + "pipeline.WorldRenderingPipeline", "getSodiumTerrainPipeline",
                    "()Lnet/irisshaders/iris/pipeline/SodiumTerrainPipeline;");
            publicInstanceMethod(SODIUM + "gl.shader.GlProgram", "delete", "()V");
            publicInstanceMethod(settings, "isReloadRequired", "()Z");
            publicInstanceMethod(settings, "clearReloadRequired", "()V");
            for (String[] property : new String[][] {
                    {"getBlockStateIds", "setBlockStateIds", "Lit/unimi/dsi/fastutil/objects/Object2IntMap;"},
                    {"getBlockTypeIds", "setBlockTypeIds", "Ljava/util/Map;"},
                    {"getEntityIds", "setEntityIds", "Lit/unimi/dsi/fastutil/objects/Object2IntFunction;"},
                    {"getItemIds", "setItemIds", "Lit/unimi/dsi/fastutil/objects/Object2IntFunction;"},
                    {"getAmbientOcclusionLevel", "setAmbientOcclusionLevel", "F"},
                    {"shouldDisableDirectionalShading", "setDisableDirectionalShading", "Z"},
                    {"shouldUseSeparateAo", "setUseSeparateAo", "Z"},
                    {"shouldUseExtendedVertexFormat", "setUseExtendedVertexFormat", "Z"},
                    {"shouldSeparateEntityDraws", "setSeparateEntityDraws", "Z"},
                    {"shouldVoxelizeLightBlocks", "setVoxelizeLightBlocks", "Z"}}) {
                publicInstanceMethod(settings, property[0], "()" + property[2]);
                publicInstanceMethod(settings, property[1], "(" + property[2] + ")V");
            }
            return null;
        } catch (Exception failure) { return failure.toString(); }
    }

    public String paintPbr() {
        try {
            String registry = IRIS + "texture.pbr.loader.PBRTextureLoaderRegistry";
            String loader = IRIS + "texture.pbr.loader.PBRTextureLoader";
            String consumer = loader + "$PBRTextureConsumer";
            if ((classes.read(registry).access & Opcodes.ACC_INTERFACE) != 0
                    || (classes.read(loader).access & Opcodes.ACC_INTERFACE) == 0
                    || (classes.read(consumer).access & Opcodes.ACC_INTERFACE) == 0)
                throw new IllegalStateException("PBR registry/loader class kinds changed");
            String registryDescriptor = "Lnet/irisshaders/iris/texture/pbr/loader/PBRTextureLoaderRegistry;";
            if (classes.read(registry).fields.stream().noneMatch(f -> f.name.equals("INSTANCE")
                    && f.desc.equals(registryDescriptor)
                    && (f.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)))
                throw new IllegalStateException(registry + ".INSTANCE unavailable");
            publicInstanceMethod(registry, "register", "(Ljava/lang/Class;Lnet/irisshaders/iris/texture/pbr/loader/PBRTextureLoader;)V");
            publicInstanceMethod(loader, "load", "(Lnet/minecraft/client/renderer/texture/AbstractTexture;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/irisshaders/iris/texture/pbr/loader/PBRTextureLoader$PBRTextureConsumer;)V");
            for (String name : new String[]{"acceptNormalTexture", "acceptSpecularTexture"})
                publicInstanceMethod(consumer, name, "(Lnet/minecraft/client/renderer/texture/AbstractTexture;)V");
            return null;
        } catch (Exception failure) { return failure.toString(); }
    }

    public String surfacePbr() {
        try {
            String tracker = IRIS + "texture.TextureTracker";
            String manager = IRIS + "texture.pbr.PBRTextureManager";
            field(tracker, "INSTANCE", "Lnet/irisshaders/iris/texture/TextureTracker;");
            field(manager, "INSTANCE", "Lnet/irisshaders/iris/texture/pbr/PBRTextureManager;");
            publicInstanceMethod(tracker, "trackTexture", "(ILnet/minecraft/client/renderer/texture/AbstractTexture;)V");
            publicInstanceMethod(manager, "onDeleteTexture", "(I)V");
            return null;
        } catch (Exception failure) { return failure.toString(); }
    }

    private void publicInstanceMethod(String owner, String name, String descriptor) throws Exception {
        if (classes.read(owner).methods.stream().noneMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)
                && (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC))
            throw new IllegalStateException(owner + "." + name + descriptor + " unavailable as a public instance method");
    }

    public String terrain() {
        try {
            String renderer = SODIUM + "render.chunk.ShaderChunkRenderer";
            field(renderer, "vertexType", "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;");
            field(renderer, "programs", "Ljava/util/Map;");
            String pass = "(Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)V";
            method(renderer, "begin", pass);
            method(renderer, "end", pass);
            method(renderer, "delete", "(Lme/jellysquid/mods/sodium/client/gl/device/CommandList;)V");
            String builder = SODIUM + "gl.shader.GlProgram$Builder";
            field(builder, "name", "Lnet/minecraft/resources/ResourceLocation;");
            method(builder, "bindAttribute", "(Ljava/lang/String;I)Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram$Builder;");
            for (String name : new String[]{"ATTRIBUTE_POSITION_ID", "ATTRIBUTE_COLOR", "ATTRIBUTE_BLOCK_TEXTURE", "ATTRIBUTE_LIGHT_TEXTURE"}) {
                field(IRIS + "compat.sodium.impl.IrisChunkShaderBindingPoints", name, "I");
            }
            field(IRIS + "compat.sodium.impl.vertex_format.IrisModelVertexFormats", "MODEL_VERTEX_XHFP",
                    "Lnet/irisshaders/iris/compat/sodium/impl/vertex_format/terrain_xhfp/XHFPModelVertexType;");
            method(IRIS + "shaderpack.materialmap.WorldRenderingSettings", "shouldUseExtendedVertexFormat", "()Z");
            return null;
        } catch (Exception failure) { return failure.toString(); }
    }
}
