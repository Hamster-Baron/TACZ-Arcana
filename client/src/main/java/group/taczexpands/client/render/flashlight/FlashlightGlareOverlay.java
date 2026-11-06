package group.taczexpands.client.render.flashlight;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Local lens glare through the normal Forge HUD event, after world composition. */
public final class FlashlightGlareOverlay {
    private record Flare(float x, float y, float radius, float alpha) {}

    private static final List<Flare> QUEUE = new ArrayList<>(8);
    private static final float[] RADII = {0, .16f, .30f, .48f, .72f, 1};
    private static final float[] OPACITY = {1, 1, .91f, .46f, .12f, 0};
    private static final float[] COS = new float[33], SIN = new float[33];
    private static final float[] EDGE = new float[33];

    static {
        for (int i = 0; i <= 32; i++) {
            double a = i * Math.PI / 16;
            COS[i] = (float) Math.cos(a);
            SIN[i] = (float) Math.sin(a);
            EDGE[i] = (float) (1 + .10 * Math.sin(3 * a + .4) + .055 * Math.cos(5 * a - .8));
        }
        MinecraftForge.EVENT_BUS.addListener(FlashlightGlareOverlay::frame);
        MinecraftForge.EVENT_BUS.addListener(FlashlightGlareOverlay::hud);
    }

    private static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) QUEUE.clear();
    }

    public static void submit(Vector3f center, Matrix4f projection, float radius, float alpha) {
        if (QUEUE.size() >= 8) return;
        Vector4f p = projection.transform(new Vector4f(center, 1));
        if (!Float.isFinite(p.w) || p.w <= 0) return;
        float x = p.x / p.w, y = p.y / p.w, r = radius * Math.abs(projection.m11()) / p.w;
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(r) || r <= 0) return;
        QUEUE.add(new Flare(x, y, r, Math.min(1, alpha)));
    }

    private static void hud(RenderGuiEvent.Post event) {
        if (QUEUE.isEmpty()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.screen != null) return;
            var gui = event.getGuiGraphics();
            int width = gui.guiWidth(), height = gui.guiHeight();
            Matrix4f matrix = gui.pose().last().pose();
            VertexConsumer v = gui.bufferSource().getBuffer(RenderType.guiOverlay());
            for (Flare f : QUEUE) {
                float x = (f.x + 1) * width * .5f, y = (1 - f.y) * height * .5f;
                float r = Math.min(height * .7f, f.radius * height * .5f);
                // Broad veiling bloom, a soft horizontal lens smear, then the overexposed core.
                // Stable low-frequency contours give the glare a softly irregular outline.
                lobe(v, matrix, x - r * .035f, y + r * .025f, r * 1.22f, r * .88f, f.alpha * .32f);
                lobe(v, matrix, x, y, r * 2.1f, r * .16f, f.alpha * .70f);
                lobe(v, matrix, x, y, r * .82f, r * .69f, f.alpha);
            }
            gui.flush();
        } finally {
            QUEUE.clear();
        }
    }

    private static void lobe(VertexConsumer v, Matrix4f m, float x, float y, float rx, float ry, float alpha) {
        for (int ring = 0; ring < RADII.length - 1; ring++) {
            for (int i = 0; i < 32; i++) {
                point(v, m, x, y, rx, ry, ring, i, alpha);
                point(v, m, x, y, rx, ry, ring, i + 1, alpha);
                point(v, m, x, y, rx, ry, ring + 1, i + 1, alpha);
                point(v, m, x, y, rx, ry, ring + 1, i, alpha);
            }
        }
    }

    private static void point(VertexConsumer v, Matrix4f m, float x, float y, float rx, float ry,
            int ring, int i, float alpha) {
        float d = RADII[ring] * EDGE[i];
        vertex(v, m, x + rx * d * COS[i], y + ry * d * SIN[i], alpha * OPACITY[ring]);
    }

    private static void vertex(VertexConsumer v, Matrix4f m, float x, float y, float a) {
        v.vertex(m, x, y, 0).color(1, .985f, .955f, a).endVertex();
    }
}
