package group.taczexpands.client.mixin;

import com.tacz.guns.client.gui.components.refit.GunPropertyDiagrams;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunPropertyDiagrams.class, remap = false)
public class MixinGunPropertyDiagrams {
    @ModifyVariable(method = "lambda$draw$3", at = @At(value = "STORE"), ordinal = 0)
    private static float modifyAimTime(float aimTime) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return aimTime;
        }

        Float modifier = GunExtras.INSTANCE.getAimTimeModifier(Minecraft.getInstance().player, player.getMainHandItem());
        if (modifier != null) {
            return aimTime * modifier;
        }
        return aimTime;
    }

    @Redirect(method = "lambda$draw$3", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoAmount()I"))
    private static int hookGetAmmoAmount(GunData instance) {
        ItemStack gunItem = null;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            gunItem = player.getMainHandItem();
        }
        return IAccessorGunData.getCurrentAmmoAmount(instance, gunItem);
    }
}
