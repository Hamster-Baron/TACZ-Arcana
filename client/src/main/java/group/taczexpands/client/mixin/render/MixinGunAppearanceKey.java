package group.taczexpands.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import group.taczexpands.client.render.gun.AppearanceKeyExtension;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.tacz.guns.client.renderer.external.ExternalGunAppearance$GeometryKey", remap = false)
public abstract class MixinGunAppearanceKey implements AppearanceKeyExtension {
    @Unique private ResourceLocation taczexpands$textureKey;
    @Unique private boolean taczexpands$textureKeyInitialized;

    public ResourceLocation taczexpands$getTextureKey() {
        return taczexpands$textureKey;
    }

    public void taczexpands$initTextureKey(ResourceLocation value) {
        if (taczexpands$textureKeyInitialized) throw new IllegalStateException("Appearance key is immutable");
        taczexpands$textureKey = value;
        taczexpands$textureKeyInitialized = true;
    }

    @ModifyReturnValue(method = "equals", at = @At("RETURN"))
    private boolean taczexpands$textureEquals(boolean result, Object other) {
        return result && other instanceof AppearanceKeyExtension ext
                && Objects.equals(taczexpands$textureKey, ext.taczexpands$getTextureKey());
    }

    @ModifyReturnValue(method = "hashCode", at = @At("RETURN"))
    private int taczexpands$textureHash(int result) {
        return taczexpands$textureKey == null ? result : 31 * result + taczexpands$textureKey.hashCode();
    }
}
