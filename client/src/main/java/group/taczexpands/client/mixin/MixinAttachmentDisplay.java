package group.taczexpands.client.mixin;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import group.taczexpands.client.accessor.IAccessorPaintAttachmentDisplay;
import group.taczexpands.client.paint.PaintConfig;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(value = AttachmentDisplay.class, remap = false)
public class MixinAttachmentDisplay implements IAccessorPaintAttachmentDisplay {
    @Unique @SerializedName("paint") private JsonElement taczexpands$paintDefinition;
    @Unique private transient PaintConfig.Recipe taczexpands$paint;
    @Unique private transient PaintConfig.Surface taczexpands$paintSurface;
    @Unique private transient boolean taczexpands$paintCompiled;

    @Override
    public void taczexpands$compilePaint(ResourceLocation displayId, Supplier<JsonElement> defaults,
                                         Consumer<String> diagnostics) {
        if (taczexpands$paintCompiled) return;
        JsonElement paint = taczexpands$paintDefinition != null ? taczexpands$paintDefinition : defaults.get();
        if (paint != null && paint.isJsonObject()) {
            var definition = paint.getAsJsonObject();
            if (definition.has("channels") || definition.has("base")) {
                taczexpands$paint = PaintConfig.compileRecipe(definition, displayId, diagnostics);
            }
            if (definition.has("mask")) taczexpands$paintSurface = PaintConfig.compileSurface(definition, displayId, diagnostics);
        } else {
            // Validate paint shape before selecting channels so surface attachments receive diagnostics.
            taczexpands$paint = PaintConfig.compileRecipe(paint, displayId, diagnostics);
        }
        taczexpands$paintCompiled = true;
    }

    @Override
    public PaintConfig.Recipe taczexpands$getPaint() { return taczexpands$paint; }

    @Override
    public PaintConfig.Surface taczexpands$getPaintSurface() { return taczexpands$paintSurface; }
}
