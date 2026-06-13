package group.taczexpands.client.mixin;

import group.taczexpands.client.accessor.IAccessorIrisApiV0Impl;
import net.irisshaders.iris.apiimpl.IrisApiV0Impl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = IrisApiV0Impl.class, remap = false)
public class MixinIrisApiV0Impl implements IAccessorIrisApiV0Impl {
    @Unique
    private boolean taczexpands$hook = false;

    @Inject(method = "isShaderPackInUse", at = @At("HEAD"), cancellable = true)
    public void isShaderPackInUse(CallbackInfoReturnable<Boolean> cir) {
        if (taczexpands$hook) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void taczexpands$setHookUsingShaderPack() {
        taczexpands$hook = true;
    }

    @Override
    public void taczexpands$unSetHookUsingShaderPack() {
        taczexpands$hook = false;
    }
}
