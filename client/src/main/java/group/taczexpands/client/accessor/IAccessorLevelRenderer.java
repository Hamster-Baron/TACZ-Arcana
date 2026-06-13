package group.taczexpands.client.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import group.taczexpands.client.render.FlashlightData;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;

public interface IAccessorLevelRenderer {
    void taczexpands$renderDepth(FlashlightData flashlight, PoseStack pPoseStack, float pPartialTick, Camera pCamera, Matrix4f pProjectionMatrix);
}
