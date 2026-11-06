package group.taczexpands.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry.AttachmentModelVisitor;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import group.taczexpands.client.render.paint.PaintResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/** External item VBO, cabinet and pegboard capture share native model identities. */
@Mixin(value = DisplayGunGeometry.class, remap = false)
public abstract class MixinPaintDisplayAttachments {
    @Unique private static final String DRAW = "Lcom/tacz/guns/client/renderer/block/DisplayGunGeometry;visitOptionalModel(Lcom/tacz/guns/client/model/ExternalGunModel;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/renderer/block/DisplayGunGeometry$AttachmentModelVisitor;)V";
    @Unique private static final String VARIANT = "Lcom/tacz/guns/api/TimelessAPI;getClientAttachmentVariantIndex(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;";

    // Capture the variant after its lookup and apply the material selector to both slices.
    @WrapOperation(method = "forEachAttachmentModel", at = @At(value = "INVOKE", target = DRAW),
            slice = @Slice(to = @At(value = "INVOKE", target = VARIANT)))
    private static void taczexpands$beforeVariant(ExternalGunModel model, ResourceLocation texture,
            ItemStack attachment, PoseStack pose, AttachmentModelVisitor visitor, Operation<Void> original,
            @Local(argsOnly = true) ItemStack gun,
            @Local ClientSlotAdapterIndex adapter,
            @Local ClientAttachmentIndex index) {
        original.call(model, taczexpands$material(gun, model, texture, adapter, null, index, false), attachment, pose, visitor);
    }

    @WrapOperation(method = "forEachAttachmentModel", at = @At(value = "INVOKE", target = DRAW),
            slice = @Slice(from = @At(value = "INVOKE", target = VARIANT)))
    private static void taczexpands$afterVariant(ExternalGunModel model, ResourceLocation texture,
            ItemStack attachment, PoseStack pose, AttachmentModelVisitor visitor, Operation<Void> original,
            @Local(argsOnly = true) ItemStack gun,
            @Local(argsOnly = true) boolean useLod,
            @Local ClientSlotAdapterIndex adapter,
            @Local ClientAttachmentVariantIndex variant,
            @Local ClientAttachmentIndex index) {
        original.call(model, taczexpands$material(gun, model, texture, adapter, variant, index, useLod), attachment, pose, visitor);
    }

    @Unique private static ResourceLocation taczexpands$material(ItemStack gun, ExternalGunModel model,
            ResourceLocation texture, ClientSlotAdapterIndex adapter, ClientAttachmentVariantIndex variant,
            ClientAttachmentIndex index, boolean useLod) {
        if (model == null || texture == null) return texture;
        if (adapter != null && model == adapter.getExternalModel() && texture.equals(adapter.getModelTexture())) {
            return PaintResolver.resolveAttachment(gun, adapter, texture, false);
        }
        if (variant != null && model == variant.getExternalModel() && texture.equals(variant.getModelTexture())) {
            return PaintResolver.resolveAttachment(gun, variant, texture, false);
        }
        var lod = useLod ? index.getExternalLodModel() : null;
        boolean bodyLod = lod != null && model == lod.getLeft() && texture.equals(lod.getRight());
        return PaintResolver.resolveAttachment(gun, index, texture, bodyLod);
    }
}
