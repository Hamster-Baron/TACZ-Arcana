package group.taczexpands.client.gui.paint;

import com.mojang.blaze3d.platform.NativeImage;
import group.taczexpands.client.render.ShaderTextureReferences;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Editor-owned pattern thumbnails; registered on first use and released with the editor. */
final class PaintThumbnails {
    private final Map<ResourceLocation, ResourceLocation> owned = new HashMap<>();
    private final Map<ResourceLocation, Boolean> failed = new HashMap<>();

    /** Returns a drawable texture id for the pattern, or null when it cannot be decoded. */
    ResourceLocation get(ResourceLocation pattern) {
        ResourceLocation id = owned.get(pattern);
        if (id != null || failed.containsKey(pattern)) return id;
        var mc = Minecraft.getInstance();
        try (var stream = mc.getResourceManager().open(pattern)) {
            id = new ResourceLocation("taczexpands", "paint/thumbnail_" + owned.size());
            mc.getTextureManager().register(id, new DynamicTexture(NativeImage.read(stream)));
            owned.put(pattern, id);
        } catch (Exception failure) {
            failed.put(pattern, Boolean.TRUE);
        }
        return id;
    }

    void close() {
        var textures = Minecraft.getInstance().getTextureManager();
        owned.values().forEach(location -> {
            ShaderTextureReferences.clear(textures.getTexture(location).getId());
            textures.release(location);
        });
        owned.clear();
        failed.clear();
    }
}
