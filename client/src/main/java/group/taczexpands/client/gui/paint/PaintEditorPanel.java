package group.taczexpands.client.gui.paint;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.tacz.guns.client.renderer.item.ItemPreviewRenderContext;
import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.client.paint.PaintParameters;
import group.taczexpands.common.paint.PaintState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Layout, drawing and input for the controller-owned paint editing session. */
public final class PaintEditorPanel extends AbstractWidget {
    private static final int PAD = 6, HEADER = 18, TAB_Y = 20, TAB_H = 18, SWATCH_Y = 42, SWATCH_H = 28;
    private static final int ROW = PaintSlider.HEIGHT + 1, PICKER_H = 56, RESET_W = 12;
    private static final int PREVIEW_MIN = 48, PREVIEW_MAX = 84;
    private static final float DEFAULT_YAW = 150, DEFAULT_PITCH = 12;
    private final PaintEditorController controller;
    private final List<AbstractWidget> controls = new ArrayList<>();
    private PaintConfig.Recipe recipe;
    private boolean[] regions;
    private int region;
    private boolean pickerOpen;
    private final String[] regionLabels = {
        "gui.taczexpands.paint.region_1",
        "gui.taczexpands.paint.region_2",
        "gui.taczexpands.paint.region_3"
    };
    private AbstractWidget focused;
    private boolean dragging;
    private int previewY, previewHeight = PREVIEW_MAX;
    // Preview camera controls the displayed view independently of the draft.
    private boolean rotating;
    private float yaw = DEFAULT_YAW, pitch = DEFAULT_PITCH, zoom = 1;
    private long lastPreviewClick;
    private final PaintThumbnails thumbnails = new PaintThumbnails();

    PaintEditorPanel(int x, int y, int width, PaintEditorController controller) {
        super(x, y, width, 184, Component.translatable("gui.taczexpands.paint.title"));
        this.controller = controller;
    }

    void refresh(PaintConfig.Recipe recipe, boolean[] regions, String[] labels) {
        this.recipe = recipe;
        this.regions = regions;
        System.arraycopy(labels, 0, regionLabels, 0, regionLabels.length);
        if (!regions[region]) {
            region = 0;
            while (region < 2 && !regions[region]) region++;
        }
        thumbnails.close();
        rebuild();
    }

    void dispose() {
        controls.clear();
        thumbnails.close();
    }

    public void relocate(int x, int y) {
        if (getX() == x && getY() == y) return;
        setX(x);
        setY(y);
        rebuild();
    }

    private int regionRgb(int r) {
        PaintParameters.Effective effective = PaintParameters.resolve(recipe, controller.draft().values()).channel(r);
        return effective == null ? 0x808080 : effective.rgb();
    }

    private Component axisLabel(int axis) {
        return Component.translatable(
                new String[] {
                            "gui.taczexpands.paint.hue", "gui.taczexpands.paint.saturation",
                            "gui.taczexpands.paint.brightness", "gui.taczexpands.paint.pattern_mix",
                            "gui.taczexpands.paint.pattern_scale"
                        }
                        [axis]);
    }

    private static double fraction(PaintConfig.Axis axis, double value) {
        return Math.max(0, Math.min(1, (value - axis.min()) / (axis.max() - axis.min())));
    }

