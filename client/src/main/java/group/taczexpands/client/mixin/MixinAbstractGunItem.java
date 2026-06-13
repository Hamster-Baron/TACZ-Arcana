package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AbstractGunItem.class, remap = false)
public class MixinAbstractGunItem {
    @Redirect(method = "getName", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"), remap = true)
    public ResourceLocation hookGetGunId(AbstractGunItem instance, ItemStack itemStack) {
        return GunExtras.INSTANCE.getGunId(itemStack);
    }

    @Redirect(method = "isSame", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation hookIsSameGetGunId(IGun instance, ItemStack itemStack) {
        return GunExtras.INSTANCE.getGunId(itemStack);
    }
}
