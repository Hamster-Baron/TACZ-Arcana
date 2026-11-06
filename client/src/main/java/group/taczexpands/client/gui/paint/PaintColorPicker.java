package group.taczexpands.client.gui.paint;

import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.client.paint.PaintParameters;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Recipe-limited saturation/value plane with state updates for unlocked axes. */
public final class PaintColorPicker extends AbstractWidget {
    private final PaintConfig.Axis saturation;
    private final PaintConfig.Axis brightness;
    private final Supplier<double[]> hsv;
    private final BiConsumer<Integer, Integer> changed;

    PaintColorPicker(
            int x,
            int y,
            int width,
            int height,
            PaintConfig.Channel channel,
            Supplier<double[]> hsv,
            BiConsumer<Integer, Integer> changed) {
        super(x, y, width, height, Component.translatable("gui.taczexpands.paint.picker_plane"));
        saturation = channel.s();
        brightness = channel.v();
        this.hsv = hsv;
        this.changed = changed;
    }

    private static double fraction(double value, PaintConfig.Axis axis) {
        return axis.editable()
                ? Math.max(0, Math.min(1, (value - axis.min()) / (axis.max() - axis.min())))
                : 0.5;
    }

    private void update(double mouseX, double mouseY) {
        double x = Math.max(0, Math.min(1, (mouseX - getX()) / (width - 1.0)));
        double y = Math.max(0, Math.min(1, (mouseY - getY()) / (height - 1.0)));
        if (saturation.editable())
            changed.accept(
                    1, saturation.quantize(saturation.min() + x * (saturation.max() - saturation.min())));
        if (brightness.editable())
            changed.accept(
                    2, brightness.quantize(brightness.max() - y * (brightness.max() - brightness.min())));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        update(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        update(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        double[] current = hsv.get();
        if ((key == 263 || key == 262) && saturation.editable()) {
            int q = saturation.quantize(current[1]);
            changed.accept(1, Math.max(0, Math.min(65535, q + (key == 263 ? -256 : 256))));
            return true;
        }
        if ((key == 264 || key == 265) && brightness.editable()) {
            int q = brightness.quantize(current[2]);
            changed.accept(2, Math.max(0, Math.min(65535, q + (key == 264 ? -256 : 256))));
            return true;
        }
        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        double[] current = hsv.get();
        graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1,
                isHoveredOrFocused() ? PaintTheme.skin().borderHi() : PaintTheme.skin().border() | 0xff000000);
        // Small flat cells keep the plane inexpensive while pointer mapping remains continuous.
        for (int y = 0; y < height; y += 3) {
            double v =
                    brightness.editable()
                            ? brightness.max() - y / (height - 1.0) * (brightness.max() - brightness.min())
                            : current[2];
            for (int x = 0; x < width; x += 3) {
                double s =
                        saturation.editable()
                                ? saturation.min() + x / (width - 1.0) * (saturation.max() - saturation.min())
                                : current[1];
                int rgb = PaintParameters.fromHsv(current[0], s, v);
                graphics.fill(
                        getX() + x,
                        getY() + y,
                        getX() + Math.min(width, x + 3),
                        getY() + Math.min(height, y + 3),
                        0xff000000 | rgb);
            }
        }
        int x = getX() + (int) Math.round(fraction(current[1], saturation) * (width - 1));
        int y = getY() + (int) Math.round((1 - fraction(current[2], brightness)) * (height - 1));
        // Ring cursor filled with the live color: black rim, white ring, swatch core.
        graphics.fill(x - 4, y - 4, x + 5, y + 5, 0xff000000);
        graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xffffffff);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xff000000 | PaintParameters.fromHsv(current[0], current[1], current[2]));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
