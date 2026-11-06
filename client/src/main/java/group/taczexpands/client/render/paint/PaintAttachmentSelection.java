package group.taczexpands.client.render.paint;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.util.SlotAdapterHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** The native adapter/overlay/replace/base-LOD choices, shared by UI support and material identity. */
public final class PaintAttachmentSelection {
    private PaintAttachmentSelection() {}
    record Selected(Object index, ResourceLocation original, boolean useLod) {}

    public static Object body(ClientAttachmentIndex index, ClientAttachmentVariantIndex variant,
                              boolean useLod) {
        return !useLod && variant != null && variant.isReplaceMode()
                && variant.getAttachmentModel() != null && variant.getModelTexture() != null ? variant : index;
    }

    static List<Selected> layers(ClientAttachmentIndex index, ClientAttachmentVariantIndex variant,
                                 ClientSlotAdapterIndex adapter, ResourceLocation lodTexture) {
        List<Selected> result = new ArrayList<>();
        if (adapter != null && adapter.getAdapterModel() != null && adapter.getModelTexture() != null) {
            result.add(new Selected(adapter, adapter.getModelTexture(), false));
        }
        if (variant != null && variant.isOverlayMode() && variant.getAttachmentModel() != null
                && variant.getModelTexture() != null) {
            result.add(new Selected(variant, variant.getModelTexture(), false));
        }
        if (lodTexture != null) {
            result.add(new Selected(index, lodTexture, true));
        } else if (body(index, variant, false) == variant) {
            result.add(new Selected(variant, variant.getModelTexture(), false));
        } else if (index.getAttachmentModel() != null && index.getModelTexture() != null) {
            result.add(new Selected(index, index.getModelTexture(), false));
        }
        return result;
    }

    static List<Selected> installedNear(ItemStack gun, Predicate<AttachmentType> visible) {
        IGun accessor = IGun.getIGunOrNull(gun);
        if (accessor == null) return List.of();
        List<Selected> result = new ArrayList<>();
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE || !visible.test(type)) continue;
            ItemStack attachment = accessor.getAttachment(gun, type);
            if (attachment.isEmpty()) attachment = accessor.getBuiltinAttachment(gun, type);
            IAttachment item = IAttachment.getIAttachmentOrNull(attachment);
            if (item == null) continue;
            ResourceLocation id = item.getAttachmentId(attachment);
            var index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) continue;
            var variant = TimelessAPI.getClientAttachmentVariantIndex(attachment).orElse(null);
            ResourceLocation adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(gun, type, id);
            var adapter = adapterId == null ? null : TimelessAPI.getClientSlotAdapterIndex(adapterId).orElse(null);
            result.addAll(layers(index, variant, adapter, null));
        }
        return result;
    }
}
