package group.taczexpands.client.accessor;

import com.google.common.collect.Lists;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.Animations;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoEntityDisplay;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IAccessorAmmoEntityDisplay {
    ResourceLocation taczexpands$getAnimation();

    String taczexpands$getAnimationName();

    int taczexpands$getAnimationDelay();

    BedrockAmmoModel taczexpands$createAmmoModel();

    @Nullable
    static ResourceLocation getAnimation(AmmoEntityDisplay ammoEntityDisplay) {
        return ((IAccessorAmmoEntityDisplay) ammoEntityDisplay).taczexpands$getAnimation();
    }

    @Nullable
    static String getAnimationName(AmmoEntityDisplay ammoEntityDisplay) {
        return ((IAccessorAmmoEntityDisplay) ammoEntityDisplay).taczexpands$getAnimationName();
    }

    static int getAnimationDelay(AmmoEntityDisplay ammoEntityDisplay) {
        return ((IAccessorAmmoEntityDisplay) ammoEntityDisplay).taczexpands$getAnimationDelay();
    }

    static boolean hasAnimation(AmmoEntityDisplay ammoEntityDisplay) {
        return getAnimation(ammoEntityDisplay) != null;
    }

    @Nullable
    static BedrockAmmoModel createAmmoModel(AmmoEntityDisplay ammoEntityDisplay) {
        return ((IAccessorAmmoEntityDisplay) ammoEntityDisplay).taczexpands$createAmmoModel();
    }

    @Nullable
    static AnimationController createAnimationController(@NotNull AmmoEntityDisplay ammoEntityDisplay, @NotNull BedrockAmmoModel bedrockAmmoModel) {
        var model = IAccessorBedrockAmmoModel.getAnimated(bedrockAmmoModel);
        ResourceLocation location = ((IAccessorAmmoEntityDisplay) ammoEntityDisplay).taczexpands$getAnimation();
        AnimationController controller;
        if (location == null) {
            controller = new AnimationController(Lists.newArrayList(), model);
        } else {
            AnimationStructure gltfAnimations = ClientAssetsManager.INSTANCE.getGltfAnimation(location);
            BedrockAnimationFile bedrockAnimationFile = ClientAssetsManager.INSTANCE.getBedrockAnimations(location);
            if (bedrockAnimationFile != null) {
                controller = Animations.createControllerFromBedrock(bedrockAnimationFile, model);
            } else if (gltfAnimations != null) {
                controller = Animations.createControllerFromGltf(gltfAnimations, model);
            } else {
                return null;
            }
        }
        return controller;
    }
}
