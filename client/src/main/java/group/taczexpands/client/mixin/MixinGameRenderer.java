package group.taczexpands.client.mixin;

import ca.weblite.objc.Client;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorGameRenderer;
import group.taczexpands.client.accessor.IAccessorLightTexture;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.render.Flashlight;
import group.taczexpands.client.util.RenderHelper;
import group.taczexpands.client.util.TrueDarknessHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
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

@Mixin(value = GameRenderer.class, priority = 1050)
public abstract class MixinGameRenderer implements IAccessorGameRenderer {
    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    protected abstract double getFov(Camera p_109142_, float p_109143_, boolean p_109144_);

    @Shadow
    public abstract Matrix4f getProjectionMatrix(double p_254507_);

    @Shadow
    public abstract void resetProjectionMatrix(Matrix4f p_253668_);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LightTexture lightTexture;

    @Shadow
    private boolean renderBlockOutline;
    @Shadow
    private boolean renderHand;

    @Shadow
    public abstract void renderLevel(float p_109090_, long p_109091_, PoseStack p_109092_);

    @Shadow
    public abstract void setRenderHand(boolean p_172737_);

    @Shadow
    public abstract void setRenderBlockOutline(boolean p_172776_);

    @Unique
    private int textureBuffer;

    @Unique
    private int fbo;

    private PostChain thermalImagingEffect = null;

    @Unique
    private boolean taczexpands$entityOutlineEnabled = true;

