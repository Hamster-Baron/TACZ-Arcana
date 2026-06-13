package group.taczexpands.client.mixin;

import group.taczexpands.client.input.KeyInputs;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @Inject(method = "tick", at = @At("TAIL"))
    public void postTick(boolean pIsSneaking, float pSneakingSpeedMultiplier, CallbackInfo ci) {
        KeyInputs.INSTANCE.onPostInputTick();
    }
}
