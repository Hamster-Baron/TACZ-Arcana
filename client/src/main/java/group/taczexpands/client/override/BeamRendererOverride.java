package group.taczexpands.client.override;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.List;

public class BeamRendererOverride {
    public static boolean hasLaser = false;
    public static boolean lastHasLaser = false;
    public static Matrix4f lastProjectionMatrix = new Matrix4f();
    public static Matrix4f lastViewMatrix = new Matrix4f();

    public static final ResourceLocation LASER_BEAM_TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/entity/beam.png");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    public static void renderLaserBeam(ItemStack gunItem, ItemStack renderItem, PoseStack poseStack, ItemDisplayContext transformType, @Nonnull List<BedrockPart> path) {
        var stack = renderItem;
        if (stack == null || !transformType.firstPerson() && !(transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }

        hasLaser = true;

        if (transformType.firstPerson() && CompatHelper.INSTANCE.hasIris() && IrisCompat.INSTANCE.hasShaderPackInUse()) {
            if (RefitTransform.getOpeningProgress() > 0.0f && !IrisCompat.INSTANCE.isHandRendererActive()) {
                return;
            } else if (RefitTransform.getOpeningProgress() <= 0.0f && IrisCompat.INSTANCE.isHandRendererActive()) {
                return;
            }
        }

        if (!GunExtras.INSTANCE.getLaser(gunItem)) return;

        var laserConfig = getLaserConfig(stack);
        float maxLen = transformType.firstPerson() ? laserConfig.getLength() : laserConfig.getLengthThird();
        float width = transformType.firstPerson() ? laserConfig.getWidth() : laserConfig.getWidthThird();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(BeamRenderer.LaserBeamRenderState.getLaserBeam());
        poseStack.pushPose();
        {
            for (int i = 0; i < path.size(); ++i) {
                path.get(i).translateAndRotateAndScale(poseStack);
            }

            Float actualLen = transformType.firstPerson() ? getHitDistance(maxLen, poseStack) : null;

            int color = LaserColorUtil.getLaserColor(stack, laserConfig);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            stringVertex(actualLen == null ? -maxLen : -actualLen, width, builder, poseStack.last(), r, g, b, RenderConfig.ENABLE_LASER_FADE_OUT.get());

            if (actualLen != null) {
                float dotZ = -actualLen + 0.01f;
                drawLaserDot(dotZ, width, builder, poseStack.last(), r, g, b);
            }
        }
        poseStack.popPose();
    }

    private static void drawLaserDot(float z, float width, VertexConsumer builder, PoseStack.Pose last, int r, int g, int b) {
        float dotSize = width * 2.5f;
        int light = LightTexture.pack(15, 15);

        drawQuad(z, dotSize, builder, last, r, g, b, 200, light);

        drawQuad(z + 0.005f, dotSize * 0.4f, builder, last, r, g, b, 255, light);
    }

    private static void drawQuad(float z, float size, VertexConsumer builder, PoseStack.Pose last, int r, int g, int b, int a, int light) {
        float h = size / 2f;
        builder.vertex(last.pose(), -h, -h, z).color(r, g, b, a).uv(0, 0).uv2(light).endVertex();
        builder.vertex(last.pose(), -h, h, z).color(r, g, b, a).uv(0, 1).uv2(light).endVertex();
        builder.vertex(last.pose(), h, h, z).color(r, g, b, a).uv(1, 1).uv2(light).endVertex();
        builder.vertex(last.pose(), h, -h, z).color(r, g, b, a).uv(1, 0).uv2(light).endVertex();
    }

    private static Float getHitDistance(float maxDistance, PoseStack poseStack) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        var viewMat = poseStack.last().pose();
        var projMat = RenderSystem.getProjectionMatrix();

        var invProjMat = new Matrix4f(lastProjectionMatrix).invert();
        var invViewMat = new Matrix4f(lastViewMatrix).invert();

        var cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        var origin4f = new Vector4f(0, 0, 0, 1);
        viewMat.transform(origin4f);
        projMat.transformProject(origin4f);
        invProjMat.transformProject(origin4f);
        invViewMat.transform(origin4f);
        var startPos = new Vec3(origin4f.x() + cameraPos.x, origin4f.y() + cameraPos.y, origin4f.z() + cameraPos.z);

        var target4f = new Vector4f(0, 0, -1, 1);
        viewMat.transform(target4f);
        projMat.transformProject(target4f);
        invProjMat.transformProject(target4f);
        invViewMat.transform(target4f);
        var targetWorldPos = new Vec3(target4f.x() + cameraPos.x, target4f.y() + cameraPos.y, target4f.z() + cameraPos.z);

        var dirVec = targetWorldPos.subtract(startPos).normalize();
        var endPos = startPos.add(dirVec.scale(maxDistance));

        var hit = mc.level.clip(new ClipContext(
                startPos,
                endPos,
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() == HitResult.Type.MISS) return null;

        return (float) hit.getLocation().distanceTo(startPos);
    }

    private static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack == null) {
            return DEFAULT_LASER_CONFIG;
        }

        if (stack.getItem() instanceof IAttachment iAttachment) {
            return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        return DEFAULT_LASER_CONFIG;
    }

    private static void stringVertex(float z, float width, VertexConsumer pConsumer, PoseStack.Pose pPose, int r, int g, int b, boolean fadeOut) {
        float halfWidth = width / 2;
        int endAlpha = fadeOut ? 0 : 255;
        int light = LightTexture.pack(15, 15);
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, 0).color(r, g, b, 255).uv(0, 0).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();

        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, 0).color(r, g, b, 255).uv(0, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), -halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 1).uv2(light).endVertex();
        pConsumer.vertex(pPose.pose(), halfWidth, -halfWidth, z).color(r, g, b, endAlpha).uv(1, 0).uv2(light).endVertex();
    }

    public static void reset() {
        lastHasLaser = hasLaser;
        hasLaser = false;
    }
}