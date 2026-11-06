package group.taczexpands.client.gui.paint;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Skinned paint-editor button; keeps vanilla Button input, narration and tooltip behavior. */
public class PaintButton extends Button {
    public enum Style {
        /** Raised neutral surface. */
        PLAIN,
        /** Accent outline while {@code lit} is true, used for the refit-screen toggle. */
        TOGGLE,
        /** Small square glyph button that dims while inactive. */
        ICON
    }

    private final Style style;
    private final String glyph;
    private BooleanSupplier lit = () -> false;

    public PaintButton(int x, int y, int width, int height, Component message, Style style, String glyph, OnPress press) {
        super(x, y, width, height, message, press, DEFAULT_NARRATION);
        this.style = style;
        this.glyph = glyph;
    }

    public PaintButton lit(BooleanSupplier lit) {
        this.lit = lit;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        var skin = PaintTheme.skin();
        int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
        boolean hot = active && isHoveredOrFocused();
        boolean on = lit.getAsBoolean();
        int textY = y0 + (height - 8) / 2;
        if (style == Style.ICON) {
            if (hot) PaintTheme.frame(graphics, x0, y0, x1, y1, skin.hover(), skin.borderHi());
            // Lit icons mark a value that differs from its preset; unlit ones stay quiet.
            int color = !active ? skin.faint() : hot ? skin.accent() : on ? skin.textSoft() : skin.faint();
            graphics.drawString(font, glyph, x0 + (width - font.width(glyph) + 1) / 2, textY, color, false);
            return;
        }
        int border = on ? skin.accent() : hot ? skin.borderHi() : skin.border();
        PaintTheme.frame(graphics, x0, y0, x1, y1, hot ? skin.hover() : style == Style.TOGGLE ? skin.panel() : skin.raised(), border);
        if (on) graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, skin.accent());
        int textColor = !active ? skin.faint() : on || hot ? skin.text() : skin.textSoft();
        String label = getMessage().getString();
        int glyphWidth = glyph == null ? 0 : font.width(glyph) + 4;
        String text = PaintTheme.fit(font, label, width - 8 - glyphWidth);
        int left = x0 + (width - font.width(text) - glyphWidth) / 2;
        if (glyph != null) {
            graphics.drawString(font, glyph, left, textY, on ? skin.accent() : skin.muted(), false);
            left += glyphWidth;
        }
        graphics.drawString(font, text, left, textY, textColor, false);
    }
}
