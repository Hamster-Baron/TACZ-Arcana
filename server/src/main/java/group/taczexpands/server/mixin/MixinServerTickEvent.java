package group.taczexpands.server.mixin;

import group.taczexpands.server.override.CycleTaskHelperOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = com.tacz.guns.event.ServerTickEvent.class,remap = false)
public class MixinServerTickEvent {
    @Inject(method = "onServerTick", at = @At("TAIL"))
    private static void postOnServerTick(CallbackInfo ci) {
        CycleTaskHelperOverride.tick();
    }
}
