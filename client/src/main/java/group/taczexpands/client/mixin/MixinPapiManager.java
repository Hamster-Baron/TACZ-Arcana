package group.taczexpands.client.mixin;

import com.tacz.guns.client.model.papi.PapiManager;
import group.taczexpands.client.gui.VariableManager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PapiManager.class, remap = false)
public class MixinPapiManager {
    @Inject(method = "getTextShow", at = @At("RETURN"), cancellable = true)
    private static void onGetTextShowReturn(String textKey, ItemStack stack, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(VariableManager.INSTANCE.processString(cir.getReturnValue()));
    }
}
