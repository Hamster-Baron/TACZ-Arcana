package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import com.tacz.guns.client.renderer.scope.ScopeVisionMode;
import group.taczexpands.client.render.scope.ScopeLensComposition;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScopeLensRenderer.class, remap = false)
public abstract class MixinScopeLensRenderer {
    @Unique private static boolean taczexpands$screenCopied;
    @Unique private static int taczexpands$sceneTexture;

    @WrapOperation(method = "beginFrame", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/client/renderer/scope/ScopeThermalRenderer;beginFrame()V"))
    private static void taczexpands$keepIndependentMask(Operation<Void> original) {
        if (!ScopeLensComposition.preserveThermalMask()) original.call();
    }

    @WrapOperation(method = "drawLens", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/client/renderer/scope/ScopeVisionMode;ordinal()I"))
    private static int taczexpands$visionOnce(ScopeVisionMode mode, Operation<Integer> original) {
        return original.call(ScopeLensComposition.usesArcanaVision() ? ScopeVisionMode.NORMAL : mode);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void taczexpands$defer(CallbackInfo ci) {
        if (!ScopeLensComposition.isComposing()) {
            ci.cancel();
            return;
        }
        taczexpands$screenCopied = false;
        taczexpands$sceneTexture = -1;
    }

    // Reuse capture-owned depth to clip tracking geometry against the division mask.
    @Inject(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/systems/RenderSystem;colorMask(ZZZZ)V", shift = At.Shift.AFTER))
    private static void taczexpands$clearReticleDepth(CallbackInfo ci) {
        RenderSystem.depthMask(true);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V"))
    private static void taczexpands$reticleDepth(Operation<Void> original) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(ScopeLensComposition.isTrackingDraw() ? GL11.GL_LEQUAL : GL11.GL_ALWAYS);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V"), index = 0)
    private static boolean taczexpands$writeReticleDepth(boolean original) {
        return !ScopeLensComposition.isTrackingDraw();
    }

    // Borrow the first official capture texture for this composition's scene snapshot.
    @WrapOperation(method = "drawLens", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/pipeline/TextureTarget;getColorTextureId()I", remap = true))
    private static int taczexpands$sceneTexture(TextureTarget target, Operation<Integer> original) {
        if (taczexpands$sceneTexture < 0) {
            taczexpands$sceneTexture = original.call(target);
        }
        return taczexpands$sceneTexture;
    }

    @WrapOperation(method = "drawLens", at = @At(value = "INVOKE", target =
            "Lcom/mojang/blaze3d/platform/GlStateManager;_glCopyTexSubImage2D(IIIIIIII)V"))
    private static void taczexpands$copySceneOnce(int target, int level, int xOffset,
            int yOffset, int x, int y, int width, int height, Operation<Void> original) {
        if (!taczexpands$screenCopied) {
            original.call(target, level, xOffset, yOffset, x, y, width, height);
            taczexpands$screenCopied = true;
        }
    }
}
