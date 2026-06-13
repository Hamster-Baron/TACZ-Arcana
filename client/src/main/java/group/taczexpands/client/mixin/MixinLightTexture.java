package group.taczexpands.client.mixin;

import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorLightTexture;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public abstract class MixinLightTexture implements IAccessorLightTexture {

    @Shadow
    private boolean updateLightTexture;


    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float onGetGamma(Double instance) {
        if (!TACZExpandsClient.Companion.shouldUseThermalImaging() && TACZExpandsClient.Companion.shouldUseNightVision()) {
            return 1000.0f;
        }
        return instance.floatValue();
    }

    @Override
    public void taczexpands$setUpdateLightTexture() {
        updateLightTexture = true;
    }
}