    @Unique
    public void taczexpands$renderScopeLevel(float partialTicks, long tick, PoseStack stack) {

    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"))
    public void beforeRenderLevel(float partialTicks, long tick, PoseStack stack, CallbackInfo ci) {
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V", remap = false))
    public void afterRenderLevel(float partialTicks, long nano, PoseStack modelViewStack, CallbackInfo ci) {
        Flashlight.INSTANCE.onRenderLevel(partialTicks, modelViewStack);
    }


    @Inject(method = "shutdownEffect", at = @At("TAIL"))
    public void afterShutdownEffect(CallbackInfo ci) {
        if (thermalImagingEffect != null) {
            thermalImagingEffect.close();
            thermalImagingEffect = null;
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"))
    public void onPreRenderItemInHand(float p_109090_, long p_109091_, PoseStack p_109092_, CallbackInfo ci) {
        if (CompatHelper.INSTANCE.hasIris() && (ClientConfig.INSTANCE.getEnableRenderFunc().get() || (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get() && BeamRendererOverride.lastHasLaser))) {
            IrisCompat.INSTANCE.setHookUsingShaderPack();
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V", shift = At.Shift.AFTER))
    public void onPostRenderItemInHand(float p_109090_, long p_109091_, PoseStack p_109092_, CallbackInfo ci) {
        if (CompatHelper.INSTANCE.hasIris() && (ClientConfig.INSTANCE.getEnableRenderFunc().get() || (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get() && BeamRendererOverride.lastHasLaser))) {
            IrisCompat.INSTANCE.unSetHookUsingShaderPack();
        }
    }


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    public void beforeRenderLevel(float p_109094_, long p_109095_, boolean p_109096_, CallbackInfo ci) {
        if (!TACZExpandsClient.Companion.isAdvancedRendering()) {
            taczexpands$entityOutlineEnabled = true;
            return;
        }

        taczexpands$entityOutlineEnabled = true;

        var player = Minecraft.getInstance().player;
        if (player != null && IGunOperator.fromLivingEntity(player).getSynAimingProgress() > 0.0f) {
            taczexpands$entityOutlineEnabled = false;
        }

        var main = minecraft.getMainRenderTarget();
        int width = Minecraft.getInstance().getWindow().getWidth();
        int height = Minecraft.getInstance().getWindow().getHeight();

        var target = TACZExpandsClient.Companion.getMainTarget();
        if (target == null) {
            target = new MainTarget(width, height);
            target.enableStencil();
            target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            target.clear(Minecraft.ON_OSX);
            TACZExpandsClient.Companion.setMainTarget(target);
        } else {
            if (target.width != width || target.height != height) {
                target.resize(width, height, Minecraft.ON_OSX);
            }

        }
        RenderHelper.INSTANCE.fitStencil(target, main);
        TACZExpandsClient.Companion.setPatchMainTargetDraw(true);

        var prevRenderHand = renderHand;
        var prevRenderBlockOutline = renderBlockOutline;
        setRenderHand(false);
        var updateLight = TACZExpandsClient.Companion.shouldUseThermalImaging() || TACZExpandsClient.Companion.shouldUseNightVision();
        var updateTrueDarkness = !TACZExpandsClient.Companion.shouldUseThermalImaging() && TACZExpandsClient.Companion.shouldUseNightVision();
        if (updateLight) {
            ((IAccessorLightTexture) lightTexture).taczexpands$setUpdateLightTexture();
            if (updateTrueDarkness) {
                try {
                    TrueDarknessHelper.INSTANCE.saveStateAndTurnOff();
                } catch (Exception e) {
                }
            }
            lightTexture.updateLightTexture(p_109094_);
        }

        this.renderLevel(p_109094_, p_109095_, new PoseStack());
        this.minecraft.levelRenderer.doEntityOutline();

        setRenderHand(prevRenderHand);
        setRenderBlockOutline(prevRenderBlockOutline);

        TACZExpandsClient.Companion.setPatchMainTargetDraw(false);

        RenderHelper.INSTANCE.copy(main, target);
        main.bindWrite(true);
        main.bindRead();
        if (updateLight) {
            ((IAccessorLightTexture) lightTexture).taczexpands$setUpdateLightTexture();
            if (updateTrueDarkness) {
                try {
                    TrueDarknessHelper.INSTANCE.restoreState();
                } catch (Exception e) {
                }
            }
            lightTexture.updateLightTexture(p_109094_);
            ((IAccessorLightTexture) lightTexture).taczexpands$setUpdateLightTexture();
        }



    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"))
    public void taczexpands$redirectDoEntityOutline(LevelRenderer levelRenderer) {
        if (taczexpands$entityOutlineEnabled) {
            levelRenderer.doEntityOutline();
        }
    }

    @Override
    public int taczexpands$getBuffer() {
        return textureBuffer;
    }

    @Override
    public int taczexpands$getFBO() {
        return fbo;
    }

    @Override
    public boolean taczexpands$getRenderBlockOutline() {
        return renderBlockOutline;
    }

    @Override
    public boolean taczexpands$getRenderHand() {
        return renderHand;
    }

    @Override
    public double taczexpands$getFOV(Camera camera, float partialTicks, boolean b) {
        return getFov(camera, partialTicks, b);
    }


    @Inject(method = "getRendertypeEntityCutoutNoCullShader", at = @At("HEAD"), cancellable = true)
    private static void hookGetRendertypeEntityCutoutNoCullShader(CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(TACZExpandsClient.Companion.getThermalImagingShaderEntity());
        }
    }

    @Inject(method = "getRendertypeArmorCutoutNoCullShader", at = @At("HEAD"), cancellable = true)
    private static void hookGetRendertypeArmorCutoutNoCullShader(CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(TACZExpandsClient.Companion.getThermalImagingShaderArmor());
        }
    }

    @Inject(method = "getRendertypeEntityTranslucentShader", at = @At("HEAD"), cancellable = true)
    private static void hookGetRendertypeEntityTranslucentShader(CallbackInfoReturnable<ShaderInstance> cir) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            cir.setReturnValue(TACZExpandsClient.Companion.getThermalImagingShaderEntityTranslucent());
        }
    }

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void hookGetNightVisionScale(LivingEntity entity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && entity == player) {
            if (!TACZExpandsClient.Companion.shouldUseThermalImaging() && TACZExpandsClient.Companion.shouldUseNightVision()) {
                cir.setReturnValue(1.0f);
            }
        }
    }



}
