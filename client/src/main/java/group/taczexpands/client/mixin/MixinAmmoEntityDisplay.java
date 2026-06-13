package group.taczexpands.client.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoEntityDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import group.taczexpands.client.accessor.IAccessorAmmoEntityDisplay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AmmoEntityDisplay.class, remap = false)
public class MixinAmmoEntityDisplay implements IAccessorAmmoEntityDisplay {
    @Shadow
    private ResourceLocation modelLocation;

    @Shadow
    protected ResourceLocation modelTexture;

    @Unique
    @SerializedName("animation")
    public ResourceLocation taczexpands$animation;

    @Unique
    @SerializedName("animation_name")
    public String taczexpands$animationName;

    @Unique
    @SerializedName("animation_delay")
    public int taczexpands$animationDelay = 0;

    @Override
    public ResourceLocation taczexpands$getAnimation() {
        return taczexpands$animation;
    }

    @Override
    public String taczexpands$getAnimationName() {
        return taczexpands$animationName;
    }

    @Override
    public int taczexpands$getAnimationDelay() {
        return taczexpands$animationDelay;
    }

    @Unique
    @Override
    public BedrockAmmoModel taczexpands$createAmmoModel() {
        if (modelLocation != null && modelTexture != null) {
            BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
            if (modelPOJO == null) {
                return null;
            }
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                return new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY);
            }
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                return new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW);
            }
        }

        return null;
    }
}
