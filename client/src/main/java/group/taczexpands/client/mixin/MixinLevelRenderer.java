package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorLevelRenderer;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.FlashlightData;
import group.taczexpands.client.render.flashlight.FlashlightDepthState;
import group.taczexpands.client.render.flashlight.FlashlightEmbeddium;
import group.taczexpands.client.compat.RendererPresence;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import group.taczexpands.client.util.RenderHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer implements IAccessorLevelRenderer {
    /** Entity outlines use the window-sized target; a camera pass owns a smaller viewport. */
    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At("RETURN"))
    private boolean taczexpands$surfaceEntityOutlines(boolean original) {
        return original && !SurfaceRenderer.isWorldPass() && !Depth.INSTANCE.getDepthRendering();
    }

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    @Final
    private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Nullable
    private Frustum capturedFrustum;

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    private boolean captureFrustum;

    @Shadow
    protected abstract void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator);

    @Shadow
    protected abstract void compileChunks(Camera pCamera);

    @Shadow
    @Final
    private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private Set<BlockEntity> globalBlockEntities;

    @Shadow
    protected abstract void renderEntity(Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource);

    @Shadow
    public abstract boolean isChunkCompiled(BlockPos pPos);

    @Shadow
    private double xTransparentOld;

    @Shadow
    private double yTransparentOld;

    @Shadow
    private double zTransparentOld;

    @Shadow
    protected abstract void renderChunkLayer(RenderType pRenderType, PoseStack pPoseStack, double pCamX, double pCamY, double pCamZ, Matrix4f pProjectionMatrix);

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void onRenderLevel(PoseStack poseStack, float partialTick, long finishTimeNano, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (TACZExpandsClient.Companion.getPatchMainTargetDraw()) {
            ScopeManager.INSTANCE.setLastProjectionMatrix(new Matrix4f(projectionMatrix));
            ScopeManager.INSTANCE.setLastViewMatrix(new Matrix4f(poseStack.last().pose()));
        }

        BeamRendererOverride.lastProjectionMatrix = new Matrix4f(projectionMatrix);
        BeamRendererOverride.lastViewMatrix = new Matrix4f(poseStack.last().pose());
    }

    // Post effects share cached RGB modes; restore their alpha factors after entity draws.
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/PostChain;process(F)V", ordinal = 0))
    private void taczexpands$outlineBlend(CallbackInfo ci) {
        FlashlightDepthState.prepareOutlineBlend();
    }

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    public void hookDoEntityOutline(CallbackInfo ci) {
        ci.cancel();
        RenderHelper.INSTANCE.doEntityOutline((LevelRenderer) (Object) this);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"))
    public void redirectSetupRender(LevelRenderer instance, Camera camera, Frustum frustum, boolean bool1, boolean bool2) {
        if (Depth.INSTANCE.getDepthRendering()) return;
        this.setupRender(camera, frustum, bool1, bool2);
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;compileChunks(Lnet/minecraft/client/Camera;)V"))
    public void redirectCompileChunks(LevelRenderer instance, Camera camera) {
        if (Depth.INSTANCE.getDepthRendering()) return;
        this.compileChunks(camera);
    }

    @Override
    public Frustum taczexpands$getCullingFrustum() {
        return cullingFrustum;
    }

    @Override
    public void taczexpands$setCullingFrustum(Frustum frustum) {
        cullingFrustum = frustum;
    }

    @Override
    public Frustum taczexpands$getCapturedFrustum() {
        return capturedFrustum;
    }

    @Override
    public void taczexpands$setCapturedFrustum(Frustum frustum) {
        capturedFrustum = frustum;
    }

    @Override
    public boolean taczexpands$getCaptureFrustum() {
        return captureFrustum;
    }

    @Override
    public void taczexpands$setCaptureFrustum(boolean capture) {
        captureFrustum = capture;
    }

    @Unique
    @Override
    public void taczexpands$renderDepth(FlashlightData flashlight, PoseStack pPoseStack, float pPartialTick, Camera pCamera, Matrix4f pProjectionMatrix) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        Frustum frustum = this.cullingFrustum;
        this.entityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.crosshairPickEntity);
        this.blockEntityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.hitResult);
        taczexpands$renderDepthLayer(RenderType.solid(), pPoseStack, vec3, pProjectionMatrix);
        taczexpands$renderDepthLayer(RenderType.cutoutMipped(), pPoseStack, vec3, pProjectionMatrix);
        taczexpands$renderDepthLayer(RenderType.cutout(), pPoseStack, vec3, pProjectionMatrix);
        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
                BlockPos blockpos = entity.blockPosition();
                if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isChunkCompiled(blockpos)) && (entity != pCamera.getEntity() || pCamera.isDetached() || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity) pCamera.getEntity()).isSleeping()) && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || (entity == minecraft.player && !minecraft.player.isSpectator()))) { 
                    MultiBufferSource multibuffersource = multibuffersource$buffersource;

                    this.renderEntity(entity, d0, d1, d2, pPartialTick, pPoseStack, multibuffersource);
                }
            }
        }

        if (RendererPresence.EMBEDDIUM_PRESENT) {
            FlashlightEmbeddium.forEachVisible(entity ->
                    taczexpands$renderDepthBlockEntity(entity, pPoseStack, pPartialTick, vec3, frustum, multibuffersource$buffersource));
        } else {
            for (var info : this.renderChunksInFrustum) {
                for (var blockEntity : info.chunk.getCompiledChunk().getRenderableBlockEntities()) {
                    taczexpands$renderDepthBlockEntity(blockEntity, pPoseStack, pPartialTick, vec3, frustum, multibuffersource$buffersource);
                }
            }
            synchronized (this.globalBlockEntities) {
                for (var blockEntity : this.globalBlockEntities) {
                    taczexpands$renderDepthBlockEntity(blockEntity, pPoseStack, pPartialTick, vec3, frustum, multibuffersource$buffersource);
                }
            }
        }
        multibuffersource$buffersource.endBatch();

        double previousX = this.xTransparentOld;
        double previousY = this.yTransparentOld;
        double previousZ = this.zTransparentOld;
        try {
            // Depth ignores blend ordering; retain the main view's translucent mesh order.
            this.xTransparentOld = d0;
            this.yTransparentOld = d1;
            this.zTransparentOld = d2;
            taczexpands$renderDepthLayer(RenderType.translucent(), pPoseStack, vec3, pProjectionMatrix);
            taczexpands$renderDepthLayer(RenderType.tripwire(), pPoseStack, vec3, pProjectionMatrix);
        } finally {
            this.xTransparentOld = previousX;
            this.yTransparentOld = previousY;
            this.zTransparentOld = previousZ;
        }
    }

    @Unique
    private void taczexpands$renderDepthLayer(RenderType layer, PoseStack pose, Vec3 camera, Matrix4f projection) {
        if (RendererPresence.EMBEDDIUM_PRESENT) {
            FlashlightEmbeddium.drawLayer(layer, pose, camera);
        } else {
            this.renderChunkLayer(layer, pose, camera.x, camera.y, camera.z, projection);
        }
    }

    @Unique
    private void taczexpands$renderDepthBlockEntity(BlockEntity entity, PoseStack pose, float partialTick,
            Vec3 camera, Frustum frustum, MultiBufferSource.BufferSource buffers) {
        if (!frustum.isVisible(entity.getRenderBoundingBox())) return;
        pose.pushPose();
        try {
            var position = entity.getBlockPos();
            pose.translate(position.getX() - camera.x, position.getY() - camera.y, position.getZ() - camera.z);
            this.blockEntityRenderDispatcher.render(entity, partialTick, pose, buffers);
        } finally {
            pose.popPose();
        }
    }

}
