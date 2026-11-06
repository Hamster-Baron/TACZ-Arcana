package group.taczexpands.client.render.surface;

import group.taczexpands.client.gui.CustomHUDOverlay;
import group.taczexpands.common.network.v2.s2c.S2CAddHUD;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Selects a feed's elements and canvas; drawing stays in the normal Arcana HUD renderer. */
public final class SurfaceHud {
    private static boolean drawing;

    private SurfaceHud() {}

    public static boolean isDrawing() {
        return drawing;
    }

    static List<S2CAddHUD> compile(SurfaceDefinition.Hud definition) {
        List<S2CAddHUD> elements = new ArrayList<>();
        int index = 0;
        for (var element : definition.elements()) {
            elements.add(
                    new S2CAddHUD(
                            "surface_static_" + index++,
                            S2CAddHUD.RenderSpace.SCREEN,
                            S2CAddHUD.Alignment.LEFT,
                            element.x(),
                            element.y(),
                            0,
                            element.scale(),
                            element.text(),
                            element.image(),
                            element.width(),
                            element.height(),
                            S2CAddHUD.Anchor.valueOf(SurfaceConfig.anchor(element.anchor())),
                            S2CAddHUD.Anchor.valueOf(SurfaceConfig.anchor(element.pivot()))));
        }
        return List.copyOf(elements);
    }

    static void render(SurfaceFeed feed, Set<String> feedIds) {
        var minecraft = Minecraft.getInstance();
        var canvas = feed.definition().hud();
        var elements = new ArrayList<>(feed.staticHud());
        elements.addAll(CustomHUDOverlay.INSTANCE.forFeed(feed.id()));
        var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        var fit =
                CanvasFit.of(
                        canvas.fit(), canvas.width(), canvas.height(), feed.definition().width(), feed.definition().height());
        graphics.pose().translate(fit.x(), fit.y(), 0);
        graphics.pose().scale(fit.sx(), fit.sy(), 1);
        drawing = true;
        try {
            CustomHUDOverlay.INSTANCE.renderElements(
                    elements, graphics, canvas.width(), canvas.height());
            graphics.flush();
        } finally {
            drawing = false;
        }
    }

}