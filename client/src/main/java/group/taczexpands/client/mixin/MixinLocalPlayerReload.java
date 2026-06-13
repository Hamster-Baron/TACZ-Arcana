package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.client.input.KeyInputs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerReload.class, remap = false)
public class MixinLocalPlayerReload {
    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    public void onReload(CallbackInfo ci) {
        if (KeyInputs.INSTANCE.getReloadCancelTicks() > 0) ci.cancel();
    }

    @Inject(method = "doReload", at = @At("HEAD"))
    public void preDoReload(IGun iGun, GunDisplayInstance display, GunData gunData, ItemStack mainHandItem, CallbackInfo ci) {
        var animationStateMachine = display.getAnimationStateMachine();
        if (animationStateMachine != null) {
            var context = animationStateMachine.getContext();
            if (context != null) {
                context.setCurrentGunItem(mainHandItem);
            }
        }
    }
}
