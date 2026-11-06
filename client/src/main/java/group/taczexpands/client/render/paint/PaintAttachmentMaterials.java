package group.taczexpands.client.render.paint;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Material identity for the native attachment/variant/LOD choices in an external snapshot. */
public final class PaintAttachmentMaterials {
    private PaintAttachmentMaterials() {}

    public static ResourceLocation textureKey(ItemStack gun, ExternalGunAppearance.Snapshot snapshot,
                                               ResourceLocation gunTexture) {
        List<ResourceLocation> materials = new ArrayList<>();
        materials.add(gunTexture);
        boolean changed = false;
        for (var key : snapshot.key().attachments()) {
            if (snapshot.model().displayInstance().attachmentPosition(key.type()) < 0) continue;
            var index = TimelessAPI.getClientAttachmentIndex(key.id()).orElse(null);
            if (index == null) continue;
            var adapter = key.slotAdapterId() == null ? null
                    : TimelessAPI.getClientSlotAdapterIndex(key.slotAdapterId()).orElse(null);
            var variant = key.variantId() == null ? null
                    : TimelessAPI.getClientAttachmentVariantIndex(key.variantId()).orElse(null);
            var lod = snapshot.useLod() ? index.getExternalLodModel() : null;
            ResourceLocation lodTexture = lod == null ? null : lod.getRight();
            for (var selected : PaintAttachmentSelection.layers(index, variant, adapter, lodTexture)) {
                if (!drawable(selected)) continue;
                ResourceLocation original = selected.original();
                ResourceLocation texture = PaintResolver.resolveAttachment(gun, selected.index(),
                        original, selected.useLod());
                materials.add(texture);
                changed |= !texture.equals(original);
            }
        }
        // This address identifies a composite material for equality comparisons.
        return changed ? PaintMaterialKeys.resolve(materials) : gunTexture;
    }

    private static boolean drawable(PaintAttachmentSelection.Selected selected) {
        Object index = selected.index();
        if (index instanceof ClientSlotAdapterIndex adapter) return adapter.getExternalModel() != null;
        if (index instanceof ClientAttachmentVariantIndex variant) return variant.getExternalModel() != null;
        if (index instanceof ClientAttachmentIndex attachment) {
            var lod = selected.useLod() ? attachment.getExternalLodModel() : null;
            return selected.useLod() ? lod != null && lod.getLeft() != null : attachment.getExternalModel() != null;
        }
        return false;
    }
}
