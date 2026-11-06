package group.taczexpands.client.render.surface;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Emissive physical faces must occlude opaque geometry submitted later by the host model. */
public final class SurfaceMaterial extends RenderStateShard {
    private SurfaceMaterial() {
        super("arcana_surface", () -> {}, () -> {});
    }

    public static RenderType create(ResourceLocation texture) {
        return RenderType.create(
                "arcana_surface",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true));
    }
}
