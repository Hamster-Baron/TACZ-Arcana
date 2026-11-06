package group.taczexpands.client.accessor;

import net.minecraft.client.Camera;

public interface IAccessorGameRenderer {
    boolean taczexpands$getRenderHand();
    boolean taczexpands$getRenderBlockOutline();
    double taczexpands$getFOV(Camera camera, float partialTicks, boolean b);
}