    private int axisRow(PaintConfig.Channel channel, int axisIndex, int x, int y) {
        PaintConfig.Axis axis = channel.axis(axisIndex);
        if (!axis.editable()) return y;
        int index = PaintState.index(region, axisIndex);
        PaintSlider.Format format = PaintSlider.Format.forAxis(axisIndex);
        double factor = format.factor;
        int inner = width - 2 * PAD;
        PaintSlider slider =
                new PaintSlider(
                        x,
                        y,
                        inner - RESET_W - 2,
                        axisLabel(axisIndex).copy().append(": "),
                        axis.min() * factor,
                        axis.max() * factor,
                        axis.resolve(controller.draft().q(index)) * factor,
                        format,
                        value -> controller.setAxis(index, axis.quantize(value / factor)));
        slider.presetMark(fraction(axis, axis.preset()));
        // Tracks preview the effect of each axis on the live color; scale keeps a neutral fill.
        switch (axisIndex) {
            case 1 -> slider.track(t -> {
                double[] hsv = currentHsv();
                return PaintParameters.fromHsv(hsv[0], axis.min() + (axis.max() - axis.min()) * t, Math.max(0.35, hsv[2]));
            });
            case 2 -> slider.track(t -> {
                double[] hsv = currentHsv();
                return PaintParameters.fromHsv(hsv[0], hsv[1], axis.min() + (axis.max() - axis.min()) * t);
            });
            case 3 -> slider.track(t -> {
                int base = regionRgb(region);
                double mix = axis.min() + (axis.max() - axis.min()) * t;
                return PaintTheme.lerp(base, PaintTheme.lerp(base, 0x1c1f17, 0.6), mix);
            });
            default -> {}
        }
        slider.setTooltip(
                Tooltip.create(
                        Component.translatable(
                                new String[] {
                                            "gui.taczexpands.paint.hue_help", "gui.taczexpands.paint.saturation_help",
                                            "gui.taczexpands.paint.brightness_help",
                                            "gui.taczexpands.paint.pattern_mix_help",
                                            "gui.taczexpands.paint.pattern_scale_help"
                                        }
                                        [axisIndex])));
        controls.add(slider);
        PaintButton reset =
                new PaintButton(
                        x + inner - RESET_W,
                        y,
                        RESET_W,
                        12,
                        Component.literal("↺"),
                        PaintButton.Style.ICON,
                        "↺",
                        ignored -> {
                            controller.setAxis(index, -1);
                            rebuild();
                            controller.submit();
                        })
                        .lit(() -> controller.draft().q(index) != -1);
        reset.setTooltip(
                Tooltip.create(
                        Component.translatable("gui.taczexpands.paint.reset_axis", axisLabel(axisIndex))));
        controls.add(reset);
        return y + ROW;
    }

    private double[] currentHsv() {
        PaintConfig.Channel channel = recipe.channel(region);
        int offset = region * 4;
        return new double[] {
            channel.h().resolve(controller.draft().q(offset)),
            channel.s().resolve(controller.draft().q(offset + 1)),
            channel.v().resolve(controller.draft().q(offset + 2))
        };
    }

    private void rebuild() {
        controls.clear();
        focused = null;
        dragging = false;
        rotating = false;
        int x = getX() + PAD, inner = width - 2 * PAD;
        int count = 0;
        for (int r = 0; r < 3; r++) if (regions[r] && recipe.channel(r) != null) count++;
        int slotWidth = count == 0 ? inner : (inner - (count - 1) * 2) / count, column = 0;
        for (int r = 0; r < 3; r++) {
            if (!regions[r] || recipe.channel(r) == null) continue;
            int selected = r;
            Component fullLabel = Component.translatable(regionLabels[r]);
            Button tab =
                    new RegionTab(
                            x + column++ * (slotWidth + 2),
                            getY() + TAB_Y,
                            slotWidth,
                            fullLabel,
                            r,
                            ignored -> {
                                region = selected;
                                rebuild();
                            });
            tab.setTooltip(Tooltip.create(fullLabel));
            tab.active = r != region;
            controls.add(tab);
        }
        PaintConfig.Channel channel = recipe.channel(region);
        if (channel == null) return;
        boolean colorEditable =
                channel.h().editable() || channel.s().editable() || channel.v().editable();
        if (!colorEditable) pickerOpen = false;
        Button swatch =
                new ColorSwatch(
                        x,
                        getY() + SWATCH_Y,
                        inner,
                        Component.translatable(
                                pickerOpen ? "gui.taczexpands.paint.picker_close" : "gui.taczexpands.paint.color"),
                        colorEditable,
                        ignored -> {
                            pickerOpen = !pickerOpen;
                            rebuild();
                        });
        swatch.active = colorEditable;
        swatch.setTooltip(
                Tooltip.create(
                        Component.translatable(
                                colorEditable
                                        ? "gui.taczexpands.paint.picker_help"
                                        : "gui.taczexpands.paint.color_fixed")));
        controls.add(swatch);
        int y = getY() + SWATCH_Y + SWATCH_H + 4;
        if (pickerOpen) {
            if (channel.s().editable() || channel.v().editable()) {
                PaintColorPicker picker =
                        new PaintColorPicker(
                                x + 1,
                                y + 1,
                                inner - 2,
                                PICKER_H,
                                channel,
                                this::currentHsv,
                                (axis, q) -> controller.setAxis(PaintState.index(region, axis), q));
                picker.setTooltip(
                        Tooltip.create(Component.translatable("gui.taczexpands.paint.picker_plane_help")));
                controls.add(picker);
                y += PICKER_H + 4;
            }
            y = axisRow(channel, 0, x, y);
        } else {
            for (int axis = 0; axis < 3; axis++) y = axisRow(channel, axis, x, y);
        }
        if (channel.texture() != null) {
            y = axisRow(channel, 3, x, y);
            y = axisRow(channel, 4, x, y);
        }
        controls.add(
                new PaintButton(
                        x,
                        y + 2,
                        inner,
                        16,
                        Component.translatable("gui.taczexpands.paint.reset_all"),
                        PaintButton.Style.PLAIN,
                        "↺",
                        ignored -> {
                            if (!controller.draft().isDefault()) {
                                controller.reset();
                                rebuild();
                                controller.submit();
                            }
                        }));
        previewY = y + 22;
        // Fit the preview to the remaining screen height so high GUI scales do not clip the panel.
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        previewHeight = Math.max(PREVIEW_MIN, Math.min(PREVIEW_MAX, screenHeight - 6 - previewY - PAD));
        height = previewY - getY() + previewHeight + PAD;
    }

