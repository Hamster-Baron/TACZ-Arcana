package group.taczexpands.common.mixin;

import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import group.taczexpands.common.accessor.IAccessorAmmoIndexPOJO;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AmmoItem.class, remap = false)
public class MixinAmmoItem {
    @Inject(method = "lambda$fillItemCategory$1", at = @At("HEAD"), cancellable = true)
    private static void taczexpands$hideIndexedAmmo(NonNullList<ItemStack> stacks,
                                                  Map.Entry<ResourceLocation, CommonAmmoIndex> entry, CallbackInfo ci) {
        if (IAccessorAmmoIndexPOJO.isHidden(entry.getValue().getPojo())) ci.cancel();
    }
}
