package group.taczexpands.client.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import group.taczexpands.client.accessor.IAccessorGunDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GunDisplay.class, remap = false)
public class MixinGunDisplay implements IAccessorGunDisplay {
    @Unique
    @SerializedName("hide_muzzle_flash")
    private boolean taczexpands$hideMuzzleFlash = false;

    @Unique
    @SerializedName("hide_shell")
    private boolean taczexpands$hideShell = false;

    @Override
    public boolean taczexpands$getHideMuzzleFlash() {
        return taczexpands$hideMuzzleFlash;
    }

    @Override
    public boolean taczexpands$getHideShell() {
        return taczexpands$hideShell;
    }
}
