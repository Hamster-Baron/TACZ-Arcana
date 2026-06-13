package group.taczexpands.client.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerDraw;
import group.taczexpands.client.gui.GunContextManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerDraw.class, remap = false)
public class MixinLocalPlayerDraw {

    @Inject(method = "resetData",at = @At("TAIL"))
    public void resetData(CallbackInfo ci) {
        GunContextManager.INSTANCE.resetUnderBarrelLockReleaseTime();
    }

}
