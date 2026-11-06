package group.taczexpands.client.render.paint;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import group.taczexpands.client.paint.PaintPbr;
import group.taczexpands.client.render.ShaderTextureReferences;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;

/** The actual GL owner tracked by the optional renderer's texture registry. */
public final class PaintResidentTexture extends DynamicTexture {
    private final PaintPbr.Sources pbr;
    private Uploaded normal, specular;
    private Runnable pbrRequest;

    PaintResidentTexture(PaintTexture.Prepared pixels, Runnable pbrRequest) {
        super(pixels.color());
        this.pbr = pixels.pbr();
        this.pbrRequest = pbrRequest;
        // DynamicTexture has uploaded the image; only the GL texture is needed afterward.
        setPixels(null);
        boolean complete = false;
        try {
            if (pixels.normal() != null) normal = new Uploaded(pixels.normal());
            if (pixels.specular() != null) specular = new Uploaded(pixels.specular());
            complete = true;
        } finally {
            if (!complete) close();
        }
    }

    public void requestPbr() {
        if (pbrRequest != null) {
            pbrRequest.run();
            pbrRequest = null;
        }
    }

    public PaintPbr.Sources pbr() {
        return pbr;
    }

    public AbstractTexture normal() { return normal; }
    public AbstractTexture specular() { return specular; }

    @Override public void setFilter(boolean blur, boolean mipmap) {
        super.setFilter(blur, mipmap);
        if (normal != null) normal.setFilter(blur, mipmap);
        if (specular != null) specular.setFilter(blur, mipmap);
    }

    @Override public void close() {
        // DynamicTexture.close skips releaseId when its CPU image is already absent.
        super.close();
        releaseId();
    }

    @Override public void releaseId() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::releaseId);
            return;
        }
        ShaderTextureReferences.clear(id);
        super.releaseId();
        if (normal != null) normal.close();
        if (specular != null) specular.close();
    }

    /** Generated maps share the resident's lifetime and release their upload buffers immediately. */
    private static final class Uploaded extends DynamicTexture {
        Uploaded(NativeImage image) {
            super(image);
            setPixels(null);
        }
        @Override public void close() {
            ShaderTextureReferences.clear(id);
            releaseId();
        }
    }
}
