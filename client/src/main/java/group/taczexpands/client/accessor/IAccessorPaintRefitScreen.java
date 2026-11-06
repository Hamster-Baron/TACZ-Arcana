package group.taczexpands.client.accessor;

import net.minecraft.client.gui.components.AbstractWidget;

/** Protected Screen operations used by the paint controller. */
public interface IAccessorPaintRefitScreen {
    void taczexpands$invalidatePaint();
    void taczexpands$addPaintWidget(AbstractWidget widget);
    void taczexpands$removePaintWidget(AbstractWidget widget);
    void taczexpands$rebuildPaintWidgets();
}
