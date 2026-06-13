package group.taczexpands.client.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerInspect;
import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.client.network.NetworkManager;
import group.taczexpands.common.network.c2s.C2SActionKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerInspect.class, remap = false)
public class MixinLocalPlayerInspect {
    @Inject(at = @At("TAIL"), method = "lambda$inspect$0")
    private void postInspect(CallbackInfo ci) {
        NetworkManager.INSTANCE.sendToServer(new C2SActionKey(C2SActionKey.Action.INSPECT, null, null, null));
    }

    @Inject(method = "inspect", at = @At("HEAD"), cancellable = true)
    private void preInspect(CallbackInfo ci) {
        if (KeyInputs.INSTANCE.getInspectCancelTicks() > 0) {
            ci.cancel();
        }
    }
}