    /** Region selector: the currently selected region is the inactive tab, as before. */
    private final class RegionTab extends Button {
        private final int index;

        RegionTab(int x, int y, int width, Component message, int index, OnPress press) {
            super(x, y, width, TAB_H, message, press, DEFAULT_NARRATION);
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            boolean selected = !active;
            boolean hot = active && isHoveredOrFocused();
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            var skin = PaintTheme.skin();
            int rgb = regionRgb(index);
            if (selected || hot) graphics.fill(x0, y0, x1, y1, selected ? skin.raised() : skin.hover());
            graphics.fill(x0, y1 - 2, x1, y1,
                    selected ? 0xff000000 | rgb : hot ? skin.borderHi() : skin.border() | 0x40000000);
            int dot = y0 + (height - 2 - 7) / 2;
            PaintTheme.swatch(graphics, x0 + 4, dot, x0 + 11, dot + 7, rgb);
            String text = PaintTheme.fit(font, getMessage().getString(), width - 18);
            graphics.drawString(
                    font, text, x0 + 14, y0 + 5, selected ? skin.text() : hot ? skin.textSoft() : skin.muted(), false);
        }
    }

    /** Color card: live swatch, hex/HSV readout, and the picker toggle. */
    private final class ColorSwatch extends Button {
        private final boolean editable;

