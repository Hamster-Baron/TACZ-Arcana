package group.taczexpands.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorLevelRenderer;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.FlashlightData;
import group.taczexpands.client.util.RenderHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
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
    @Final
    private Vector3d frustumPos;

    @Shadow
    private Frustum cullingFrustum;

    @Shadow
    private boolean captureFrustum;

    @Shadow
    protected abstract void captureFrustum(Matrix4f pViewMatrix, Matrix4f pProjectionMatrix, double pCamX, double pCamY, double pCamZ, Frustum pCapturedFrustrum);

    @Shadow
    protected abstract void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator);

    @Shadow
    protected abstract void compileChunks(Camera pCamera);


    @Shadow
    private int renderedEntities;

    @Shadow
    private int culledEntities;

    @Shadow
    protected abstract void checkPoseStack(PoseStack pPoseStack);

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
    @Nullable
    private RenderTarget itemEntityTarget;

    @Shadow
    @Nullable
    private RenderTarget weatherTarget;

    @Shadow
    public abstract void renderLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix);

    @Shadow
    private double xTransparentOld;

    @Shadow
    private double yTransparentOld;

    @Shadow
    private double zTransparentOld;

    @Shadow
    @Nullable
    private ChunkRenderDispatcher chunkRenderDispatcher;

    @Shadow
    private int ticks;

    @Shadow
    public abstract Frustum getFrustum();

    @Shadow
    protected abstract void renderChunkLayer(RenderType pRenderType, PoseStack pPoseStack, double pCamX, double pCamY, double pCamZ, Matrix4f pProjectionMatrix);

    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void onRenderLevel(PoseStack p_109600_, float p_109601_, long p_109602_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_254120_, CallbackInfo ci) {
        if (TACZExpandsClient.Companion.getPatchMainTargetDraw()) {
            ScopeManager.INSTANCE.setLastProjectionMatrix(new Matrix4f(p_254120_));
            ScopeManager.INSTANCE.setLastViewMatrix(new Matrix4f(p_109600_.last().pose()));
        }

        BeamRendererOverride.lastProjectionMatrix = new Matrix4f(p_254120_);
        BeamRendererOverride.lastViewMatrix = new Matrix4f(p_109600_.last().pose());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    public void hookPreEntityRender(PoseStack p_109600_, float p_109601_, long p_109602_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_254120_, CallbackInfo ci) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            if (CompatHelper.INSTANCE.hasIris()) {
                IrisCompat.INSTANCE.saveAndDisableExtendedVertexFormat();
            }
        }


    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderStage(Lnet/minecraftforge/client/event/RenderLevelStageEvent$Stage;Lnet/minecraft/client/renderer/LevelRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;ILnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;)V", ordinal = 1, remap = false))
    public void hookPostEntityRender(PoseStack p_109600_, float p_109601_, long p_109602_, boolean p_109603_, Camera p_109604_, GameRenderer p_109605_, LightTexture p_109606_, Matrix4f p_254120_, CallbackInfo ci) {
        if (TACZExpandsClient.Companion.shouldUseThermalImaging()) {
            if (CompatHelper.INSTANCE.hasIris()) {
                IrisCompat.INSTANCE.restoreExtendedVertexFormat();
            }
        }
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

    @Unique
    @Override
    public void taczexpands$renderDepth(FlashlightData flashlight, PoseStack pPoseStack, float pPartialTick, Camera pCamera, Matrix4f pProjectionMatrix) {
        ProfilerFiller profilerfiller = this.level.getProfiler();
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        Matrix4f matrix4f = pPoseStack.last().pose();
        profilerfiller.popPush("culling");
        boolean flag = this.capturedFrustum != null;
        Frustum frustum;
        if (flag) {
            frustum = this.capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
        } else {
            frustum = this.cullingFrustum;
        }

        this.minecraft.getProfiler().popPush("captureFrustum");
        if (this.captureFrustum) {
            this.captureFrustum(matrix4f, pProjectionMatrix, vec3.x, vec3.y, vec3.z, flag ? new Frustum(matrix4f, pProjectionMatrix) : frustum);
            this.captureFrustum = false;
        }

        profilerfiller.popPush("clear");
        FogRenderer.setupNoFog();
        RenderSystem.clear(16640, Minecraft.ON_OSX);

        profilerfiller.popPush("terrain_setup");
        profilerfiller.popPush("compilechunks");
        profilerfiller.popPush("terrain");
        this.renderChunkLayer(RenderType.solid(), pPoseStack, d0, d1, d2, pProjectionMatrix);
        this.renderChunkLayer(RenderType.cutoutMipped(), pPoseStack, d0, d1, d2, pProjectionMatrix);
        this.renderChunkLayer(RenderType.cutout(), pPoseStack, d0, d1, d2, pProjectionMatrix);
        profilerfiller.pop();
        profilerfiller.popPush("entities");
        this.renderedEntities = 0;
        this.culledEntities = 0;

        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
                BlockPos blockpos = entity.blockPosition();
                if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isChunkCompiled(blockpos)) && (entity != pCamera.getEntity() || pCamera.isDetached() || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity) pCamera.getEntity()).isSleeping()) && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || (entity == minecraft.player && !minecraft.player.isSpectator()))) { 
                    ++this.renderedEntities;

                    MultiBufferSource multibuffersource = multibuffersource$buffersource;

                    this.renderEntity(entity, d0, d1, d2, pPartialTick, pPoseStack, multibuffersource);
                }
            }
        }

        multibuffersource$buffersource.endLastBatch();
        multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
    }




}
