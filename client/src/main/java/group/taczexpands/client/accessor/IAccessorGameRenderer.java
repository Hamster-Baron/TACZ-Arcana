package group.taczexpands.client.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public interface IAccessorGameRenderer {
    void taczexpands$renderScopeLevel(float partialTicks, long tick, PoseStack stack);
    int taczexpands$getBuffer();
    int taczexpands$getFBO();
    boolean taczexpands$getRenderHand();
    boolean taczexpands$getRenderBlockOutline();
    double taczexpands$getFOV(Camera camera, float partialTicks, boolean b);
}
