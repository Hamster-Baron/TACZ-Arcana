package group.taczexpands.client.accessor;

import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoEntityDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IAccessorEntityKineticBullet {
    @Nullable
    Map.Entry<BedrockAmmoModel, AnimationController> taczexpands$getAnimatedModel(@NotNull AmmoEntityDisplay ammoEntityDisplay);
}
