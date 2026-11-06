package group.taczexpands.client.gui.paint;

import group.taczexpands.client.paint.PaintParameters;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;

/** Slider callback for paint draft updates. */
final class PaintSlider extends ForgeSlider {
    static final int HEIGHT = 20;
    private static final int TRACK_Y = 12, TRACK_H = 4;

    enum Format {
        HUE(1, "°", 0),
        PERCENT(100, "%", 1),
        SCALE(1, "×", 2);

        final double factor;
        final String suffix;
        final int precision;

        Format(double factor, String suffix, int precision) {
            this.factor = factor;
            this.suffix = suffix;
            this.precision = precision;
        }

        static Format forAxis(int axis) {
            return switch (axis) {
                case 0 -> HUE;
                case 1, 2, 3 -> PERCENT;
                case 4 -> SCALE;
                default -> throw new IndexOutOfBoundsException(axis);
            };
        }
    }

    private final DoubleConsumer changed;
    private final boolean hue;
    /** Track color for a 0..1 position; null draws a neutral fill up to the knob. */
    private DoubleToIntFunction track;
    /** Optional 0..1 position of the recipe preset, drawn as a small tick. */
    private double presetMark = -1;

    PaintSlider(
            int x,
            int y,
            int width,
            Component label,
            double min,
            double max,
            double initial,
            Format format,
            DoubleConsumer changed) {
        super(
                x,
                y,
                width,
                HEIGHT,
                label,
                Component.literal(format.suffix),
                min,
                max,
                initial,
                0,
                format.precision,
                true);
        this.changed = changed;
        hue = format == Format.HUE;
        if (hue) track = t -> PaintParameters.fromHsv(minValue + (maxValue - minValue) * t, 0.85, 0.95);
    }

    PaintSlider track(DoubleToIntFunction track) {
        this.track = track;
        return this;
    }

    PaintSlider presetMark(double fraction) {
        presetMark = fraction;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;
        var skin = PaintTheme.skin();
        boolean hot = active && isHoveredOrFocused();
        int x0 = getX(), y0 = getY();
        if (hot) graphics.fill(x0, y0, x0 + width, y0 + height, (skin.hover() & 0xffffff) | 0x60000000);
        String name = prefix.getString();
        if (name.endsWith(": ")) name = name.substring(0, name.length() - 2);
        String shown = getValueString() + suffix.getString();
        graphics.drawString(font, shown, x0 + width - 4 - font.width(shown), y0 + 2, skin.text(), false);
        graphics.drawString(
                font,
                PaintTheme.fit(font, name, width - 12 - font.width(shown)),
                x0 + 4,
                y0 + 2,
                isFocused() ? skin.accent() : skin.muted(),
                false);
        int th = skin.trackHeight();
        int left = x0 + 4, right = x0 + width - 4, top = y0 + TRACK_Y + (TRACK_H - th) / 2;
        int knob = left + (int) Math.round(value * (right - left - 1));
        graphics.fill(left - 1, top - 1, right + 1, top + th + 1, 0xff000000);
        if (track != null) PaintTheme.hGradient(graphics, left, top, right, top + th, track);
        else {
            graphics.fill(left, top, right, top + th, skin.track());
            graphics.fill(left, top, knob, top + th, hot ? skin.accent() : (skin.accent() & 0xffffff) | 0xc0000000);
        }
        if (presetMark >= 0) {
            int mark = left + (int) Math.round(presetMark * (right - left - 1));
            graphics.fill(mark, top + th + 1, mark + 1, top + th + 3, skin.muted());
        }
        // Knob: dark rim and light core so it reads on any gradient.
        graphics.fill(knob - 2, top - 3, knob + 3, top + th + 3, 0xff000000);
        graphics.fill(knob - 1, top - 2, knob + 2, top + th + 2, hot ? 0xffffffff : 0xffd9dee5);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible) return false;
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        return handled || keyCode == 262 || keyCode == 263;
    }

    @Override
    protected void applyValue() {
        if (changed != null) changed.accept(getValue());
    }
}
