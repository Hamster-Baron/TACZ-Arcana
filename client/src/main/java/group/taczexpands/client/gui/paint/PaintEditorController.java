package group.taczexpands.client.gui.paint;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.GunRefitScreen;
import group.taczexpands.client.accessor.IAccessorPaintRefitScreen;
import group.taczexpands.client.config.PaintUiConfig;
import group.taczexpands.client.network.NetworkManager;
import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.client.render.paint.PaintPreviewCache;
import group.taczexpands.client.render.paint.PaintQueries;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.network.c2s.C2SRaw;
import group.taczexpands.common.paint.PaintConstants;
import group.taczexpands.common.paint.PaintState;
import group.taczexpands.common.paint.PaintUpdate;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Owns refit integration, the editing target, private draft and ordered network submission. */
public final class PaintEditorController {
    private final Screen screen;
    private final IAccessorPaintRefitScreen host;
    private int slot;
    private String uuid;
    private ResourceLocation moduleId;
    private PaintState draft, submitted;
    private PaintConfig.Recipe recipe;
    private boolean invalid = true, previewDirty;
    private long changedAt;
    private ItemStack preview = ItemStack.EMPTY;

    public PaintEditorController(Screen screen, IAccessorPaintRefitScreen host) {
        this.screen = screen;
        this.host = host;
    }

    public static void beforeRefit(int gunSlot, AttachmentType type) {
        var mc = Minecraft.getInstance();
        if (type.name().equals(PaintConstants.MODULE) && mc.player != null
                && mc.player.getInventory().selected == gunSlot
                && mc.screen instanceof GunRefitScreen screen) {
            ((IAccessorPaintRefitScreen) screen).taczexpands$invalidatePaint();
        }
    }

    public static boolean supports(ItemStack gun) {
        if (IGun.getIGunOrNull(gun) == null) return false;
        String uuid = GunExtras.INSTANCE.getUUID(gun);
        return uuid != null && !uuid.isEmpty() && hasRegions(PaintQueries.editableRegions(gun));
    }

    private static boolean hasRegions(boolean[] regions) {
        return regions[0] || regions[1] || regions[2];
    }

