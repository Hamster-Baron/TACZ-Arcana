package group.taczexpands.client.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerAim;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LocalPlayerAim.class, remap = false)
public class MixinLocalPlayerArm {
    @Shadow
    @Final
    private LocalPlayer player;

    @ModifyVariable(method = "getAlphaProgress", at = @At(value = "LOAD", ordinal = 0), ordinal = 0)
    private float modifyAimTime(float aimTime) {
        Float modifier = GunExtras.INSTANCE.getAimTimeModifier(Minecraft.getInstance().player, player.getMainHandItem());
        if(modifier != null) {
            return aimTime * modifier;
        }
        return aimTime;
    }



}
