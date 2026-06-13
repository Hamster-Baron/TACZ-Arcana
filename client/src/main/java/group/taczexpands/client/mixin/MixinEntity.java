package group.taczexpands.client.mixin;

import group.taczexpands.client.input.InputManager;
import group.taczexpands.client.input.KeyInputs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    public void turn(double yaw, double pitch, CallbackInfo ci) {
        if (KeyInputs.INSTANCE.getRotateCancelTicks() > 0) {
            ci.cancel();
        }

        if (InputManager.INSTANCE.getSendCameraInput() && ((Entity) (Object) this) == Minecraft.getInstance().player) {
            ci.cancel();
            InputManager.INSTANCE.turn(yaw, pitch);
        }
    }
}
