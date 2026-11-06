package group.taczexpands.client.compat.iris;

import com.mojang.blaze3d.platform.NativeImage;
import group.taczexpands.client.render.surface.SurfaceFeed;
import net.irisshaders.iris.texture.TextureTracker;
import net.irisshaders.iris.texture.pbr.PBRTextureManager;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.server.packs.resources.ResourceManager;

/** Screen emission is owned and released by the feed's Oculus PBR holder. */
public final class IrisSurfacePbr implements PBRTextureLoader<SurfaceFeed> {
    private IrisSurfacePbr() {}

    public static void register() {
        PBRTextureLoaderRegistry.INSTANCE.register(SurfaceFeed.class, new IrisSurfacePbr());
    }

    public static void track(SurfaceFeed feed) {
        // Framebuffer allocation bypasses AbstractTexture's texture-owner registration.
        TextureTracker.INSTANCE.trackTexture(feed.getId(), feed);
        PBRTextureManager.INSTANCE.onDeleteTexture(feed.getId());
    }

    @Override public void load(SurfaceFeed feed, ResourceManager manager, PBRTextureConsumer consumer) {
        // LabPBR reserves alpha 255 for non-emissive; 254 is maximum emission.
        var image = new NativeImage(1, 1, false);
        image.setPixelRGBA(0, 0, 0xfe000000);
        consumer.acceptSpecularTexture(new DynamicTexture(image));
    }
}
