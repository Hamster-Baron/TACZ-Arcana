package group.taczexpands.client.compat.iris;

import com.mojang.logging.LogUtils;
import group.taczexpands.client.render.paint.PaintResidentTexture;
import java.io.IOException;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Resource companions belong to Oculus; composed maps are borrowed from the paint resident. */
public final class IrisPaintPbr implements PBRTextureLoader<PaintResidentTexture> {
    private IrisPaintPbr() {}

    public static void register() {
        PBRTextureLoaderRegistry.INSTANCE.register(PaintResidentTexture.class, new IrisPaintPbr());
    }

    @Override public void load(PaintResidentTexture texture, ResourceManager manager, PBRTextureConsumer consumer) {
        texture.requestPbr();
        AbstractTexture normal = texture.normal() == null ? map(texture.pbr().normal(), manager)
                : new Borrowed(texture.normal());
        if (normal != null) consumer.acceptNormalTexture(normal);
        AbstractTexture specular = texture.specular() == null ? map(texture.pbr().specular(), manager)
                : new Borrowed(texture.specular());
        if (specular != null) consumer.acceptSpecularTexture(specular);
    }

    /** Holder resets leave the resident's generated maps available for the next shader pipeline. */
    private static final class Borrowed extends AbstractTexture {
        private final AbstractTexture owner;
        Borrowed(AbstractTexture owner) { this.owner = owner; }
        @Override public int getId() { return owner.getId(); }
        @Override public void setFilter(boolean blur, boolean mipmap) { owner.setFilter(blur, mipmap); }
        @Override public void load(ResourceManager manager) {}
        @Override public void close() {}
        @Override public void releaseId() {}
    }

    private static AbstractTexture map(ResourceLocation id, ResourceManager manager) {
        if (id == null || manager.getResource(id).isEmpty()) return null;
        SimpleTexture texture = new SimpleTexture(id);
        try {
            texture.load(manager);
            return texture;
        } catch (IOException failure) {
            texture.close();
            LogUtils.getLogger().warn("Arcana paint: unreadable PBR map {}; using shader default", id, failure);
            return null;
        }
    }
}
