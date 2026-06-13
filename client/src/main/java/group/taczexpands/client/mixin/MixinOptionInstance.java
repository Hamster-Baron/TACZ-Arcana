package group.taczexpands.client.mixin;

import group.taczexpands.client.TACZExpandsClient;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class MixinOptionInstance<T> {
    @Shadow
    T value;

    @Shadow
    @Final
    Component caption;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void get(CallbackInfoReturnable<T> cir) {
        if (value instanceof Double) {
            var contents = caption.getContents();
            if (contents instanceof TranslatableContents translatableContents) {
                if (translatableContents.getKey().equals("options.gamma")) {
                    if (!TACZExpandsClient.Companion.shouldUseThermalImaging() && TACZExpandsClient.Companion.shouldUseNightVision()) {
                        cir.setReturnValue((T) (Double) 1000.0);
                    }
                }
            }
        }
    }
}
