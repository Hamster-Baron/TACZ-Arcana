package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.LocalPlayerBolt;
import com.tacz.guns.client.input.ShootKey;
import group.taczexpands.client.input.InputManager;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerBolt.class, remap = false)
public class MixinLocalPlayerBolt {
    @Inject(method = "tickAutoBolt", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preTickAutoBolt(CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        var mainHand = player.getMainHandItem();
        var iGun = IGun.getIGunOrNull(mainHand);
        if (iGun == null) return;
        var gunExtra = IAccessorGunData.getExtraHolder(mainHand);
        if (gunExtra == null) return;
        if (gunExtra.onlyBoltOnRelease && ShootKey.SHOOT_KEY.isDown()) {
            ci.cancel();
        }
    }
}
