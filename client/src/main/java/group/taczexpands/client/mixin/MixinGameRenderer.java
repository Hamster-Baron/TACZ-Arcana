package group.taczexpands.client.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;

import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorGameRenderer;
import group.taczexpands.client.accessor.IAccessorLightTexture;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.compat.iris.ScopeShaderPipelines;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.render.Flashlight;
import group.taczexpands.client.render.scope.ScopeVisibility;
import group.taczexpands.client.render.scope.ScopeLensComposition;
import group.taczexpands.client.render.surface.SurfaceEngine;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import group.taczexpands.client.util.RenderHelper;
import group.taczexpands.client.util.TrueDarknessHelper;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = GameRenderer.class,
        priority = 1050)
public abstract class MixinGameRenderer implements IAccessorGameRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private LightTexture lightTexture;
    @Shadow private boolean renderBlockOutline;
    @Shadow private boolean renderHand;
    @Unique private boolean taczexpands$entityOutlineEnabled = true;

    @Shadow
    protected abstract double getFov(Camera camera, float partialTick, boolean useFovSetting);

    @Shadow
    public abstract void renderLevel(float partialTick, long finishTimeNano, PoseStack poseStack);

    @Shadow
    public abstract void setRenderHand(boolean renderHand);

    @Unique private boolean taczexpands$deferredHand;
    @Unique private boolean taczexpands$handShaderHook;

    @Inject(
            method = "renderLevel",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V",
                remap = false))
    public void afterRenderLevel(
            float partialTicks, long nano, PoseStack modelViewStack, CallbackInfo ci) {
        Flashlight.INSTANCE.onRenderLevel(partialTicks, modelViewStack);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"))
    public void onPreRenderItemInHand(
            float partialTicks, long nanos, PoseStack pose, CallbackInfo ci) {
        taczexpands$deferredHand = CompatHelper.INSTANCE.hasIris()
                && minecraft.player != null
                && IGun.getIGunOrNull(minecraft.player.getMainHandItem()) != null
                && (ClientConfig.INSTANCE.getEnableRenderFunc().get()
                        || ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()
                                && BeamRendererOverride.lastHasLaser);
        if (taczexpands$deferredHand)
            taczexpands$handShaderHook = IrisCompat.INSTANCE.setHookUsingShaderPack();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V",
                shift = At.Shift.AFTER))
    public void onPostRenderItemInHand(
            float partialTicks, long nanos, PoseStack pose, CallbackInfo ci) {
        if (taczexpands$deferredHand) {
            IrisCompat.INSTANCE.restoreHookUsingShaderPack(taczexpands$handShaderHook);
            taczexpands$deferredHand = false;
        }
    }

    // Accelerated hand layers can capture another lens after TacZ's in-world composition.
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER))
    private void taczexpands$finishScopeLenses(float partialTicks, long nanos,
            boolean renderLevel, CallbackInfo ci) {
        ScopeLensComposition.finish();
    }

    @Inject(
            method = "render",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    public void beforeRenderLevel(
            float partialTicks, long nanos, boolean renderLevel, CallbackInfo ci) {
        SurfaceEngine.render(partialTicks, nanos);
        if (!ScopeVisibility.gate((boolean) TACZExpandsClient.Companion.isAdvancedRendering())) {
            this.taczexpands$entityOutlineEnabled = true;
            return;
        }
        this.taczexpands$entityOutlineEnabled = true;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null
                && IGunOperator.fromLivingEntity((LivingEntity) player).getSynAimingProgress()
                        > 0.0f) {
            this.taczexpands$entityOutlineEnabled = false;
        }
        RenderTarget main = this.minecraft.getMainRenderTarget();
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();
        RenderTarget target = TACZExpandsClient.Companion.getMainTarget();
        if (target == null) {
            target = new MainTarget(width, height);
            target.enableStencil();
            target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            target.clear(Minecraft.ON_OSX);
            TACZExpandsClient.Companion.setMainTarget(target);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        RenderHelper.INSTANCE.fitStencil(target, main);
        TACZExpandsClient.Companion.setPatchMainTargetDraw(true);
        boolean prevRenderHand = this.renderHand;
        this.setRenderHand(false);
        boolean updateLight =
                TACZExpandsClient.Companion.shouldUseThermalImaging()
                        || TACZExpandsClient.Companion.shouldUseNightVision();
        boolean updateTrueDarkness =
                !TACZExpandsClient.Companion.shouldUseThermalImaging()
                        && TACZExpandsClient.Companion.shouldUseNightVision();
        if (updateLight) {
            ((IAccessorLightTexture) this.lightTexture).taczexpands$setUpdateLightTexture();
            if (updateTrueDarkness) {
                TrueDarknessHelper.INSTANCE.saveStateAndTurnOff();
            }
            this.lightTexture.updateLightTexture(partialTicks);
        }
        boolean lightweight = CompatHelper.INSTANCE.hasIris()
                && ScopeShaderPipelines.isLightweightScope();
        boolean previousLayout = lightweight && IrisCompat.INSTANCE.beginLightweightLayout();
        boolean previousShaderHook = lightweight && IrisCompat.INSTANCE.setHookUsingShaderPack();
        try {
            this.renderLevel(partialTicks, nanos, new PoseStack());
            this.minecraft.levelRenderer.doEntityOutline();
        } finally {
            if (lightweight) {
                IrisCompat.INSTANCE.restoreHookUsingShaderPack(previousShaderHook);
                IrisCompat.INSTANCE.endLightweightLayout(previousLayout);
            }
            this.setRenderHand(prevRenderHand);
            TACZExpandsClient.Companion.setPatchMainTargetDraw(false);
        }
        RenderHelper.INSTANCE.copy(main, target);
        main.bindWrite(true);
        main.bindRead();
        if (updateLight) {
            ((IAccessorLightTexture) this.lightTexture).taczexpands$setUpdateLightTexture();
            if (updateTrueDarkness) {
                TrueDarknessHelper.INSTANCE.restoreState();
            }
            this.lightTexture.updateLightTexture(partialTicks);
            ((IAccessorLightTexture) this.lightTexture).taczexpands$setUpdateLightTexture();
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
    public void taczexpands$redirectDoEntityOutline(LevelRenderer levelRenderer) {
        if (this.taczexpands$entityOutlineEnabled) {
            levelRenderer.doEntityOutline();
        }
    }

    public boolean taczexpands$getRenderBlockOutline() {
        return this.renderBlockOutline;
    }

    public boolean taczexpands$getRenderHand() {
        return this.renderHand;
    }

    public double taczexpands$getFOV(Camera camera, float partialTicks, boolean b) {
        return this.getFov(camera, partialTicks, b);
    }

    @Inject(
            method = "getRendertypeEntityCutoutNoCullShader",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookGetRendertypeEntityCutoutNoCullShader(
            CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(TACZExpandsClient.Companion.getThermalImagingShaderEntity());
        }
    }

    @Inject(
            method = "getRendertypeArmorCutoutNoCullShader",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookGetRendertypeArmorCutoutNoCullShader(
            CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(TACZExpandsClient.Companion.getThermalImagingShaderArmor());
        }
    }

    @Inject(
            method = "getRendertypeEntityTranslucentShader",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookGetRendertypeEntityTranslucentShader(
            CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(
                    TACZExpandsClient.Companion.getThermalImagingShaderEntityTranslucent());
        }
    }

    @Inject(
            method = "getNightVisionScale",
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void hookGetNightVisionScale(
            LivingEntity entity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null
                && entity == player
                && !TACZExpandsClient.Companion.shouldUseThermalImaging()
                && TACZExpandsClient.Companion.shouldUseNightVision()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getMainCamera", at = @At("HEAD"), cancellable = true)
    private void taczexpands$surfaceCamera(CallbackInfoReturnable<Camera> cir) {
        if (SurfaceRenderer.isCameraView()) {
            cir.setReturnValue(SurfaceRenderer.camera());
        }
    }

    @Inject(method = "getRenderDistance", at = @At("HEAD"), cancellable = true)
    private void taczexpands$surfaceDistance(CallbackInfoReturnable<Float> cir) {
        if (SurfaceRenderer.cameraPass())
            cir.setReturnValue(SurfaceRenderer.cameraDistance());
    }
}
