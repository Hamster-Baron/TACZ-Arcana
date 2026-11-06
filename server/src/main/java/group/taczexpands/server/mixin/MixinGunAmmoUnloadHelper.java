package group.taczexpands.server.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.util.GunAmmoUnloadHelper;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunAmmoUnloadHelper.class, remap = false)
public abstract class MixinGunAmmoUnloadHelper {
    @ModifyExpressionValue(method = "unloadAmmo", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation taczexpands$selectedAmmo(ResourceLocation original, LivingEntity shooter,
            ItemStack stack, AbstractGunItem gun, CommonGunIndex index, boolean unloadBarrel, int amount) {
        return IAccessorGunData.getCurrentAmmoId(index.getGunData(), stack);
    }
}
