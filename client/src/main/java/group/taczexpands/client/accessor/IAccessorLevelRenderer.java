package group.taczexpands.client.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import group.taczexpands.client.render.FlashlightData;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

public interface IAccessorLevelRenderer {
    Frustum taczexpands$getCullingFrustum();
    void taczexpands$setCullingFrustum(Frustum frustum);
    Frustum taczexpands$getCapturedFrustum();
    void taczexpands$setCapturedFrustum(Frustum frustum);
    boolean taczexpands$getCaptureFrustum();
    void taczexpands$setCaptureFrustum(boolean capture);
    void taczexpands$renderDepth(FlashlightData flashlight, PoseStack pPoseStack, float pPartialTick, Camera pCamera, Matrix4f pProjectionMatrix);
}
