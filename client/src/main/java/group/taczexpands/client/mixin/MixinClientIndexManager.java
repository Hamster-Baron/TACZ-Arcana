package group.taczexpands.client.mixin;

import com.tacz.guns.client.resource.ClientIndexManager;
import group.taczexpands.client.entity.CustomDisplayManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientIndexManager.class, remap = false)
public class MixinClientIndexManager {
    @Inject(method = "reload", at = @At("TAIL"))
     private static void onReload(CallbackInfo ci) {
        CustomDisplayManager.INSTANCE.onReload();
    }
}
