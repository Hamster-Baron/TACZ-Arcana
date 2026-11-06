package group.taczexpands.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.functional.AttachmentRender;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.index.ClientSlotAdapterIndex;
import com.tacz.guns.util.RenderDistance;
import group.taczexpands.client.render.paint.PaintResolver;
import group.taczexpands.client.render.paint.PaintAttachmentSelection;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** All traditional attachment draws, including first person, preview and variant/adapter layers. */
@Mixin(value = AttachmentRender.class, remap = false)
public abstract class MixinPaintAttachmentRender {
    @Unique private static final String RENDER = "Lcom/tacz/guns/client/model/BedrockAttachmentModel;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFLnet/minecraft/client/renderer/MultiBufferSource;)V";

    @WrapOperation(method = "renderSlotAdapter", at = @At(value = "INVOKE", target = RENDER))
    private static void taczexpands$adapter(BedrockAttachmentModel model, ItemStack attachment,
            ItemStack gun, PoseStack pose, ItemDisplayContext context, RenderType type,
            int light, int overlay, float alpha, MultiBufferSource buffers, Operation<Void> original,
            @Local(argsOnly = true) ClientSlotAdapterIndex index) {
        ResourceLocation base = index.getModelTexture();
        ResourceLocation texture = PaintResolver.resolveAttachment(gun, index, base, false);
        original.call(model, attachment, gun, pose, context, taczexpands$type(type, base, texture),
                light, overlay, alpha, buffers);
    }

    @WrapOperation(method = "renderVariantOverlay", at = @At(value = "INVOKE", target = RENDER))
    private static void taczexpands$variant(BedrockAttachmentModel model, ItemStack attachment,
            ItemStack gun, PoseStack pose, ItemDisplayContext context, RenderType type,
            int light, int overlay, float alpha, MultiBufferSource buffers, Operation<Void> original,
            @Local(argsOnly = true) ClientAttachmentVariantIndex index) {
        ResourceLocation base = index.getModelTexture();
        ResourceLocation texture = PaintResolver.resolveAttachment(gun, index, base, false);
        original.call(model, attachment, gun, pose, context, taczexpands$type(type, base, texture),
                light, overlay, alpha, buffers);
    }

    @WrapOperation(method = "lambda$renderAttachment$1", at = @At(value = "INVOKE", target = RENDER))
    private static void taczexpands$body(BedrockAttachmentModel model, ItemStack attachment,
            ItemStack gun, PoseStack pose, ItemDisplayContext context, RenderType type,
            int light, int overlay, float alpha, MultiBufferSource buffers, Operation<Void> original,
            @Local(argsOnly = true) ClientAttachmentIndex index,
            @Local ClientAttachmentVariantIndex variant,
            @Local Pair<BedrockAttachmentModel, ResourceLocation> lod) {
        // Mirror the native selection: a base attachment LOD wins over a replace variant.
        boolean useLod = lod != null && !context.firstPerson()
                && !RenderDistance.inRenderHighPolyModelDistance(pose);
        Object selected = PaintAttachmentSelection.body(index, variant, useLod);
        ResourceLocation base = useLod ? lod.getRight()
                : selected == variant ? variant.getModelTexture() : index.getModelTexture();
        ResourceLocation texture = PaintResolver.resolveAttachment(gun, selected, base, useLod);
        original.call(model, attachment, gun, pose, context, taczexpands$type(type, base, texture),
                light, overlay, alpha, buffers);
    }

    @Unique private static RenderType taczexpands$type(RenderType original, ResourceLocation base,
                                                       ResourceLocation texture) {
        if (texture == null || texture.equals(base)) return original;
        return original == RenderType.entityTranslucent(base)
                ? RenderType.entityTranslucent(texture) : RenderType.entityCutout(texture);
    }
}
