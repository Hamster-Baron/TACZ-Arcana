package group.taczexpands.client.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.client.gameplay.LocalPlayerAim;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerAim;
import group.taczexpands.client.config.RuntimeConfig;
import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerAim.class, remap = false)
public class MixinLocalPlayerAim {
    @Shadow
    @Final
    private LocalPlayer player;

    @Shadow
    @Final
    private LocalPlayerDataHolder data;

    @Inject(method = "aim", at = @At("HEAD"), cancellable = true)
    public void onAim(boolean isAim, CallbackInfo ci) {
        if (isAim && KeyInputs.INSTANCE.getAimCancelTicks() > 0) ci.cancel();
    }

    @Inject(method = "tickAimingProgress", at = @At("HEAD"))
    public void taczexpands$preTickAimingProgress(CallbackInfo ci) {
        var mainHand = player.getMainHandItem();
        var gunExtra = IAccessorGunData.getExtraHolder(mainHand);
        if (gunExtra == null) return;

        var gunOperator = IGunOperator.fromLivingEntity(player);
        var shouldBlock = (gunExtra.blockAimWhileReloading != null && gunExtra.blockAimWhileReloading) || (gunExtra.blockAimWhileReloading == null && RuntimeConfig.INSTANCE.getDefaultBlockAimWhileReloading());
        if (shouldBlock && gunOperator.getSynReloadState().getStateType().isReloading() && data.clientIsAiming) {
            data.clientIsAiming = false;
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerAim(false));
        }
    }
}
