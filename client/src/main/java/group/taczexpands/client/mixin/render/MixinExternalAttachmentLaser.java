package group.taczexpands.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry.AttachmentModelVisitor;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.client.renderer.external.ExternalGunEffects;
import com.tacz.guns.client.renderer.item.GunItemTransforms;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import group.taczexpands.client.compat.sbm.ExternalGunRenderBridge;
import group.taczexpands.client.render.flashlight.FlashlightLodRenderer;
import java.util.EnumMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ExternalGunEffects.class, remap = false)
public abstract class MixinExternalAttachmentLaser {
    @WrapOperation(method = "renderThirdPerson", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/client/renderer/block/DisplayGunGeometry;forEachAttachmentModel(Lcom/tacz/guns/client/model/DisplayGunModelInstance;Lcom/mojang/blaze3d/vertex/PoseStack;ZLcom/tacz/guns/api/item/IGun;Lnet/minecraft/world/item/ItemStack;Ljava/util/EnumMap;Lcom/tacz/guns/client/renderer/block/DisplayGunGeometry$AttachmentModelVisitor;)V"))
    private static void taczexpands$attachmentLaser(DisplayGunModelInstance instance, PoseStack pose,
            boolean useLod, IGun gun, ItemStack stack, EnumMap<AttachmentType, ClientAttachmentIndex> attachments,
            AttachmentModelVisitor visitor, Operation<Void> original,
            @Local(argsOnly = true) MultiBufferSource buffers) {
        AttachmentModelVisitor selected = ExternalGunRenderBridge.advancedBeam()
                ? (model, texture, attachment, modelPose) ->
                        ExternalGunRenderBridge.renderExternalLaser(stack, attachment, model, modelPose, buffers)
                : visitor;
        original.call(instance, pose, useLod, gun, stack, attachments, selected);
    }

    @Inject(method = "renderThirdPerson", at = @At("HEAD"))
    private static void taczexpands$externalLamp(ExternalGunAppearance.Snapshot snapshot, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        var display = snapshot.display();
        var lod = display.getLodModel();
        var externalLod = display.getExternalLodModel();
        var model = lod != null && externalLod != null && externalLod.getLeft() == snapshot.model()
                ? lod.getLeft() : display.getGunModel();
        pose.pushPose();
        try {
            GunItemTransforms.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    display.getTransform().getScale(), model, pose);
            FlashlightLodRenderer.external(model, snapshot.stack(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    pose, buffers);
        } finally {
            pose.popPose();
        }
    }
}
