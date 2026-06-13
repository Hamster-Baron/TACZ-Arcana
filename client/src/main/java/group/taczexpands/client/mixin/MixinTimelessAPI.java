package group.taczexpands.client.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.ClientIndexManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TimelessAPI.class, remap = false)
public class MixinTimelessAPI {
    @Redirect(method = "getGunDisplay(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunDisplayId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation hookGetGunDisplayId(IGun instance, ItemStack itemStack) {
        var location = instance.getGunDisplayId(itemStack);
        var underBarrel = GunExtras.INSTANCE.getUnderBarrel(itemStack);
        if (underBarrel != null) {
            var index = TimelessAPI.getCommonGunIndex(GunExtras.INSTANCE.getGunId(itemStack)).orElseGet(() -> null);
            if (index != null) {
                var newLocation = underBarrel.getDisplayId(index.getPojo().getDisplay());
                if (ClientIndexManager.GUN_DISPLAY.get(newLocation) != null) {
                    return newLocation;
                }
            }
        }
        return location;
    }
}