        ColorSwatch(int x, int y, int width, Component message, boolean editable, OnPress press) {
            super(x, y, width, SWATCH_H, message, press, DEFAULT_NARRATION);
            this.editable = editable;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var font = Minecraft.getInstance().font;
            PaintConfig.Channel channel = recipe.channel(region);
            if (channel == null) return;
            var skin = PaintTheme.skin();
            boolean hot = active && isHoveredOrFocused();
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            PaintTheme.frame(
                    graphics, x0, y0, x1, y1, hot ? skin.hover() : skin.raised(),
                    pickerOpen ? skin.accent() : hot ? skin.borderHi() : skin.border());
            PaintParameters.Effective effective = PaintParameters.resolve(recipe, controller.draft().values()).channel(region);
            int rgb = effective.rgb();
            int s0 = x0 + 3, s1 = s0 + height - 6;
            PaintTheme.swatch(graphics, s0, y0 + 3, s1, y1 - 3, rgb);
            ResourceLocation thumbnail =
                    channel.texture() != null && effective.mix() > 0 ? thumbnails.get(channel.texture()) : null;
            if (thumbnail != null)
                PaintTheme.pattern(graphics, s0 + 1, y0 + 4, s1 - 1, y1 - 4, thumbnail, rgb,
                        effective.mix(), effective.scale());
            int textX = s1 + 5;
            graphics.drawString(
                    font, String.format(Locale.ROOT, "#%06X", rgb & 0xffffff), textX, y0 + 5, skin.text(), false);
            String detail;
            if (channel.texture() != null && !channel.mix().editable()) {
                detail = Component.translatable(
                                "gui.taczexpands.paint.fixed_mix", Math.round(effective.mix() * 100))
                        .getString();
            } else {
                double[] hsv = currentHsv();
                detail = String.format(
                        Locale.ROOT, "H%d° S%d%% V%d%%",
                        Math.round(((hsv[0] % 360) + 360) % 360), Math.round(hsv[1] * 100), Math.round(hsv[2] * 100));
            }
            String side = editable
                    ? Component.translatable("gui.taczexpands.paint.picker_open").getString()
                            + (pickerOpen ? " ▴" : " ▾")
                    : Component.translatable("gui.taczexpands.paint.fixed").getString();
            int sideX = x1 - 5 - font.width(side);
            graphics.drawString(
                    font, PaintTheme.fit(font, detail, sideX - textX - 4), textX, y0 + 16, skin.muted(), false);
            graphics.drawString(
                    font, side, sideX, y0 + 10,
                    !editable ? skin.faint() : pickerOpen || hot ? skin.accent() : skin.muted(), false);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!controller.prepareFrame(dragging)) return;
        renderChrome(graphics);
        for (AbstractWidget control : controls) control.render(graphics, mouseX, mouseY, partialTick);
        renderPreview(graphics, mouseX, mouseY);
    }

