package group.taczexpands.client.render.surface;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPolygon;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;

import org.joml.Vector4f;

/** Changes a selected face's output; TacZ still owns the complete cube/part traversal. */
public final class FaceRenderer {
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new BufferBuilder(1024));

    private FaceRenderer() {}

    /** Returns true for a replaced submission, or false to delegate to the caller. */
    public static boolean drawVertex(
            SurfaceFace face,
            BedrockPolygon polygon,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float alpha,
            float u,
            float v,
            int overlay,
            float normalX,
            float normalY,
            float normalZ) {
        if (face == null || SurfaceRenderer.secondaryPass()) return false;
        SurfaceFeed feed = SurfaceEngine.feed(face.feed);
        if (feed == null) return false;
        if (!SurfaceBoneAnchor.displays(feed)) return false;
        if (feed.seenFrame() != SurfaceEngine.frame() && visible(polygon, pose)) {
            feed.markSeen(SurfaceEngine.frame());
        }
        if (feed.target() == null) return false;
        u = (u - face.minU) * face.scaleU;
        v = (v - face.minV) * face.scaleV;
        if (face.mirror) u = 1 - u;
        for (int turn = 0; turn < face.quarterTurns; turn++) {
            float previousU = u;
            u = v;
            v = 1 - previousU;
        }
        // Framebuffer textures have a bottom-left origin.
        BUFFERS.getBuffer(feed.renderType())
                .vertex(
                        x,
                        y,
                        z,
                        1,
                        1,
                        1,
                        alpha,
                        u,
                        1 - v,
                        overlay,
                        LightTexture.FULL_BRIGHT,
                        normalX,
                        normalY,
                        normalZ);
        return true;
    }

    public static void flush() {
        BUFFERS.endBatch();
    }

    private static boolean visible(BedrockPolygon polygon, PoseStack.Pose pose) {
        int left = 0, right = 0, up = 0, down = 0, behind = 0;
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (var vertex : polygon.vertices) {
            var position =
                    new Vector4f(vertex.pos.x / 16, vertex.pos.y / 16, vertex.pos.z / 16, 1)
                            .mul(pose.pose())
                            .mul(RenderSystem.getModelViewMatrix())
                            .mul(RenderSystem.getProjectionMatrix());
            if (position.w <= 0) behind++;
            if (position.x < -position.w) left++;
            if (position.x > position.w) right++;
            if (position.y < -position.w) down++;
            if (position.y > position.w) up++;
            if (position.w > 0) {
                minX = Math.min(minX, position.x / position.w);
                maxX = Math.max(maxX, position.x / position.w);
                minY = Math.min(minY, position.y / position.w);
                maxY = Math.max(maxY, position.y / position.w);
            }
        }
        int count = polygon.vertices.length;
        var target = Minecraft.getInstance().getMainRenderTarget();
        return behind < count && left < count && right < count && up < count && down < count
                && (behind > 0 || (maxX - minX) * target.width / 2 >= 16
                    || (maxY - minY) * target.height / 2 >= 16);
    }
}
