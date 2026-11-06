package group.taczexpands.client.mixin;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonElement;
import group.taczexpands.client.accessor.IAccessorPaintGunDisplay;
import group.taczexpands.client.paint.PaintConfig;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;

import group.taczexpands.client.accessor.IAccessorGunDisplay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
        value = GunDisplay.class,
        remap = false)
public class MixinGunDisplay implements IAccessorGunDisplay, IAccessorPaintGunDisplay {
    @Unique @SerializedName("paint") private JsonElement taczexpands$paintDefinition;
    @Unique private transient PaintConfig.Surface taczexpands$paint;
    @Unique private transient boolean taczexpands$paintCompiled;

    @Override
    public void taczexpands$compilePaint(ResourceLocation displayId, Supplier<JsonElement> defaults,
                                         Consumer<String> diagnostics) {
        if (taczexpands$paintCompiled) return;
        JsonElement definition = taczexpands$paintDefinition != null ? taczexpands$paintDefinition : defaults.get();
        taczexpands$paint = PaintConfig.compileSurface(definition, displayId, diagnostics);
        taczexpands$paintCompiled = true;
    }

    @Override
    public PaintConfig.Surface taczexpands$getPaint() { return taczexpands$paint; }


    @Unique
    @SerializedName(value = "hide_muzzle_flash")
    private boolean taczexpands$hideMuzzleFlash = false;

    @Unique
    @SerializedName(value = "hide_shell")
    private boolean taczexpands$hideShell = false;

    public boolean taczexpands$getHideMuzzleFlash() {
        return this.taczexpands$hideMuzzleFlash;
    }

    public boolean taczexpands$getHideShell() {
        return this.taczexpands$hideShell;
    }
}
