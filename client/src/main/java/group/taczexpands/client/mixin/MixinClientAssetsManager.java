package group.taczexpands.client.mixin;

import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.render.model.CoplanarFaceSeparation;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientAssetsManager.class, remap = false)
public class MixinClientAssetsManager {
    // Every gun, attachment, ammo and block model is built from a POJO handed out here.
    @Inject(method = "getBedrockModelPOJO", at = @At("RETURN"))
    private void taczexpands$separateCoplanarFaces(ResourceLocation id, CallbackInfoReturnable<BedrockModelPOJO> cir) {
        BedrockModelPOJO pojo = cir.getReturnValue();
        if (pojo != null && ClientConfig.INSTANCE.getCoplanarFaceSeparationNamespaces().get().contains(id.getNamespace())) {
            CoplanarFaceSeparation.separateOnce(pojo);
        }
    }
}
