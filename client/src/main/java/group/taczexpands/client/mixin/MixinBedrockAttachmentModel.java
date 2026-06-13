package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
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
import group.taczexpands.client.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.math3.geometry.spherical.oned.Arc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = BedrockAttachmentModel.class, remap = false)
public abstract class MixinBedrockAttachmentModel extends BedrockAnimatedModel {

    @Shadow
    protected List<List<BedrockPart>> ocularNodePaths;
    @Shadow
    protected List<List<BedrockPart>> divisionNodePaths;
    @Shadow
    protected List<Boolean> isScopeOcular;
    @Shadow
    private float scopeViewRadiusModifier;

    @Shadow
    protected abstract Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path);

    @Shadow
    protected abstract void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nonnull List<BedrockPart> path);

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
    protected abstract void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay);

    @Shadow
    protected abstract void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay);

    @Shadow
    protected abstract void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay);

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
    protected abstract void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean isScope);

    @Shadow
    protected abstract void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean selective);

    @Shadow
    protected abstract void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay);

    @Unique
    private @Nullable List<BedrockPart> taczexpands$flashlightPaths;

    public MixinBedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void postInit(BedrockModelPOJO pojo, BedrockVersion version, CallbackInfo ci) {
        taczexpands$flashlightPaths = getPath(modelMap.get("flashlight"));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void rewriteRender(ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, CallbackInfo ci) {
        ci.cancel();


        if (this.isScope)
            ScopeManager.INSTANCE.setLastScopeModel((BedrockAttachmentModel) (Object) this);

        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) {
            if (isScope && isSight) {
                renderBoth(matrixStack, transformType, renderType, light, overlay);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, light, overlay);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, light, overlay);
            }
        } else {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
        }
        if (!isScope && !isSight) {
            if (taczexpands$flashlightPaths != null) {
                Flashlight.INSTANCE.renderHand(currentGunItem, attachmentItem, matrixStack, transformType, taczexpands$flashlightPaths);
            }
            if (laserBeamPaths != null) {
                for (var entry : laserBeamPaths) {
                    if (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()) {
                        BeamRendererOverride.renderLaserBeam(currentGunItem, attachmentItem, matrixStack, transformType, entry);
                    } else {
                        BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
                    }
                }
            }
        }

        if (!transformType.firstPerson() || (!isSight && !isScope)) {
            super.render(matrixStack, transformType, renderType, light, overlay);
        }
        if (isScope || isSight) {
            if (taczexpands$flashlightPaths != null) {
                Flashlight.INSTANCE.renderHand(currentGunItem, attachmentItem, matrixStack, transformType, taczexpands$flashlightPaths);
            }
            if (laserBeamPaths != null) {
                for (var entry : laserBeamPaths) {
                    if (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()) {
                        BeamRendererOverride.renderLaserBeam(currentGunItem, attachmentItem, matrixStack, transformType, entry);
                    } else {
                        BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
                    }
                }
            }
        }


    }





    @Inject(method = "renderOcularAndDivision", at = @At("HEAD"), cancellable = true)
    private void overwriteRenderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, boolean selective, CallbackInfo ci) {
        ci.cancel();

        if (!ocularNodePaths.isEmpty()) {
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.depthMask(false);
            float rad = 80 * scopeViewRadiusModifier;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                rad *= IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getFrameTime());
            }
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                if (selective && !isScopeOcular.get(i)) {
                    continue;
                }
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                Vector3f ocularCenter = getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
                float centerX = ocularCenter.x() * 16 * 90;
                float centerY = ocularCenter.y() * 16 * 90;
                builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(centerX, centerY, -90.0D).color(255, 255, 255, 255).endVertex();
                for (int j = 0; j <= 90; j++) {
                    float angle = (float) j * ((float) Math.PI * 2F) / 90.0F;
                    float sin = Mth.sin(angle);
                    float cos = Mth.cos(angle);
                    builder.vertex(centerX + cos * rad, centerY + sin * rad, -90.0D).color(255, 255, 255, 255).endVertex();
                }
                BufferUploader.drawWithShader(builder.end());
            }
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            for (int i = 0; i < ocularNodePaths.size() && i < divisionNodePaths.size(); i++) {
                if (i > Byte.MAX_VALUE) {
                    throw new IllegalArgumentException("Index of oculus is out of range for 127");
                }
                if (selective && !isScopeOcular.get(i)) {
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                } else {
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                    int b = ~(i + 1) & 0xFF;
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF);


                    var isIrisHand = CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse() && IrisCompat.INSTANCE.isHandRendererActive();
                    if (TACZExpandsClient.Companion.isAdvancedRendering()) {
                        var mainTarget = TACZExpandsClient.Companion.getMainTarget();
                        if (mainTarget != null) {
                            RenderHelper.INSTANCE.copyStencil(Minecraft.getInstance().getMainRenderTarget(), mainTarget);
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
                    if (renderType instanceof IAccessorRenderType accessor && TACZExpandsClient.Companion.isAdvancedRendering()) {
                        var location = accessor.taczexpands$getTexture();
                        if (location != null) {
                            if (CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse()) {
                                renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                            } else {
                                renderTempPart(matrixStack, transformType, TACZExpandsClient.Companion.getCURSOR().apply(location), LightTexture.pack(15, 15), overlay, divisionNodePaths.get(i));

                            }
                        } else {
                            renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                        }
                    } else {
                        renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                    }
                }
            }
        }
    }




    @Inject(method = "renderBothAccelerated", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preRenderBothAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, CallbackInfo ci) {
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
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
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

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, true);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        });

        ARCompat.setRenderAfterFunction(() -> {
            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, false);
            renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, true);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            com.tacz.guns.util.RenderHelper.disableItemEntityStencilTest();

            super.render(matrixStack, transformType, renderType, light, overlay);
        });

        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 2);
        ARCompat.setRenderAfterFunction(() -> {
            super.render(matrixStack, transformType, renderType, light, overlay);
        });



        ARCompat.resetRenderLayer();
        ARCompat.resetRenderAfterFunction();
    }

    @Inject(method = "renderScopeAccelerated", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preRenderScopeAccelerated(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, CallbackInfo ci) {
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
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
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

            renderOcularStencil(poseStack, transformType, renderType, light, overlay, false);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF);
        });

        ARCompat.setRenderAfterFunction(() -> {
            ARCompat.disableAcceleration();

            int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            BufferUploader.invalidate();

            renderOcularAndDivision(poseStack, transformType, renderType, light, overlay, false);

            GL30.glBindVertexArray(vao);

            ARCompat.resetAcceleration();

            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            com.tacz.guns.util.RenderHelper.disableItemEntityStencilTest();

            super.render(matrixStack, transformType, renderType, light, overlay);
        });

        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }

        ARCompat.resetRenderLayer();
        ARCompat.resetRenderBeforeFunction();
        ARCompat.resetRenderAfterFunction();

        ARCompat.setRenderLayer(-943 + 2);
        ARCompat.setRenderAfterFunction(() -> {
            super.render(matrixStack, transformType, renderType, light, overlay);
        });





        ARCompat.resetRenderLayer();
        ARCompat.resetRenderAfterFunction();
    }
}