package group.taczexpands.common.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import group.taczexpands.common.accessor.IAccessorMiscPOJO;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = AttachmentItem.class, remap = false)
public class MixinAttachmentItem {
    @Inject(method = "lambda$fillItemCategory$1", at = @At("HEAD"), cancellable = true)
    private static void rewriteFillCondition(AttachmentType type, NonNullList<ItemStack> stacks, Map.Entry<ResourceLocation, CommonAttachmentIndex> entry, CallbackInfo ci) {
        ci.cancel();

        if (entry.getValue().getPojo().isHidden()) {
            return;
        }

        if (IAccessorMiscPOJO.isMisc(entry.getValue().getPojo())) {
            return;
        }

        if (type.equals(entry.getValue().getType())) {
            ItemStack itemStack = AttachmentItemBuilder.create().setId(entry.getKey()).setCount(1).build();
            stacks.add(itemStack);
        }
    }
}
