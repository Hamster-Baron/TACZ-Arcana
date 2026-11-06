package group.taczexpands.client.gui.paint;

import group.taczexpands.client.config.PaintUiConfig;
import java.util.function.DoubleToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Paint editor skins and drawing helpers. Skins share a common layout. */
public final class PaintTheme {
    /** @param trackHeight slider track thickness */
    public record Skin(
            String id,
            int shadow, int panel, int raised, int hover, int border, int borderHi,
            int text, int textSoft, int muted, int faint, int accent, int ok, int track,
            int previewTop, int previewBottom, int trackHeight) {}

    // Dark slate workshop with amber accents.
    private static final Skin WORKSHOP = new Skin("workshop", 0x66000000, 0xf0121519, 0xff1c2127, 0xff262c34,
            0xff303843, 0xff4a5563, 0xffe9edf2, 0xffc9d0d9, 0xff8b95a3, 0xff4d5663, 0xffe0a33a, 0xff62c286,
            0xff2a3039, 0xff20262e, 0xff0c0e11, 4);
    // Matches TacZ's own translucent FlatColorButton and cream text.
    private static final Skin TACZ = new Skin("tacz", 0x00000000, 0xaf222222, 0x60000000, 0x80383838,
            0x00000000, 0xfff3efe0, 0xfff3efe0, 0xffe0dccd, 0xffaaa697, 0xff6b685f, 0xfff3efe0, 0xff9fd39a,
            0x80000000, 0x80303030, 0x80101010, 3);

    private PaintTheme() {}

    /** Configured in taczexpands-client.toml; read live so a config edit applies on the next frame. */
    public static Skin skin() {
        return PaintUiConfig.style() == PaintUiConfig.Style.TACZ ? TACZ : WORKSHOP;
    }

    /** Filled box with a 1px border; a transparent border leaves the fill visible. */
    public static void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1, int fill, int border) {
        if ((border >>> 24) == 0) {
            graphics.fill(x0, y0, x1, y1, fill);
            return;
        }
        graphics.fill(x0, y0, x1, y0 + 1, border);
        graphics.fill(x0, y1 - 1, x1, y1, border);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, border);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, border);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    /** Horizontal gradient sampled in two-pixel strips using GuiGraphics. */
    public static void hGradient(GuiGraphics graphics, int x0, int y0, int x1, int y1, DoubleToIntFunction color) {
        int span = Math.max(1, x1 - x0 - 1);
        for (int x = x0; x < x1; x += 2)
            graphics.fill(x, y0, Math.min(x1, x + 2), y1, 0xff000000 | color.applyAsInt((x - x0) / (double) span));
    }

    public static int lerp(int from, int to, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int) Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int) Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return r << 16 | g << 8 | b;
    }

    public static void swatch(GuiGraphics graphics, int x0, int y0, int x1, int y1, int rgb) {
        graphics.fill(x0, y0, x1, y1, 0xff000000);
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 0xff000000 | rgb);
        // Top sheen keeps dark swatches distinguishable from the panel.
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, 0x40ffffff);
    }

    /**
     * Real recipe pattern as the region would show it: tiles sized by the pattern scale, then the
     * base color laid over with (1 - mix) opacity, mirroring the compositor's linear blend.
     */
    public static void pattern(GuiGraphics graphics, int x0, int y0, int x1, int y1, ResourceLocation texture,
                               int rgb, double mix, double scale) {
        int tile = Math.max(4, (int) Math.round((y1 - y0) * scale));
        graphics.enableScissor(x0, y0, x1, y1);
        try {
            for (int y = y0; y < y1; y += tile)
                for (int x = x0; x < x1; x += tile)
                    graphics.blit(texture, x, y, tile, tile, 0, 0, 1, 1, 1, 1);
        } finally {
            graphics.disableScissor();
        }
        int alpha = (int) Math.round(Math.max(0, Math.min(1, 1 - mix)) * 255);
        if (alpha > 0) graphics.fill(x0, y0, x1, y1, alpha << 24 | rgb & 0xffffff);
    }

    public static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }
}
