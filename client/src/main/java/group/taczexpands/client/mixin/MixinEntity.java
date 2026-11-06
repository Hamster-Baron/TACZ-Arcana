package group.taczexpands.client.mixin;

import group.taczexpands.client.input.InputManager;
import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.client.input.rotation.RotationController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    public void turn(double yaw, double pitch, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (KeyInputs.INSTANCE.getRotateCancelTicks() > 0) callback.cancel();
        if (InputManager.INSTANCE.getSendCameraInput() && entity == Minecraft.getInstance().player) {
            callback.cancel();
            InputManager.INSTANCE.turn(yaw, pitch);
        }
        if (!callback.isCancelled() && RotationController.turn(entity, yaw, pitch)) callback.cancel();
    }
}
