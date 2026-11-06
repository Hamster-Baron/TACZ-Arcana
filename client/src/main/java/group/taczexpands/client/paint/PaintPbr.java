package group.taczexpands.client.paint;

import net.minecraft.resources.ResourceLocation;

/** Pure selection of companions on the selected surface's own UV. */
public final class PaintPbr {
    private PaintPbr() {}

    public record Sources(ResourceLocation normal, ResourceLocation specular) {}

    /** Shared immutable ARGB pixels, sampled in the pattern's normalized UV space. */
    public record Image(int width, int height, int[] argb) {
        int sample(double u, double v) {
            int x = Math.min(width - 1, (int) ((u - Math.floor(u)) * width));
            int y = Math.min(height - 1, (int) ((v - Math.floor(v)) * height));
            return argb[y * width + x];
        }
    }

    // Oculus PBRType defaults, converted from RGBA to the compositor's ARGB layout.
    public static int defaultPixel(boolean normal) { return normal ? 0xff7f7fff : 0; }

    public static Sources select(ResourceLocation original, PaintConfig.Pbr painted, boolean switchPbr) {
        return select(original, null, painted, switchPbr);
    }

    public static Sources select(ResourceLocation original, PaintConfig.Pbr base,
                                 PaintConfig.Pbr painted, boolean switchPbr) {
        ResourceLocation normal = base != null && base.normal() != null ? base.normal() : companion(original, "_n");
        ResourceLocation specular = base != null && base.specular() != null ? base.specular() : companion(original, "_s");
        if (switchPbr && painted != null) {
            if (painted.normal() != null) normal = painted.normal();
            if (painted.specular() != null) specular = painted.specular();
        }
        return new Sources(normal, specular);
    }

    public static ResourceLocation companion(ResourceLocation texture, String suffix) {
        String path = texture.getPath();
        int dot = path.lastIndexOf('.');
        return new ResourceLocation(texture.getNamespace(), dot > path.lastIndexOf('/')
                ? path.substring(0, dot) + suffix + path.substring(dot) : path + suffix);
    }
}
