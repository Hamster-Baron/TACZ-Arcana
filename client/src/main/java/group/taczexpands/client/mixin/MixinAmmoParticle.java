package group.taczexpands.client.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AmmoParticle.class, remap = false)
public class MixinAmmoParticle implements IAccessorAmmoParticle {
    @Unique
    @SerializedName("next")
    private AmmoParticle taczexpands$next;


    @Override
    public @Nullable AmmoParticle taczexpands$getNext() {
        return taczexpands$next;
    }
}
