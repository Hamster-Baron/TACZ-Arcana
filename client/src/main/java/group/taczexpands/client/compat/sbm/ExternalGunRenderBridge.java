package group.taczexpands.client.compat.sbm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.renderer.item.SbmGunTransforms;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class ExternalGunRenderBridge {
    private ExternalGunRenderBridge() {}

    public static String overrideTexture(ItemStack gun) {
        return GunExtras.INSTANCE.getOverrideTexture(gun);
    }

    public static boolean advancedBeam() {
        return ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get();
    }

    public static void renderLaser(ItemStack gun, ItemStack attachment, PoseStack pose,
            ItemDisplayContext context, List<BedrockPart> path, MultiBufferSource buffers) {
        BeamRendererOverride.renderLaserBeam(gun, attachment, pose, context, path, buffers);
    }

    public static void renderExternalLaser(ItemStack gun, ItemStack attachment, ExternalGunModel model,
            PoseStack pose, MultiBufferSource buffers) {
        var instance = model.displayInstance();
        for (int bone : instance.laserBeamBones()) {
            pose.pushPose();
            SbmGunTransforms.applyBonePivot(pose, instance, bone);
            pose.scale(-1, -1, 1);
            renderLaser(gun, attachment, pose, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, List.of(), buffers);
            pose.popPose();
        }
    }
}
