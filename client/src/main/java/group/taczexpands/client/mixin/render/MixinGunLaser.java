package group.taczexpands.client.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.renderer.external.ExternalGunEffects;
import group.taczexpands.client.compat.sbm.ExternalGunRenderBridge;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ExternalGunEffects.class, remap = false)
public abstract class MixinGunLaser {
    @WrapOperation(method = "renderThirdPerson", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/client/renderer/external/ExternalGunEffects;renderLasers(Lcom/tacz/guns/client/model/ExternalGunModel;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private static void taczexpands$gunLaser(ExternalGunModel model, ItemStack gun, PoseStack pose,
            MultiBufferSource buffers, Operation<Void> original) {
        if (ExternalGunRenderBridge.advancedBeam()) {
            ExternalGunRenderBridge.renderExternalLaser(gun, gun, model, pose, buffers);
        } else {
            original.call(model, gun, pose, buffers);
        }
    }
}
