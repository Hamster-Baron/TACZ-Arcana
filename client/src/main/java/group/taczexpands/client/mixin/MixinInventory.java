package group.taczexpands.client.mixin;

import group.taczexpands.client.input.KeyInputs;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class MixinInventory {
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    public void swapPaint(double pDirection, CallbackInfo ci) {
        if (KeyInputs.INSTANCE.getInventoryCancelTicks() > 0) {
            ci.cancel();
        }
    }
}
