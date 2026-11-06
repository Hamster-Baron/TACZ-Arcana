package group.taczexpands.client.accessor;

import group.taczexpands.client.paint.PaintConfig;

import net.minecraft.resources.ResourceLocation;
import com.google.gson.JsonElement;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IAccessorPaintGunDisplay {
    /** {@code defaults} is consulted only when the display itself has no {@code paint} field. */
    void taczexpands$compilePaint(ResourceLocation displayId, Supplier<JsonElement> defaults, Consumer<String> diagnostics);
    PaintConfig.Surface taczexpands$getPaint();
}
