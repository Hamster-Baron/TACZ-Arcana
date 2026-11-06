package group.taczexpands.client.render.surface;

import com.tacz.guns.client.model.bedrock.BedrockPolygon;

/** Immutable UV mapping prepared at model load, independent of the feed's GPU lifetime. */
public final class SurfaceFace {
    final String feed;
    final float minU, minV, scaleU, scaleV;
    final boolean mirror;
    final int quarterTurns;

    public SurfaceFace(SurfaceConfig.Face definition, BedrockPolygon polygon) {
        feed = definition.feed;
        mirror = definition.mirror;
        quarterTurns = Math.floorMod(definition.rotation / 90, 4);
        float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
        for (var vertex : polygon.vertices) {
            left = Math.min(left, vertex.u);
            top = Math.min(top, vertex.v);
            right = Math.max(right, vertex.u);
            bottom = Math.max(bottom, vertex.v);
        }
        minU = left;
        minV = top;
        scaleU = right == left ? 0 : 1 / (right - left);
        scaleV = bottom == top ? 0 : 1 / (bottom - top);
    }
}
