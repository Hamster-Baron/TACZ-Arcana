package group.taczexpands.common.mixin;

import com.tacz.guns.api.item.IGun;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.tacz.guns.util.SlotAdapterHelper", remap = false)
public abstract class MixinSlotAdapterHelper {
    @Redirect(method = "getHostGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation taczexpands$effectiveHostGunId(IGun instance, ItemStack holder) {
        return GunExtras.INSTANCE.getGunId(holder);
    }
}
