package group.taczexpands.client.render.flashlight;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import group.taczexpands.client.TACZExpandsClient;
import group.taczexpands.client.compat.RendererPresence;
import group.taczexpands.client.render.Depth;
import group.taczexpands.client.render.surface.SurfaceRenderer;
import group.taczexpands.common.accessor.IAccessorAttachmentData;
import group.taczexpands.common.nbt.GunExtras;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Renders the decorative third-person lamp into the caller's entity buffer. */
public final class FlashlightEmitterRenderer {
    private static final RenderType TYPE =
            RenderType.entityTranslucentEmissive(
                    new ResourceLocation("taczexpands", "textures/effect/flashlight_lamp.png"),
                    false);

    private record Sight(long tick, int x, int y, int z, int cx, int cy, int cz) {}

    private static final WeakReference<?>[] sightLevel = {
        new WeakReference<>(null)
    };

    static {
        MinecraftForge.EVENT_BUS.addListener(
                FlashlightEmitterRenderer::logout);
    }

    private static void logout(
            ClientPlayerNetworkEvent.LoggingOut event) {
        SIGHT.clear();
        sightLevel[0].clear();
        FlashlightLodRenderer.clear();
    }

    private static final Map<Sight, Boolean> SIGHT =
            new LinkedHashMap<>() {
                protected boolean removeEldestEntry(Map.Entry<Sight, Boolean> e) {
                    return size() > 128;
                }
            };
    private static final boolean ENABLED = !Boolean.getBoolean("arcana.flashlight.hideLamp");

    private static boolean allowed(ItemStack gun) {
        if (!GunExtras.INSTANCE.getFlashlight(gun)) return false;
        var flashlight =
                IAccessorAttachmentData.getFlashlight(gun);
        return flashlight != null && flashlight.enable;
    }

    private static boolean secondary() {
        return Depth.INSTANCE.getDepthRendering();
    }

    private static boolean shadow() {
        return RendererPresence.OCULUS_PRESENT
                && IrisApi.getInstance().isRenderingShadowPass();
    }

    public static void render(
            ItemStack gun,
            PoseStack stack,
            ItemDisplayContext context,
            List<BedrockPart> path,
            MultiBufferSource buffers,
            float alpha) {
        if (path == null
                || path.isEmpty()
                || buffers == null
                || alpha <= 0
                || !eligible(gun, context)) return;
        stack.pushPose();
        try {
            // TaCZ explicitly renders hidden scope parents, so use the locator's own
            // visibility for the attachment lamp.
            if (!path.get(path.size() - 1).visible) return;
            for (BedrockPart part : path) part.translateAndRotateAndScale(stack);
            draw(stack, buffers, alpha);
        } finally {
            stack.popPose();
        }
    }

    public static boolean eligible(ItemStack gun, ItemDisplayContext context) {
        if (!ENABLED
                || gun == null
                || (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                || secondary()
                || !allowed(gun)
                || shadow()) return false;
        return true;
    }

    public static void renderCached(
            PoseStack stack, Matrix4f transform, MultiBufferSource buffers, float alpha) {
        stack.pushPose();
        try {
            stack.mulPoseMatrix(transform);
            draw(stack, buffers, alpha);
        } finally {
            stack.popPose();
        }
    }

    private static void draw(PoseStack stack, MultiBufferSource buffers, float alpha) {
        Matrix4f pose = stack.last().pose();
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
        Vector3f center = view.transformPosition(new Vector3f());
        float distance = center.length();
        if (!Float.isFinite(distance) || distance < 0.02f || distance > 64) return;
        Vector3f forward = view.transformDirection(new Vector3f(0, 0, -1));
        float squaredLength = forward.lengthSquared();
        if (!Float.isFinite(squaredLength) || squaredLength < 1.0e-12f) return;
        forward.mul(1.0f / (float) Math.sqrt(squaredLength));
        float facing = -forward.dot(center) / distance;
        float fade = Math.max(0, Math.min(1, (facing - 0.05f) / 0.55f));
        fade = fade * fade * (3 - 2 * fade);
        if (fade < 0.005f) return;
        VertexConsumer out = buffers.getBuffer(TYPE);
        float radius = 0.045f, z = -0.002f;
        vertex(out, stack, -radius, -radius, z, 0, 1, alpha * fade);
        vertex(out, stack, radius, -radius, z, 1, 1, alpha * fade);
        vertex(out, stack, radius, radius, z, 1, 0, alpha * fade);
        vertex(out, stack, -radius, radius, z, 0, 0, alpha * fade);
        float direct = Math.max(0, Math.min(1, (facing - 0.82f) / 0.17f));
        direct = direct * direct * (3 - 2 * direct);
        float range = Math.max(0, Math.min(1, (64 - distance) / 40));
        if (!SurfaceRenderer.isRendering()
                && !TACZExpandsClient.Companion.getPatchMainTargetDraw()
                && direct > 0.01f && range > 0 && visible(center)) {
            float size = Math.min(2.0f, 0.65f + distance * 0.09f);
            FlashlightGlareOverlay.submit(
                    center,
                    RenderSystem.getProjectionMatrix(),
                    size,
                    alpha * direct * range * 1.4f);
        }
    }

    private static boolean visible(Vector3f center) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        var camera = mc.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        Vector3f delta = new Vector3f(-center.x, center.y, -center.z).rotate(camera.rotation());
        Vec3 source = eye.add(delta.x, delta.y, delta.z);
        if (sightLevel[0].get() != mc.level) {
            SIGHT.clear();
            sightLevel[0] = new WeakReference<>(mc.level);
        }
        Sight key =
                new Sight(
                        mc.level.getGameTime(),
                        (int) Math.round(source.x * 32),
                        (int) Math.round(source.y * 32),
                        (int) Math.round(source.z * 32),
                        (int) Math.round(eye.x * 32),
                        (int) Math.round(eye.y * 32),
                        (int) Math.round(eye.z * 32));
        Boolean cached = SIGHT.get(key);
        if (cached != null) return cached;
        var hit =
                mc.level.clip(
                        new ClipContext(
                                eye,
                                source,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                camera.getEntity()));
        boolean clear =
                hit.getType() == HitResult.Type.MISS
                        || hit.getLocation().distanceTo(eye) + 0.015 >= source.distanceTo(eye);
        SIGHT.put(key, clear);
        return clear;
    }

    private static void vertex(
            VertexConsumer out,
            PoseStack stack,
            float x,
            float y,
            float z,
            float u,
            float v,
            float alpha) {
        out.vertex(stack.last().pose(), x, y, z)
                .color(1, 0.97f, 0.9f, alpha)
                .uv(u, v)
                .overlayCoords(655360)
                .uv2(15728880)
                .normal(stack.last().normal(), 0, 0, -1)
                .endVertex();
    }
}