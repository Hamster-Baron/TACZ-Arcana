package group.taczexpands.client.mixin;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.LocalPlayerShoot;
import group.taczexpands.client.input.KeyInputs;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayerShoot.class, remap = false)
public class MixinLocalPlayerShoot {
    @Shadow
    @Final
    private LocalPlayer player;

    @Inject(method = "shoot", at = @At("HEAD"), cancellable = true)
    public void onShoot(CallbackInfoReturnable<ShootResult> cir) {
        if (KeyInputs.INSTANCE.getFireCancelTicks() > 0 || KeyInputs.INSTANCE.getMissileFireCancelTicks() > 0) cir.setReturnValue(ShootResult.COOL_DOWN);
        else {
            var mainHand = player.getMainHandItem();
            var gunExtra = IAccessorGunData.getExtraHolder(mainHand);
            if (gunExtra == null) return;
            if (gunExtra.durability > 0 && GunExtras.INSTANCE.getDurabilityDamage(mainHand) >= gunExtra.durability) {
                cir.setReturnValue(ShootResult.COOL_DOWN);
            }
        }
    }
}
