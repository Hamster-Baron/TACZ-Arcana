package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.compat.ar.ARCompat;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.accessor.IAccessorRenderType;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.render.Flashlight;
import group.taczexpands.client.render.flashlight.FlashlightEmitterRenderer;
import group.taczexpands.client.render.flashlight.FlashlightLodRenderer;
import group.taczexpands.client.render.scope.ScopeLensComposition;
import group.taczexpands.client.render.scope.ScopeVisibility;
import group.taczexpands.client.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
public abstract class MixinBedrockAttachmentModel extends BedrockAnimatedModel {

    @Shadow
    protected abstract void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nonnull List<BedrockPart> path, MultiBufferSource bufferSource);

    @Shadow
    protected abstract void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nonnull List<BedrockPart> path, float alpha, MultiBufferSource bufferSource);

    @Shadow
    private boolean isScope;

    @Shadow
    @Nullable
    private ItemStack currentGunItem;

    @Shadow
    @Nullable
    private ItemStack attachmentItem;
    @Shadow
    private boolean isSight;

    @Shadow
    protected abstract void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource);

    @Shadow
    protected abstract void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource);

    @Shadow
    protected abstract void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource);

    @Shadow
    @Nullable
    protected List<BedrockPart> scopeBodyPath;
    @Shadow
    @Nullable
    protected List<BedrockPart> ocularRingPath;
    @Shadow
    @Nullable
    protected List<List<BedrockPart>> laserBeamPaths;

    @Shadow
    protected abstract void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean isScope, MultiBufferSource bufferSource);

    @Shadow
    protected abstract void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean selective, MultiBufferSource bufferSource);

    @Unique
    private @Nullable List<BedrockPart> taczexpands$flashlightPaths;

    public MixinBedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void postInit(BedrockModelPOJO pojo, BedrockVersion version, CallbackInfo ci) {
        taczexpands$flashlightPaths = getPath(modelMap.get("flashlight"));
    }

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    public void rewriteRender(ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, float alpha, MultiBufferSource bufferSource, CallbackInfo ci) {
        ci.cancel();


        if (this.isScope)
            ScopeManager.INSTANCE.setLastScopeModel((BedrockAttachmentModel) (Object) this);

        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) {
            if (isScope && isSight) {
                renderBoth(matrixStack, transformType, renderType, light, overlay, bufferSource);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay, bufferSource);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay, bufferSource);
            }
        } else {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, alpha, bufferSource);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, alpha, bufferSource);
            }
        }
        if (!isScope && !isSight) {
            if (taczexpands$flashlightPaths != null) {
                Flashlight.INSTANCE.renderHand(currentGunItem, attachmentItem, matrixStack, transformType, taczexpands$flashlightPaths);
                FlashlightEmitterRenderer.render(currentGunItem, matrixStack, transformType, taczexpands$flashlightPaths, bufferSource, alpha);
                FlashlightLodRenderer.mark(currentGunItem, attachmentItem);
            }
            if (laserBeamPaths != null) {
                for (var entry : laserBeamPaths) {
                    if (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()) {
                        BeamRendererOverride.renderLaserBeam(currentGunItem, attachmentItem, matrixStack, transformType, entry, bufferSource);
                    } else {
                        BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry, bufferSource);
                    }
                }
            }
        }

        if (!transformType.firstPerson() || (!isSight && !isScope)) {
            super.render(matrixStack, transformType, renderType, light, overlay, 1.0f, 1.0f, 1.0f, alpha, bufferSource);
        }
        if (isScope || isSight) {
            if (taczexpands$flashlightPaths != null) {
                Flashlight.INSTANCE.renderHand(currentGunItem, attachmentItem, matrixStack, transformType, taczexpands$flashlightPaths);
                FlashlightEmitterRenderer.render(currentGunItem, matrixStack, transformType, taczexpands$flashlightPaths, bufferSource, alpha);
                FlashlightLodRenderer.mark(currentGunItem, attachmentItem);
            }
            if (laserBeamPaths != null) {
                for (var entry : laserBeamPaths) {
                    if (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()) {
                        BeamRendererOverride.renderLaserBeam(currentGunItem, attachmentItem, matrixStack, transformType, entry, bufferSource);
                    } else {
                        BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry, bufferSource);
                    }
                }
            }
        }


    }





    @Unique
    private static final String TACZEXPANDS_LENS_CAPTURE =
            "Lcom/tacz/guns/client/renderer/scope/ScopeLensRenderer;capture";
    @Unique
    private static final String TACZEXPANDS_RENDER_PART =
            "Lcom/tacz/guns/client/model/BedrockAttachmentModel;renderTempPart(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IILjava/util/List;Lnet/minecraft/client/renderer/MultiBufferSource;)V";

    @Inject(method = "renderOcularAndDivision", at = @At(value = "INVOKE", target = TACZEXPANDS_LENS_CAPTURE))
    private void taczexpands$prepareScopeLens(PoseStack matrixStack, ItemDisplayContext transformType,
            RenderType renderType, int light, int overlay, boolean selective,
            MultiBufferSource bufferSource, CallbackInfo ci) {
        if (ScopeVisibility.gate(TACZExpandsClient.Companion.isAdvancedRendering())) {
            var mainTarget = TACZExpandsClient.Companion.getMainTarget();
            if (mainTarget != null) {
                int viewport[] = new int[4];
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
                RenderSystem.backupProjectionMatrix();
                var prevDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
                RenderSystem.disableDepthTest();
                var prevBlend = GL11.glIsEnabled(GL11.GL_BLEND);
                var prevShader = RenderSystem.getShader();
                RenderHelper.INSTANCE.blitScope(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), true, mainTarget);
                RenderSystem.setShader(() -> prevShader);
                if (prevBlend) {
                    RenderSystem.enableBlend();
                } else {
                    RenderSystem.disableBlend();
                }
                if (prevDepthTest) {
                    RenderSystem.enableDepthTest();
                } else {
                    RenderSystem.disableDepthTest();
                }
                GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

                RenderSystem.restoreProjectionMatrix();
            }
        }
    }

    @WrapOperation(method = "renderOcularAndDivision", at = @At(value = "INVOKE",
            target = TACZEXPANDS_RENDER_PART, ordinal = 2))
    private void taczexpands$fallbackDivision(BedrockAttachmentModel model, PoseStack pose,
            ItemDisplayContext context, RenderType type, int light, int overlay,
            List<BedrockPart> path, MultiBufferSource buffers, Operation<Void> original) {
        taczexpands$renderDivision(model, pose, context, type, light, overlay, path, buffers, original, false);
    }

    @WrapOperation(method = "captureLensDivision", at = @At(value = "INVOKE",
            target = TACZEXPANDS_RENDER_PART))
    private void taczexpands$capturedDivision(BedrockAttachmentModel model, PoseStack pose,
            ItemDisplayContext context, RenderType type, int light, int overlay,
            List<BedrockPart> path, MultiBufferSource buffers, Operation<Void> original) {
        taczexpands$renderDivision(model, pose, context, type, light, overlay, path, buffers, original, true);
    }

    @Unique
    private void taczexpands$renderDivision(BedrockAttachmentModel model, PoseStack pose,
            ItemDisplayContext context, RenderType type, int light, int overlay,
            List<BedrockPart> path, MultiBufferSource buffers, Operation<Void> original, boolean captured) {
        if (type instanceof IAccessorRenderType accessor && TACZExpandsClient.Companion.isAdvancedRendering()
                && !(CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse())) {
            var texture = accessor.taczexpands$getTexture();
            if (texture != null) {
                type = TACZExpandsClient.Companion.getCURSOR().apply(texture);
                light = LightTexture.pack(15, 15);
            }
        }
        RenderType previous = ScopeLensComposition.divisionType(captured ? type : null);
        try {
            original.call(model, pose, context, type, light, overlay, path, buffers);
        } finally {
            ScopeLensComposition.divisionType(previous);
        }
    }

    @Inject(method = "renderBothAccelerated(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preRenderBothAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource, CallbackInfo ci) {
        ci.cancel();

        var poseStack = new PoseStack();
        poseStack.last().pose().set(matrixStack.last().pose());
        poseStack.last().normal().set(matrixStack.last().normal());

        ARCompat.setRenderLayer(-943);
        ARCompat.setRenderBeforeFunction(() -> {
            com.tacz.guns.util.RenderHelper.enableItemEntityStencilTest();
            RenderSystem.clearStencil(0);
            RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        });

        ARCompat.setRenderAfterFunction(com.tacz.guns.util.RenderHelper::disableItemEntityStencilTest);

        if (ocularRingPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 1);
        ARCompat.setRenderBeforeFunction(() -> {
            com.tacz.guns.util.RenderHelper.enableItemEntityStencilTest();

            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, true, bufferSource);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        });

        ARCompat.setRenderAfterFunction(() -> {
            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, false, bufferSource);
            renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, true, bufferSource);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            com.tacz.guns.util.RenderHelper.disableItemEntityStencilTest();

            super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
        });

        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 2);
        ARCompat.setRenderAfterFunction(() -> {
            super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
        });



        ARCompat.resetRenderLayer();
        ARCompat.resetRenderAfterFunction();
    }

    @Inject(method = "renderScopeAccelerated(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IILnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preRenderScopeAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, MultiBufferSource bufferSource, CallbackInfo ci) {
        ci.cancel();
        var poseStack = new PoseStack();
        poseStack.last().pose().set(matrixStack.last().pose());
        poseStack.last().normal().set(matrixStack.last().normal());

        ARCompat.setRenderLayer(-943);
        ARCompat.setRenderBeforeFunction(() -> {
            com.tacz.guns.util.RenderHelper.enableItemEntityStencilTest();
            RenderSystem.clearStencil(0);
            RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        });

        ARCompat.setRenderAfterFunction(com.tacz.guns.util.RenderHelper::disableItemEntityStencilTest);

        if (ocularRingPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath, bufferSource);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 1);
        ARCompat.setRenderBeforeFunction(() -> {
            com.tacz.guns.util.RenderHelper.enableItemEntityStencilTest();

            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, false, bufferSource);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        });

        ARCompat.setRenderAfterFunction(() -> {
            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, false, bufferSource);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            com.tacz.guns.util.RenderHelper.disableItemEntityStencilTest();

            super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
        });

        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath, bufferSource);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 2);
        ARCompat.setRenderAfterFunction(() -> {
            super.render(matrixStack, transformType, renderType, light, overlay, bufferSource);
        });





        ARCompat.resetRenderLayer();
        ARCompat.resetRenderAfterFunction();
    }
}
