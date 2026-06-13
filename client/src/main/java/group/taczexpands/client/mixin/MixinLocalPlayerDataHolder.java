package group.taczexpands.client.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import group.taczexpands.client.gui.GunContextManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerDataHolder.class, remap = false)
public class MixinLocalPlayerDataHolder {
    @Inject(method = "tickStateLock", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/entity/IGunOperator;getSynShootCoolDown()J"), cancellable = true)
    private void tickStateLock(CallbackInfo ci) {
        if (System.currentTimeMillis() < GunContextManager.INSTANCE.getUnderBarrelLockReleaseTime())
            ci.cancel();
    }
}
