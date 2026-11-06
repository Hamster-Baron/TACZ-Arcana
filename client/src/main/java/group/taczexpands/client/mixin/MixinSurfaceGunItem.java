package group.taczexpands.client.mixin;

import com.tacz.guns.client.renderer.item.StaticGunRenderResult;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.item.ExternalGunItemVboRenderer;

import group.taczexpands.client.render.surface.ModelTargets;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExternalGunItemVboRenderer.class, remap = false)
public class MixinSurfaceGunItem {
    @Inject(method = "tryRender", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/renderer/external/ExternalGunAppearance;resolve"),
            cancellable = true)
    private static void taczexpands$surfacedynamic(
            ItemStack stack,
            ItemDisplayContext context,
            boolean lod,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay,
            CallbackInfoReturnable<StaticGunRenderResult> ci) {
        if (ModelTargets.dynamic(stack, context))
            ci.setReturnValue(StaticGunRenderResult.UNSUPPORTED);
    }
}
