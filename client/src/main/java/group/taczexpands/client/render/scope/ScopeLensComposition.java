package group.taczexpands.client.render.scope;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import group.taczexpands.client.TACZExpandsClient;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Runs the upstream compositor after all hand layers have finished. */
public final class ScopeLensComposition {
    private static boolean composing;
    private static RenderType divisionType;
    private static boolean trackingDraw;
    private static final Function<RenderType, RenderType> TRACKING_TYPE = Util.memoize(base -> new RenderType(
            "arcana_scope_tracking", base.format(), base.mode(), base.bufferSize(), false, false,
            () -> {
                base.setupRenderState();
                trackingDraw = true;
            },
            () -> {
                trackingDraw = false;
                base.clearRenderState();
            }) {});

    private ScopeLensComposition() {}

    public static boolean isComposing() {
        return composing;
    }

    public static RenderType divisionType(RenderType type) {
        RenderType previous = divisionType;
        divisionType = type;
        return previous;
    }

    public static VertexConsumer trackingConsumer(VertexConsumer original, MultiBufferSource buffers) {
        if (divisionType == null || buffers == null) return original;
        return buffers.getBuffer(TRACKING_TYPE.apply(divisionType));
    }

    public static boolean isTrackingDraw() {
        return trackingDraw;
    }

    public static boolean preserveThermalMask() {
        return !TACZExpandsClient.Companion.getPatchMainTargetDraw()
                && ScopeVisibility.gate(TACZExpandsClient.Companion.isAdvancedRendering());
    }

    public static boolean usesArcanaVision() {
        return ScopeVisibility.gate(TACZExpandsClient.Companion.isAdvancedRendering())
                && (TACZExpandsClient.Companion.shouldUseThermalImaging(true)
                        || TACZExpandsClient.Companion.shouldUseNightVision(true));
    }

    public static void finish() {
        composing = true;
        try {
            ScopeLensRenderer.render();
        } finally {
            composing = false;
        }
    }
}
