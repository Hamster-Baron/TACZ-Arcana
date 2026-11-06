package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockCubeBox;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPolygon;

import group.taczexpands.client.accessor.IAccessorSurfaceCube;
import group.taczexpands.client.render.surface.FaceRenderer;
import group.taczexpands.client.render.surface.SurfaceConfig;
import group.taczexpands.client.render.surface.SurfaceFace;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Substitutes the bound vertex output inside the compile methods. */
@Mixin(
        value = {BedrockCubeBox.class, BedrockCubePerFace.class},
        remap = false)
public class MixinSurfaceCube implements IAccessorSurfaceCube {
    @Shadow(remap = false)
    @Final
    private BedrockPolygon[] polygons;

    @Unique private Map<BedrockPolygon, SurfaceFace> taczexpands$surfaceFaces;

    @Override
    public void taczexpands$bindSurfaceFaces(List<SurfaceConfig.Face> faces) {
        if (faces.isEmpty()) {
            taczexpands$surfaceFaces = null;
            return;
        }
        var bindings = new IdentityHashMap<BedrockPolygon, SurfaceFace>();
        for (var definition : faces) {
            var polygon = polygons[SurfaceConfig.faceIndex(definition.face)];
            bindings.put(polygon, new SurfaceFace(definition, polygon));
        }
        taczexpands$surfaceFaces = bindings;
    }

    @WrapWithCondition(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            remap = true,
                            target =
                                    "Lcom/mojang/blaze3d/vertex/VertexConsumer;vertex(FFFFFFFFFIIFFF)V"))
    private boolean taczexpands$surfaceVertex(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int overlay,
            int light,
            float normalX,
            float normalY,
            float normalZ,
            @Local(argsOnly = true) PoseStack.Pose pose,
            @Local BedrockPolygon polygon) {
        if (taczexpands$surfaceFaces == null) return true;
        return !FaceRenderer.drawVertex(
                taczexpands$surfaceFaces.get(polygon),
                polygon,
                pose,
                x,
                y,
                z,
                alpha,
                u,
                v,
                overlay,
                normalX,
                normalY,
                normalZ);
    }

    @Inject(method = "compile", at = @At("TAIL"))
    private void taczexpands$flushSurfaces(CallbackInfo ci) {
        if (taczexpands$surfaceFaces != null) FaceRenderer.flush();
    }
}
