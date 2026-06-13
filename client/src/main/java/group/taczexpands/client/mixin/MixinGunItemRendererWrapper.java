package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.util.RenderDistance;
import group.taczexpands.client.accessor.IAccessorBedrockGunModel;
import group.taczexpands.client.accessor.IAccessorGunDisplay;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public class MixinGunItemRendererWrapper {
    @Inject(method = "lambda$renderFirstPerson$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;getRenderHand()Z"))
    private void onPostFieldSet(ItemStack stack, LocalPlayer player, float partialTick, PoseStack poseStack, ItemDisplayContext ctx, int light, GunDisplayInstance display, CallbackInfo ci) {
        if (IAccessorGunDisplay.getHideMuzzleFlash(display)) {
            MuzzleFlashRender.isSelf = false;
        }
        if (IAccessorGunDisplay.getHideShell(display)) {
            ShellRender.isSelf = false;
        }
    }

    @Inject(method = "cacheMuzzlePosition", at = @At("HEAD"))
    private static void preCacheMuzzlePosition(PoseStack poseStack, BedrockGunModel gunModel, CallbackInfo ci) {
        var hookPath = ((IAccessorBedrockGunModel) gunModel).taczexpands$getHookPath();
        if (hookPath == null) {
            hookPath = List.of();
        }

        poseStack.pushPose();
        for (BedrockPart bedrockPart : hookPath) {
            bedrockPart.translateAndRotateAndScale(poseStack);
        }
        var pos = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul(poseStack.last().pose()).mul(RenderSystem.getProjectionMatrix());
        ScopeManager.INSTANCE.getLastHookPosNDC().set(pos);
        poseStack.popPose();
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void onGetTextureLocation(ItemStack stack, CallbackInfoReturnable<ResourceLocation> cir) {
        var texture = GunExtras.INSTANCE.getOverrideTexture(stack);
        if (texture != null) {
            var location = new ResourceLocation(texture);
            if (Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) {
                cir.setReturnValue(location);
            }
        }
    }

    @Redirect(method = "lambda$renderFirstPerson$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V"))
    public void redirectRenderFirstPersonModel(BedrockGunModel instance, PoseStack poseStack, ItemStack itemStack, ItemDisplayContext itemDisplayContext, RenderType renderType, int light, int overlay) {
        var texture = GunExtras.INSTANCE.getOverrideTexture(itemStack);
        if (texture != null) {
            var location = new ResourceLocation(texture);
            if (Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) {
                instance.render(poseStack, itemStack, itemDisplayContext, RenderType.entityCutout(location), light, overlay);
                return;
            }
        }

        instance.render(poseStack, itemStack, itemDisplayContext, renderType, light, overlay);
    }

    @Redirect(method = "lambda$renderByItem$6", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V"))
    private static void redirectRenderItemModel(BedrockGunModel instance, PoseStack poseStack, ItemStack itemStack, ItemDisplayContext itemDisplayContext, RenderType renderType, int light, int overlay) {
        if (RenderDistance.inRenderHighPolyModelDistance(poseStack)) {
            var texture = GunExtras.INSTANCE.getOverrideTexture(itemStack);
            if (texture != null) {
                var location = new ResourceLocation(texture);
                if (Minecraft.getInstance().getResourceManager().getResource(location).isPresent()) {
                    instance.render(poseStack, itemStack, itemDisplayContext, RenderType.entityCutout(location), light, overlay);
                    return;
                }
            }

        }
        instance.render(poseStack, itemStack, itemDisplayContext, renderType, light, overlay);
    }

}
