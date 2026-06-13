package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.client.animation.AnimationListener;
import com.tacz.guns.api.client.animation.AnimationListenerSupplier;
import com.tacz.guns.api.client.animation.ObjectAnimationChannel;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.listener.model.ModelRotateListener;
import com.tacz.guns.client.model.listener.model.ModelScaleListener;
import com.tacz.guns.client.model.listener.model.ModelTranslateListener;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.client.accessor.IAccessorBedrockAmmoModel;
import group.taczexpands.client.override.ModelTranslateListenerOverride;
import group.taczexpands.common.accessor.IAccessorBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BedrockAmmoModel.class, remap = false)
public abstract class MixinBedrockAmmoModel extends BedrockModel implements IAccessorBedrockAmmoModel, AnimationListenerSupplier {
    @Unique
    private List<BedrockPart> taczexpands$hookPath = null;

    @Unique
    private Vector3f taczexpands$cachedHookPos = new Vector3f(0.0f, 0.0f, 0.0f);


    @Inject(method = "<init>", at = @At("TAIL"))
    public void postInit(BedrockModelPOJO pojo, BedrockVersion version, CallbackInfo ci) {
        taczexpands$hookPath = getPath(modelMap.get("hook"));
    }

    @Override
    public Vector3f taczexpands$getHookPos() {
        return taczexpands$cachedHookPos;
    }

    @Override
    public void taczexpands$render(EntityKineticBullet entity, PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        var owner = entity.getOwner();
        if (owner instanceof LivingEntity ownerLivingEntity) {
            var bullet = (IAccessorBullet) entity;
            if (bullet.taczexpands$isHook()) {
                var tempPoseStack = new PoseStack();
                tempPoseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(Minecraft.getInstance().getPartialTick(), entity.yRotO, entity.getYRot()) - 180.0F));
                tempPoseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(Minecraft.getInstance().getPartialTick(), entity.xRotO, entity.getXRot())));
                tempPoseStack.translate(0, 1.5, 0);
                tempPoseStack.scale(-1, -1, 1);
                if (taczexpands$hookPath != null) {
                    for (int i = 0; i < taczexpands$hookPath.size(); ++i) {
                        taczexpands$hookPath.get(i).translateAndRotateAndScale(tempPoseStack);
                    }
                }
                var pos = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).mul(tempPoseStack.last().pose());
                taczexpands$cachedHookPos.set(pos.x, pos.y, pos.z);
            }
        }

        this.render(poseStack, transformType, renderType, light, overlay);
    }

    @Override
    public @Nullable AnimationListener supplyListeners(String nodeName, ObjectAnimationChannel.ChannelType type) {
        ModelRendererWrapper model = modelMap.get(nodeName);
        if (model == null) {
            return null;
        }

        if (type.equals(ObjectAnimationChannel.ChannelType.TRANSLATION)) {
            return new ModelTranslateListenerOverride((BedrockAmmoModel) (Object) this, model, nodeName);
        }
        if (type.equals(ObjectAnimationChannel.ChannelType.ROTATION)) {
            return new ModelRotateListener(model);
        }
        if (type.equals(ObjectAnimationChannel.ChannelType.SCALE)) {
            return new ModelScaleListener(model);
        }
        return null;
    }
}
