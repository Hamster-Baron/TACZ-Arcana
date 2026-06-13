package group.taczexpands.client.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import group.taczexpands.common.nbt.GunExtras;
import it.unimi.dsi.fastutil.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MuzzleFlashRender.class, remap = false)
public class MixinMuzzleFlashRender {
    @Shadow
    @Final
    private BedrockGunModel bedrockGunModel;

    @Redirect(method = "lambda$render$1", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/Pair;right()Ljava/lang/Object;"))
    public Object lambda$render$1(Pair instance) {
        var gunItem = bedrockGunModel.getCurrentGunItem();
        if (gunItem != null) {
            if (GunExtras.INSTANCE.getUsingUnderBarrel(gunItem)) {
                var underBarrel = GunExtras.INSTANCE.getUnderBarrel(gunItem);
                if (underBarrel != null && underBarrel.ignoreSilencer) {
                    return false;
                }
            }
        }

        return instance.right();
    }
}
