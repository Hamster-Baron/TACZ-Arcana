package group.taczexpands.common.accessor;


import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import group.taczexpands.common.data.AttachmentExtraHolder;
import group.taczexpands.common.data.Flashlight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface IAccessorAttachmentData {
    @Nullable
    static Flashlight getFlashlight(ItemStack itemStack) {
        return getAllExtraHolder(itemStack).stream().map(holder -> holder.flashlight).filter(flashlight -> flashlight.enable).findFirst().orElse(null);
    }

    static List<AttachmentExtraHolder> getAllExtraHolder(ItemStack itemStack) {
        return Arrays.stream(AttachmentType.values()).filter(type -> type != AttachmentType.NONE)
                .map(type -> getExtraHolder(itemStack, type))
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    static AttachmentExtraHolder getExtraHolder(ItemStack itemStack, AttachmentType type) {
        if (itemStack == null) return null;
        var gunItem = ModItems.MODERN_KINETIC_GUN.get();
        if (itemStack.getItem() == gunItem) {
            var attachmentId = gunItem.getAttachmentId(itemStack, type);
            if (attachmentId == DefaultAssets.EMPTY_ATTACHMENT_ID) {
                attachmentId = gunItem.getBuiltInAttachmentId(itemStack, type);
            }

            var optional = TimelessAPI.getCommonAttachmentIndex(attachmentId);
            if (optional.isPresent()) {
                return getExtraHolder(optional.get().getData());
            }
        }
        return null;
    }

    @Nullable
    static AttachmentExtraHolder getExtraHolder(ResourceLocation attachmentId) {
        var optional = TimelessAPI.getCommonAttachmentIndex(attachmentId);
        if (optional.isPresent()) {
            return getExtraHolder(optional.get().getData());
        }

        return null;
    }


    @Nullable
    static AttachmentExtraHolder getExtraHolder(AttachmentData data) {
        return ((IAccessorAttachmentData) data).taczexpands$getExtraHolder();
    }

    AttachmentExtraHolder taczexpands$getExtraHolder();
}