    private void renderChrome(GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        var skin = PaintTheme.skin();
        int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
        if ((skin.shadow() >>> 24) != 0) graphics.fill(x0 + 2, y0 + 2, x1 + 2, y1 + 2, skin.shadow());
        PaintTheme.frame(graphics, x0, y0, x1, y1, skin.panel(), skin.border());
        // Region strip follows the selected region's live color.
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, 0xff000000 | regionRgb(region));
        graphics.drawString(font, getMessage(), x0 + PAD, y0 + 7, skin.text(), false);
        boolean editing = dragging || controller.pending();
        String status =
                Component.translatable(
                                editing
                                        ? "gui.taczexpands.paint.status_editing"
                                        : "gui.taczexpands.paint.status_applied")
                        .getString();
        int statusX = x1 - PAD - font.width(status);
        graphics.drawString(font, status, statusX, y0 + 7, skin.muted(), false);
        graphics.fill(statusX - 7, y0 + 9, statusX - 3, y0 + 13, editing ? skin.accent() : skin.ok());
    }

    private boolean overPreview(double x, double y) {
        return x >= getX() + PAD && x < getX() + width - PAD && y >= previewY && y < previewY + previewHeight;
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        if (controller.preview().isEmpty()) return;
        var mc = Minecraft.getInstance();
        var font = mc.font;
        int x0 = getX() + PAD, x1 = getX() + width - PAD, y0 = previewY, y1 = previewY + previewHeight;
        boolean hot = rotating || overPreview(mouseX, mouseY);
        var skin = PaintTheme.skin();
        PaintTheme.frame(graphics, x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0x00000000, hot ? skin.borderHi() : skin.border());
        graphics.fillGradient(x0, y0, x1, y1, skin.previewTop(), skin.previewBottom());
        // Soft stacked shadow under the model.
        int floor = y0 + previewHeight * 4 / 5, cx = (x0 + x1) / 2;
        for (int i = 0; i < 4; i++) {
            int half = (x1 - x0) * (7 - i) / 20;
            graphics.fill(cx - half, floor + i, cx + half, floor + i + 1, 0x22000000);
        }
        graphics.drawString(
                font, Component.translatable("gui.taczexpands.paint.preview"), x0 + 4, y0 + 4, skin.muted(), false);
        if (hot) {
            String hint = Component.translatable("gui.taczexpands.paint.preview_hint").getString();
            hint = PaintTheme.fit(font, hint, x1 - x0 - 8);
            graphics.drawString(font, hint, x1 - 4 - font.width(hint), y1 - 11, skin.muted(), false);
        }
        var pose = graphics.pose();
        float oldAlpha = ItemPreviewRenderContext.alpha();
        graphics.enableScissor(x0, y0, x1, y1);
        pose.pushPose();
        try {
            // isPreview() is alpha < 1 in the pinned 1010 artifact; it bypasses cached VBO.
            ItemPreviewRenderContext.setAlpha(0.999f);
            pose.translate((x0 + x1) * 0.5 - width * 0.05, y0 + previewHeight * 0.48, 150);
            // Leave room for long muzzle attachments and vertical space below the origin for magazines.
            float scale = Math.min((width - 16) * 0.31f, (previewHeight - 12) * 1.2f) * zoom;
            pose.scale(scale, -scale, scale);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            Lighting.setupForFlatItems();
            var buffers = mc.renderBuffers().bufferSource();
            mc.getItemRenderer()
                    .renderStatic(
                            controller.preview(),
                            ItemDisplayContext.FIXED,
                            0xf000f0,
                            OverlayTexture.NO_OVERLAY,
                            pose,
                            buffers,
                            null,
                            0);
            buffers.endBatch();
        } finally {
            Lighting.setupFor3DItems();
            ItemPreviewRenderContext.setAlpha(oldAlpha);
            pose.popPose();
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!controller.targetValid()) return false;
        for (AbstractWidget control : List.copyOf(controls))
            if (control.mouseClicked(x, y, button)) {
                if (!controls.contains(control)) return true;
                if (focused != null) focused.setFocused(false);
                focused = control;
                focused.setFocused(true);
                dragging =
                        button == 0 && (control instanceof PaintSlider || control instanceof PaintColorPicker);
                return true;
            }
        if (button == 0 && !controller.preview().isEmpty() && overPreview(x, y)) {
            long now = System.currentTimeMillis();
            if (now - lastPreviewClick < 300) {
                yaw = DEFAULT_YAW;
                pitch = DEFAULT_PITCH;
                zoom = 1;
            }
            lastPreviewClick = now;
            rotating = true;
            return true;
        }
        // Consume panel-body clicks to protect covered refit widgets.
        return isMouseOver(x, y);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (rotating) {
            yaw += (float) dx * 1.5f;
            pitch = Math.max(-35, Math.min(70, pitch + (float) dy * 1.2f));
            return true;
        }
        return dragging && focused != null && focused.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (controller.preview().isEmpty() || !overPreview(x, y)) return false;
        zoom = Math.max(0.6f, Math.min(2.2f, zoom * (delta > 0 ? 1.12f : 1 / 1.12f)));
        return true;
    }

    public boolean hasPointerCapture() {
        return (dragging && focused != null) || rotating;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button != 0) return false;
        if (rotating) {
            rotating = false;
            return true;
        }
        boolean handled = focused != null && focused.mouseReleased(x, y, button);
        if (dragging) {
            dragging = false;
            controller.submit();
            return true;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 258 && !controls.isEmpty()) {
            int step = (modifiers & 1) != 0 ? -1 : 1;
            int index = controls.indexOf(focused);
            if (index < 0) index = step > 0 ? -1 : controls.size();
            while ((index += step) >= 0 && index < controls.size()) {
                AbstractWidget next = controls.get(index);
                if (next.active && next.visible) {
                    if (focused != null) focused.setFocused(false);
                    focused = next;
                    focused.setFocused(true);
                    return true;
                }
            }
            // At either boundary, let Screen navigate to the next outer widget.
            if (focused != null) focused.setFocused(false);
            focused = null;
            return false;
        }
        return focused != null && focused.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scan, int modifiers) {
        return focused != null && focused.keyReleased(key, scan, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.focused != null) {
            dragging = false;
            this.focused.setFocused(false);
            this.focused = null;
        }
        if (!focused) rotating = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
