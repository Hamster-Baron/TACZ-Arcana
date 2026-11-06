package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import group.taczexpands.client.accessor.IAccessorGunDisplay;
import group.taczexpands.common.data.UnderBarrel;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MuzzleParticleManager.class, remap = false)
public class MixinMuzzleParticleManager {
    @ModifyExpressionValue(method = "resolveShot", at = @At(value = "INVOKE", target =
        "Lcom/tacz/guns/client/particle/MuzzleParticleManager$MuzzleAttachmentEffect;silenced()Z"))
    private static boolean taczexpands$ignoreUnderBarrelSilencer(boolean original, ItemStack gunItem) {
        if (!GunExtras.INSTANCE.getUsingUnderBarrel(gunItem)) return original;
        UnderBarrel underBarrel = GunExtras.INSTANCE.getUnderBarrel(gunItem);
        return underBarrel != null && underBarrel.ignoreSilencer ? false : original;
    }

    @ModifyReturnValue(method = "resolveShot", at = @At("RETURN"))
    private static MuzzleParticleManager.MuzzleShot taczexpands$hideMuzzleFlash(
            MuzzleParticleManager.MuzzleShot original, ItemStack gunItem) {
        if (original == null) return null;
        GunDisplayInstance display = TimelessAPI.getGunDisplay(gunItem).orElse(null);
        if (display == null || !IAccessorGunDisplay.getHideMuzzleFlash(display)) return original;
        return new MuzzleParticleManager.MuzzleShot(null, 0.0f, 0,
            original.smokeId(), original.smokeScale(), original.offsetBb(), null,
            original.thirdPersonSmokeId(), original.speedScale(), original.smokeParticleCount(),
            original.smokeSize(), original.smokeDirection());
    }
}
