package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.client.accessor.IAccessorAmmoEntityDisplay;
import group.taczexpands.client.accessor.IAccessorBedrockAmmoModel;
import group.taczexpands.client.accessor.IAccessorEntityKineticBullet;
import group.taczexpands.client.mixin.accessor.IAccessorClientAmmoIndex;
import group.taczexpands.client.render.HookManager;
import group.taczexpands.common.accessor.IAccessorBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Optional;

@Mixin(value = EntityBulletRenderer.class, remap = false)
public abstract class MixinEntityBulletRenderer {
    @Shadow
    public abstract void renderTracerAmmo(EntityKineticBullet bullet, float[] tracerColor, float partialTicks, PoseStack poseStack, int packedLight);

    @Inject(method = "render(Lcom/tacz/guns/entity/EntityKineticBullet;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    public void rewriteRender(EntityKineticBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        ci.cancel();

        ResourceLocation gunId = bullet.getGunId();
        ResourceLocation gunDisplayId = bullet.getGunDisplayId();
        Optional<GunDisplayInstance> display = TimelessAPI.getGunDisplay(gunDisplayId, gunId);
        if (display.isEmpty()) {
            return;
        }
        float @Nullable [] tracerColor = bullet.getTracerColorOverride().orElse(display.get().getTracerColor());
        ResourceLocation ammoId = bullet.getAmmoId();
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(ammoIndex -> {
            BedrockAmmoModel ammoEntityModel = ammoIndex.getAmmoEntityModel();
            ResourceLocation textureLocation = ammoIndex.getAmmoEntityTextureLocation();

            if (ammoEntityModel != null && textureLocation != null) {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, bullet.yRotO, bullet.getYRot()) - 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, bullet.xRotO, bullet.getXRot())));
                poseStack.translate(0, 1.5, 0);
                poseStack.scale(-1, -1, 1);

                var ammoDisplay = ((IAccessorClientAmmoIndex) ammoIndex).taczexpands$getDisplay();
                if (ammoDisplay != null) {
                    var ammoEntityDisplay = ammoDisplay.getAmmoEntity();
                    if (ammoEntityDisplay != null && IAccessorAmmoEntityDisplay.hasAnimation(ammoEntityDisplay)) {
                        var animated = ((IAccessorEntityKineticBullet) bullet).taczexpands$getAnimatedModel(ammoEntityDisplay);
                        if (animated != null) {
                            ammoEntityModel = animated.getKey();
                            var controller = animated.getValue();
                            controller.update();
                        }
                    }
                }

                ((IAccessorBedrockAmmoModel) ammoEntityModel).taczexpands$render(bullet, poseStack, ItemDisplayContext.GROUND, RenderType.entityTranslucentCull(textureLocation), packedLight, OverlayTexture.NO_OVERLAY);

                poseStack.popPose();
            }

            if (bullet.isTracerAmmo() && bullet != Minecraft.getInstance().cameraEntity) {
                float[] actualTracerColor = Objects.requireNonNullElse(tracerColor, ammoIndex.getTracerColor());
                renderTracerAmmo(bullet, actualTracerColor, partialTicks, poseStack, packedLight);
            }

            var owner = bullet.getOwner();
            if (owner instanceof LivingEntity ownerLivingEntity) {
                if (((IAccessorBullet) bullet).taczexpands$isHook() && ammoEntityModel != null) {
                    HookManager.INSTANCE.renderLeash(bullet, ((IAccessorBedrockAmmoModel) ammoEntityModel).taczexpands$getHookPos(), partialTicks, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), ownerLivingEntity);
                }
            }
        });
    }

    @Inject(method = "shouldRender(Lcom/tacz/guns/entity/EntityKineticBullet;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z", at = @At("RETURN"), cancellable = true)
    private void postShouldRender(EntityKineticBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ, CallbackInfoReturnable<Boolean> cir) {
        var result = cir.getReturnValue();
        if (!result && ((IAccessorBullet) bullet).taczexpands$isHook() && bullet.getOwner() == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