    private PaintEditorPanel open(int x, int y, int width) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack gun = player.getMainHandItem();
        IGun accessor = IGun.getIGunOrNull(gun);
        if (accessor == null) return null;
        AttachmentType module = AttachmentType.valueOf(PaintConstants.MODULE);
        if (accessor.getAttachment(gun, module).isEmpty()) return null;
        String targetUuid = GunExtras.INSTANCE.getUUID(gun);
        PaintConfig.Recipe current = PaintQueries.recipe(gun);
        boolean[] regions = PaintQueries.editableRegions(gun);
        if (targetUuid == null || targetUuid.isEmpty() || current == null || !hasRegions(regions)) return null;
        slot = player.getInventory().selected;
        uuid = targetUuid;
        moduleId = accessor.getAttachmentId(gun, module);
        draft = submitted = PaintState.read(accessor.getAttachmentTag(gun, module));
        recipe = current;
        invalid = false;
        changedAt = 0;
        previewDirty = true;
        PaintEditorPanel next = new PaintEditorPanel(x, y, width, this);
        next.refresh(recipe, regions, labels(gun));
        return next;
    }

    private static String[] labels(ItemStack gun) {
        return new String[] {PaintQueries.regionLabel(gun, 0), PaintQueries.regionLabel(gun, 1),
                PaintQueries.regionLabel(gun, 2)};
    }

    boolean targetValid() {
        if (invalid) return false;
        var player = Minecraft.getInstance().player;
        if (player == null || player.getInventory().selected != slot) return false;
        ItemStack gun = player.getMainHandItem();
        IGun accessor = IGun.getIGunOrNull(gun);
        AttachmentType module = AttachmentType.valueOf(PaintConstants.MODULE);
        return accessor != null && uuid.equals(GunExtras.INSTANCE.getUUID(gun))
                && moduleId.equals(accessor.getAttachmentId(gun, module))
                && !accessor.getAttachment(gun, module).isEmpty();
    }

    private void invalidateSession() {
        invalid = true;
        preview = ItemStack.EMPTY;
        PaintPreviewCache.clear();
        panel.dispose();
    }

    private void closeSession() {
        submit();
        invalidateSession();
    }

    PaintState draft() {
        return draft;
    }

    boolean pending() {
        return !draft.equals(submitted);
    }

    ItemStack preview() {
        return preview;
    }

    void setAxis(int index, int q) {
        setDraft(draft.with(index, q));
    }

    private void setDraft(PaintState next) {
        if (next.equals(draft)) return;
        draft = next;
        previewDirty = true;
        changedAt = System.nanoTime();
    }

    void reset() {
        setDraft(PaintState.DEFAULT);
    }

    void submit() {
        if (!targetValid() || !pending()) return;
        if (NetworkManager.INSTANCE.sendToServer(new C2SRaw(PaintUpdate.TYPE_PAINT_UPDATE,
                new PaintUpdate(slot, uuid, moduleId, draft).encode()))) submitted = draft;
    }

    boolean prepareFrame(boolean dragging) {
        if (!targetValid()) return false;
        ItemStack gun = Minecraft.getInstance().player.getMainHandItem();
        PaintConfig.Recipe current = PaintQueries.recipe(gun);
        if (current != recipe) {
            boolean[] regions = PaintQueries.editableRegions(gun);
            if (current == null || !hasRegions(regions)) {
                invalid = true;
                return false;
            }
            recipe = current;
            panel.refresh(recipe, regions, labels(gun));
            previewDirty = true;
        }
        if (!dragging && changedAt != 0 && System.nanoTime() - changedAt >= 200_000_000L) submit();
        if (previewDirty) {
            preview = gun.copy();
            IGun accessor = IGun.getIGunOrNull(preview);
            draft.write(accessor.getAttachmentTag(preview, AttachmentType.valueOf(PaintConstants.MODULE)));
            PaintPreviewCache.begin(preview);
            previewDirty = false;
        }
        return true;
    }

    public boolean hasPointerCapture() {
        return panel != null && panel.hasPointerCapture();
    }

    public boolean mouseReleased(double x, double y, int button) {
        return panel != null && panel.mouseReleased(x, y, button);
    }

    // In paint mode the left property-diagram column hosts the paint editor. TacZ's
    // show/hide buttons control that column; unknown layouts place the editor on the right.
    private static final String SHOW_DIAGRAMS = "gui.tacz.gun_refit.property_diagrams.show";
    private static final String HIDE_DIAGRAMS = "gui.tacz.gun_refit.property_diagrams.hide";
    private static final int PAINT_Y = 11, PAINT_W = 210;
    /** Set while this mod has collapsed diagrams the player had expanded; reopened outside paint mode. */
    private static boolean collapsedDiagrams;
    private static boolean switchingDiagrams;
    private boolean paintMode;
    private int paintX, toggleX, toggleW;

    private PaintEditorPanel panel;
    private Button toggle;
    private int coveredHeight = -1;

    private final Map<AbstractWidget, boolean[]> covered = new IdentityHashMap<>();

    private Button diagramButton(String key) {
        for (var child : screen.children())
            if (child instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals(key)) return button;
        return null;
    }

    /** Presses a TacZ diagram button; TacZ toggles its own state and re-runs init. */
    private boolean pressDiagrams(Button button) {
        if (button == null || switchingDiagrams) return false;
        switchingDiagrams = true;
        try {
            button.onPress();
        } finally {
            switchingDiagrams = false;
        }
        return true;
    }

    public void initialize() {
        restoreCovered();
        paintMode = canPaint();
        Button show = diagramButton(SHOW_DIAGRAMS);
        if (!paintMode) {
            toggle = null;
            close();
            if (collapsedDiagrams && show != null) {
                collapsedDiagrams = false;
                pressDiagrams(show);
            }
            return;
        }
        // Expanded diagrams would share the column: collapse through TacZ's own button first.
        if (pressDiagrams(diagramButton(HIDE_DIAGRAMS))) {
            collapsedDiagrams = true;
            return;
        }
        if (show != null && PaintUiConfig.layout() == PaintUiConfig.Layout.LEFT) {
            // The paint toggle takes the place of TacZ's "show diagrams" button.
            show.visible = false;
            show.active = false;
            paintX = toggleX = 11;
            toggleW = PAINT_W;
        } else {
            // RIGHT, or an unknown TacZ layout: panel ends left of the variant/adapter columns; a compact tab
            // is right-aligned to it on the top row, between TacZ's diagram button and slot icons.
            paintX = Math.max(5, screen.width - 30 - 2 * (78 + 4) - 4 - PAINT_W);
            toggleW = 96;
            toggleX = paintX + PAINT_W - toggleW;
        }
        if (panel != null && !targetValid())
            invalidate();
        // Retain drafts across acknowledgement, page changes and resize rebuilds.
        if (panel != null) mount();
        toggle =
                new PaintButton(
                                toggleX,
                                PAINT_Y,
                                toggleW,
                                16,
                                toggleText(),
                                PaintButton.Style.TOGGLE,
                                "✎",
                                ignored -> {
                                    if (panel != null) close();
                                    else {
                                        panel =
                                                open(
                                                        paintX, PAINT_Y + 18, PAINT_W);
                                        if (panel != null) mount();
                                    }
                                    toggle.setMessage(toggleText());
                                })
                        .lit(() -> panel != null);
        host.taczexpands$addPaintWidget(toggle);
    }

    public void validate() {
        // Remove stale panels before Screen iterates renderables.
        boolean available = canPaint();
        if (available != paintMode) {
            // Swap the column between TacZ diagrams and the paint editor.
            host.taczexpands$rebuildPaintWidgets();
            return;
        }
        if (toggle != null) {
            toggle.visible = available;
            toggle.active = available;
        }
        if (panel != null) {
            if (!available || !targetValid()) invalidate();
            else if (coveredHeight != panel.getHeight()) {
                restoreCovered();
                cover();
            }
        }
    }

    private boolean canPaint() {
        var player = Minecraft.getInstance().player;
        return RefitTransform.getCurrentTransformType().name().equals(PaintConstants.MODULE)
                && player != null
                && supports(player.getMainHandItem());
    }

    private Component toggleText() {
        return Component.translatable(
                panel == null
                        ? "gui.taczexpands.paint.open"
                        : "gui.taczexpands.paint.close");
    }

    private void mount() {
        PaintEditorPanel panel = this.panel;
        panel.relocate(paintX, PAINT_Y + 18);
        cover();
        host.taczexpands$addPaintWidget(panel);
    }

    private void cover() {
        PaintEditorPanel panel = this.panel;
        coveredHeight = panel.getHeight();
        // Reserve the native inventory plus both optional variant/adapter columns.
        // Disable covered controls on narrow GUI scales to keep input on the visible page.
        for (var child : screen.children())
            if (child instanceof AbstractWidget widget
                    && widget != panel
                    && widget != toggle
                    && widget.getX() < panel.getX() + panel.getWidth()
                    && widget.getX() + widget.getWidth() > panel.getX()
                    && widget.getY() < panel.getY() + panel.getHeight()
                    && widget.getY() + widget.getHeight() > panel.getY()) {
                covered.put(widget, new boolean[] {widget.visible, widget.active});
                widget.visible = false;
                widget.active = false;
                widget.setFocused(false);
            }
    }

    private void restoreCovered() {
        covered.forEach(
                (widget, state) -> {
                    widget.visible = state[0];
                    widget.active = state[1];
                });
        covered.clear();
        coveredHeight = -1;
    }

    public void close() {
        if (panel != null) {
            closeSession();
            host.taczexpands$removePaintWidget(panel);
            panel = null;
        }
        restoreCovered();
    }

    public void invalidate() {
        if (panel != null) {
            invalidateSession();
            host.taczexpands$removePaintWidget(panel);
            panel = null;
        }
        restoreCovered();
        if (toggle != null)
            toggle.setMessage(toggleText());
    }

}
