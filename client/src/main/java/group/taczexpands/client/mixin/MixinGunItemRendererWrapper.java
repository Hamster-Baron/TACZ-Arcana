package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
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
import group.taczexpands.client.render.flashlight.FlashlightLodRenderer;
import group.taczexpands.client.render.paint.PaintResolver;
import group.taczexpands.client.render.surface.SurfaceBoneAnchor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public class MixinGunItemRendererWrapper {
    @Inject(method = "lambda$renderFirstPerson$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/statemachine/LuaAnimationStateMachine;update()V", shift = At.Shift.AFTER))
    private void taczexpands$captureCameraBone(ItemStack stack, LocalPlayer player, float tick, PoseStack pose,
            ItemDisplayContext context, int light, MultiBufferSource buffers, GunDisplayInstance display, CallbackInfo ci) {
        SurfaceBoneAnchor.capture(stack, player, tick, context, display.getGunModel());
    }
    @Inject(method = "lambda$renderFirstPerson$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;getRenderHand()Z"))
    private void onPostFieldSet(ItemStack stack, LocalPlayer player, float tick, PoseStack pose, ItemDisplayContext context,
                                int light, MultiBufferSource buffers, GunDisplayInstance display, CallbackInfo ci) {
        if (IAccessorGunDisplay.getHideMuzzleFlash(display)) MuzzleFlashRender.isSelf = false;
        if (IAccessorGunDisplay.getHideShell(display)) ShellRender.isSelf = false;
    }
    @Inject(method = "cacheMuzzlePosition", at = @At("HEAD"))
    private static void preCacheMuzzlePosition(PoseStack pose, BedrockGunModel model, CallbackInfo ci) {
        List<BedrockPart> path = ((IAccessorBedrockGunModel) model).taczexpands$getHookPath();
        if (path == null) path = List.of();
        pose.pushPose();
        for (BedrockPart part : path) part.translateAndRotateAndScale(pose);
        Vector4f position = new Vector4f(0, 0, 0, 1).mul(pose.last().pose()).mul(RenderSystem.getProjectionMatrix());
        ScopeManager.INSTANCE.getLastHookPosNDC().set(position);
        pose.popPose();
    }
    @Inject(method = "getTextureLocation", at = @At("RETURN"), cancellable = true)
    private void onGetTextureLocation(ItemStack stack, CallbackInfoReturnable<ResourceLocation> cir) {
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        ResourceLocation original = cir.getReturnValue();
        cir.setReturnValue(PaintResolver.resolve(stack, display, original, false));
    }
    @WrapOperation(method = "lambda$renderFirstPerson$5", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/MultiBufferSource;)V"))
    public void wrapRenderFirstPersonModel(BedrockGunModel model, PoseStack pose, ItemStack stack, ItemDisplayContext context,
                                           RenderType type, int light, int overlay, MultiBufferSource buffers, Operation<Void> original) {
        RenderType selected = taczexpands$renderType(model, stack, type, true);
        boolean previousDraw = SurfaceBoneAnchor.beginDraw(stack, context, true);
        try { original.call(model, pose, stack, context, selected, light, overlay, buffers); }
        finally { SurfaceBoneAnchor.endDraw(previousDraw); }
    }
    @WrapOperation(method = "lambda$renderByItem$6", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private static void taczexpands$wrapItemModel(BedrockGunModel model, PoseStack pose, ItemStack stack, ItemDisplayContext context,
                                                RenderType type, int light, int overlay, float red, float green, float blue, float alpha,
                                                MultiBufferSource buffers, Operation<Void> original) {
        Object flashlight = FlashlightLodRenderer.begin(model, stack, context);
        RenderType selected = taczexpands$renderType(model, stack, type, RenderDistance.inRenderHighPolyModelDistance(pose));
        boolean previousDraw = SurfaceBoneAnchor.beginDraw(stack, context, false);
        try {
            original.call(model, pose, stack, context, selected, light, overlay, red, green, blue, alpha, buffers);
            FlashlightLodRenderer.finish(flashlight, pose, buffers, alpha);
        } catch (Throwable error) {
            FlashlightLodRenderer.abort(flashlight);
            throw error;
        } finally { SurfaceBoneAnchor.endDraw(previousDraw); }
    }
    @Unique private static RenderType taczexpands$renderType(BedrockGunModel model, ItemStack stack, RenderType original, boolean legacyAllowed) {
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (display == null) return original;
        // Consult the lazy LOD model when rendering the LOD context.
        var lod = display.getGunModel() == model ? null : display.getLodModel();
        boolean useGunLod = lod != null && lod.getLeft() == model;
        ResourceLocation base = useGunLod ? lod.getRight() : display.getModelTexture();
        ResourceLocation resolved = PaintResolver.resolve(stack, display, base, useGunLod, legacyAllowed);
        if (resolved == null || resolved.equals(base)) return original;
        // The caller chose cutout or translucent for this exact invocation. Preserve it.
        return original == RenderType.entityTranslucent(base)
                ? RenderType.entityTranslucent(resolved) : RenderType.entityCutout(resolved);
    }
}
