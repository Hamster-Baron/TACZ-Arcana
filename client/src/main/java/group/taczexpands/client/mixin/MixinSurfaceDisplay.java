package group.taczexpands.client.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;

import group.taczexpands.client.accessor.IAccessorSurfaceDisplay;
import group.taczexpands.client.render.surface.SurfaceConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Guns and attachments use the same authoring schema and validation. */
@Mixin(
        value = {GunDisplay.class, AttachmentDisplay.class},
        remap = false)
public class MixinSurfaceDisplay implements IAccessorSurfaceDisplay {
    @Unique
    @SerializedName("faces")
    private SurfaceConfig.Face[] taczexpands$surfaceDefinitions = new SurfaceConfig.Face[0];

    @Unique private transient List<SurfaceConfig.Face> taczexpands$surfaceFaces = List.of();

    @Override
    public List<SurfaceConfig.Face> taczexpands$getSurfaceFaces() {
        return taczexpands$surfaceFaces;
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void taczexpands$validateSurfaces(CallbackInfo ci) {
        taczexpands$surfaceFaces = SurfaceConfig.parseFaces(taczexpands$surfaceDefinitions);
    }
}
